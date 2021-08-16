@file:Suppress("unused")

package com.duke.orca.android.kotlin.lockscreencalendar.util

import android.content.res.Resources

val Float.toDp get() = this / Resources.getSystem().displayMetrics.density
val Float.toPx get() = this * Resources.getSystem().displayMetrics.density

val Int.toDp get() = (this / Resources.getSystem().displayMetrics.density).toInt()
val Int.toPx get() = (this * Resources.getSystem().displayMetrics.density).toInt()
