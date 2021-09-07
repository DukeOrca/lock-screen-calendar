package com.duke.orca.android.kotlin.lockscreencalendar.calendar.model

import com.duke.orca.android.kotlin.lockscreencalendar.calendar.VISIBLE_INSTANCE_COUNT
import java.util.*
import kotlin.collections.ArrayList

class CalItem2(
    val position: Int,
    val date: Int,
    val dayOfWeek: Int,
    val month: Int,
    val year: Int,
    val nextKey: Int,
    val instances: ArrayList<Instance> = arrayListOf(),
    val visibleInstances: Array<Instance?> = arrayOfNulls(VISIBLE_INSTANCE_COUNT),
    val invisibleInstanceCount: Int = 1,
) {
    val yearMonthDay = year * 10000 + month * 100 + date
}