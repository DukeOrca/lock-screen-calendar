package com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapters

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.duke.orca.android.kotlin.lockscreencalendar.R
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.DAYS_PER_MONTH
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.DAYS_PER_WEEK
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.TEXT_SIZE_10DP
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.Model
import com.duke.orca.android.kotlin.lockscreencalendar.databinding.CalendarViewItemBinding
import com.duke.orca.android.kotlin.lockscreencalendar.databinding.InstanceBinding
import com.duke.orca.android.kotlin.lockscreencalendar.util.hide

class CalendarViewItemAdapter(private val currentArray: Array<CalendarViewItem?>) : RecyclerView.Adapter<CalendarViewItemAdapter.ViewHolder>() {
    private object Column {
        var Saturday = 6
        var Sunday = 0
    }

    private val visibleInstanceCount = 2
    private var layoutInflater: LayoutInflater? = null
    private var selectedAdapterPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = this.layoutInflater ?: LayoutInflater.from(parent.context).also {
            this.layoutInflater = it
        }

        return ViewHolder(CalendarViewItemBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentArray[position])
    }

    override fun getItemCount(): Int = DAYS_PER_MONTH

    override fun getItemId(position: Int): Long { return position.toLong() }

    inner class ViewHolder(private val viewBinding: CalendarViewItemBinding) : RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(item: CalendarViewItem?) {
            item ?: return

            val context = viewBinding.root.context

            when(adapterPosition % DAYS_PER_WEEK) {
                Column.Saturday -> viewBinding.textViewDayOfMonth.setTextColor(ContextCompat.getColor(context, R.color.light_blue_400))
                Column.Sunday -> viewBinding.textViewDayOfMonth.setTextColor(ContextCompat.getColor(context, R.color.red_400))
            }

            viewBinding.textViewDayOfMonth.text = item.dayOfMonth.toString()
            viewBinding.linearLayoutInstance.removeAllViews()

            val visibleInstances = mutableListOf<Model.Instance>()

            item.instances.iterator().withIndex().forEach {
                if (it.index > visibleInstanceCount.dec()) {
                    return@forEach
                }

                visibleInstances.add(it.value)
            }

            with(visibleInstances) {
                forEach { instance ->
                    val layoutInflater = layoutInflater ?: LayoutInflater.from(context).also {
                        layoutInflater = it
                    }

                    InstanceBinding.inflate(layoutInflater).also { instanceBinding ->
                        instanceBinding.textViewInstance.text = instance.title
                        instanceBinding.viewCalendarColor.setBackgroundColor(instance.calendarColor)

                        viewBinding.linearLayoutInstance.addView(instanceBinding.root)
                    }
                }
            }

            if (item.instances.count() > visibleInstanceCount) {
                val text = "+${item.instances.count() - visibleInstanceCount}"

                viewBinding.linearLayoutInstance.addView(
                    TextView(context).apply {
                        setText(text)
                        setTextColor(ContextCompat.getColor(context, R.color.high_emphasis_light))
                        setTextSize(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_10DP)
                    }
                )
            }

            when(item) {
                is CalendarViewItem.DayOfMonth -> {
                    viewBinding.root.alpha = 1.0F

                    if (adapterPosition == selectedAdapterPosition) {
                        viewBinding.root.setBackgroundResource(R.drawable.background_calendar_view_item_selected)
                    } else {
                        viewBinding.root.background = null
                    }

                    viewBinding.root.setOnClickListener {
                        if (adapterPosition == selectedAdapterPosition) {
                            if (item.instances.isEmpty()) {
                                //open insertact.. that date.
                            }
                        } else {
                            notifyItemChanged(selectedAdapterPosition)
                            notifyItemChanged(adapterPosition)

                            selectedAdapterPosition = adapterPosition

                            if (item.instances.isNotEmpty()) {
                                // dialog 띄우기.
                            }
                        }
                    }
                }
                is CalendarViewItem.DayOfNextMonth -> { viewBinding.root.alpha = 0.5F }
                is CalendarViewItem.DayOfPreviousMonth -> { viewBinding.root.alpha = 0.5F }
            }
        }
    }
}

sealed class CalendarViewItem {
    abstract val dayOfMonth: Int
    abstract val instances: ArrayList<Model.Instance>
    abstract val position: Int

    open class DayOfMonth(
        override val dayOfMonth: Int,
        override val instances: ArrayList<Model.Instance> = arrayListOf(),
        override val position: Int
    ) : CalendarViewItem()

    class DayOfPreviousMonth(
        override val dayOfMonth: Int,
        override val instances: ArrayList<Model.Instance> = arrayListOf(),
        override val position: Int
    ) : CalendarViewItem()

    class DayOfNextMonth(
        override val dayOfMonth: Int,
        override val instances: ArrayList<Model.Instance> = arrayListOf(),
        override val position: Int
    ) : CalendarViewItem()
}