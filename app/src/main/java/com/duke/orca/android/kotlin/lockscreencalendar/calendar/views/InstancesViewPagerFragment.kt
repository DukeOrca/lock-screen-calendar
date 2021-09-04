package com.duke.orca.android.kotlin.lockscreencalendar.calendar.views

import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.transition.Explode
import androidx.viewpager2.widget.ViewPager2
import com.duke.orca.android.kotlin.lockscreencalendar.PACKAGE_NAME
import com.duke.orca.android.kotlin.lockscreencalendar.R
import com.duke.orca.android.kotlin.lockscreencalendar.base.BaseFragment
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapter.InstancesViewAdapter
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapter.InstancesViewAdapter.Companion.START_POSITION
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.pagetransformer.PageTransformer
import com.duke.orca.android.kotlin.lockscreencalendar.databinding.ActivityInstancesViewPagerBinding
import com.duke.orca.android.kotlin.lockscreencalendar.main.viewmodel.MainViewModel
import java.util.*
import java.util.concurrent.TimeUnit

class InstancesViewPagerFragment : BaseFragment<ActivityInstancesViewPagerBinding>() {
    override fun inflate(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): ActivityInstancesViewPagerBinding {
        return ActivityInstancesViewPagerBinding.inflate(inflater, container, false)
    }

    object Key {
        private const val PREFIX = "$PACKAGE_NAME.calendar.views" +
                ".InstancesViewPagerFragment.Key"
        const val YEAR = "$PREFIX.YEAR"
        const val MONTH = "$PREFIX.MONTH"
        const val DATE = "$PREFIX.DATE"
    }

    private val viewModel by activityViewModels<MainViewModel>()

    private val adapter by lazy { InstancesViewAdapter(this) }
    private val calendar = Calendar.getInstance()
    private val offscreenPageLimit = 3

    private val onPageChangeCallback by lazy {
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val amount = position - START_POSITION
                val calendar = Calendar.getInstance().apply {
                    add(Calendar.DATE, amount)
                }

                viewModel.selectDate(calendar)
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

    override fun onDestroyView() {
        viewBinding.viewPager2.adapter = null
        super.onDestroyView()
    }

    private fun initializeViews() {
        viewBinding.viewPager2.adapter = adapter
        viewBinding.viewPager2.offscreenPageLimit = offscreenPageLimit
        viewBinding.viewPager2.registerOnPageChangeCallback(onPageChangeCallback)
        viewBinding.viewPager2.setPageTransformer(PageTransformer())
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

        val diff = calendar.timeInMillis - to.timeInMillis
        val addVal = if (diff <  0)
            0
        else
            -1
        //TimeUnit.MILLISECONDS.toDays(millionSeconds)
        val ret = START_POSITION - TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt()

        return ret + addVal
    }
}