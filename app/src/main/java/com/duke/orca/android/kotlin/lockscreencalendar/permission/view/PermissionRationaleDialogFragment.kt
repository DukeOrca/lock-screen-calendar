package com.duke.orca.android.kotlin.lockscreencalendar.permission.view

import android.Manifest.permission.READ_CALENDAR
import android.Manifest.permission.WRITE_CALENDAR
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.duke.orca.android.kotlin.lockscreencalendar.R
import com.duke.orca.android.kotlin.lockscreencalendar.base.BaseDialogFragment
import com.duke.orca.android.kotlin.lockscreencalendar.databinding.FragmentPermissionDialogBinding
import com.duke.orca.android.kotlin.lockscreencalendar.permission.Permission
import com.duke.orca.android.kotlin.lockscreencalendar.permission.PermissionChecker
import com.duke.orca.android.kotlin.lockscreencalendar.permission.adapter.PermissionAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PermissionRationaleDialogFragment: BaseDialogFragment<FragmentPermissionDialogBinding>() {
    private val permissionsDenied = mutableListOf<Permission>()
    private var onPermissionActionClickListener: OnPermissionActionClickListener? = null

    private object Delay {
        const val TIME_MILLIS = 300L
    }

    interface OnPermissionActionClickListener {
        fun onPermissionAllowClick()
        fun onPermissionDenyClick()
    }

    override fun inflate(inflater: LayoutInflater, container: ViewGroup?): FragmentPermissionDialogBinding {
        return FragmentPermissionDialogBinding.inflate(inflater, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is OnPermissionActionClickListener) {
            onPermissionActionClickListener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val permissions = arrayOf(
            Permission(
                icon = Icon.createWithResource(requireContext(), R.drawable.ic_sharp_event_24),
                isRequired = false,
                permissions = listOf(
                    READ_CALENDAR,
                    WRITE_CALENDAR
                ),
                permissionName = getString(R.string.calendar)
            )
        )

        for (permission in permissions) {
            if (PermissionChecker.checkPermissions(requireContext(), permission.permissions)) {
                continue
            }

            permissionsDenied.add(permission)
        }

        viewBinding.recyclerView.apply {
            adapter = PermissionAdapter(permissionsDenied)
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewBinding.textViewAllow.setOnClickListener {
            onPermissionActionClickListener?.onPermissionAllowClick()

            lifecycleScope.launch {
                delay(Delay.TIME_MILLIS)
                dismiss()
            }
        }

        viewBinding.textViewDeny.setOnClickListener {
            onPermissionActionClickListener?.onPermissionDenyClick()

            lifecycleScope.launch {
                delay(Delay.TIME_MILLIS)
                dismiss()
            }
        }

        return viewBinding.root
    }

    companion object {
        val requiredPermissions = listOf(READ_CALENDAR, WRITE_CALENDAR)

        fun permissionsGranted(context: Context): Boolean {
            if (PermissionChecker.checkPermissions(context, requiredPermissions).not()) {
                return false
            }

            return true
        }
    }
}