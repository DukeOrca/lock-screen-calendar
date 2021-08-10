package com.duke.orca.android.kotlin.lockscreencalendar.permission

import android.graphics.drawable.Icon

data class Permission(
    val icon: Icon,
    val isRequired: Boolean,
    val permissionName: String,
    val permissions: List<String>
)