package com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapters.CalendarItem

class CalendarItemView : View {
    private var item: CalendarItem? = null

    private val bounds = Rect()
    private var paint: Paint = Paint()
    private var paint2: Paint = Paint().apply { color = Color.BLUE }
    val rect = Rect()

    constructor(context: Context, item: CalendarItem?): super(context) {
        this.item = item
        paint = TextPaint().apply {
            isAntiAlias = true
            textSize = 20f
            color = Color.WHITE
        }
    }

    private constructor(context: Context): super(context)
    private constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    private constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null)
            return

        if (item?.dayOfMonth == 2)
            return

        val date = item?.dayOfMonth.toString()
        paint.getTextBounds(date, 0, date.length, bounds)
        val height = (height / 2 + bounds.height() / 2).toFloat()
        canvas.drawText(date,
            (width / 2 - bounds.width() / 2).toFloat() - 2,
            height, paint )

//        if (item?.dayOfMonth == 1) {
//            canvas.drawRect(
//                rect.apply { set(0, height / 2, width * 2, height) }, paint2
//            )
//        }
//
        item?.instances?.forEachIndexed { index, instance ->
            if (index > 1) {
                return@forEachIndexed
            }

            paint.getTextBounds(instance.title, 0, instance.title.length, bounds)
            canvas.drawText(instance.title,
                (width / 2 - bounds.width() / 2).toFloat() - 2,
                height + bounds.height() * (index.inc()), paint )
        }
    }
}