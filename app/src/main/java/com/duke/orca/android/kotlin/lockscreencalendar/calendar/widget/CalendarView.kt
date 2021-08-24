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
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.Model
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.Model.CalendarItem
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.getFirstDayOfWeekOfMonth
import com.duke.orca.android.kotlin.lockscreencalendar.util.toPx
import java.util.*

class CalendarView : ViewGroup {
    private var onItemClickListener: OnItemClickListener? = null

    private var selectedPosition = -1
    private var selectedCalendarItem: CalendarItem? = null
    private var currentArray = arrayOfNulls<CalendarItem?>(DAYS_PER_MONTH)

    private var year: Int = 0
    private var previousMonth: Int = 0
    private var month: Int = 0
    private var nextMonth: Int = 0

    private lateinit var calendar: Calendar
    private lateinit var previousCalendar: Calendar
    private lateinit var nextCalendar: Calendar

    var indexOfFirstDayOfMonth = 0

    interface OnItemClickListener {
        fun onItemClick(item: CalendarItem)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    data class ViewHolder(val calendarView: CalendarView, val calendarItem: CalendarItem)

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

//    fun init(year: Int, month: Int) {
//        calendar = Calendar.getInstance().apply {
//            set(Calendar.YEAR, year)
//            set(Calendar.MONTH, month)
//        }
//
//        previousCalendar = Calendar.getInstance().apply {
//            set(Calendar.YEAR, calendar.get(Calendar.YEAR))
//            set(Calendar.MONTH, calendar.get(Calendar.MONTH))
//            add(Calendar.MONTH, -1)
//        }
//
//        nextCalendar = Calendar.getInstance().apply {
//            set(Calendar.YEAR, calendar.get(Calendar.YEAR))
//            set(Calendar.MONTH, calendar.get(Calendar.MONTH))
//            add(Calendar.MONTH, 1)
//        }
//
//        this.year = year
//        previousMonth = previousCalendar.get(Calendar.MONTH)
//        this.month = month
//        nextMonth = nextCalendar.get(Calendar.MONTH)
//
//        val indexOfFirstDayOfMonth = getFirstDayOfWeekOfMonth(year, month).dec()
//        val indexOfLastDayOfMonth = indexOfFirstDayOfMonth + calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
//
//        val lastDayOfPreviousMonth = previousCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
//        val firstVisibleDayOfPreviousMonth = lastDayOfPreviousMonth - indexOfFirstDayOfMonth
//
//        addDaysOfPreviousMonth(currentArray, lastDayOfPreviousMonth, indexOfFirstDayOfMonth)
//        addDaysOfMonth(currentArray, indexOfFirstDayOfMonth, indexOfLastDayOfMonth)
//        addDaysOfNextMonth(currentArray, indexOfLastDayOfMonth)
//
//        repeat(DAYS_PER_MONTH) { index ->
//            addView(CalendarItemView(context, currentArray[index]).apply {
//                setOnClickListener { currentArray[index]?.let {
//                    onItemClickListener?.onItemClick(it)
//                } }
//            })
//        }
//    }

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

    fun setInstances(list: List<Model.Instance?>) {
        //this.currentArray = currentArray

//        repeat(DAYS_PER_MONTH) { index ->
//            addView(CalendarItemView(context, currentArray[index]).apply {
//                setOnClickListener { currentArray[index]?.let {
//                    onItemClickListener?.onItemClick(it)
//                } }
//            })
//        }

        indexOfFirstDayOfMonth = getFirstDayOfWeekOfMonth(year, month).dec()
        val indexOfLastDayOfMonth = indexOfFirstDayOfMonth + calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val lastDayOfPreviousMonth = previousCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstVisibleDayOfPreviousMonth = lastDayOfPreviousMonth - indexOfFirstDayOfMonth

        currentArray.filterNotNull().forEach {
            it.instances.clear()
            it.visibleInstances.forEachIndexed { index, instance ->
                it.visibleInstances[index] = null
            }
        }

        list.filterNotNull().forEach { instance ->
            val beginDayOfMonth = instance.beginDayOfMonth
            var endDayOfMonth = instance.endDayOfMonth
            var duration = endDayOfMonth - beginDayOfMonth
            var fillBackground = instance.isAllDay || (duration > 0)

            val index = when(instance.month) {
                previousMonth -> beginDayOfMonth.dec() - firstVisibleDayOfPreviousMonth
                nextMonth -> beginDayOfMonth + indexOfLastDayOfMonth.dec()
                else -> beginDayOfMonth.dec() + indexOfFirstDayOfMonth
            }

            if (index in 0 until DAYS_PER_MONTH) {
                currentArray[index]?.instances?.add(instance)

                val k = currentArray[index]?.instances?.indexOf(instance) ?: -1

                if (k in 0 until VISIBLE_INSTANCE_COUNT) {
                    currentArray[index]?.visibleInstances?.let { visibleInstances ->
                        for (i in 0 until VISIBLE_INSTANCE_COUNT) {
                            if (visibleInstances[i] == null) {
                                visibleInstances[i] = instance
                                break
                            }
                        }
                    }

                    if (instance.duration > 0) {
                        for (i in 1..instance.duration) {
                            val j = index + i

                            if (j in 0 until DAYS_PER_MONTH) {
                                if (instance.isAllDay.not()) {
                                    instance.deepCopy().apply {
                                        this.beginDayOfMonth = beginDayOfMonth + i
                                        this.duration = endDayOfMonth - (beginDayOfMonth + i)
                                        isVisible = (j % DAYS_PER_WEEK) == Calendar.SUNDAY.dec()
                                    }.also {
                                        currentArray[j]?.instances?.add(it)
                                        currentArray[j]?.visibleInstances?.set(k, it)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        children.forEachIndexed { index, view ->
            if (view is CalendarItemView) {
                currentArray[index]?.let {
                    view.setData(it)
                }
            }
        }
    }

    private fun addDaysOfPreviousMonth(calendarItems: Array<CalendarItem?>, lastDayOfPreviousMonth: Int, indexOfFirstDayOfMonth: Int) {
        if (indexOfFirstDayOfMonth == 0) {
            return
        }

        val from = lastDayOfPreviousMonth - indexOfFirstDayOfMonth.dec()

        for ((i, j) in (from .. lastDayOfPreviousMonth).withIndex()) {
            calendarItems[i] = Model.CalendarItem.DayOfPreviousMonth(year = year,
                month = month, j, position = i)
        }
    }

    private fun addDaysOfMonth(calendarItems: Array<CalendarItem?>, indexOfFirstDayOfMonth: Int, indexOfLastDayOfMonth: Int) {
        for ((i, j) in (indexOfFirstDayOfMonth until indexOfLastDayOfMonth).withIndex()) {
            calendarItems[j] = CalendarItem.DayOfMonth(
                year = year,
                month = month,
                date = i.inc(),
                position = j)
        }
    }

    private fun addDaysOfNextMonth(calendarItems: Array<CalendarItem?>, indexOfLastDayOfMonth: Int) {
        for ((i, j) in (indexOfLastDayOfMonth until DAYS_PER_MONTH).withIndex()) {
            calendarItems[j] = CalendarItem.DayOfNextMonth(
                year = year,
                month = month,
                i.inc(), position = j)
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

    companion object {
        const val VISIBLE_INSTANCE_COUNT = 3
    }
}