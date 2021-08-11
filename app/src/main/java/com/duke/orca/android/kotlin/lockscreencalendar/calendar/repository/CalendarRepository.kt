@file:Suppress("LocalVariableName", "ObjectPropertyName")

package com.duke.orca.android.kotlin.lockscreencalendar.calendar.repository

import android.Manifest.permission.READ_CALENDAR
import android.Manifest.permission.WRITE_CALENDAR
import android.content.AsyncQueryHandler
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.provider.CalendarContract
import android.util.AttributeSet
import androidx.activity.result.ActivityResultLauncher
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.duke.orca.android.kotlin.lockscreencalendar.BLANK
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.DAYS_PER_MONTH
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapters.CalendarItem
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.Model
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.getFirstDayOfWeekOfMonth
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.toDayOfMonth
import com.duke.orca.android.kotlin.lockscreencalendar.permission.PermissionChecker
import java.lang.ref.WeakReference
import java.util.*

class CalendarRepository(private val applicationContext: Context) {
    private val calendar = Calendar.getInstance()
    private val contentResolver = applicationContext.contentResolver
    private val calendarAsyncQueryHandler = CalendarAsyncQueryHandler(WeakReference(this))
    private val hashMap = hashMapOf<Int, MutableLiveData<Array<CalendarItem?>>>()
    private val offset = 6
    private val permissions = listOf(READ_CALENDAR, WRITE_CALENDAR)

    fun get(year: Int, month: Int): LiveData<Array<CalendarItem?>>? {
        return hashMap[(year * 100) + month]
    }

    init {
        for (i in -offset..offset) {
            hashMap[i] = MutableLiveData()
        }
    }

