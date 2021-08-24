package com.duke.orca.android.kotlin.lockscreencalendar.main.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.Model
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.repository.CalendarRepository
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarView
import com.duke.orca.android.kotlin.lockscreencalendar.util.SingleLiveEvent
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = CalendarRepository(application.applicationContext)

    private var _refresh = MutableLiveData<Unit>()
    val refresh: LiveData<Unit>
        get() = _refresh

    fun refresh() {
        _refresh.value = Unit
    }

    private var _selectedDate = MutableLiveData<Calendar>()
    val selectedDate: LiveData<Calendar>
        get() = _selectedDate

    fun selectDate(value: Calendar) {
        _selectedDate.value = value
    }

    private val _showEvents = SingleLiveEvent<CalendarView.ViewHolder>()
    val showEvents: LiveData<CalendarView.ViewHolder>
        get() = _showEvents

    fun callShowEvents(calendar: CalendarView.ViewHolder) {
        _showEvents.value = calendar
    }

    var lastEvent: Model.Event? = null
    var selectedItem: Model.CalendarItem? = null

    private val _intent = SingleLiveEvent<Intent>()
    val intent: LiveData<Intent>
        get() = _intent

    fun setIntent(intent: Intent) {
        _intent.value = intent
    }

    fun load() {
        repository.initialLoad()
    }
}