@file:Suppress("LocalVariableName")

package com.duke.orca.android.kotlin.lockscreencalendar.calendar.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.duke.orca.android.kotlin.lockscreencalendar.PACKAGE_NAME
import com.duke.orca.android.kotlin.lockscreencalendar.base.BaseFragment
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.DAYS_PER_MONTH
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.MONTHS_PER_YEAR
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapter.CalendarItem
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapter.CalendarItemAdapter
import com.duke.orca.android.kotlin.lockscreencalendar.databinding.FragmentCalendarViewBinding
import com.duke.orca.android.kotlin.lockscreencalendar.main.viewmodel.MainViewModel
import java.util.*

class CalendarViewFragment : BaseFragment<FragmentCalendarViewBinding>() {
    override fun inflate(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCalendarViewBinding {
        return FragmentCalendarViewBinding.inflate(inflater, container, false)
    }

    private val adapter by lazy { CalendarItemAdapter(currentArray) }
    private val currentArray = arrayOfNulls<CalendarItem>(DAYS_PER_MONTH)
    private val viewModel by activityViewModels<MainViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val year = arguments?.getInt(Key.YEAR) ?: 0
        val month = arguments?.getInt(Key.MONTH) ?: 0

        viewModel.calendarRepository.getCalendarItems(year, month)?.observe(viewLifecycleOwner, {
            viewBinding.calendarView.init(it)
        }) ?: let {
            viewModel.calendarRepository.setCalendarItems(year, month) {
                Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    add(Calendar.MONTH, -MONTHS_PER_YEAR.inc())
                    viewModel.calendarRepository.clear(get(Calendar.YEAR), get(Calendar.MONTH))
                }

                Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    add(Calendar.MONTH, MONTHS_PER_YEAR.inc())
                    viewModel.calendarRepository.clear(get(Calendar.YEAR), get(Calendar.MONTH))
                }

                it?.observe(viewLifecycleOwner, { calendarItems ->
                    viewBinding.calendarView.init(calendarItems)
                })
            }

            viewModel.calendarRepository.load(year, month)
        }

        return viewBinding.root
    }

    companion object {
        private object Key {
            private const val PREFIX = "$PACKAGE_NAME.CalendarViewFragment.companion.KEY"
            const val MONTH = "$PREFIX.MONTH"
            const val YEAR = "$PREFIX.YEAR"
        }

        fun newInstance(year: Int, month: Int): CalendarViewFragment {
            return CalendarViewFragment().apply {
                arguments = Bundle().apply {
                    putInt(Key.YEAR, year)
                    putInt(Key.MONTH, month)
                }
            }
        }
    }
}