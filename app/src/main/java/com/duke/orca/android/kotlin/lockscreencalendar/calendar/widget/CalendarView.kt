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
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapter.CalendarItem
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.Model
import com.duke.orca.android.kotlin.lockscreencalendar.util.toPx

class CalendarView : ViewGroup {
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

    fun init(currentArray: Array<CalendarItem?>) {
        repeat(DAYS_PER_MONTH) { index ->
            addView(
                CalendarItemView(context, currentArray[index])
//                CalendarItemView(context, currentArray[index]).apply {
//                    setOnClickListener {
//
//                        if (it is ViewGroup) {
//                            this@CalendarView.getChildAt(0).also { v ->
//                                val tv = TextView(context).apply {
//                                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
//
//                                    setBackgroundColor(Color.GREEN)
//                                    text = "ttt"
//                                    tag = index // addView 에서 requestLayout 호출함
//                                }
//                                if (v is CalendarItemView) {
//                                    v.addView(tv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))
//                                    v.notiadd(tv)
//                                    //v.setBackgroundColor(Color.YELLOW)
//                                    Toast.makeText(context, "hi? ${tv.tag}", Toast.LENGTH_SHORT).show()
//                                }
//                            }
//
//                        }
//                    }
//                }
                //TextView(context).apply { text = "a" }
                //CalendarItemViewBinding.inflate(LayoutInflater.from(context), this, false).root
            )
        }
    }

    fun s(currentArray: Array<Model.Instance>) {
        currentArray.forEach {

        }
    }
}