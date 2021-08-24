package com.duke.orca.android.kotlin.lockscreencalendar.calendar.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.duke.orca.android.kotlin.lockscreencalendar.calendar.model.Model.Instance
import com.duke.orca.android.kotlin.lockscreencalendar.databinding.InstanceItemViewBinding

class InstanceAdapter : ListAdapter<Instance, InstanceAdapter.ViewHolder>(DiffCallback()) {
    interface OnItemClickListener {
        fun onItemClick(item: Instance)
    }

    private var layoutInflater: LayoutInflater? = null
    private var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = this.layoutInflater ?: LayoutInflater.from(parent.context).also {
            this.layoutInflater = it
        }

        return ViewHolder(InstanceItemViewBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    inner class ViewHolder(val viewBinding: InstanceItemViewBinding) : RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(item: Instance) {
            viewBinding.textViewTitle.text = item.title
            viewBinding.viewCalendarColor.backgroundTintList = ColorStateList.valueOf(item.calendarColor)

            viewBinding.root.setOnClickListener {
                onItemClickListener?.onItemClick(item)
            }
        }
    }
}

class DiffCallback: DiffUtil.ItemCallback<Instance>() {
    override fun areItemsTheSame(oldItem: Instance, newItem: Instance): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Instance, newItem: Instance): Boolean {
        return oldItem == newItem
    }
}