package com.duke.orca.android.kotlin.lockscreencalendar.base

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<VB: ViewBinding> : AppCompatActivity() {
    private var _viewBinding: VB? = null
    protected val viewBinding: VB
        get() = _viewBinding!!

    abstract fun inflate(inflater: LayoutInflater): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _viewBinding = inflate(layoutInflater)
        setContentView(viewBinding.root)
    }

    private val activityResultLauncherMap = mutableMapOf<String, ActivityResultLauncher<Intent>>()

    protected fun putActivityResultLauncher(key: String, value: ActivityResultLauncher<Intent>) {
        activityResultLauncherMap[key] = value
    }

    protected fun getActivityResultLauncher(key: String) = activityResultLauncherMap[key]
}