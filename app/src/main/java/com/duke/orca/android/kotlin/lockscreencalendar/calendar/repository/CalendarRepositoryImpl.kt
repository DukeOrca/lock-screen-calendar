@file:Suppress("LocalVariableName", "ObjectPropertyName")

package com.duke.orca.android.kotlin.lockscreencalendar.calendar.repository

import android.Manifest.permission.READ_CALENDAR
import android.Manifest.permission.WRITE_CALENDAR
import android.content.ContentUris
import android.content.Context
import android.graphics.Color
import android.provider.CalendarContract
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.LiveData
import com.duke.orca.android.kotlin.lockscreencalendar.BLANK
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.DAYS_PER_WEEK
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.WEEKS_PER_MONTH
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.Instance
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.Model
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.toDate
import com.duke.orca.android.kotlin.lockscreencalendar.permission.PermissionChecker
import java.util.*
import kotlinx.coroutines.*
import kotlin.collections.LinkedHashMap

class CalendarRepositoryImpl(private val applicationContext: Context) {
    private val contentResolver = applicationContext.contentResolver
    private val permissions = listOf(READ_CALENDAR, WRITE_CALENDAR)

    private val WHERE_CALENDARS_SELECTED = CalendarContract.Calendars.VISIBLE + "=?"
    private val WHERE_CALENDARS_ARGS = arrayOf(
        "1"
    )

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

    object Instances {
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
            CalendarContract.Instances.TITLE
        )

        val sortOrder = CalendarContract.Instances.BEGIN + " ASC, " +
                CalendarContract.Instances.END_DAY + " DESC, " +
                CalendarContract.Instances.TITLE + " ASC"

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

    suspend fun getInstances(year: Int, month: Int): LinkedHashMap<Int, ArrayList<Instance>> {
        val linkedHashMap = linkedMapOf<Int, ArrayList<Instance>>()

        if (permissionsGranted().not()) return linkedHashMap

        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.WEEK_OF_MONTH, 1)
            set(Calendar.DAY_OF_WEEK, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 1)
        }

        withContext(Dispatchers.IO) {
            repeat(WEEKS_PER_MONTH) {
                val list = arrayListOf<Instance>()

                val dtstart = calendar.timeInMillis
                val dtend = Calendar.getInstance().apply {
                    timeInMillis = dtstart
                    add(Calendar.DATE, DAYS_PER_WEEK.dec())
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }.timeInMillis

                val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()

                ContentUris.appendId(builder, dtstart)
                ContentUris.appendId(builder, dtend)

                val cursor = contentResolver.query(
                    builder.build(),
                    Instances.projections,
                    WHERE_CALENDARS_SELECTED,
                    WHERE_CALENDARS_ARGS,
                    Instances.sortOrder
                ) ?: return@withContext

                while (cursor.moveToNext()) {
                    if (this.isActive.not()) return@withContext

                    val allDay = cursor.getIntOrNull(Instances.Index.ALL_DAY) ?: 0
                    val begin = cursor.getLongOrNull(Instances.Index.BEGIN) ?: 0L
                    val calendarColor = cursor.getIntOrNull(Instances.Index.CALENDAR_COLOR) ?: Color.TRANSPARENT
                    val end = cursor.getLongOrNull(Instances.Index.END) ?: 0L
                    val endDay = cursor.getIntOrNull(Instances.Index.END_DAY) ?: 0
                    val eventId = cursor.getLongOrNull(Instances.Index.EVENT_ID) ?: continue
                    val startDay = cursor.getIntOrNull(Instances.Index.START_DAY) ?: 0
                    val title = cursor.getStringOrNull(Instances.Index.TITLE) ?: BLANK

                    list.add(
                        Instance(
                            begin = begin,
                            startDay = startDay,
                            calendarColor = calendarColor,
                            end = end,
                            endDay = endDay,
                            eventId = eventId,
                            duration = endDay - startDay,
                            isAllDay = allDay == 1,
                            title = title,
                        )
                    )
                }

                // todo 필요성 체크..
//                list.sortWith { o1, o2 ->
//                    o1.startDay - o2.startDay
//                }

                linkedHashMap[it] = list
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                cursor.close()
            }
        }

        return linkedHashMap
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