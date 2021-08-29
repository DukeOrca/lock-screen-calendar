package com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.views.CalendarViewFragment
import java.util.*

class CalendarViewAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = ITEM_COUNT

    override fun createFragment(position: Int): Fragment {
        val amount = position - START_POSITION
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MONTH, amount)
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)

        return CalendarViewFragment.newInstance(year, month)
    }

    companion object {
        private const val ITEM_COUNT = Int.MAX_VALUE
        const val START_POSITION = ITEM_COUNT / 2
    }
}