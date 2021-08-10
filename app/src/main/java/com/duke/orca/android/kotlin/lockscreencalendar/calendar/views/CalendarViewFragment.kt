@file:Suppress("LocalVariableName")

package com.duke.orca.android.kotlin.lockscreencalendar.calendar.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.duke.orca.android.kotlin.lockscreencalendar.PACKAGE_NAME
import com.duke.orca.android.kotlin.lockscreencalendar.base.BaseFragment
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.DAYS_PER_MONTH
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.DAYS_PER_WEEK
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapters.CalendarViewItem
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapters.CalendarViewItemAdapter
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.repository.CalendarRepository
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.getFirstDayOfWeekOfMonth
import com.duke.orca.android.kotlin.lockscreencalendar.databinding.FragmentCalendarViewBinding
import kotlinx.coroutines.*
import java.util.*

class CalendarViewFragment : BaseFragment<FragmentCalendarViewBinding>() {
    override fun inflate(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCalendarViewBinding {
        return FragmentCalendarViewBinding.inflate(inflater, container, false)
    }

    private val adapter by lazy { CalendarViewItemAdapter(currentArray) }
    private val calendarRepository by lazy { CalendarRepository(requireContext().applicationContext) }
    private val currentArray = arrayOfNulls<CalendarViewItem>(DAYS_PER_MONTH)

    private var isDaysOfPreviousMonthVisible = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val month = arguments?.getInt(Key.MONTH) ?: 0
        val year = arguments?.getInt(Key.YEAR) ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.MONTH, month)
            set(Calendar.YEAR, year)
        }

        val previousMonthCalendar = Calendar.getInstance().apply {
            set(Calendar.MONTH, calendar.get(Calendar.MONTH))
            set(Calendar.YEAR, calendar.get(Calendar.YEAR))
            add(Calendar.MONTH, -1)
        }

        val nextMonthCalendar = Calendar.getInstance().apply {
            set(Calendar.MONTH, calendar.get(Calendar.MONTH))
            set(Calendar.YEAR, calendar.get(Calendar.YEAR))
            add(Calendar.MONTH, 1)
        }

        val indexOfFirstDayOfMonth = getFirstDayOfWeekOfMonth(year, month).dec()
        val indexOfLastDayOfMonth = indexOfFirstDayOfMonth + calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val lastDayOfPreviousMonth = previousMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        isDaysOfPreviousMonthVisible = indexOfFirstDayOfMonth != 0

        lifecycleScope.launch {
            addDaysOfPreviousMonth(lastDayOfPreviousMonth, indexOfFirstDayOfMonth)
            addDaysOfMonth(indexOfFirstDayOfMonth, indexOfLastDayOfMonth)
            addDaysOfNextMonth(indexOfLastDayOfMonth)

            initializeViews()

            withContext(Dispatchers.IO) {
                val updatedIndices = arrayListOf<Int>()

                currentArray.filterNotNull().forEach {
                    val DTSTART = Calendar.getInstance().apply {
                        when(it) {
                            is CalendarViewItem.DayOfPreviousMonth -> set(previousMonthCalendar.get(Calendar.YEAR), previousMonthCalendar.get(Calendar.MONTH), it.dayOfMonth)
                            is CalendarViewItem.DayOfMonth -> set(year, month, it.dayOfMonth)
                            is CalendarViewItem.DayOfNextMonth -> set(nextMonthCalendar.get(Calendar.YEAR), nextMonthCalendar.get(Calendar.MONTH), it.dayOfMonth)
                        }
                    }

                    val DTEND = Calendar.getInstance().apply {
                        when(it) {
                            is CalendarViewItem.DayOfPreviousMonth -> set(previousMonthCalendar.get(Calendar.YEAR), previousMonthCalendar.get(Calendar.MONTH), it.dayOfMonth)
                            is CalendarViewItem.DayOfMonth -> set(year, month, it.dayOfMonth)
                            is CalendarViewItem.DayOfNextMonth -> set(nextMonthCalendar.get(Calendar.YEAR), nextMonthCalendar.get(Calendar.MONTH), it.dayOfMonth)
                        }
                    }

                    DTSTART.set(Calendar.HOUR_OF_DAY, 0)
                    DTSTART.set(Calendar.MINUTE, 0)
                    DTSTART.set(Calendar.SECOND, 0)

                    DTEND.set(Calendar.HOUR_OF_DAY, 23)
                    DTEND.set(Calendar.MINUTE, 59)
                    DTEND.set(Calendar.SECOND, 59)

                    with(calendarRepository.instances(DTSTART, DTEND)) {
                        if (isNotEmpty()) {
                            it.instances.addAll(this)
                            updatedIndices.add(it.position)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    updatedIndices.forEach {
                        adapter.notifyItemChanged(it)
                    }
                }
            }
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
            currentArray[i] = CalendarViewItem.DayOfPreviousMonth(j, position = i)
        }
    }

    private fun addDaysOfMonth(indexOfFirstDayOfMonth: Int, indexOfLastDayOfMonth: Int) {
        for ((i, j) in (indexOfFirstDayOfMonth until indexOfLastDayOfMonth).withIndex()) {
            currentArray[j] = CalendarViewItem.DayOfMonth(i.inc(), position = j)
        }
    }

    private fun addDaysOfNextMonth(indexOfLastDayOfMonth: Int) {
        for ((i, j) in (indexOfLastDayOfMonth until DAYS_PER_MONTH).withIndex()) {
            currentArray[j] = CalendarViewItem.DayOfNextMonth(i.inc(), position = j)
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