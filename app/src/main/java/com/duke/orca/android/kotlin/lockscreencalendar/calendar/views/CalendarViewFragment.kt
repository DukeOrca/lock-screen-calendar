@file:Suppress("LocalVariableName")

package com.duke.orca.android.kotlin.lockscreencalendar.calendar.views

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.transition.Explode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.duke.orca.android.kotlin.lockscreencalendar.PACKAGE_NAME
import com.duke.orca.android.kotlin.lockscreencalendar.base.BaseFragment
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.VISIBLE_INSTANCE_COUNT
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.WEEKS_PER_MONTH
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.CalItem2
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarItemView
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarView
import com.duke.orca.android.kotlin.lockscreencalendar.databinding.FragmentCalendarViewBinding
import com.duke.orca.android.kotlin.lockscreencalendar.main.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
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

        viewBinding.calendarView.tag = year * 100 + month

        val tcm = CalendarView.CalendarMap(year, month)
        lifecycleScope.launch {
            val inst = viewModel.getInstances(year, month)

            repeat(WEEKS_PER_MONTH) { weekOfMonth -> // 한달의 주를 반복.
                val weekMap = inst[weekOfMonth] ?: emptyList()

                for (instance in weekMap) {
                    val beginYearMonthDay = instance.beginYearMonthDay

                    val week = tcm.linkedHashMap[weekOfMonth] ?: continue
                    val firstDay = week.getFirstDay() ?: continue // 그 주의 시작 시간.

                    if (beginYearMonthDay < firstDay.yearMonthDay) { // 시작일이 이번 주 보다 빠른 경우,
                        // 첫 번째 날에 삽입한다.
                        var j = -1

                        firstDay.visibleInstances.apply {
                            for (i in 0 until VISIBLE_INSTANCE_COUNT) {
                                if (get(i) == null) {
                                    set(i, instance)
                                    j = i
                                    break
                                }
                            }
                        }

                        var nextKey = firstDay.nextKey
                        var spanCount = 1

                        for (i in 0 until instance.duration) {
                            val nextDay = week.dates[nextKey] ?: break

                            if (instance.endYearMonthDay < nextDay.yearMonthDay) break

                            ++spanCount

                            if (j in 0 until VISIBLE_INSTANCE_COUNT) {
                                nextDay.visibleInstances[j] = instance.copy().apply {
                                    isVisible = false
                                }

                                nextKey = nextDay.nextKey
                            } else {
                                break
                            }
                        }

                        if (j in 0 until VISIBLE_INSTANCE_COUNT) {
                            firstDay.visibleInstances[j]?.spanCount = spanCount
                        }

                    } else {
                        // 이번 주에 시작일이 있는 경우.
                        val day = week.dates[beginYearMonthDay] ?: continue // 캘린더 아이템.
                        var j = -1

                        day.visibleInstances.apply {
                            for (i in 0 until VISIBLE_INSTANCE_COUNT) {
                                if (get(i) == null) {
                                    set(i, instance)
                                    j = i
                                    break
                                }
                            }
                        }

                        var nextKey = day.nextKey
                        var spanCount = 1

                        for (i in 0 until instance.duration) {
                            val nextDay = week.dates[nextKey] ?: break

                            if (instance.endYearMonthDay < nextDay.yearMonthDay) break

                            ++spanCount

                            if (j in 0 until VISIBLE_INSTANCE_COUNT) {
                                nextDay.visibleInstances[j] = instance.copy().apply {
                                    isVisible = false
                                }

                                nextKey = nextDay.nextKey
                            } else {
                                break
                            }
                        }

                        if (j in 0 until VISIBLE_INSTANCE_COUNT) {
                            day.visibleInstances[j]?.spanCount = spanCount
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                viewBinding.calendarView.set(tcm)
            }
        }

//        viewModel.refresh.observe(viewLifecycleOwner, {
//            loaderManager.forceLoad()
//        })

        observe()
        viewBinding.calendarView.setOnItemClickListener(object : CalendarView.OnItemClickListener {
            override fun onItemClick(view: CalendarItemView, item: CalItem2) {

//                val activityOptions = ActivityOptions.makeSceneTransitionAnimation(
//                    requireActivity(), view, "transition_name"
//                )
//                val intent = Intent(requireContext(), InstancesViewPagerActivity::class.java).apply {
//                    putExtra(InstancesViewPagerActivity.Key.YEAR, item.year)
//                    putExtra(InstancesViewPagerActivity.Key.MONTH, item.month)
//                    putExtra(InstancesViewPagerActivity.Key.DATE, item.date)
//                }
//                Timber.tag("sjk")
//                Timber.d("king : ${item.yearMonthDay}")
//                startActivity(intent, activityOptions.toBundle())

                viewModel.callShowEvents(item)
            }
        })

        return viewBinding.root
    }

    private fun observe() {
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