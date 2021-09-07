package com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import com.duke.orca.android.kotlin.lockscreencalendar.R
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.DAYS_PER_WEEK
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.VISIBLE_INSTANCE_COUNT
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.WEEKS_PER_MONTH
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.*
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.getYearMonthDay
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.toDate
import com.duke.orca.android.kotlin.lockscreencalendar.util.toPx
import timber.log.Timber
import java.util.*

class CalendarView : ViewGroup {
    private var onItemClickListener: OnItemClickListener? = null

    private var selectedPosition = -1
    private var selectedCalendarItem: CalItem2? = null
    private lateinit var calendarMap : CalendarMap

    private val pairMap = linkedMapOf<Int, Pair<CalItem2, CalendarItemView>>()

    interface OnItemClickListener {
        fun onItemClick(view: CalendarItemView, item: CalItem2)
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
        }
    }

    fun select(date: Int) {
//        val index = indexOfFirstDayOfMonth + date
//        getChildAt(index).also {
//            if (it is CalendarItemView) {
//                val item = currentArray[index] ?: return
//                val position = currentArray[index]?.position ?: -1
//
//                if (selectedPosition == position) {
//                    // 뷰페이저 오픈.
//                    // 인스턴스 없는 경우 인서트 오픈.
//                } else {
//                    if (selectedPosition != -1) {
//                        getChildAt(selectedPosition).background = null
//                    }
//
//                    it.setBackgroundResource(R.drawable.background_calendar_view_item_selected)
//
//                    selectedCalendarItem = item
//                    selectedPosition = position
//
//                    // 인스턴스가 있는 경우, 뷰페이저 오픈.
//                }
//            }
//        }
    }

    fun getView(key: Int) = pairMap[key]?.second

    fun set(calendarMap: CalendarMap) {
        this.calendarMap = calendarMap
        removeAllViews()

        repeat(WEEKS_PER_MONTH) {
            val dates = this.calendarMap.linkedHashMap[it]?.dates ?: emptyMap()

            dates.forEach { entry ->
                val item = entry.value

                val view = CalendarItemView(context, entry.value).apply {
                    tag = item.yearMonthDay

                }

                pairMap[item.yearMonthDay] = item to view

                view.setOnClickListener {
                    if (selectedPosition != item.yearMonthDay) {
                        pairMap[selectedPosition]?.second?.background = null
                    }

                    it.setBackgroundResource(R.drawable.background_calendar_view_item_selected)

                    selectedPosition = item.yearMonthDay
                    selectedCalendarItem = item

                    onItemClickListener?.onItemClick(view, item)
                }

                addView(view)
            }
        }
    }

    fun unselect() {
        if (selectedPosition != -1) {
            getChildAt(selectedPosition).background = null
            selectedPosition = -1
        }
    }

    class CalendarMap(val year: Int, val month: Int) {
        val linkedHashMap = linkedMapOf<Int, AdapterItem.Week>()

        fun get(weekOfMonth: Int): AdapterItem.Week? {
            if (weekOfMonth in 0 until WEEKS_PER_MONTH) {
                return linkedHashMap[weekOfMonth]
            } else {
                throw IndexOutOfBoundsException()
            }
        }

        fun setInstances() {

        }

        init {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.WEEK_OF_MONTH, 1)
                //set(Calendar.DATE, 1)
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 1)
            }

            Timber.tag("sjk")
            Timber.d("caldddd: ${calendar.get(Calendar.MONTH)} ${calendar.get(Calendar.DATE)}")

            for (i in 0 until WEEKS_PER_MONTH) {
                val week = AdapterItem.Week(linkedMapOf())

                linkedHashMap[i] = week

                for (j in 0 until DAYS_PER_WEEK) {
                    val key = calendar.getYearMonthDay()
                    val date = calendar.get(Calendar.DATE)
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    val month = calendar.get(Calendar.MONTH)
                    val year = calendar.get(Calendar.YEAR)

                    calendar.add(Calendar.DATE, 1)
                    week.dates[key] = (CalItem2(
                        position = i * 7 + j,
                        date = date,
                        dayOfWeek = dayOfWeek,
                        month = month,
                        year = year,
                        nextKey = calendar.getYearMonthDay()
                    ))
                }
            }
        }

        fun createView(context: Context, item: CalItem2): CalendarItemView {
            return CalendarItemView(context, item)
        }
    }
}