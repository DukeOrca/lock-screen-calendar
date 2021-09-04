package com.duke.orca.android.kotlin.lockscreencalendar.calendar.model

sealed class AdapterItem {
    class Week(
        val dates: LinkedHashMap<Int, CalItem2>
    ) : AdapterItem() {
        // todo try catch 처리필요..
        fun getFirstDay() = dates[dates.keys.first()]
        fun getLastDay() = dates[dates.keys.last()]
    }

    class Date()
}