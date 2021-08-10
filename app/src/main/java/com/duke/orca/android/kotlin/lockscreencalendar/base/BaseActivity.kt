package com.duke.orca.android.kotlin.lockscreencalendar.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<VB: ViewBinding> : AppCompatActivity() {
    private var _viewBinding: VB? = null
    protected val viewBinding: VB
        get() = _viewBinding!!

    abstract fun inflate(layoutInflater: LayoutInflater): VB

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _viewBinding = inflate(layoutInflater)
        setContentView(viewBinding.root)
    }

    override fun onDestroy() {
        _viewBinding = null
        super.onDestroy()
    }
}