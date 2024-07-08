package com.example.appcompatactivity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NumberAdapter(private val callLogs: Map<String, Int>) : RecyclerView.Adapter<NumberAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val numberTextView: TextView = itemView.findViewById(R.id.numberTextView)
        val frequencyTextView: TextView = itemView.findViewById(R.id.frequencyTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_number, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val number = callLogs.keys.elementAt(position)
        val frequency = callLogs[number]!!

        holder.numberTextView.text = number
        holder.frequencyTextView.text = "Calls: $frequency"
    }

    override fun getItemCount(): Int {
        return callLogs.size
    }
}