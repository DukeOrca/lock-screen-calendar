@file:Suppress("LocalVariableName")

package com.duke.orca.android.kotlin.lockscreencalendar.calendar.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.duke.orca.android.kotlin.lockscreencalendar.PACKAGE_NAME
import com.duke.orca.android.kotlin.lockscreencalendar.base.BaseFragment
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.DAYS_PER_MONTH
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.DAYS_PER_WEEK
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapters.CalendarItem
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapters.CalendarItemAdapter
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.repository.CalendarRepository
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.getFirstDayOfWeekOfMonth
import com.duke.orca.android.kotlin.lockscreencalendar.databinding.FragmentCalendarViewBinding
import com.duke.orca.android.kotlin.lockscreencalendar.main.viewmodel.MainViewModel
import kotlinx.coroutines.*
import java.util.*

class CalendarViewFragment : BaseFragment<FragmentCalendarViewBinding>() {
    override fun inflate(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCalendarViewBinding {
        return FragmentCalendarViewBinding.inflate(inflater, container, false)
    }

    private val adapter by lazy { CalendarItemAdapter(currentArray) }
    private val calendarRepository by lazy { CalendarRepository(requireContext().applicationContext) }
    private val currentArray = arrayOfNulls<CalendarItem>(DAYS_PER_MONTH)
    private val viewModel by activityViewModels<MainViewModel>()

    private var isDaysOfPreviousMonthVisible = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val month = arguments?.getInt(Key.MONTH) ?: 0
        val year = arguments?.getInt(Key.YEAR) ?: 0

        viewModel.calendarRepository.get(year, month)?.observe(viewLifecycleOwner, {
            viewBinding.calendarView.init(it)
        }) ?: let {
            // 로드 요청필수.
            println("what the fuck: $month")
        }

        return viewBinding.root
    }

    private fun initializeViews() {
        viewBinding.recyclerView.apply {
            adapter = this@CalendarViewFragment.adapter
            itemAnimator = null
            layoutManager = GridLayoutManager(requireContext(), DAYS_PER_WEEK)
            setHasFixedSize(true)
            setItemViewCacheSize(DAYS_PER_MONTH)
        }
    }

    private fun addDaysOfPreviousMonth(lastDayOfPreviousMonth: Int, indexOfFirstDayOfMonth: Int) {
        if (isDaysOfPreviousMonthVisible.not()) {
            return
        }

        val from = lastDayOfPreviousMonth - indexOfFirstDayOfMonth.dec()

        for ((i, j) in (from .. lastDayOfPreviousMonth).withIndex()) {
            currentArray[i] = CalendarItem.DayOfPreviousMonth(j, position = i)
        }
    }

    private fun addDaysOfMonth(indexOfFirstDayOfMonth: Int, indexOfLastDayOfMonth: Int) {
        for ((i, j) in (indexOfFirstDayOfMonth until indexOfLastDayOfMonth).withIndex()) {
            currentArray[j] = CalendarItem.DayOfMonth(i.inc(), position = j)
        }
    }

    private fun addDaysOfNextMonth(indexOfLastDayOfMonth: Int) {
        for ((i, j) in (indexOfLastDayOfMonth until DAYS_PER_MONTH).withIndex()) {
            currentArray[j] = CalendarItem.DayOfNextMonth(i.inc(), position = j)
        }
    }

    companion object {
        private object Key {
            private const val PREFIX = "$PACKAGE_NAME.CalendarViewFragment.companion.KEY"
            const val MONTH = "$PREFIX.MONTH"
            const val YEAR = "$PREFIX.YEAR"
        }

        fun newInstance(month: Int, year: Int): CalendarViewFragment {
            return CalendarViewFragment().apply {
                arguments = Bundle().apply {
                    putInt(Key.MONTH, month)
                    putInt(Key.YEAR, year)
                }
            }
        }
    }
}