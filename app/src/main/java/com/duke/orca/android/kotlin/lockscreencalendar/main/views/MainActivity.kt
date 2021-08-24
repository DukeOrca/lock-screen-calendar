package com.duke.orca.android.kotlin.lockscreencalendar.main.views

import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.duke.orca.android.kotlin.lockscreencalendar.PACKAGE_NAME
import com.duke.orca.android.kotlin.lockscreencalendar.R
import com.duke.orca.android.kotlin.lockscreencalendar.base.BaseActivity
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.TRANSITION_NAME
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapter.CalendarViewAdapter
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.views.InstancesViewPagerFragment
import com.duke.orca.android.kotlin.lockscreencalendar.databinding.ActivityMainBinding
import com.duke.orca.android.kotlin.lockscreencalendar.main.views.MainActivity.ActivityResultLauncher.KEY_PERMISSION
import com.duke.orca.android.kotlin.lockscreencalendar.main.viewmodel.MainViewModel
import com.duke.orca.android.kotlin.lockscreencalendar.permission.PermissionChecker
import com.duke.orca.android.kotlin.lockscreencalendar.permission.view.PermissionRationaleDialogFragment
import com.duke.orca.android.kotlin.lockscreencalendar.permission.view.PermissionRationaleDialogFragment.Companion.requiredPermissions
import java.util.*

class MainActivity : BaseActivity(), PermissionRationaleDialogFragment.OnPermissionActionClickListener {
    private var _viewBinding: ActivityMainBinding? = null
    private val viewBinding: ActivityMainBinding
        get() = _viewBinding!!

    private val viewModel by viewModels<MainViewModel>()

    private object ActivityResultLauncher {
        private const val PREFIX = "$PACKAGE_NAME.main.view.MainActivity.ActivityResultLauncher"
        const val KEY_INSERT = "$PREFIX.KEY_INSERT"
        const val KEY_PERMISSION = "$PREFIX.KEY_PERMISSION"
    }

    private object Today {
        var year = 0
        var month = 0
        var date = 0
    }

    private val adapter by lazy { CalendarViewAdapter(this) }
    private val months by lazy { resources.getStringArray(R.array.months) }
    private val offscreenPageLimit = 6

    private var currentYear: Int = -1
    private var currentMonth: Int = -1

