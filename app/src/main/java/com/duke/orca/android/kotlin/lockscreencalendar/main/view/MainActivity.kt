package com.duke.orca.android.kotlin.lockscreencalendar.main.view

import android.os.Bundle
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.duke.orca.android.kotlin.lockscreencalendar.R
import com.duke.orca.android.kotlin.lockscreencalendar.main.viewmodel.MainViewModel
import com.duke.orca.android.kotlin.lockscreencalendar.permission.PermissionChecker
import com.duke.orca.android.kotlin.lockscreencalendar.permission.view.PermissionRationaleDialogFragment
import com.duke.orca.android.kotlin.lockscreencalendar.permission.view.PermissionRationaleDialogFragment.Companion.requiredPermissions

class MainActivity : AppCompatActivity(), PermissionRationaleDialogFragment.OnPermissionActionClickListener {
    private val viewModel by viewModels<MainViewModel>()

    private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refresh.call()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel.load()

        window?.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        if (PermissionRationaleDialogFragment.permissionsGranted(this).not()) {
            PermissionRationaleDialogFragment().also {
                it.show(supportFragmentManager, it.tag)
            }
        }
    }

    override fun onPermissionAllowClick() {
        if (PermissionChecker.checkPermissions(this, requiredPermissions).not()) {
            if (isFinishing.not()) {
                PermissionChecker.checkPermissions(this, requiredPermissions, {
                    viewModel.refresh.call()
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
            activityResultLauncher
        )
    }
}