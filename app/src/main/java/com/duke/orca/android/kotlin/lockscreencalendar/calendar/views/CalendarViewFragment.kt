@file:Suppress("LocalVariableName")

package com.duke.orca.android.kotlin.lockscreencalendar.calendar.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.duke.orca.android.kotlin.lockscreencalendar.PACKAGE_NAME
import com.duke.orca.android.kotlin.lockscreencalendar.base.BaseFragment
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.CalendarItem
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.Model
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarView
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

    private val viewModel by activityViewModels<MainViewModel>()

    private var year = 0
    private var month = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        year = arguments?.getInt(Key.YEAR) ?: 0
        month = arguments?.getInt(Key.MONTH) ?: 0

        viewModel.repository.get(year, month)?.observe(viewLifecycleOwner, { calendar ->
            viewBinding.calendarView.setCalendar(calendar)
            viewBinding.calendarView.setOnItemClickListener(object : CalendarView.OnItemClickListener {
                override fun onItemClick(item: CalendarItem) {
                    if (viewModel.selectedItem == item) {
                        if (item.instances.isEmpty()) {
                            viewModel.insertEvent(item.year, item.month, item.date)
                        } else {
                            Calendar.getInstance().apply {
                                set(Calendar.YEAR, item.year)
                                set(Calendar.MONTH, item.month)
                                set(Calendar.DATE, item.date)

                                viewModel.callShowEvents(item)
                            }
                        }
                    } else {
                        viewModel.selectedItem = item

                        if (item.instances.isEmpty()) {
                            // pass
                        } else {
                            Calendar.getInstance().apply {
                                set(Calendar.YEAR, item.year)
                                set(Calendar.MONTH, item.month)
                                set(Calendar.DATE, item.date)

                                viewModel.callShowEvents(item)
                            }
                        }
                    }

                }
            })
        }) ?: let {
            viewModel.repository.put(year, month) {
                it?.observe(viewLifecycleOwner, { calendar ->
                    viewBinding.calendarView.setCalendar(calendar)
                    viewBinding.calendarView.setOnItemClickListener(object : CalendarView.OnItemClickListener {
                        override fun onItemClick(item: CalendarItem) {
                            viewModel.selectedItem = item

                            Calendar.getInstance().apply {
                                set(Calendar.YEAR, item.year)
                                set(Calendar.MONTH, item.month)
                                set(Calendar.DATE, item.date)

                                viewModel.callShowEvents(item)
                            }
                        }
                    })
                })
            }
        }

        observe()

        return viewBinding.root
    }

    private fun observe() {
        viewModel.refresh.observe(viewLifecycleOwner, {
            viewModel.repository.put(year, month)
        })

        viewModel.selectedDate.observe(viewLifecycleOwner, {
            val year = it.get(Calendar.YEAR)
            val month = it.get(Calendar.MONTH)
            val date = it.get(Calendar.DATE)

            if (year == this.year && month == this.month) {
                viewBinding.calendarView.select(date.dec())
            }
        })
    }

    companion object {
        private object Key {
            private const val PREFIX = "$PACKAGE_NAME.CalendarViewFragment.companion.KEY"
            const val YEAR = "$PREFIX.YEAR"
            const val MONTH = "$PREFIX.MONTH"
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