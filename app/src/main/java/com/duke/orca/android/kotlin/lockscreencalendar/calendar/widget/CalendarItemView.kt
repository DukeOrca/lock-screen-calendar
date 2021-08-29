package com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.duke.orca.android.kotlin.lockscreencalendar.BLANK
import com.duke.orca.android.kotlin.lockscreencalendar.R
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.DAYS_PER_WEEK
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.VISIBLE_INSTANCE_COUNT
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.CalendarItem
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView.Height
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView.Margin.CALENDAR_COLOR_END
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView.Margin.CALENDAR_COLOR_START
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView.Margin.DATE_TOP
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView.Margin.DATE_BOTTOM
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView.Margin.DATE_START
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView.Margin.INSTANCE_BOTTOM
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView.Margin.INSTANCE_END
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView.Margin.INSTANCE_START_LARGE
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView.Margin.INSTANCE_START_SMALL
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView.Width.CALENDAR_COLOR
import com.duke.orca.android.kotlin.lockscreencalendar.color.ColorCalculator
import com.duke.orca.android.kotlin.lockscreencalendar.util.toPx
import timber.log.Timber
import java.util.*

class CalendarItemView : View {
    private object Height {
        const val INSTANCE = 11
    }

    private object Margin {
        const val CALENDAR_COLOR_END = 1
        const val CALENDAR_COLOR_START = 1
        const val DATE_BOTTOM = 6F
        const val DATE_START = 2F
        const val DATE_TOP = 4F
        const val INSTANCE_BOTTOM = 2
        const val INSTANCE_END = 2
        const val INSTANCE_START_LARGE = 4F
        const val INSTANCE_START_SMALL = 2F
    }

    private object TextSize {
        const val DATE = 11F
        const val INSTANCE = 10F
        const val INVISIBLE_INSTANCE_COUNT = 8F
    }

    private object Width {
        const val CALENDAR_COLOR = 2
    }

    private var item: CalendarItem? = null
    private val bounds = Rect()

    private lateinit var dateTextPaint: TextPaint
    private lateinit var instanceTextPaint: TextPaint
    private lateinit var invisibleInstanceCountPaint: TextPaint

    private val calendarColorPaint = Paint()
    private val calendarColorRect = Rect()

    constructor(context: Context, item: CalendarItem?): super(context) {
        this.item = item

        val dayOfWeek = (item?.position ?: 0) % DAYS_PER_WEEK

        dateTextPaint = TextPaint().apply {
            isAntiAlias = true
            textSize = TextSize.DATE.toPx
            color = when(dayOfWeek) {
                Calendar.SATURDAY.dec() -> ContextCompat.getColor(context, R.color.light_blue_400)
                Calendar.SUNDAY.dec() -> ContextCompat.getColor(context, R.color.red_400)
                else -> Color.WHITE
            }
        }

        instanceTextPaint = TextPaint().apply {
            color = ContextCompat.getColor(context, R.color.high_emphasis)
            isAntiAlias = true
            textSize = TextSize.INSTANCE.toPx
            style = Paint.Style.FILL
        }

        invisibleInstanceCountPaint = TextPaint().apply {
            color = ContextCompat.getColor(context, R.color.high_emphasis)
            isAntiAlias = true
            textSize = TextSize.INVISIBLE_INSTANCE_COUNT.toPx
            style = Paint.Style.FILL
        }
    }

    private constructor(context: Context): super(context)
    private constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    private constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return

        val item = this.item ?: return
        val text = item.date.toString()
        var currentY = DATE_TOP.toPx

        if (item !is CalendarItem.DayOfMonth) {
            calendarColorPaint.alpha = ALPHA
            dateTextPaint.alpha = ALPHA
            instanceTextPaint.alpha = ALPHA
        }

        with(bounds) {
            dateTextPaint.getTextBounds(text, 0, text.length, this)
            currentY += height().toFloat()
        }

        canvas.drawText(text, DATE_START.toPx, currentY, dateTextPaint)

        currentY += DATE_BOTTOM.toPx

        item.visibleInstances.forEachIndexed { index, instance ->
            if (index > VISIBLE_INSTANCE_COUNT) {
                return@forEachIndexed
            }

            bounds.set(0, 0, width, Height.INSTANCE.toPx)

            instance?.let {
                val right = this.width * it.getColumnCount() - CALENDAR_COLOR_END.toPx

                canvas.save()
                canvas.clipRect(0, 0, right, this.height)

                val title = if (it.isVisible) {
                    calendarColorPaint.color = instance.calendarColor
                    instanceTextPaint.color = ColorCalculator.onBackgroundColor(
                        instance.calendarColor,
                        ContextCompat.getColor(context, R.color.high_emphasis_dark),
                        ContextCompat.getColor(context, R.color.high_emphasis_light)
                    )

                    instance.title
                } else {
                    calendarColorPaint.color = Color.TRANSPARENT
                    instanceTextPaint.color = Color.TRANSPARENT
                    BLANK
                }

                if (item !is CalendarItem.DayOfMonth) {
                    calendarColorPaint.alpha = ALPHA
                    instanceTextPaint.alpha = ALPHA
                }

                if (it.isVisible) {
                    val bottom = currentY.toInt() + bounds.height()

                    if (instance.fillBackgroundColor) {
                        canvas.drawRect(calendarColorRect.apply {
                            set(CALENDAR_COLOR_START.toPx, currentY.toInt(), right, bottom)
                        }, calendarColorPaint)
                    } else {
                        canvas.drawRect(calendarColorRect.apply {
                            set(CALENDAR_COLOR_START.toPx, currentY.toInt(), CALENDAR_COLOR.inc().toPx, bottom)
                        }, calendarColorPaint)
                    }
                }

                val x = if (it.fillBackgroundColor)
                    INSTANCE_START_SMALL.toPx
                else
                    INSTANCE_START_LARGE.toPx

                val fondHeight = instanceTextPaint.fontMetrics.run {
                    descent - ascent
                }

                var bm = fondHeight - bounds.height()

                if (bm < 0F) {
                    bm = 0F
                }

                canvas.drawText(
                    title,
                    x,
                    currentY + bounds.height() - bm.toPx,
                    instanceTextPaint
                )

                canvas.restore()
                currentY += (bounds.height() + INSTANCE_BOTTOM.toPx).toFloat()
            } ?: let {
                canvas.drawRect(calendarColorRect.apply {
                    set(
                        0,
                        currentY.toInt(),
                        width,
                        currentY.toInt() + bounds.height()
                    )
                }, calendarColorPaint.apply { color = Color.TRANSPARENT })

                currentY += (bounds.height() + INSTANCE_BOTTOM.toPx).toFloat()
            }
        }

        if (item.instances.count() > VISIBLE_INSTANCE_COUNT) {
            canvas.drawText(
                "+${(item.instances.count() - VISIBLE_INSTANCE_COUNT)}",
                INSTANCE_START_SMALL.toPx,
                currentY + bounds.height() - INSTANCE_END.toPx,
                invisibleInstanceCountPaint
            )
        }
    }

    private fun TextPaint.fontHeight() = fontMetrics.run {
        bottom - ascent
    }

    companion object {
        private const val ALPHA = 128
    }
}