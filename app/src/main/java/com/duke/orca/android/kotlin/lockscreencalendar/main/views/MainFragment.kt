package com.duke.orca.android.kotlin.lockscreencalendar.main.views

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.transition.Explode
import androidx.viewpager2.widget.ViewPager2
import com.duke.orca.android.kotlin.lockscreencalendar.PACKAGE_NAME
import com.duke.orca.android.kotlin.lockscreencalendar.R
import com.duke.orca.android.kotlin.lockscreencalendar.base.BaseFragment
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.DAYS_PER_WEEK
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.MONTHS_PER_YEAR
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapter.CalendarViewAdapter
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.views.InstancesViewPagerActivity
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarView
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.DayOfWeekItemView
import com.duke.orca.android.kotlin.lockscreencalendar.databinding.FragmentMainBinding
import com.duke.orca.android.kotlin.lockscreencalendar.main.viewmodel.MainViewModel
import com.duke.orca.android.kotlin.lockscreencalendar.main.views.MainFragment.ActivityResultLauncher.KEY_PERMISSION
import com.duke.orca.android.kotlin.lockscreencalendar.permission.PermissionChecker
import com.duke.orca.android.kotlin.lockscreencalendar.permission.view.PermissionRationaleDialogFragment
import java.util.*

class MainFragment : BaseFragment<FragmentMainBinding>(), PermissionRationaleDialogFragment.OnPermissionActionClickListener {
    override fun inflate(inflater: LayoutInflater, container: ViewGroup?): FragmentMainBinding {
        return FragmentMainBinding.inflate(inflater, container, false)
    }

    private val viewModel by activityViewModels<MainViewModel>()

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
                lastEvent.DTSTART
                Calendar.getInstance().apply {
                    timeInMillis = lastEvent.DTSTART

                    val year = get(Calendar.YEAR)
                    val month = get(Calendar.MONTH)
                    val date = get(Calendar.DATE)

                    scrollTo(year, month, true)
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        sharedElementReturnTransition = Explode()

        initData()
        initializeViews()
        observe()
        initializeActivityResultLaunchers()

        if (PermissionRationaleDialogFragment.permissionsGranted(requireContext()).not()) {
            PermissionRationaleDialogFragment().also {
                it.show(childFragmentManager, it.tag)
            }
        }

        return viewBinding.root
    }

    override fun onPermissionAllowClick() {
        if (PermissionChecker.checkPermissions(requireContext(),
                PermissionRationaleDialogFragment.requiredPermissions
            ).not()) {
            if (isNotFinishing()) {
                PermissionChecker.checkPermissions(requireContext(),
                    PermissionRationaleDialogFragment.requiredPermissions, {
                    viewModel.refresh()
                }) {
                    showRequestCalendarPermissionSnackbar()
                }
            }
        }
    }

    override fun onPermissionDenyClick() {
        if (PermissionChecker.checkPermissions(requireContext(),
                PermissionRationaleDialogFragment.requiredPermissions
            ).not()) {
            if (isNotFinishing()) {
                showRequestCalendarPermissionSnackbar()
            }
        }
    }

    private fun isNotFinishing() = requireActivity().isFinishing.not()

    private fun showRequestCalendarPermissionSnackbar() {
        PermissionChecker.showRequestCalendarPermissionSnackbar(
            viewBinding.frameLayout,
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
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    viewModel.refresh()
                }
            }
        )
    }

    private fun initializeViews() {
        repeat(DAYS_PER_WEEK) {
            viewBinding.linearLayoutDayOfWeek.addView(DayOfWeekItemView(requireContext(), it))
        }

        viewBinding.viewPager2.adapter = adapter
        viewBinding.viewPager2.offscreenPageLimit = offscreenPageLimit
        viewBinding.viewPager2.registerOnPageChangeCallback(onPageChangeCallback)
        viewBinding.viewPager2.setCurrentItem(CalendarViewAdapter.START_POSITION, false)

        viewBinding.imageViewInsert.setOnClickListener {
            viewModel.selectedItem?.let {
                viewModel.insertEvent(currentYear, currentMonth, it.date)
            }
        }

        viewBinding.textViewToday.text = viewModel.today.get(Calendar.DATE).toString()
    }

    private fun observe() {
//        viewModel.refresh.observe(this, {
        // 필요한가? 얘 때문인거 같기도한데?
//            viewBinding.viewPager2.adapter = adapter
//            viewBinding.viewPager2.setCurrentItem(CalendarViewAdapter.START_POSITION, false)
//        })

        viewModel.showEvents.observe(viewLifecycleOwner, { item ->
            val arguments = Bundle().apply {
                putInt(InstancesViewPagerActivity.Key.YEAR, item.year)
                putInt(InstancesViewPagerActivity.Key.MONTH, item.month)
                putInt(InstancesViewPagerActivity.Key.DATE, item.date)
            }

            val view = viewBinding.viewPager2.findViewWithTag<CalendarView>(item.year * 100 + item.month)
            val rview = view.getChildAt(item.position)

            val activityOptions = ActivityOptions.makeSceneTransitionAnimation(
                requireActivity(), rview, "transition_name"
            )
            val intent = Intent(requireContext(), InstancesViewPagerActivity::class.java).apply {
                putExtra(InstancesViewPagerActivity.Key.YEAR, item.year)
                putExtra(InstancesViewPagerActivity.Key.MONTH, item.month)
                putExtra(InstancesViewPagerActivity.Key.DATE, item.date)
            }
            startActivity(intent, activityOptions.toBundle())
        })
            //, activityOptions.toBundle())
            //InstancesViewPagerFragment().also {


//                it.arguments = arguments
//
//                // todo find view 로직으로 찾아와야한다..
//
//                val transaction = parentFragmentManager.beginTransaction()
//
//                val view = viewBinding.viewPager2.findViewWithTag<CalendarView>(item.year * 100 + item.month)
//                val rview = view.getChildAt(item.position)
//
//                val changeImageTransform = TransitionInflater.from(requireContext())
//                    .inflateTransition(R.transition.explode).apply {
//                        duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
//                    }
//
//                it.setSharedElementEnterTransition(changeImageTransform)
//                it.setSharedElementReturnTransition(changeImageTransform)
//
//                Timber.tag("sjk")
//                Timber.d("view is null?: $rview")
//                if (rview is CalendarItemView) {
//                    viewBinding.textClockTime.transitionName = "transition_name"
//                    transaction.addSharedElement(viewBinding.textClockTime, "transition_name")
//                }
//
//                transaction
//                    .setReorderingAllowed(true)
//                    .replace(R.id.fragment_container_view, it, it.tag)
//                    .addToBackStack(null)
//                    .commit()
           // }
        //})

        viewModel.intent.observe(viewLifecycleOwner, { intent ->
            intent.action?.let { action ->
                when(action) {
                    Intent.ACTION_EDIT -> activityResultLauncher2.launch(intent)
                    Intent.ACTION_INSERT -> activityResultLauncher.launch(intent)
                }
            }
        })

        viewModel.selectedDate.observe(viewLifecycleOwner, {
            val year = it.get(Calendar.YEAR)
            val month = it.get(Calendar.MONTH)
            scrollTo(year, month, smoothScroll = true)
        })
    }

    private fun scrollTo(year: Int, month: Int, smoothScroll: Boolean) {
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

        val amount = toMonth - fromMonth + (toYear - fromYear) * MONTHS_PER_YEAR

        viewBinding.viewPager2.setCurrentItem(currentItem + amount, smoothScroll)
    }
}