package com.duke.orca.android.kotlin.lockscreencalendar.calendar.pagetransformer

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class PageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        view.apply {
            var scalingFactor = if (abs(position) < 0.4F) {
                1F
            } else {
                1.4F - abs(position)
            }

            alpha = scalingFactor

            if (scalingFactor < 1F)
                scalingFactor += (1F - scalingFactor) / 2F

            val left = width * (1F - scalingFactor) / 2F

            translationX = if (position < 0F) {
                left / 2F
            } else {
                -left / 2F
            }

            scaleX = scalingFactor
            scaleY = scalingFactor
        }
    }
}