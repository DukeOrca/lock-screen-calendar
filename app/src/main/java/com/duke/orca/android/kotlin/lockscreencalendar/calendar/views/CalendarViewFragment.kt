@file:Suppress("LocalVariableName")

package com.duke.orca.android.kotlin.lockscreencalendar.calendar.views

import android.content.ContentUris
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.loader.app.LoaderManager.LoaderCallbacks
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.duke.orca.android.kotlin.lockscreencalendar.BLANK
import com.duke.orca.android.kotlin.lockscreencalendar.PACKAGE_NAME
import com.duke.orca.android.kotlin.lockscreencalendar.base.BaseFragment
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.DAYS_PER_MONTH
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.DAYS_PER_WEEK
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.VISIBLE_INSTANCE_COUNT
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.WEEKS_PER_MONTH
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.CalendarItem
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.CalendarMap
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.Model
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.repository.CalendarRepositoryImpl
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.getFirstDayOfWeekOfMonth
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.toDate
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.util.toMonth
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.widget.CalendarView
import com.duke.orca.android.kotlin.lockscreencalendar.databinding.FragmentCalendarViewBinding
import com.duke.orca.android.kotlin.lockscreencalendar.main.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*

class CalendarViewFragment : BaseFragment<FragmentCalendarViewBinding>(), LoaderCallbacks<Cursor> {
    override fun inflate(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCalendarViewBinding {
        return FragmentCalendarViewBinding.inflate(inflater, container, false)
    }

    private val viewModel by activityViewModels<MainViewModel>()

    private var year = 0
    private var month = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        year = arguments?.getInt(Key.YEAR) ?: 0
        month = arguments?.getInt(Key.MONTH) ?: 0

        viewBinding.calendarView.tag = year * 100 + month

        val tcm = CalendarMap(year, month)
        lifecycleScope.launch {
            val inst = viewModel.getInstances(year, month)

            repeat(WEEKS_PER_MONTH) { weekOfMonth -> // 한달의 주를 반복.
                val weekMap = inst[weekOfMonth] ?: emptyList()

                for (instance in weekMap) {
                    val beginYearMonthDay = instance.beginYearMonthDay

                    val week = tcm.linkedHashMap[weekOfMonth] ?: continue
                    val firstDay = week.getFirstDay() ?: continue // 그 주의 시작 시간.

                    val duration = instance.endYearMonthDay - firstDay.yearMonthDay

                    if (beginYearMonthDay < firstDay.yearMonthDay) { // 시작일이 이번 주 보다 빠른 경우,
                        // 첫 번째 날에 삽입한다.
                        var k = -1

                        firstDay.visibleInstances.apply {
                            for (i in 0 until VISIBLE_INSTANCE_COUNT) {
                                if (get(i) == null) {
                                    set(i, instance)
                                    k = i
                                    break
                                }
                            }
                        }

//                        var nextKey = firstDay.nextKey
//
//                        for (i in 0 until duration) {
//                            val nextDay = week.dates[nextKey] ?: break
//
//                            if (k in 0 until VISIBLE_INSTANCE_COUNT) {
//                                nextDay.visibleInstances[k] = instance.copy().apply {
//                                    isVisible = false
//                                }
//
//                                nextKey = nextDay.nextKey
//                            } else {
//                                break
//                            }
//                        }

                    } else {
                        // 이번 주에 시작일이 있는 경우.
                        val day = week.dates[beginYearMonthDay] ?: continue // 캘린더 아이템.
                        var j = -1
                        Timber.tag("sjk")
                        Timber.d("who: ${day.date}")
                        Timber.tag("sjk")
                        Timber.d("ymd: ${instance.beginYearMonthDay} instance::: " + instance.title)

                        day.visibleInstances.apply {
                            for (i in 0 until VISIBLE_INSTANCE_COUNT) {
                                if (get(i) == null) {
                                    set(i, instance)
                                    j = i
                                    break
                                }
                            }
                        }

//                        var nextKey = firstDay.nextKey
//
//                        for (i in 0 until duration) {
//                            val nextDay = week.dates[nextKey] ?: break
//
//                            if (j in 0 until VISIBLE_INSTANCE_COUNT) {
//                                nextDay.visibleInstances[j] = instance.copy().apply {
//                                    isVisible = false
//                                }
//
//                                nextKey = nextDay.nextKey
//                            } else {
//                                break
//                            }
//                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                viewBinding.calendarView.set(tcm)
            }
        }



        //val loaderManager = LoaderManager.getInstance(this).initLoader(year * 100 + month, null, this)

//        viewModel.refresh.observe(viewLifecycleOwner, {
//            loaderManager.forceLoad()
//        })

        //observe()

        return viewBinding.root
    }

    private fun observe() {
        viewModel.selectedDate.observe(viewLifecycleOwner, {
            val year = it.get(Calendar.YEAR)
            val month = it.get(Calendar.MONTH)
            val date = it.get(Calendar.DATE)

            if (year == this.year && month == this.month) {
                viewBinding.calendarView.select(date.dec())
            }
        })
    }

    companion object {
        private object Key {
            private const val PREFIX = "$PACKAGE_NAME.CalendarViewFragment.companion.KEY"
            const val YEAR = "$PREFIX.YEAR"
            const val MONTH = "$PREFIX.MONTH"
        }

        fun newInstance(year: Int, month: Int): CalendarViewFragment {
            return CalendarViewFragment().apply {
                arguments = Bundle().apply {
                    putInt(Key.YEAR, year)
                    putInt(Key.MONTH, month)
                }
            }
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()

        val currentCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DATE, 1)
        }

        val previousCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DATE, 1)
            add(Calendar.MONTH, -1)
        }

