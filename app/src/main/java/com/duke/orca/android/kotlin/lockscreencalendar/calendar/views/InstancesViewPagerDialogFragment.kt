package com.duke.orca.android.kotlin.lockscreencalendar.calendar.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.duke.orca.android.kotlin.lockscreencalendar.PACKAGE_NAME
import com.duke.orca.android.kotlin.lockscreencalendar.base.BaseFragment
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapter.CalendarViewAdapter
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapter.InstancesViewAdapter
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapter.InstancesViewAdapter.Companion.START_POSITION
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.transformer.ZoomOutPageTransformer
import com.duke.orca.android.kotlin.lockscreencalendar.databinding.FragmentInstancesViewPagerBinding
import com.duke.orca.android.kotlin.lockscreencalendar.main.viewmodel.MainViewModel
import java.util.*
import java.util.concurrent.TimeUnit

class InstancesViewPagerDialogFragment : BaseFragment<FragmentInstancesViewPagerBinding>() {
    override fun inflate(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentInstancesViewPagerBinding {
        return FragmentInstancesViewPagerBinding.inflate(inflater, container, false)
    }

    object Key {
        const val YEAR = "$PREFIX.Key.YEAR"
        const val MONTH = "$PREFIX.Key.MONTH"
        const val DATE = "$PREFIX.Key.DATE"
    }

    private val viewModel by activityViewModels<MainViewModel>()

    private val adapter by lazy { InstancesViewAdapter(requireActivity()) }
    private val calendar = Calendar.getInstance()
    private val offscreenPageLimit = 6

    private val onPageChangeCallback by lazy {
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val amount = position - CalendarViewAdapter.START_POSITION
                val calendar = Calendar.getInstance().apply {
                    add(Calendar.DATE, amount)
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

        val year = arguments?.getInt(Key.YEAR) ?: 0
        val month = arguments?.getInt(Key.MONTH) ?: 0
        val date = arguments?.getInt(Key.DATE) ?: 0

        initializeViews()
        viewBinding.viewPager2.setCurrentItem(startPosition(year, month, date), false)
        return viewBinding.root
    }

    private fun initializeViews() {
        viewBinding.viewPager2.adapter = adapter
        viewBinding.viewPager2.offscreenPageLimit = 3
        viewBinding.viewPager2.registerOnPageChangeCallback(onPageChangeCallback)
        viewBinding.viewPager2.setPageTransformer(ZoomOutPageTransformer())
    }

    private fun startPosition(year: Int, month: Int, date: Int): Int {
        val to = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DATE, date)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }

        calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }

        val year1 = calendar.get(Calendar.YEAR)
        val month1 = calendar.get(Calendar.MONTH) // current year, month.
        val date1 = calendar.get(Calendar.DATE)

        val year2 = to.get(Calendar.YEAR)
        val month2 = to.get(Calendar.MONTH)
        val date2 = to.get(Calendar.DATE)

        val diff = calendar.timeInMillis - to.timeInMillis
        //TimeUnit.MILLISECONDS.toDays(millionSeconds)

        return START_POSITION - TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt()
    }

    companion object {
        private const val PREFIX = "$PACKAGE_NAME.calendar.views.InstancesViewPagerDialogFragment" +
                ".companion.PREFIX"
        private const val OFFSCREEN_PAGE_LIMIT = 6
    }
}