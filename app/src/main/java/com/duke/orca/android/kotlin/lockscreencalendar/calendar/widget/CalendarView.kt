package com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.core.view.children
import com.duke.orca.android.kotlin.lockscreencalendar.R
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.DAYS_PER_MONTH
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.DAYS_PER_WEEK
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.WEEKS_PER_MONTH
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.CalendarItem
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.Model
import com.duke.orca.android.kotlin.lockscreencalendar.util.toPx
import java.util.*

class CalendarView : ViewGroup {
    private var onItemClickListener: OnItemClickListener? = null

    private var selectedPosition = -1
    private var selectedCalendarItem: CalendarItem? = null
    private var currentArray = arrayOfNulls<CalendarItem?>(DAYS_PER_MONTH)

    private var year: Int = 0
    private var month: Int = 0

    var indexOfFirstDayOfMonth = 0

    interface OnItemClickListener {
        fun onItemClick(item: CalendarItem)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        getAttrs(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        getAttrs(attrs, defStyleAttr)
    }

    private var itemHeight = 0F

    private fun getAttrs(attrs: AttributeSet) {
        applyStyledAttributes(context.obtainStyledAttributes(attrs, R.styleable.CalendarView))
    }

    private fun getAttrs(attrs: AttributeSet, defStyleAttr: Int) {
        applyStyledAttributes(
            context.obtainStyledAttributes(
                attrs,
                R.styleable.CalendarView,
                defStyleAttr,
                0
            )
        )
    }

    private fun applyStyledAttributes(styledAttributes: TypedArray) {
        itemHeight = styledAttributes.getDimension(R.styleable.CalendarView_itemHeight, 80F.toPx)

        styledAttributes.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val measuredHeight = itemHeight * WEEKS_PER_MONTH

        setMeasuredDimension(getDefaultSize(suggestedMinimumWidth, widthMeasureSpec), measuredHeight.toInt())
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = (width / DAYS_PER_WEEK).toFloat()
        val height = itemHeight

        children.forEachIndexed { index, view ->
            val left = (index % DAYS_PER_WEEK) * width
            val top = (index / DAYS_PER_WEEK) * height

            view.layout(left.toInt(), top.toInt(), (left + width).toInt(), (top + height).toInt())
            view.setOnClickListener {
                val item = currentArray[index] ?: return@setOnClickListener
                val position = currentArray[index]?.position ?: -1

                if (selectedPosition == position) {
                    // 뷰페이저 오픈.
                    // 인스턴스 없는 경우 인서트 오픈.
                } else {
                    if (selectedPosition != -1) {
                        getChildAt(selectedPosition).background = null
                    }

                    selectedCalendarItem = item
                    selectedPosition = position
                    view.setBackgroundResource(R.drawable.background_calendar_view_item_selected)
                    // 인스턴스가 있는 경우, 뷰페이저 오픈.
                }

                onItemClickListener?.onItemClick(item)
            }

            view.tag = index
        }
    }

    fun setCalendar(calendar: Model.Calendar) {
        currentArray = calendar.items

        year = calendar.year
        month = calendar.month
        indexOfFirstDayOfMonth = calendar.indexOfFirstDayOfMonth

        removeAllViews()

        repeat(DAYS_PER_MONTH) { index ->
            addView(CalendarItemView(context, currentArray[index]).apply {
                setOnClickListener { currentArray[index]?.let {
                    onItemClickListener?.onItemClick(it)
                } }
            })
        }
    }

    fun select(date: Int) {
        val index = indexOfFirstDayOfMonth + date
        getChildAt(index).also {
            if (it is CalendarItemView) {
                val item = currentArray?.get(index) ?: return
                val position = currentArray?.get(index)?.position ?: -1

                if (selectedPosition == position) {
                    // 뷰페이저 오픈.
                    // 인스턴스 없는 경우 인서트 오픈.
                } else {
                    if (selectedPosition != -1) {
                        getChildAt(selectedPosition).background = null
                    }

                    selectedCalendarItem = item
                    selectedPosition = position
                    it.setBackgroundResource(R.drawable.background_calendar_view_item_selected)
                    // 인스턴스가 있는 경우, 뷰페이저 오픈.
                }
            }
        }
    }
}