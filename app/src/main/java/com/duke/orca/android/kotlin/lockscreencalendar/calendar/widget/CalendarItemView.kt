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
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.Model.CalendarItem
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView.Height.INSTANCE
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView.Margin.CALENDAR_COLOR_END
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView.Margin.CALENDAR_COLOR_START
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView.Margin.DATE_TOP
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView.Margin.DAY_OF_MONTH_BOTTOM
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView.Margin.DATE_START
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView.Margin.INSTANCE_BOTTOM
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView.Margin.INSTANCE_END
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView.Margin.INSTANCE_START_LARGE
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView.Margin.INSTANCE_START_SMALL
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView.Width.CALENDAR_COLOR
import com.duke.orca.android.kotlin.lockscreencalendar.color.ColorCalculator
import com.duke.orca.android.kotlin.lockscreencalendar.util.toPx
import java.util.*
import kotlin.math.abs

class CalendarItemView : View {
    private object Height {
        const val INSTANCE = 11
    }

    private object Margin {
        const val CALENDAR_COLOR_END = 1
        const val CALENDAR_COLOR_START = 1
        const val DATE_START = 2F
        const val DAY_OF_MONTH_BOTTOM = 8F
        const val DATE_TOP = 4F
        const val INSTANCE_BOTTOM = 2
        const val INSTANCE_END = 2
        const val INSTANCE_START_LARGE = 4F
        const val INSTANCE_START_SMALL = 1F
    }

    private object TextSize {
        const val DAY_OF_MONTH = 12F
        const val INSTANCE = 10F
        const val INVISIBLE_INSTANCE_COUNT = 8F
    }

    private object Width {
        const val CALENDAR_COLOR = 2
    }

    private var item: CalendarItem? = null
    private val bounds = Rect()
    private val bound2 = Rect()

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
            textSize = TextSize.DAY_OF_MONTH.toPx
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
        var currentX = DATE_TOP.toPx

        if (item !is CalendarItem.DayOfMonth) {
            calendarColorPaint.alpha = ALPHA
            dateTextPaint.alpha = ALPHA
            instanceTextPaint.alpha = ALPHA
        }

        dateTextPaint.getTextBounds(text, 0, text.length, bounds)

        currentX += bounds.height().toFloat()

        canvas.drawText(text, DATE_START.toPx, currentX, dateTextPaint)

        currentX += DAY_OF_MONTH_BOTTOM.toPx

        item.visibleInstances.forEachIndexed { index, instance ->
            if (index > VISIBLE_INSTANCE_COUNT) {
                return@forEachIndexed
            }

            bounds.set(0, 0, width, INSTANCE.toPx)

            instance?.let {
                val right = this.width * it.columnCount() - CALENDAR_COLOR_END.toPx

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
                    val bottom = currentX.toInt() + bounds.height()

                    if (instance.fillBackground) {
                        canvas.drawRect(calendarColorRect.apply {
                            set(CALENDAR_COLOR_START.toPx, currentX.toInt(), right, bottom)
                        }, calendarColorPaint)
                    } else {
                        canvas.drawRect(calendarColorRect.apply {
                            set(CALENDAR_COLOR_START.toPx, currentX.toInt(), CALENDAR_COLOR.inc().toPx, bottom)
                        }, calendarColorPaint)
                    }
                }

                val x = if (it.fillBackground)
                    INSTANCE_START_SMALL.toPx
                else
                    INSTANCE_START_LARGE.toPx

                val fm: Paint.FontMetrics = instanceTextPaint.getFontMetrics()
                val height22 = fm.descent - fm.ascent

                instanceTextPaint.getTextBounds(title, 0, title.length, bound2)

                val bm = abs(bounds.height() - height22)

                canvas.drawText(
                    title,
                    x,
                    currentX + bounds.height() - bm.toPx,
                    instanceTextPaint
                )

                canvas.restore()
                currentX += (bounds.height() + INSTANCE_BOTTOM.toPx).toFloat()
            } ?: let {
                canvas.drawRect(calendarColorRect.apply {
                    set(
                        0,
                        currentX.toInt(),
                        width,
                        currentX.toInt() + bounds.height()
                    )
                }, calendarColorPaint.apply { color = Color.TRANSPARENT })

                currentX += (bounds.height() + INSTANCE_BOTTOM.toPx).toFloat()
            }
        }

        if (item.instances.count() > VISIBLE_INSTANCE_COUNT) {
            canvas.drawText(
                "+${(item.instances.count() - VISIBLE_INSTANCE_COUNT)}",
                INSTANCE_START_SMALL.toPx,
                currentX + bounds.height() - INSTANCE_END.toPx,
                instanceTextPaint
            )
        }
    }

    fun setData(item: CalendarItem) {
        this.item = item
        invalidate()
    }

    companion object {
        private const val ALPHA = 128
    }
}