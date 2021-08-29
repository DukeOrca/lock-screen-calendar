package com.duke.orca.android.kotlin.lockscreencalendar.main.viewmodel

import android.app.Application
import android.content.Intent
import android.provider.CalendarContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.CalendarItem
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.Model
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.repository.CalendarRepositoryImpl
import com.duke.orca.android.kotlin.lockscreencalendar.util.SingleLiveEvent
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = CalendarRepositoryImpl(application.applicationContext)
    val today = Calendar.getInstance()

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

    private val _showEvents = SingleLiveEvent<CalendarItem>()
    val showEvents: LiveData<CalendarItem>
        get() = _showEvents

    fun callShowEvents(item: CalendarItem) {
        _showEvents.value = item
    }

    var lastEvent: Model.Event? = null
    var selectedItem: CalendarItem? = null

    fun insertEvent(year: Int, month: Int, date: Int) {
        lastEvent = repository.getLastEvent()

        val eventBeginTime = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DATE, date)
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
        }.timeInMillis

        val eventEndTime = Calendar.getInstance().apply {
            timeInMillis = eventBeginTime
            add(Calendar.HOUR_OF_DAY, 1)
        }.timeInMillis

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, eventBeginTime)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, eventEndTime)
        }

        setIntent(intent)
    }

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