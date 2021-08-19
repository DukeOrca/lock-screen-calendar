@file:Suppress("LocalVariableName", "ObjectPropertyName", "unused")

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
import androidx.activity.result.ActivityResultLauncher
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
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.entity.Entity
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.Model
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.Model.CalendarItem
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.getFirstDayOfWeekOfMonth
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.toDayOfMonth
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.toMonth
import com.duke.orca.android.kotlin.lockscreencalendar.permission.PermissionChecker
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.LinkedHashMap
import android.content.ContentResolver
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.toYear
import kotlinx.coroutines.*


class CalendarRepository(private val applicationContext: Context) {
    private val calendar = Calendar.getInstance()
    private val contentResolver = applicationContext.contentResolver
    private val calendarAsyncQueryHandler = CalendarAsyncQueryHandler(WeakReference(this))
    private val linkedMap = createEmptyLinkedMap()

    private val permissions = listOf(READ_CALENDAR, WRITE_CALENDAR)

    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + job)

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

    fun getCalendarItems(year: Int, month: Int): LiveData<Array<CalendarItem?>>? {
        return linkedMap[key(year, month)]
    }

    fun setCalendarItems(year: Int, month: Int, @MainThread onSet: (liveData: LiveData<Array<CalendarItem?>>?) -> Unit) {
        linkedMap[key(year, month)] = MutableLiveData()
        onSet.invoke(linkedMap[key(year, month)])
    }

    fun initialLoad() {
        (-OFFSET..OFFSET).forEach {
            Calendar.getInstance().apply {
                add(Calendar.MONTH, it)
            }.also { load(it) }
        }
    }

    fun load(year: Int, month: Int) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
        }.also { load(it) }
    }

    private fun load(calendar: Calendar) {
        val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val token = key(year, month)

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

        val indexOfFirstDayOfMonth = getFirstDayOfWeekOfMonth(year, month).dec()
        val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val lastDayOfPreviousMonth = previousMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val DTSTART = if (indexOfFirstDayOfMonth == 0) {
            calendar.apply { set(Calendar.DAY_OF_MONTH, 1) }
        } else {
            previousMonthCalendar.apply {
                set(Calendar.DAY_OF_MONTH, lastDayOfPreviousMonth - indexOfFirstDayOfMonth)
            }
        }

        val DTEND = nextMonthCalendar.apply {
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
                    CalendarContract.Instances.BEGIN + " ASC"
        )

        CalendarContract.Events.MAX_REMINDERS
    }

    fun clear(year: Int, month: Int) {
        linkedMap.remove(key(year, month))
    }

    fun clearCoroutine() {
        calendarAsyncQueryHandler.clear()
    }

    class CalendarAsyncQueryHandler(private val weakReference: WeakReference<CalendarRepository>)
        : AsyncQueryHandler(weakReference.get()?.contentResolver) {
        private val job = Job()
        private val coroutineScope = CoroutineScope(Dispatchers.IO + job)

        fun clear() {
            job.cancel()
        }

        override fun onQueryComplete(token: Int, cookie: Any?, cursor: Cursor?) {
            super.onQueryComplete(token, cookie, cursor)
            cursor ?: return

            val calendarItems = arrayOfNulls<Model.CalendarItem>(DAYS_PER_MONTH)
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

            val previousMonth = previousMonthCalendar.get(Calendar.MONTH)
            val nextMonth = nextMonthCalendar.get(Calendar.MONTH)

            val indexOfFirstDayOfMonth = getFirstDayOfWeekOfMonth(year, month).dec()
            val indexOfLastDayOfMonth = indexOfFirstDayOfMonth + calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

            val lastDayOfPreviousMonth = previousMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val firstVisibleDayOfPreviousMonth = lastDayOfPreviousMonth - indexOfFirstDayOfMonth

            addDaysOfPreviousMonth(calendarItems, lastDayOfPreviousMonth, indexOfFirstDayOfMonth)
            addDaysOfMonth(calendarItems, indexOfFirstDayOfMonth, indexOfLastDayOfMonth)
            addDaysOfNextMonth(calendarItems, indexOfLastDayOfMonth)

            //
            coroutineScope.launch {
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
                    val fillBackground = allDay == 1 || (endDayOfMonth - beginDayOfMonth > 0)

                    val entity = Entity.Instance(
                        allDay = allDay == 1,
                        begin = begin,
                        calendarColor = calendarColor,
                        calendarDisplayName = calendarDisplayName,
                        calendarId = calendarId,
                        end = end,
                        eventId = eventId,
                        id = _id,
                        month = begin.toMonth(),
                        title = title
                    )

                    instances.add(
                        Model.Instance(
                            entity = entity,
                            beginDayOfMonth = beginDayOfMonth,
                            endDayOfMonth = endDayOfMonth,
                            duration = endDayOfMonth - beginDayOfMonth,
                            fillBackground = fillBackground
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
                    val endDayOfMonth = instance.endDayOfMonth
                    val duration = endDayOfMonth - beginDayOfMonth
                    val fillBackground = instance.allDay || (duration > 0)

                    val index = when(instance.month) {
                        previousMonth -> beginDayOfMonth.dec() - firstVisibleDayOfPreviousMonth
                        nextMonth -> beginDayOfMonth + indexOfLastDayOfMonth.dec()
                        else -> beginDayOfMonth.dec() + indexOfFirstDayOfMonth
                    }

                    if (index in 0 until DAYS_PER_MONTH) {
                        calendarItems[index]?.instances?.add(instance)

                        val k = calendarItems[index]?.instances?.indexOf(instance) ?: NO_INDEX

                        if (k in 0 until VISIBLE_INSTANCE_COUNT) {
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
                                        if (instance.allDay.not()) {
                                            Model.Instance(
                                                entity = instance.entity,
                                                beginDayOfMonth = beginDayOfMonth + i,
                                                endDayOfMonth = endDayOfMonth,
                                                duration = endDayOfMonth - (beginDayOfMonth + i),
                                                fillBackground = fillBackground,
                                                isVisible = (j % DAYS_PER_WEEK) == Calendar.SUNDAY.dec()
                                            ).also {
                                                calendarItems[j]?.instances?.add(it)
                                                calendarItems[j]?.visibleInstances?.set(k, it)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    weakReference.get()?.linkedMap?.let { hashMap ->
                        hashMap[token]?.let {
                            it.value = calendarItems
                        } ?: let {
                            weakReference.get()?.linkedMap?.set(
                                token,
                                MutableLiveData(calendarItems)
                            )
                        }
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
                calendarItems[i] = Model.CalendarItem.DayOfPreviousMonth(j, position = i)
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

    private fun permissionsGranted(): Boolean {
        return PermissionChecker.checkPermissions(applicationContext, permissions)
    }

    fun calendars(): List<Entity.Calendar> {
        if (permissionsGranted().not())
            return emptyList()

        val calendars = mutableListOf<Entity.Calendar>()
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

            calendars.add(Entity.Calendar(_id, calendarDisplayName))
        }

        cursor.close()

        return calendars
    }

    fun events() {
//        if (permissionsGranted().not())
//            return emptyList()

        val calendars = mutableListOf<Entity.Calendar>()
        val contentUri = CalendarContract.Events.CONTENT_URI
        val cursor = contentResolver.query(
            contentUri,
            Events.projections,
            null,
            null,
            null
        )

        cursor ?: return
        //cursor ?: return emptyList()
//        cursor.moveToFirst()
//
//        while (cursor.moveToNext()) {
//            val _id = cursor.getLong(Events.Index._ID)
//            val title = cursor.getString(Events.Index.TITLE)
//            val DTSTART = cursor.getLong(Events.Index.DTSTART)
//
//           // calendars.add(Entity.Calendar(_id, calendarDisplayName))
//            println("events tit: $title, id: ${_id}, DOM ${DTSTART.toDayOfMonth()},, M: ${DTSTART.toMonth()} Y:${DTSTART.toYear()} ")
//        }

        cursor.moveToLast() // 최후의 존재.

        val _id = cursor.getLong(Events.Index._ID)

            val DTSTART = cursor.getLong(Events.Index.DTSTART)

// 더해진놈의 dt로 이동하고. 그 달 리플레시.
        cursor.close()
    }

    fun instances(year: Int, month: Int, date: Int): List<Model.Instance> {
        val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        val instances = mutableListOf<Model.Instance>()

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
                    if (end in DTSTART..DTEND) {
                        continue
                    }
                }
            }

            val entity = Entity.Instance(
                allDay = allDay == 1,
                begin = begin,
                calendarColor = calendarColor,
                calendarDisplayName = calendarDisplayName,
                calendarId = calendarId,
                end = end,
                eventId = eventId,
                id = _id,
                month = begin.toMonth(),
                title = title
            )

            instances.add(Model.Instance(
                entity = entity,
                beginDayOfMonth = beginDayOfMonth,
                endDayOfMonth = endDayOfMonth,
                duration = duration,
                fillBackground = fillBackground
            ))
        }

        cursor.close()

        return instances
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
            val duration = endDayOfMonth - beginDayOfMonth
            val fillBackground = (allDay == 1) || ((endDayOfMonth - beginDayOfMonth) > 0)

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

            val entity = Entity.Instance(
                allDay = allDay == 1,
                begin = begin,
                calendarColor = calendarColor,
                calendarDisplayName = calendarDisplayName,
                calendarId = calendarId,
                end = end,
                eventId = eventId,
                id = _id,
                month = begin.toMonth(),
                title = title
            )

            instances.add(Model.Instance(
                entity = entity,
                beginDayOfMonth = beginDayOfMonth,
                endDayOfMonth = endDayOfMonth,
                duration = duration,
                fillBackground = fillBackground
            ))
        }

        cursor.close()

        return instances
    }

    private fun createEmptyLinkedMap(): LinkedHashMap<Int, MutableLiveData<Array<CalendarItem?>>> {
        val linkedMap = linkedMapOf<Int, MutableLiveData<Array<CalendarItem?>>>()
        val month = calendar.get(Calendar.MONTH)

        for (i in -OFFSET..OFFSET) {
            Calendar.getInstance().apply {
                add(Calendar.MONTH, i)
            }.also {
                linkedMap[it.key()] = MutableLiveData<Array<CalendarItem?>>()
            }

            calendar.set(Calendar.MONTH, month)
        }

        return linkedMap
    }

    private fun key(year: Int, month: Int) = year * 100 + month

    private fun Calendar.key() = key(get(Calendar.YEAR), get(Calendar.MONTH))

    fun getNewEventId(cr: ContentResolver, cal_uri: Uri?): Long {
        var local_uri = cal_uri
        if (cal_uri == null) {
            //local_uri = Uri.parse(calendar_uri.toString() + "events")
        }
        val cursor = cr.query(local_uri!!, arrayOf("MAX(_id) as max_id"), null, null, "_id")
        cursor!!.moveToFirst()
        val max_val = cursor.getLong(cursor.getColumnIndex("max_id"))
        return max_val + 1

        // 라스트 아이디가 변경되었는지 검사하는 식으로..
        // 이벤트로부터 인스턴스 적출하고..
//        val projection = arrayOf(CalendarContract.Calendars._ID)
//        cursor = contentResolver.query(CalendarContract.Events.CONTENT_URI, projection, null, null, null)
//        if (cursor.moveToLast()) {
//            val lastId = cursor.getLong(0)
//            // compare lastId with a previous one, if not changed - result is CANCELLED
//        }
    }

    fun lastEvent(): Entity.Event? {
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

        return Entity.Event(
            id = _id,
            DTSTART = DTSTART
        )
    }

    companion object {
        private const val NO_INDEX = -1
        private const val OFFSET = 12

        fun edit(activityResultLauncher: ActivityResultLauncher<Model.Instance>, instance: Model.Instance) {
            activityResultLauncher.launch(instance)
        }

        fun insert(activityResultLauncher: ActivityResultLauncher<Model.Instance>) {
            activityResultLauncher.launch(null)
        }
    }
}