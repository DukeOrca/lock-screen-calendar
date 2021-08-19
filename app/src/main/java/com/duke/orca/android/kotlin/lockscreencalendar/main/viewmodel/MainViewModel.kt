package com.duke.orca.android.kotlin.lockscreencalendar.main.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.entity.Entity
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.Model
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.repository.CalendarRepository
import com.duke.orca.android.kotlin.lockscreencalendar.util.SingleLiveEvent
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val refresh = MutableLiveData<Unit>()

    fun refresh() {
        refresh.value = Unit
    }

    val calendarRepository = CalendarRepository(application.applicationContext)

    private val _showEvents = SingleLiveEvent<Calendar>()
    val showEvents: LiveData<Calendar>
        get() = _showEvents
    fun callShowEvents(calendar: Calendar) {
        _showEvents.value = calendar
    }

    var lastEvent: Entity.Event? = null

    // 캘린더 뷰모델 적출필요
    var selectedItem: Model.CalendarItem? = null

    private val _intent = SingleLiveEvent<Intent>()
    val intent: LiveData<Intent>
        get() = _intent

    fun setIntent(intent: Intent) {
        _intent.value = intent
    }

    fun load() {
        calendarRepository.initialLoad()
    }
}

data class Date(
    val year: Int,
    val month: Int,
    val date: Int
)