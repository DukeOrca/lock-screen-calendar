package com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.views.InstancesViewFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class InstancesViewAdapter2(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = ITEM_COUNT

    override fun createFragment(position: Int): Fragment {
        val amount = position - START_POSITION
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DATE, amount)
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val date = calendar.get(Calendar.DATE)

        return InstancesViewFragment.newInstance(year, month, date)
    }

    companion object {
        private const val ITEM_COUNT = Int.MAX_VALUE
        const val START_POSITION = ITEM_COUNT / 2
    }
}