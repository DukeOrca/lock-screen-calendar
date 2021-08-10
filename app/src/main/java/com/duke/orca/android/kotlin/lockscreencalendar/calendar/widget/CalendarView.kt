package com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.ViewGroup
import com.duke.orca.android.kotlin.lockscreencalendar.R

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
    private var itemTextSize = 0F

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
        itemHeight = styledAttributes.getDimension(R.styleable.CalendarView_itemHeight, 0F)
        itemTextSize = styledAttributes.getDimension(R.styleable.CalendarView_itemTextSize, 0F)

        styledAttributes.recycle()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        TODO("Not yet implemented")
    }
}