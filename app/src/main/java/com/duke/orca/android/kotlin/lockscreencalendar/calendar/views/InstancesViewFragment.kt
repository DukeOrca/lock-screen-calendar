package com.duke.orca.android.kotlin.lockscreencalendar.calendar.views

import android.content.ContentUris
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Explode
import com.duke.orca.android.kotlin.lockscreencalendar.PACKAGE_NAME
import com.duke.orca.android.kotlin.lockscreencalendar.base.BaseFragment
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.TRANSITION_NAME
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapter.InstanceAdapter
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.Model
import com.duke.orca.android.kotlin.lockscreencalendar.databinding.FragmentInstancesViewBinding
import com.duke.orca.android.kotlin.lockscreencalendar.main.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class InstancesViewFragment : BaseFragment<FragmentInstancesViewBinding>() {
    override fun inflate(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentInstancesViewBinding {
        return FragmentInstancesViewBinding.inflate(inflater, container, false)
    }

    private val viewModel by activityViewModels<MainViewModel>()

    private val adapter = InstanceAdapter().apply {
        setOnItemClickListener(object : InstanceAdapter.OnItemClickListener {
            override fun onItemClick(item: Model.Instance) {
                val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, item.eventId)
                val intent = Intent(Intent.ACTION_EDIT).apply {
                    data = uri
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, item.begin)
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, item.end)
                }

                viewModel.setIntent(intent)
            }
        })
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

        viewBinding.textViewDate.text = date.toString()

        viewBinding.recyclerView.apply {
            adapter = this@InstancesViewFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val list = viewModel.repository.getInstances(year, month, date)

            withContext(Dispatchers.Main) {
                adapter.submitList(list)
            }
        }

        viewModel.refresh.observe(viewLifecycleOwner, {
            loadInstances(year, month, date)
        })

        return viewBinding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun loadInstances(year: Int, month: Int, date: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.repository.getInstances(year, month, date).also {
                withContext(Dispatchers.Main) {
                    adapter.submitList(it)
                }
            }
        }
    }

    companion object {
        private object Key {
            private const val PREFIX = "$PACKAGE_NAME.InstancesFragment.companion.KEY"
            const val YEAR = "$PREFIX.YEAR"
            const val MONTH = "$PREFIX.MONTH"
            const val DATE = "$PREFIX.DATE"
        }

        fun newInstance(year: Int, month: Int, date: Int): InstancesViewFragment {
            return InstancesViewFragment().apply {
                arguments = Bundle().apply {
                    putInt(Key.YEAR, year)
                    putInt(Key.MONTH, month)
                    putInt(Key.DATE, date)
                }
            }
        }
    }
}