        val nextCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DATE, 1)
            add(Calendar.MONTH, 1)
        }

        val firstDayOfWeekOfMonth = getFirstDayOfWeekOfMonth(year, month)
        val lastDayOfMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val lastDayOfPreviousMonth = previousCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val DTSTART = if (firstDayOfWeekOfMonth == Calendar.SUNDAY) {
            currentCalendar.apply { set(Calendar.DATE, 1) }

        } else {
            previousCalendar.apply {
                set(Calendar.DATE, lastDayOfPreviousMonth - firstDayOfWeekOfMonth.dec())
            }
        }

        val DTEND = nextCalendar.apply {
            set(Calendar.DAY_OF_MONTH, DAYS_PER_MONTH - firstDayOfWeekOfMonth.dec() - lastDayOfMonth)
        }

        ContentUris.appendId(builder, previousCalendar.timeInMillis)
        ContentUris.appendId(builder, nextCalendar.timeInMillis)

        return CursorLoader(
            requireContext(),
            builder.build(),
            CalendarRepositoryImpl.Instances.projections,
            null,
            null,
            CalendarContract.Instances.START_DAY + " DESC, " +
                    CalendarContract.Instances.END_DAY + " DESC, " +
                    CalendarContract.Instances.BEGIN + " ASC, " +
                    CalendarContract.Instances.TITLE + " ASC"
        )
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        data ?: return

        val instances = arrayListOf<Model.Instance>()

        data.moveToFirst()

        val calendarItems = arrayOfNulls<CalendarItem>(DAYS_PER_MONTH)

        val currentCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DATE, 1)
        }

        val nextCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentCalendar.get(Calendar.YEAR))
            set(Calendar.MONTH, currentCalendar.get(Calendar.MONTH))
            set(Calendar.DATE, 1)
            add(Calendar.MONTH, 1)
        }

        val previousCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentCalendar.get(Calendar.YEAR))
            set(Calendar.MONTH, currentCalendar.get(Calendar.MONTH))
            set(Calendar.DATE, 1)
            add(Calendar.MONTH, -1)
        }

        val nextYear = nextCalendar.get(Calendar.YEAR)
        val nextMonth = nextCalendar.get(Calendar.MONTH)

        val previousYear = previousCalendar.get(Calendar.YEAR)
        val previousMonth = previousCalendar.get(Calendar.MONTH)

        val indexOfFirstDayOfMonth = getFirstDayOfWeekOfMonth(year, month).dec()
        val indexOfLastDayOfMonth = indexOfFirstDayOfMonth + currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val lastDayOfPreviousMonth = previousCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfPreviousMonth = lastDayOfPreviousMonth - indexOfFirstDayOfMonth

        addDaysOfMonth(calendarItems, indexOfFirstDayOfMonth, indexOfLastDayOfMonth, year, month)
        addDaysOfNextMonth(calendarItems, indexOfLastDayOfMonth, nextYear, nextMonth)
        addDaysOfPreviousMonth(calendarItems, lastDayOfPreviousMonth, indexOfFirstDayOfMonth, previousYear, previousMonth)

        while (data.moveToNext()) {
            val _id = data.getLongOrNull(CalendarRepositoryImpl.Instances.Index._ID) ?: 0L
            val allDay = data.getIntOrNull(CalendarRepositoryImpl.Instances.Index.ALL_DAY) ?: 0
            val begin = data.getLongOrNull(CalendarRepositoryImpl.Instances.Index.BEGIN) ?: 0L
            val calendarColor = data.getIntOrNull(CalendarRepositoryImpl.Instances.Index.CALENDAR_COLOR) ?: Color.TRANSPARENT
            val calendarDisplayName = data.getStringOrNull(CalendarRepositoryImpl.Instances.Index.CALENDAR_DISPLAY_NAME) ?: BLANK
            val calendarId = data.getLongOrNull(CalendarRepositoryImpl.Instances.Index.CALENDAR_ID) ?: continue
            val end = data.getLongOrNull(CalendarRepositoryImpl.Instances.Index.END) ?: 0L
            val endDay = data.getIntOrNull(CalendarRepositoryImpl.Instances.Index.END_DAY) ?: 0
            val eventId = data.getLongOrNull(CalendarRepositoryImpl.Instances.Index.EVENT_ID) ?: continue
            val startDay = data.getIntOrNull(CalendarRepositoryImpl.Instances.Index.START_DAY) ?: 0
            val title = data.getStringOrNull(CalendarRepositoryImpl.Instances.Index.TITLE) ?: BLANK

            val beginDayOfMonth = begin.toDate()
            val endDayOfMonth = end.toDate()
            val fillBackground = allDay == 1 || (endDay - startDay > 0)

            instances.add(
                Model.Instance(
                    isAllDay = allDay == 1,
                    begin = begin,
                    calendarColor = calendarColor,
                    calendarId = calendarId,
                    end = end,
                    endDay = endDay,
                    eventId = eventId,
                    id = _id,
                    month = begin.toMonth(),
                    startDay = startDay,
                    title = title,
                    beginDayOfMonth = beginDayOfMonth,
                    endDayOfMonth = endDayOfMonth,
                    duration = endDay - startDay,
                    fillBackgroundColor = fillBackground
                ))
        }

        instances.sortWith(Comparator { o1, o2 ->
            return@Comparator when {
                o1.beginDayOfMonth > o2.beginDayOfMonth -> 1
                o1.beginDayOfMonth < o2.beginDayOfMonth -> -1
                else -> 0
            }
        })

        instances.forEach { instance ->
            val beginDayOfMonth = instance.beginDayOfMonth
            var endDayOfMonth = instance.endDayOfMonth

            val index = when(instance.month) {
                previousMonth -> beginDayOfMonth.dec() - firstDayOfPreviousMonth
                nextMonth -> beginDayOfMonth + indexOfLastDayOfMonth.dec()
                else -> beginDayOfMonth.dec() + indexOfFirstDayOfMonth
            }

            if (index in 0 until DAYS_PER_MONTH) {
                calendarItems[index]?.instances?.add(instance)

                //val k = calendarItems[index]?.instances?.indexOf(instance) ?: -1

                /*if (k in 0 until VISIBLE_INSTANCE_COUNT)*/
                calendarItems[index]?.visibleInstances?.let { visibleInstances ->
                    for (i in 0 until VISIBLE_INSTANCE_COUNT) {
                        if (visibleInstances[i] == null) {
                            visibleInstances[i] = instance
                            break
                        }
                    }
                }

                if (instance.duration > 0) {
                    for (i in 1..instance.duration) {
                        val j = index + i

                        if (j in 0 until DAYS_PER_MONTH) {
                            if (instance.isAllDay.not()) {
                                instance.deepCopy().apply {
                                    this.beginDayOfMonth = beginDayOfMonth + i
                                    this.duration = endDayOfMonth - (beginDayOfMonth + i)
                                    isVisible = (j % DAYS_PER_WEEK) == Calendar.SUNDAY.dec()
                                }.also {
                                    calendarItems[j]?.instances?.add(it)
                                    val k = calendarItems[index]?.instances?.indexOf(instance) ?: -1

                                    if (k in 0 until VISIBLE_INSTANCE_COUNT) {
                                        calendarItems[j]?.visibleInstances?.set(k, it)
                                    }
                                }
                            }
                        }
                    }
                }
                //}
            }
        }

        val calendar = Model.Calendar(
            year = year,
            month = month,
            items = calendarItems,
            indexOfFirstDayOfMonth,
            indexOfLastDayOfMonth,
        )

        viewBinding.calendarView.setCalendar(calendar)
        viewBinding.calendarView.setOnItemClickListener(object : CalendarView.OnItemClickListener{
            override fun onItemClick(item: CalendarItem) {
                if (item !is CalendarItem.DayOfMonth) {
                    Calendar.getInstance().apply {
                        set(Calendar.YEAR, item.year)
                        set(Calendar.MONTH, item.month)
                        set(Calendar.DATE, item.date)

                        viewModel.selectDate(this)
                    }

                    return
                }

                if (viewModel.selectedItem == item) {
                    if (item.instances.isEmpty()) {
                        viewModel.insertEvent(item.year, item.month, item.date)
                    } else {
                        Calendar.getInstance().apply {
                            set(Calendar.YEAR, item.year)
                            set(Calendar.MONTH, item.month)
                            set(Calendar.DATE, item.date)

                            viewModel.callShowEvents(item)
                        }
                    }
                } else {
                    viewModel.selectedItem = item

                    if (item.instances.isEmpty()) {
                        // pass
                    } else {
                        Calendar.getInstance().apply {
                            set(Calendar.YEAR, item.year)
                            set(Calendar.MONTH, item.month)
                            set(Calendar.DATE, item.date)

                            viewModel.callShowEvents(item)
                        }
                    }
                }
            }
        })

        //data.close()
    }

    private fun addDaysOfPreviousMonth(
        calendarItems: Array<CalendarItem?>,
        lastDayOfPreviousMonth: Int,
        indexOfFirstDayOfMonth: Int,
        year: Int,
        month: Int
    ) {
        if (indexOfFirstDayOfMonth == 0) return

        val from = lastDayOfPreviousMonth - indexOfFirstDayOfMonth.dec()

        for ((i, j) in (from .. lastDayOfPreviousMonth).withIndex()) {
            calendarItems[i] = CalendarItem.DayOfPreviousMonth(
                year = year,
                month = month,
                date = j,
                position = i
            )
        }
    }

    private fun addDaysOfMonth(
        calendarItems: Array<CalendarItem?>,
        indexOfFirstDayOfMonth: Int,
        indexOfLastDayOfMonth: Int,
        year: Int,
        month: Int
    ) {
        for ((i, j) in (indexOfFirstDayOfMonth until indexOfLastDayOfMonth).withIndex()) {
            calendarItems[j] = CalendarItem.DayOfMonth(
                year = year,
                month = month,
                date = i.inc(),
                position = j
            )
        }
    }

    private fun addDaysOfNextMonth(
        calendarItems: Array<CalendarItem?>,
        indexOfLastDayOfMonth: Int,
        year: Int,
        month: Int
    ) {
        for ((i, j) in (indexOfLastDayOfMonth until DAYS_PER_MONTH).withIndex()) {
            calendarItems[j] = CalendarItem.DayOfNextMonth(
                year = year,
                month = month,
                i.inc(),
                position = j
            )
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {

    }
}