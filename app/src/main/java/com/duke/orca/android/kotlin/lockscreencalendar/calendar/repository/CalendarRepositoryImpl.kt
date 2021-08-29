@file:Suppress("LocalVariableName", "ObjectPropertyName")

package com.duke.orca.android.kotlin.lockscreencalendar.calendar.repository

import android.Manifest.permission.READ_CALENDAR
import android.Manifest.permission.WRITE_CALENDAR
import android.content.AsyncQueryHandler
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.provider.CalendarContract
import androidx.annotation.MainThread
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.duke.orca.android.kotlin.lockscreencalendar.BLANK
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.DAYS_PER_MONTH
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.DAYS_PER_WEEK
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.VISIBLE_INSTANCE_COUNT
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.CalendarItem
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.Model
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.getFirstDayOfWeekOfMonth
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.toDayOfMonth
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.toMonth
import com.duke.orca.android.kotlin.lockscreencalendar.permission.PermissionChecker
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlinx.coroutines.*

class CalendarRepositoryImpl(private val applicationContext: Context) {
    private val contentResolver = applicationContext.contentResolver

    private val calendarAsyncQueryHandler = CalendarAsyncQueryHandler(WeakReference(this))
    private val linkedMap = createLinkedMap()
    private val permissions = listOf(READ_CALENDAR, WRITE_CALENDAR)

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

    private object Events {
        val projections: Array<String> = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.DTSTART
        )

        object Index {
            const val _ID = 0
            const val DTSTART = 1
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
            CalendarContract.Instances.END_DAY,
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.START_DAY,
            CalendarContract.Instances.TITLE,
        )

        object Index {
            const val _ID = 0
            const val ALL_DAY = 1
            const val BEGIN = 2
            const val CALENDAR_COLOR = 3
            const val CALENDAR_DISPLAY_NAME = 4
            const val CALENDAR_ID = 5
            const val END = 6
            const val END_DAY = 7
            const val EVENT_ID = 8
            const val START_DAY = 9
            const val TITLE = 10
        }
    }

    fun initialLoad() {
        (-OFFSET..OFFSET).forEach {
            Calendar.getInstance().apply {
                add(Calendar.MONTH, it)
            }.also {
                put(it.get(Calendar.YEAR), it.get(Calendar.MONTH))
            }
        }
    }

    fun get(year: Int, month: Int): LiveData<Model.Calendar>? {
        return linkedMap[getKey(year, month)]
    }

    fun getInstances(year: Int, month: Int, date: Int): List<Model.Instance> {
        val DTSTART = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DATE, date)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }.timeInMillis

        val DTEND = Calendar.getInstance().apply {
            timeInMillis = DTSTART
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }.timeInMillis

        val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        val instances = mutableListOf<Model.Instance>()
