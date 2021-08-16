package com.duke.orca.android.kotlin.lockscreencalendar.main.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.repository.CalendarRepository
import com.duke.orca.android.kotlin.lockscreencalendar.util.SingleLiveEvent

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val refresh = SingleLiveEvent<Unit>()
    val calendarRepository = CalendarRepository(application.applicationContext)

    fun load() {
        calendarRepository.initialLoad()
    }

    override fun onCleared() {
        refresh.clear()
        super.onCleared()
    }
}