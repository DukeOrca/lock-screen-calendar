package com.duke.orca.android.kotlin.lockscreencalendar.main.views

import android.os.Bundle
import android.transition.TransitionInflater
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.transition.Explode
import com.duke.orca.android.kotlin.lockscreencalendar.R
import com.duke.orca.android.kotlin.lockscreencalendar.base.BaseActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window?.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_view, MainFragment())
            .commit()
    }
}