//        val string = calendars().map { it.id }.joinToString(separator = ", ") { "\"$it\"" }
//         val selection = "(${CalendarContract.Instances.CALENDAR_ID} IN ($string))"

        ContentUris.appendId(builder, DTSTART)
        ContentUris.appendId(builder, DTEND)

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
            val duration = endDayOfMonth - beginDayOfMonth
            val fillBackground = (allDay == 1) || ((endDayOfMonth - beginDayOfMonth) > 0)

            if (allDay == 1) {
                val timeInMillis = GregorianCalendar.getInstance().apply {
                    timeInMillis = DTSTART
                    add(GregorianCalendar.DATE, -1)
                }.timeInMillis

                if (begin in timeInMillis..DTSTART) {
                    if (end in DTSTART..DTEND) continue
                }
            }

            instances.add(Model.Instance(
                isAllDay = allDay == 1,
                begin = begin,
                calendarColor = calendarColor,
                calendarDisplayName = calendarDisplayName,
                calendarId = calendarId,
                end = end,
                eventId = eventId,
                id = _id,
                month = begin.toMonth(),
                title = title,
                beginDayOfMonth = beginDayOfMonth,
                endDayOfMonth = endDayOfMonth,
                duration = duration,
                fillBackgroundColor = fillBackground
            ))
        }

        cursor.close()

        return instances
    }

    fun put(year: Int, month: Int) {
        val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        val token = getKey(year, month)

        val currentCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
        }

        val previousCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            add(Calendar.MONTH, -1)
        }

        val nextCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            add(Calendar.MONTH, 1)
        }

        val indexOfFirstDayOfMonth = getFirstDayOfWeekOfMonth(year, month).dec()
        val lastDayOfMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val lastDayOfPreviousMonth = previousCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val DTSTART = if (indexOfFirstDayOfMonth == 0) {
            currentCalendar.apply { set(Calendar.DAY_OF_MONTH, 1) }
        } else {
            previousCalendar.apply {
                set(Calendar.DAY_OF_MONTH, lastDayOfPreviousMonth - indexOfFirstDayOfMonth)
            }
        }

        val DTEND = nextCalendar.apply {
            set(Calendar.DAY_OF_MONTH, DAYS_PER_MONTH - indexOfFirstDayOfMonth - lastDayOfMonth)
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
            CalendarContract.Instances.START_DAY + " DESC, " +
                    CalendarContract.Instances.END_DAY + " DESC, " +
                    CalendarContract.Instances.BEGIN + " ASC, " +
                    CalendarContract.Instances.TITLE + " ASC"

        )
    }

    fun put(year: Int, month: Int, @MainThread onPut: (liveData: LiveData<Model.Calendar>?) -> Unit) {
        linkedMap[getKey(year, month)] = MutableLiveData()
        put(year, month)
        onPut.invoke(linkedMap[getKey(year, month)])
    }

    fun remove(year: Int, month: Int) {
        linkedMap.remove(getKey(year, month))
    }

    class CalendarAsyncQueryHandler(private val weakReference: WeakReference<CalendarRepositoryImpl>)
        : AsyncQueryHandler(weakReference.get()?.contentResolver) {
        private val job = Job()
        private val coroutineScope = CoroutineScope(Dispatchers.IO + job)

        override fun onQueryComplete(token: Int, cookie: Any?, cursor: Cursor?) {
            super.onQueryComplete(token, cookie, cursor)
            put(token, cursor ?: return)
        }

        private fun put(token: Int, cursor: Cursor) {
            coroutineScope.launch {
                val calendarItems = arrayOfNulls<CalendarItem>(DAYS_PER_MONTH)
                val instances = arrayListOf<Model.Instance>()

                val year = token / KEY
                val month = token % KEY

                val currentCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                }

                val nextCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentCalendar.get(Calendar.YEAR))
                    set(Calendar.MONTH, currentCalendar.get(Calendar.MONTH))
                    add(Calendar.MONTH, 1)
                }

                val previousCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentCalendar.get(Calendar.YEAR))
                    set(Calendar.MONTH, currentCalendar.get(Calendar.MONTH))
                    add(Calendar.MONTH, -1)
                }

                val nextYear = nextCalendar.get(Calendar.YEAR)
                val nextMonth = nextCalendar.get(Calendar.MONTH)

                val previousYear = previousCalendar.get(Calendar.YEAR)
                val previousMonth = previousCalendar.get(Calendar.MONTH)

                val indexOfFirstDayOfMonth = getFirstDayOfWeekOfMonth(year, month).dec()
                val indexOfLastDayOfMonth = indexOfFirstDayOfMonth + currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                val lastDayOfPreviousMonth = previousCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                val firstDayOfPreviousMonth = lastDayOfPreviousMonth - indexOfFirstDayOfMonth

                addDaysOfMonth(calendarItems, indexOfFirstDayOfMonth, indexOfLastDayOfMonth, year, month)
                addDaysOfNextMonth(calendarItems, indexOfLastDayOfMonth, nextYear, nextMonth)
                addDaysOfPreviousMonth(calendarItems, lastDayOfPreviousMonth, indexOfFirstDayOfMonth, previousYear, previousMonth)

                while (cursor.moveToNext()) {
                    if (this.isActive.not()) return@launch

                    val _id = cursor.getLongOrNull(Instances.Index._ID) ?: 0L
                    val allDay = cursor.getIntOrNull(Instances.Index.ALL_DAY) ?: 0
                    val begin = cursor.getLongOrNull(Instances.Index.BEGIN) ?: 0L
                    val calendarColor = cursor.getIntOrNull(Instances.Index.CALENDAR_COLOR) ?: Color.TRANSPARENT
                    val calendarDisplayName = cursor.getStringOrNull(Instances.Index.CALENDAR_DISPLAY_NAME) ?: BLANK
                    val calendarId = cursor.getLongOrNull(Instances.Index.CALENDAR_ID) ?: continue
                    val end = cursor.getLongOrNull(Instances.Index.END) ?: 0L
                    val endDay = cursor.getIntOrNull(Instances.Index.END_DAY) ?: 0
                    val eventId = cursor.getLongOrNull(Instances.Index.EVENT_ID) ?: continue
                    val startDay = cursor.getIntOrNull(Instances.Index.START_DAY) ?: 0
                    val title = cursor.getStringOrNull(Instances.Index.TITLE) ?: BLANK

                    val beginDayOfMonth = begin.toDayOfMonth()
                    val endDayOfMonth = end.toDayOfMonth()
                    val fillBackground = allDay == 1 || (endDay - startDay > 0)

                    instances.add(
                        Model.Instance(
                            isAllDay = allDay == 1,
                            begin = begin,
                            calendarColor = calendarColor,
                            calendarDisplayName = calendarDisplayName,
                            calendarId = calendarId,
                            end = end,
                            eventId = eventId,
                            id = _id,
                            month = begin.toMonth(),
                            title = title,
                            beginDayOfMonth = beginDayOfMonth,
                            endDayOfMonth = endDayOfMonth,
                            duration = endDay - startDay,
                            fillBackgroundColor = fillBackground
                        ))
                }

                instances.sortWith(Comparator { o1, o2 ->
                    return@Comparator when {
                        o1.beginDayOfMonth > o2.beginDayOfMonth -> 1
                        o1.beginDayOfMonth < o2.beginDayOfMonth -> -1
                        else -> 0
                    }
                })

                instances.forEach { instance ->
                    val beginDayOfMonth = instance.beginDayOfMonth
                    var endDayOfMonth = instance.endDayOfMonth

                    val index = when(instance.month) {
                        previousMonth -> beginDayOfMonth.dec() - firstDayOfPreviousMonth
                        nextMonth -> beginDayOfMonth + indexOfLastDayOfMonth.dec()
                        else -> beginDayOfMonth.dec() + indexOfFirstDayOfMonth
                    }

                    if (index in 0 until DAYS_PER_MONTH) {
                        calendarItems[index]?.instances?.add(instance)

                        //val k = calendarItems[index]?.instances?.indexOf(instance) ?: -1

                        /*if (k in 0 until VISIBLE_INSTANCE_COUNT)*/
                            calendarItems[index]?.visibleInstances?.let { visibleInstances ->
                                for (i in 0 until VISIBLE_INSTANCE_COUNT) {
                                    if (visibleInstances[i] == null) {
                                        visibleInstances[i] = instance
                                        break
                                    }
                                }
                            }

                            if (instance.duration > 0) {
                                for (i in 1..instance.duration) {
                                    val j = index + i

                                    if (j in 0 until DAYS_PER_MONTH) {
                                        if (instance.isAllDay.not()) {
                                            instance.deepCopy().apply {
                                                this.beginDayOfMonth = beginDayOfMonth + i
                                                this.duration = endDayOfMonth - (beginDayOfMonth + i)
                                                isVisible = (j % DAYS_PER_WEEK) == Calendar.SUNDAY.dec()
                                            }.also {
                                                calendarItems[j]?.instances?.add(it)
                                                val k = calendarItems[index]?.instances?.indexOf(instance) ?: -1

                                                if (k in 0 until VISIBLE_INSTANCE_COUNT) {
                                                    calendarItems[j]?.visibleInstances?.set(k, it)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        //}
                    }
                }

                val calendar = Model.Calendar(
                    year = year,
                    month = month,
                    items = calendarItems,
                    indexOfFirstDayOfMonth,
                    indexOfLastDayOfMonth,
                )

                withContext(Dispatchers.Main) {
                    weakReference.get()?.linkedMap?.let { hashMap ->
                        hashMap[token]?.let {
                            it.value = calendar
                        } ?: let {
                            weakReference.get()?.linkedMap?.set(
                                token,
                                MutableLiveData(calendar)
                            )
                        }
                    }
                }

                cursor.close()
            }
        }

        private fun addDaysOfPreviousMonth(
            calendarItems: Array<CalendarItem?>,
            lastDayOfPreviousMonth: Int,
            indexOfFirstDayOfMonth: Int,
            year: Int,
            month: Int
        ) {
            if (indexOfFirstDayOfMonth == 0) return

            val from = lastDayOfPreviousMonth - indexOfFirstDayOfMonth.dec()

            for ((i, j) in (from .. lastDayOfPreviousMonth).withIndex()) {
                calendarItems[i] = CalendarItem.DayOfPreviousMonth(
                    year = year,
                    month = month,
                    date = j,
                    position = i
                )
            }
        }

        private fun addDaysOfMonth(
            calendarItems: Array<CalendarItem?>,
            indexOfFirstDayOfMonth: Int,
            indexOfLastDayOfMonth: Int,
            year: Int,
            month: Int
        ) {
            for ((i, j) in (indexOfFirstDayOfMonth until indexOfLastDayOfMonth).withIndex()) {
                calendarItems[j] = CalendarItem.DayOfMonth(
                    year = year,
                    month = month,
                    date = i.inc(),
                    position = j
                )
            }
        }

        private fun addDaysOfNextMonth(
            calendarItems: Array<CalendarItem?>,
            indexOfLastDayOfMonth: Int,
            year: Int,
            month: Int
        ) {
            for ((i, j) in (indexOfLastDayOfMonth until DAYS_PER_MONTH).withIndex()) {
                calendarItems[j] = CalendarItem.DayOfNextMonth(
                    year = year,
                    month = month,
                    i.inc(),
                    position = j
                )
            }
        }
    }

    private fun createLinkedMap(): LinkedHashMap<Int, MutableLiveData<Model.Calendar>> {
        val linkedMap = linkedMapOf<Int, MutableLiveData<Model.Calendar>>()

        for (i in -OFFSET..OFFSET) {
            Calendar.getInstance().apply {
                add(Calendar.MONTH, i)
            }.also {
                linkedMap[it.getKey()] = MutableLiveData<Model.Calendar>()
            }
        }

        return linkedMap
    }

    fun getLastEvent(): Model.Event? {
        val contentUri = CalendarContract.Events.CONTENT_URI
        val cursor = contentResolver.query(
            contentUri,
            Events.projections,
            null,
            null,
            null
        )

        cursor ?: return null
        cursor.moveToLast()

        val _id = cursor.getLong(Events.Index._ID)
        val DTSTART = cursor.getLong(Events.Index.DTSTART)

        cursor.close()

        return Model.Event(
            id = _id,
            DTSTART = DTSTART
        )
    }

    private fun permissionsGranted(): Boolean {
        return PermissionChecker.checkPermissions(applicationContext, permissions)
    }

    private fun Calendar.getKey() = getKey(get(Calendar.YEAR), get(Calendar.MONTH))

    private fun getKey(year: Int, month: Int) = year * KEY + month

    companion object {
        private const val KEY = 100
        private const val OFFSET = 3
    }
}