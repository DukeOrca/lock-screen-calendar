package com.duke.orca.android.kotlin.lockscreencalendar.calendar.viewmodel

import android.Manifest
import android.app.Application
import android.content.AsyncQueryHandler
import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.duke.orca.android.kotlin.lockscreencalendar.BLANK
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.Model
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.toDate
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.toMonth
import com.duke.orca.android.kotlin.lockscreencalendar.permission.PermissionChecker
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.util.*

class InstancesViewModel(application: Application) : AndroidViewModel(application) {
    private val projections: Array<String> = arrayOf(
        CalendarContract.Instances.ALL_DAY,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.CALENDAR_COLOR,
        CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
        CalendarContract.Instances.CALENDAR_ID,
        CalendarContract.Instances.END,
        CalendarContract.Instances.END_DAY,
        CalendarContract.Instances.EVENT_ID,
        CalendarContract.Instances._ID,
        CalendarContract.Instances.START_DAY,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.DURATION
    )

    private object Index {
        const val ALL_DAY = 0
        const val BEGIN = 1
        const val CALENDAR_COLOR = 2
        const val CALENDAR_DISPLAY_NAME = 3
        const val CALENDAR_ID = 4
        const val END = 5
        const val END_DAY = 6
        const val EVENT_ID = 7
        const val ID = 8
        const val START_DAY = 9
        const val TITLE = 10
        const val DURATION = 11
    }

    private val asyncQueryHandler = InstancesAsyncQueryHandler(application.contentResolver, WeakReference(this))

    private val _instances = MutableLiveData<List<Model.Instance>>()
    val instances: LiveData<List<Model.Instance>>
        get() = _instances

    private fun permissionsGranted(): Boolean {
        return PermissionChecker.checkPermissions(getApplication(), listOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        ))
    }

    fun query(year: Int, month: Int, date: Int) {
        if (permissionsGranted().not())
            return

        val dtstart = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DATE, date)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }.timeInMillis

        val dtend = Calendar.getInstance().apply {
            timeInMillis = dtstart
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }.timeInMillis

        val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()

        ContentUris.appendId(builder, dtstart)
        ContentUris.appendId(builder, dtend)

        asyncQueryHandler.startQuery(
            0,
            null,
            builder.build(),
            projections,
            null,
            null,
            CalendarContract.Instances.START_DAY + " DESC, " +
                    CalendarContract.Instances.END_DAY + " DESC, " +
                    CalendarContract.Instances.BEGIN + " ASC, " +
                    CalendarContract.Instances.TITLE + " ASC"

        )
    }

    class InstancesAsyncQueryHandler(
        contentResolver: ContentResolver,
        private val weakReference: WeakReference<InstancesViewModel>
    ) : AsyncQueryHandler(contentResolver) {
        private val job = Job()
        private val coroutineScope = CoroutineScope(Dispatchers.IO + job)

        override fun onQueryComplete(token: Int, cookie: Any?, cursor: Cursor?) {
            super.onQueryComplete(token, cookie, cursor)
            set(cursor ?: return)
        }

        private fun set(cursor: Cursor) {
            coroutineScope.launch {
                val instances = mutableListOf<Model.Instance>()

                while (cursor.moveToNext()) {
                    if (this.isActive.not()) return@launch

                    val id = cursor.getLongOrNull(Index.ID) ?: 0L
                    val allDay = cursor.getIntOrNull(Index.ALL_DAY) ?: 0
                    val begin = cursor.getLongOrNull(Index.BEGIN) ?: 0L
                    val calendarColor = cursor.getIntOrNull(Index.CALENDAR_COLOR) ?: Color.TRANSPARENT
                    val calendarDisplayName = cursor.getStringOrNull(Index.CALENDAR_DISPLAY_NAME) ?: BLANK
                    val calendarId = cursor.getLongOrNull(Index.CALENDAR_ID) ?: continue
                    val end = cursor.getLongOrNull(Index.END) ?: 0L
                    val endDay = cursor.getIntOrNull(Index.END_DAY) ?: 0
                    val eventId = cursor.getLongOrNull(Index.EVENT_ID) ?: continue
                    val startDay = cursor.getIntOrNull(Index.START_DAY) ?: 0
                    val title = cursor.getStringOrNull(Index.TITLE) ?: BLANK
                    val duration = cursor.getStringOrNull(Index.DURATION) ?: BLANK

                    val beginDayOfMonth = begin.toDate()
                    val endDayOfMonth = end.toDate()
                    val fillBackground = allDay == 1 || (endDay - startDay > 0)

                    instances.add(
                        Model.Instance(
                            isAllDay = allDay == 1,
                            begin = begin,
                            calendarColor = calendarColor,
                            calendarId = calendarId,
                            end = end,
                            endDay = endDay,
                            eventId = eventId,
                            id = id,
                            month = begin.toMonth(),
                            startDay = startDay,
                            title = title,
                            beginDayOfMonth = beginDayOfMonth,
                            endDayOfMonth = endDayOfMonth,
                            duration = endDay - startDay,
                            fillBackgroundColor = fillBackground
                        ))
                }

                instances.sortWith { o1, o2 ->
                    o1.startDay - o2.startDay
                }

                cursor.close()

                withContext(Dispatchers.Main) {
                    weakReference.get()?._instances?.value = instances
                }
            }
        }
    }
}