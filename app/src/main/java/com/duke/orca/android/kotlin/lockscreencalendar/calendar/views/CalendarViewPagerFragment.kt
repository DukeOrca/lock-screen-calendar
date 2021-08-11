package com.duke.orca.android.kotlin.lockscreencalendar.calendar.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.duke.orca.android.kotlin.lockscreencalendar.R
import com.duke.orca.android.kotlin.lockscreencalendar.base.BaseFragment
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapters.CalendarViewAdapter
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapters.CalendarViewAdapter.Companion.START_POSITION
import com.duke.orca.android.kotlin.lockscreencalendar.databinding.FragmentCalendarViewPagerBinding
import com.duke.orca.android.kotlin.lockscreencalendar.main.viewmodel.MainViewModel
import java.util.*

class CalendarViewPagerFragment : BaseFragment<FragmentCalendarViewPagerBinding>() {
    override fun inflate(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCalendarViewPagerBinding {
        return FragmentCalendarViewPagerBinding.inflate(inflater, container, false)
    }

    private val mainViewModel by activityViewModels<MainViewModel>()

    private val adapter by lazy { CalendarViewAdapter(requireActivity()) }
    private val months by lazy { resources.getStringArray(R.array.months) }

    private val onPageChangeCallback by lazy {
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val amount = position - START_POSITION
                val calendar = Calendar.getInstance().apply {
                    add(Calendar.MONTH, amount)
                }

                val month = calendar.get(Calendar.MONTH)
                val year = calendar.get(Calendar.YEAR)

                viewBinding.textViewMonth.text = months[month]
                "$year${getString(R.string.year)}".also {
                    viewBinding.textViewYear.text = it
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        initializeViews()
        initializeObservers()

        return viewBinding.root
    }

    override fun onDestroyView() {
        mainViewModel.refresh.clear()
        viewBinding.viewPager2.unregisterOnPageChangeCallback(onPageChangeCallback)
        super.onDestroyView()
    }

    private fun initializeViews() {
        viewBinding.viewPager2.adapter = adapter
        viewBinding.viewPager2.registerOnPageChangeCallback(onPageChangeCallback)
        viewBinding.viewPager2.setCurrentItem(START_POSITION, false)
    }

    private fun initializeObservers() {
        mainViewModel.refresh.observe(viewLifecycleOwner, {
            viewBinding.viewPager2.adapter = adapter
            viewBinding.viewPager2.setCurrentItem(START_POSITION, false)
        })
    }
}