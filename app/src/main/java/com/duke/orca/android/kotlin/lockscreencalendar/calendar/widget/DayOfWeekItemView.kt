package com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.duke.orca.android.kotlin.lockscreencalendar.R
import com.duke.orca.android.kotlin.lockscreencalendar.util.toPx
import java.util.*

class DayOfWeekItemView : View {
    private object Margin {
        const val START = 2F
    }

    private object TextSize {
        const val DAY_OF_WEEK = 11F
    }

    private val daysOfWeek = context.resources.getStringArray(R.array.days_of_week)
    private var dayOfWeek = 0

    private lateinit var textPaint: TextPaint

    constructor(context: Context, dayOfWeek: Int): super(context) {
        this.dayOfWeek = dayOfWeek

        textPaint = TextPaint().apply {
            isAntiAlias = true
            textSize = TextSize.DAY_OF_WEEK.toPx
            color = when(dayOfWeek) {
                Calendar.SATURDAY.dec() -> ContextCompat.getColor(context, R.color.light_blue_400)
                Calendar.SUNDAY.dec() -> ContextCompat.getColor(context, R.color.red_400)
                else -> Color.WHITE
            }
        }
    }

    private constructor(context: Context): super(context)
    private constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    private constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return

        val text = daysOfWeek[dayOfWeek]

        canvas.drawText(text, Margin.START.toPx, textPaint.height(), textPaint)
    }

    private fun TextPaint.height() = fontMetrics.run {
        descent() - ascent()
    }
}