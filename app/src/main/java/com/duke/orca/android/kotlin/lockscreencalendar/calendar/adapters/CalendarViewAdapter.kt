package com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.views.CalendarViewFragment
import java.util.*

class CalendarViewAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = ITEM_COUNT

    override fun createFragment(position: Int): Fragment {
        val amount = position - START_POSITION
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MONTH, amount)
        }

        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        return CalendarViewFragment.newInstance(month, year)
    }

    companion object {
        private const val ITEM_COUNT = Int.MAX_VALUE
        const val START_POSITION = ITEM_COUNT / 2
    }
}