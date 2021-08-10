@file:Suppress("LocalVariableName", "ObjectPropertyName")

package com.duke.orca.android.kotlin.lockscreencalendar.calendar.repository

import android.Manifest.permission.READ_CALENDAR
import android.Manifest.permission.WRITE_CALENDAR
import android.content.ContentUris
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.provider.CalendarContract
import androidx.activity.result.ActivityResultLauncher
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.duke.orca.android.kotlin.lockscreencalendar.BLANK
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.Model
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.toDayOfMonth
import com.duke.orca.android.kotlin.lockscreencalendar.permission.PermissionChecker
import java.util.*

class CalendarRepository(private val applicationContext: Context) {
    private val contentResolver = applicationContext.contentResolver
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
            CalendarContract.Instances.TITLE
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
        val string = calendars().map { it.id }.joinToString(separator = ", ") { "\"$it\"" }
        val selection = "(${CalendarContract.Instances.CALENDAR_ID} IN ($string))"

        ContentUris.appendId(builder, DTSTART.timeInMillis)
        ContentUris.appendId(builder, DTEND.timeInMillis)

        if (permissionsGranted().not())
            return emptyList()

        val cursor = contentResolver.query(
            builder.build(),
            Instances.projections,
            selection,
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