    private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.lastEvent?.let {
            val lastEvent = viewModel.repository.getLastEvent() ?: return@let

            if (it.id < lastEvent.id) {
                //새 인스턴스 추가됨. dtstart 로 해당 month 로 이동 후. 리플래시.
                lastEvent.DTSTART
                Calendar.getInstance().apply {
                    timeInMillis = lastEvent.DTSTART

                    val year = get(Calendar.YEAR)
                    val month = get(Calendar.MONTH)
                    val date = get(Calendar.DATE)

                    scrollTo(year, month, true, false)
                }
            }
        }
    }

    private val activityResultLauncher2 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refresh()
    }

    private val onPageChangeCallback by lazy {
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val amount = position - CalendarViewAdapter.START_POSITION
                val calendar = Calendar.getInstance().apply {
                    add(Calendar.MONTH, amount)
                }

                currentYear = calendar.get(Calendar.YEAR)
                currentMonth = calendar.get(Calendar.MONTH)

                viewBinding.textViewMonth.text = months[currentMonth]
                "$currentYear${getString(R.string.year)}".also {
                    viewBinding.textViewYear.text = it
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        viewModel.load()

//        window?.setFlags(
//            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
//            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
//        )

        initData()
        initializeViews()
        observe()
        initializeActivityResultLaunchers()

        if (PermissionRationaleDialogFragment.permissionsGranted(this).not()) {
            PermissionRationaleDialogFragment().also {
                it.show(supportFragmentManager, it.tag)
            }
        }
    }

    override fun onDestroy() {
        _viewBinding = null
        super.onDestroy()
    }

    override fun onPermissionAllowClick() {
        if (PermissionChecker.checkPermissions(this, requiredPermissions).not()) {
            if (isFinishing.not()) {
                PermissionChecker.checkPermissions(this, requiredPermissions, {
                    viewModel.refresh()
                }) {
                    showRequestCalendarPermissionSnackbar()
                }
            }
        }
    }

    override fun onPermissionDenyClick() {
        if (PermissionChecker.checkPermissions(this, requiredPermissions).not()) {
            if (isFinishing.not()) {
                showRequestCalendarPermissionSnackbar()
            }
        }
    }

    private fun showRequestCalendarPermissionSnackbar() {
        PermissionChecker.showRequestCalendarPermissionSnackbar(
            findViewById<FrameLayout>(R.id.frame_layout),
            getActivityResultLauncher(KEY_PERMISSION)
        )
    }

    private fun initData() {
        with(Calendar.getInstance()) {
            Today.year = get(Calendar.YEAR)
            Today.month = get(Calendar.MONTH)
            Today.date = get(Calendar.DATE)
        }
    }

    private fun initializeActivityResultLaunchers() {
        putActivityResultLauncher(
            KEY_PERMISSION,
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    viewModel.refresh()
                }
            }
        )
    }

    private fun initializeViews() {
        viewBinding.viewPager2.adapter = adapter
        viewBinding.viewPager2.offscreenPageLimit = offscreenPageLimit
        viewBinding.viewPager2.registerOnPageChangeCallback(onPageChangeCallback)
        viewBinding.viewPager2.setCurrentItem(CalendarViewAdapter.START_POSITION, false)

        viewBinding.linearLayoutInsert.setOnClickListener {
            viewModel.selectedItem?.let {
                insertEvent(currentYear, currentMonth, it.date)
            }
        }
    }

    private fun observe() {
//        viewModel.refresh.observe(this, {
        // 필요한가? 얘 때문인거 같기도한데?
//            viewBinding.viewPager2.adapter = adapter
//            viewBinding.viewPager2.setCurrentItem(CalendarViewAdapter.START_POSITION, false)
//        })

        viewModel.showEvents.observe(this, { viewHolder ->
            val calendarItem = viewHolder.calendarItem
            InstancesViewPagerFragment().also {
                val arguments = Bundle().apply {
                    putInt(InstancesViewPagerFragment.Key.YEAR, calendarItem.year)
                    putInt(InstancesViewPagerFragment.Key.MONTH, calendarItem.month)
                    putInt(InstancesViewPagerFragment.Key.DATE, calendarItem.date)
                }

                it.arguments = arguments

                val calendarView = viewHolder.calendarView
                calendarView.transitionName = TRANSITION_NAME

                supportFragmentManager.beginTransaction()
                    .addSharedElement(calendarView, TRANSITION_NAME)
                    .add(R.id.fragment_container_view, it, it.tag)
                    .addToBackStack(null)
                    .commit()
            }
        })

        viewModel.intent.observe(this, {
            val action = it.action ?: return@observe

            when(action) {
                Intent.ACTION_EDIT -> activityResultLauncher2.launch(it)
                Intent.ACTION_INSERT -> {}
            }
        })

        viewModel.selectedDate.observe(this, {
            val year = it.get(Calendar.YEAR)
            val month = it.get(Calendar.MONTH)
            scrollTo(year, month, smoothScroll = true)
        })
    }

    private fun insertEvent(year: Int, month: Int, date: Int) {
        viewModel.lastEvent = viewModel.repository.getLastEvent()

        val eventBeginTime = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DATE, date)
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
        }.timeInMillis

        val eventEndTime = Calendar.getInstance().apply {
            timeInMillis = eventBeginTime
            add(Calendar.HOUR_OF_DAY, 1)
        }.timeInMillis

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, eventBeginTime)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, eventEndTime)
        }

        activityResultLauncher.launch(intent)
    }

    private fun scrollTo(year: Int, month: Int, reload: Boolean = false, smoothScroll: Boolean = false) {
        val currentItem = viewBinding.viewPager2.currentItem

        val from = Calendar.getInstance().apply {
            add(Calendar.MONTH, currentItem - CalendarViewAdapter.START_POSITION)
        }

        val to = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
        }

        val fromYear = from.get(Calendar.YEAR)
        val fromMonth = from.get(Calendar.MONTH)

        val toYear = to.get(Calendar.YEAR)
        val toMonth = to.get(Calendar.MONTH)

       val amount = toMonth - fromMonth + (toYear - fromYear) * 12

        viewBinding.viewPager2.setCurrentItem(currentItem + amount, smoothScroll)

        if (reload) {
            viewModel.repository.put(year, month)
        }
    }
}