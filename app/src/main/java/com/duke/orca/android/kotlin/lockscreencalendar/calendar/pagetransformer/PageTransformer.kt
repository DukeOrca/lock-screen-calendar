package com.duke.orca.android.kotlin.lockscreencalendar.calendar.pagetransformer

import android.util.Log
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

private const val MIN_SCALE = 0.75F
private const val MIN_ALPHA = 0.87F

class PageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        view.apply {
            var p = position
            if (abs(position) < 0.5)
                p = 0.5F

            var scalingFactor = 1.5F - abs(p)

            if (scalingFactor < 1)
                scalingFactor += (1 - scalingFactor) / 2

            var horizontalMargin = width * (1.0F - scalingFactor) / 2F

            scaleX = scalingFactor
            scaleY = scalingFactor

            alpha = (MIN_ALPHA + (((scalingFactor - MIN_SCALE) / (1 - MIN_SCALE)) * (1 - MIN_ALPHA)))
        }
    }
}