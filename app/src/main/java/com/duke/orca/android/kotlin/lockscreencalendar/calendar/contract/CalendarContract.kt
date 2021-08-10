package com.duke.orca.android.kotlin.lockscreencalendar.calendar.contract

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.activity.result.contract.ActivityResultContract
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.Model

class CalendarContract: ActivityResultContract<Model.Instance?, Int>() {
    override fun createIntent(context: Context, input: Model.Instance?): Intent {
        return input?.let {
            Intent(Intent.ACTION_INSERT).apply {
                data = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, it.eventId)
            }
        } ?: let {
            Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Int {
        return resultCode
    }
}