    fun load() {
        for (i in -offset..offset) {
            Calendar.getInstance().apply {
                add(Calendar.MONTH, i)
            }.also {
                val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()

                val year = it.get(Calendar.YEAR)
                val month = it.get(Calendar.MONTH)
                val date = it.getActualMaximum(Calendar.DAY_OF_MONTH)
                val token = (year * 100) + month

                val previousMonthCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    add(Calendar.MONTH, -1)
                }

                val nextMonthCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    add(Calendar.MONTH, 1)
                }

                val firstDayOfWeekOfMonth = getFirstDayOfWeekOfMonth(year, month)
                val lastDayOfMonth = it.getActualMaximum(Calendar.DAY_OF_MONTH)
                val lastDayOfPreviousMonth = previousMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

                val DTSTART = if (firstDayOfWeekOfMonth == Calendar.SUNDAY) {
                    it.apply { set(Calendar.DAY_OF_MONTH, 1) }
                } else {
                    previousMonthCalendar.apply {
                        set(Calendar.DAY_OF_MONTH, lastDayOfPreviousMonth - firstDayOfWeekOfMonth.dec())
                    }
                }

                val DTEND = nextMonthCalendar.apply {
                    set(Calendar.DAY_OF_MONTH, DAYS_PER_MONTH - firstDayOfWeekOfMonth - lastDayOfMonth)
                }

                ContentUris.appendId(builder, DTSTART.timeInMillis)
                ContentUris.appendId(builder, DTEND.timeInMillis)

                calendarAsyncQueryHandler.startQuery(
                    token,
                    null,
                    builder.build(),
                    Instances.projections,
                    null,
                    null,
                    CalendarContract.Instances.DTSTART + " ASC"
                )
            }
        }
    }

    class CalendarAsyncQueryHandler(private val weakReference: WeakReference<CalendarRepository>)
        : AsyncQueryHandler(weakReference.get()?.contentResolver) {

        override fun onQueryComplete(token: Int, cookie: Any?, cursor: Cursor?) {
            super.onQueryComplete(token, cookie, cursor)

            val calendarItems = arrayOfNulls<CalendarItem>(DAYS_PER_MONTH)
            val instances = arrayListOf<Model.Instance>()
            val month = token % 100
            val year = token / 100

            val calendar = Calendar.getInstance().apply {
                set(Calendar.MONTH, month)
                set(Calendar.YEAR, year)
            }

            val previousMonthCalendar = Calendar.getInstance().apply {
                set(Calendar.MONTH, calendar.get(Calendar.MONTH))
                set(Calendar.YEAR, calendar.get(Calendar.YEAR))
                add(Calendar.MONTH, -1)
            }

            val nextMonthCalendar = Calendar.getInstance().apply {
                set(Calendar.MONTH, calendar.get(Calendar.MONTH))
                set(Calendar.YEAR, calendar.get(Calendar.YEAR))
                add(Calendar.MONTH, 1)
            }

            val indexOfFirstDayOfMonth = getFirstDayOfWeekOfMonth(year, month).dec()
            val indexOfLastDayOfMonth = indexOfFirstDayOfMonth + calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val lastDayOfPreviousMonth = previousMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

            addDaysOfPreviousMonth(calendarItems, lastDayOfPreviousMonth, indexOfFirstDayOfMonth)
            addDaysOfMonth(calendarItems, indexOfFirstDayOfMonth, indexOfLastDayOfMonth)
            addDaysOfNextMonth(calendarItems, indexOfLastDayOfMonth)

            cursor?.let { cursor ->
                while (cursor.moveToNext()) {
                    val _id = cursor.getLongOrNull(Instances.Index._ID) ?: 0L
                    val allDay = cursor.getIntOrNull(Instances.Index.ALL_DAY) ?: 0
                    val begin = cursor.getLongOrNull(Instances.Index.BEGIN) ?: 0L
                    val calendarColor = cursor.getIntOrNull(Instances.Index.CALENDAR_COLOR) ?: Color.TRANSPARENT
                    val calendarDisplayName = cursor.getStringOrNull(Instances.Index.CALENDAR_DISPLAY_NAME) ?: BLANK
                    val calendarId = cursor.getLongOrNull(Instances.Index.CALENDAR_ID) ?: continue
                    val end = cursor.getLongOrNull(Instances.Index.END) ?: 0L
                    val eventId = cursor.getLongOrNull(Instances.Index.EVENT_ID) ?: continue
                    val title = cursor.getStringOrNull(Instances.Index.TITLE) ?: BLANK

                    val beginDayOfMonth = begin.toDayOfMonth()
                    val endDayOfMonth = end.toDayOfMonth()
                    val period = endDayOfMonth - beginDayOfMonth

                    instances.add(Model.Instance(
                        allDay = allDay == 1,
                        begin = begin,
                        beginDayOfMonth = beginDayOfMonth,
                        calendarColor = calendarColor,
                        calendarDisplayName = calendarDisplayName,
                        calendarId = calendarId,
                        end = end,
                        endDayOfMonth = endDayOfMonth,
                        eventId = eventId,
                        id = _id,
                        period = period,
                        title = title
                    ))
                }

                instances.forEach { instance ->
                    val beginDayOfMonth = instance.beginDayOfMonth
                    val endDayOfMonth = instance.endDayOfMonth

                    calendarItems[beginDayOfMonth]?.instances?.add(instance)
                }

                weakReference.get()?.hashMap?.let { hashMap ->
                    hashMap[token]?.let {
                        it.value = calendarItems
                    } ?: let {
                        weakReference.get()?.hashMap?.set(token, MutableLiveData(calendarItems))
                    }
                }

                cursor.close()
            }
        }

        private fun addDaysOfPreviousMonth(calendarItems: Array<CalendarItem?>, lastDayOfPreviousMonth: Int, indexOfFirstDayOfMonth: Int) {
            if (indexOfFirstDayOfMonth == 0) {
                return
            }

            val from = lastDayOfPreviousMonth - indexOfFirstDayOfMonth.dec()

            for ((i, j) in (from .. lastDayOfPreviousMonth).withIndex()) {
                calendarItems[i] = CalendarItem.DayOfPreviousMonth(j, position = i)
            }
        }

        private fun addDaysOfMonth(calendarItems: Array<CalendarItem?>, indexOfFirstDayOfMonth: Int, indexOfLastDayOfMonth: Int) {
            for ((i, j) in (indexOfFirstDayOfMonth until indexOfLastDayOfMonth).withIndex()) {
                calendarItems[j] = CalendarItem.DayOfMonth(i.inc(), position = j)
            }
        }

        private fun addDaysOfNextMonth(calendarItems: Array<CalendarItem?>, indexOfLastDayOfMonth: Int) {
            for ((i, j) in (indexOfLastDayOfMonth until DAYS_PER_MONTH).withIndex()) {
                calendarItems[j] = CalendarItem.DayOfNextMonth(i.inc(), position = j)
            }
        }
    }

    private object Calendars {
        val projections: Array<String> = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.OWNER_ACCOUNT,
        )

        object Index {
            const val _ID = 0
            const val ACCOUNT_NAME = 1
            const val ACCOUNT_TYPE = 2
            const val CALENDAR_ACCESS_LEVEL = 3
            const val CALENDAR_DISPLAY_NAME = 4
            const val IS_PRIMARY = 5
            const val OWNER_ACCOUNT = 6
        }
    }

    private object Instances {
        val projections: Array<String> = arrayOf(
            CalendarContract.Instances._ID,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.CALENDAR_COLOR,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.START_DAY
        )

        object Index {
            const val _ID = 0
            const val ALL_DAY = 1
            const val BEGIN = 2
            const val CALENDAR_COLOR = 3
            const val CALENDAR_DISPLAY_NAME = 4
            const val CALENDAR_ID = 5
            const val END = 6
            const val EVENT_ID = 7
            const val TITLE = 8
            const val START_DAY = 9
        }
    }

    private fun permissionsGranted(): Boolean {
        return PermissionChecker.checkPermissions(applicationContext, permissions)
    }

    fun calendars(): List<Model.Calendar> {
        if (permissionsGranted().not())
            return emptyList()

        val calendars = mutableListOf<Model.Calendar>()
        val contentUri = CalendarContract.Calendars.CONTENT_URI
        val cursor = contentResolver.query(
            contentUri,
            Calendars.projections,
            null,
            null,
            null
        )

        cursor ?: return emptyList()
        cursor.moveToFirst()

        while (cursor.moveToNext()) {
            val _id = cursor.getLong(Calendars.Index._ID)
            val calendarDisplayName = cursor.getString(Calendars.Index.CALENDAR_DISPLAY_NAME)

            calendars.add(Model.Calendar(_id, calendarDisplayName))
        }

        cursor.close()

        return calendars
    }

    fun instances(DTSTART: Calendar, DTEND: Calendar): List<Model.Instance> {
        val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        val instances = mutableListOf<Model.Instance>()
//        val string = calendars().map { it.id }.joinToString(separator = ", ") { "\"$it\"" }
//        val selection = "(${CalendarContract.Instances.CALENDAR_ID} IN ($string))"

        ContentUris.appendId(builder, DTSTART.timeInMillis)
        ContentUris.appendId(builder, DTEND.timeInMillis)

        if (permissionsGranted().not())
            return emptyList()

        val cursor = contentResolver.query(
            builder.build(),
            Instances.projections,
            null,
            null,
            CalendarContract.Instances.DTSTART + " ASC"
        ) ?: return emptyList()

        while (cursor.moveToNext()) {
            val _id = cursor.getLongOrNull(Instances.Index._ID) ?: 0L
            val allDay = cursor.getIntOrNull(Instances.Index.ALL_DAY) ?: 0
            val begin = cursor.getLongOrNull(Instances.Index.BEGIN) ?: 0L
            val calendarColor = cursor.getIntOrNull(Instances.Index.CALENDAR_COLOR) ?: Color.TRANSPARENT
            val calendarDisplayName = cursor.getStringOrNull(Instances.Index.CALENDAR_DISPLAY_NAME) ?: BLANK
            val calendarId = cursor.getLongOrNull(Instances.Index.CALENDAR_ID) ?: continue
            val end = cursor.getLongOrNull(Instances.Index.END) ?: 0L
            val eventId = cursor.getLongOrNull(Instances.Index.EVENT_ID) ?: continue
            val title = cursor.getStringOrNull(Instances.Index.TITLE) ?: BLANK

            val beginDayOfMonth = begin.toDayOfMonth()
            val endDayOfMonth = end.toDayOfMonth()
            val period = endDayOfMonth - beginDayOfMonth

            if (allDay == 1) {
                val timeInMillis = GregorianCalendar.getInstance().apply {
                    timeInMillis = DTSTART.timeInMillis
                    add(GregorianCalendar.DATE, -1)
                }.timeInMillis

                if (timeInMillis <= begin && begin <= DTSTART.timeInMillis) {
                    if (DTSTART.timeInMillis <= end && end <= DTEND.timeInMillis)
                        continue
                }
            }

            instances.add(Model.Instance(
                allDay = allDay == 1,
                begin = begin,
                beginDayOfMonth = beginDayOfMonth,
                calendarColor = calendarColor,
                calendarDisplayName = calendarDisplayName,
                calendarId = calendarId,
                end = end,
                endDayOfMonth = endDayOfMonth,
                eventId = eventId,
                id = _id,
                period = period,
                title = title
            ))
        }

        cursor.close()

        return instances
    }

    companion object {
        fun edit(activityResultLauncher: ActivityResultLauncher<Model.Instance>, instance: Model.Instance) {
            activityResultLauncher.launch(instance)
        }

        fun insert(activityResultLauncher: ActivityResultLauncher<Model.Instance>) {
            activityResultLauncher.launch(null)
        }
    }
}