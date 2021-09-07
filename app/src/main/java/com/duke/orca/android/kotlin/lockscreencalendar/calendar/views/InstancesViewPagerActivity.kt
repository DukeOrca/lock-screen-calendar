package com.duke.orca.android.kotlin.lockscreencalendar.calendar.views

import android.os.Bundle
import android.transition.Transition
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.duke.orca.android.kotlin.lockscreencalendar.PACKAGE_NAME
import com.duke.orca.android.kotlin.lockscreencalendar.base.BaseActivity
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapter.InstancesViewAdapter
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.pagetransformer.PageTransformer
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.viewmodel.InstancesViewModel
import com.duke.orca.android.kotlin.lockscreencalendar.databinding.ActivityInstancesViewPagerBinding
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.transition.Explode
import com.duke.orca.android.kotlin.lockscreencalendar.base.BaseFragment
import timber.log.Timber


class InstancesViewPagerActivity : BaseActivity<ActivityInstancesViewPagerBinding>() {
    override fun inflate(inflater: LayoutInflater): ActivityInstancesViewPagerBinding {
        return ActivityInstancesViewPagerBinding.inflate(inflater)
    }

    private val viewModel by viewModels<InstancesViewModel>()

    object Key {
        private const val PREFIX = "$PACKAGE_NAME.calendar.views" +
                ".InstancesViewPagerFragment.Key"
        const val YEAR = "$PREFIX.YEAR"
        const val MONTH = "$PREFIX.MONTH"
        const val DATE = "$PREFIX.DATE"
    }

    private val adapter by lazy { InstancesViewAdapter(this) }
    private val calendar = Calendar.getInstance()
    private val offscreenPageLimit = 3

    private val onPageChangeCallback by lazy {
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                //startPostponedEnterTransition()
                super.onPageSelected(position)
                val amount = position - InstancesViewAdapter.START_POSITION
                val calendar = Calendar.getInstance().apply {
                    add(Calendar.DATE, amount)
                }

                //viewModel.selectDate(calendar) rx로 대체.
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding.viewPager2.transitionName = "transition_name"

        window?.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        setFinishOnTouchOutside(true)
        val year = intent?.getIntExtra(Key.YEAR, 0) ?: 0
        val month = intent?.getIntExtra(Key.MONTH, 0) ?: 0
        val date = intent?.getIntExtra(Key.DATE, 0) ?: 0

        window.sharedElementEnterTransition = android.transition.Explode()

        initializeViews()
        viewBinding.viewPager2.setCurrentItem(startPosition(year, month, date), false)
    }

    private fun initializeViews() {
        viewBinding.viewPager2.adapter = adapter
        viewBinding.viewPager2.offscreenPageLimit = offscreenPageLimit
        viewBinding.viewPager2.registerOnPageChangeCallback(onPageChangeCallback)
        viewBinding.viewPager2.setPageTransformer(PageTransformer())
    }

    private fun startPosition(year: Int, month: Int, date: Int): Int {
        val from = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }

        val to = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DATE, date)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }

        val diff = from.timeInMillis - to.timeInMillis
        val addVal = if (diff <  0)
            0
        else
            0

        val ret = InstancesViewAdapter.START_POSITION - TimeUnit.DAYS.convert(
            diff,
            TimeUnit.MILLISECONDS
        ).toInt()

        return ret + addVal
    }
}