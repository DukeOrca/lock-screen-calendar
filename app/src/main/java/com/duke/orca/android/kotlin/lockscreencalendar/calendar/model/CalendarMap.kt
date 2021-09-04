package com.duke.orca.android.kotlin.lockscreencalendar.calendar.model

import com.duke.orca.android.kotlin.lockscreencalendar.calendar.DAYS_PER_WEEK
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.WEEKS_PER_MONTH
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.getYearMonthDay
import java.util.*

class CalendarMap(val year: Int, val month: Int) {
    val linkedHashMap = linkedMapOf<Int, AdapterItem.Week>()

    init {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DATE, 1)
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }

        //calendar.add(Calendar.DATE, -1)

        for (i in 0 until WEEKS_PER_MONTH) {
            val week = AdapterItem.Week(linkedMapOf())
            linkedHashMap[i] = week

            for (j in 0 until DAYS_PER_WEEK) {
                val key = calendar.getYearMonthDay()
                val date = calendar.get(Calendar.DATE)
                val timeInMillis = calendar.timeInMillis

                calendar.add(Calendar.DATE, 1)
                week.dates[key] = (CalItem2(
                    timeInMillis2 = timeInMillis,
                    nextKey = calendar.getYearMonthDay()
                ))
            }
        }
    }
}