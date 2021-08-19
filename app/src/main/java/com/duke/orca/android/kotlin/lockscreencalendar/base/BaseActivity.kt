package com.duke.orca.android.kotlin.lockscreencalendar.base

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

open class BaseActivity : AppCompatActivity() {
    private val activityResultLauncherMap = mutableMapOf<String, ActivityResultLauncher<Intent>>()

    protected fun putActivityResultLauncher(key: String, value: ActivityResultLauncher<Intent>) {
        activityResultLauncherMap[key] = value
    }

    protected fun getActivityResultLauncher(key: String) = activityResultLauncherMap[key]
}