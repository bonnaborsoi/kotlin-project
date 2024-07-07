package com.example.appcompatactivity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NumberAdapter(private val callLogs: Map<String, Map<String, Int>>) : RecyclerView.Adapter<NumberAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val numberTextView: TextView = itemView.findViewById(R.id.numberTextView)
        val totalTextView: TextView = itemView.findViewById(R.id.totalTextView)
        val yearTextView: TextView = itemView.findViewById(R.id.yearTextView)
        val monthTextView: TextView = itemView.findViewById(R.id.monthTextView)
        val weekTextView: TextView = itemView.findViewById(R.id.weekTextView)
        val dayTextView: TextView = itemView.findViewById(R.id.dayTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_number, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val number = callLogs.keys.elementAt(position)
        val frequencies = callLogs[number]!!

        holder.numberTextView.text = number
        holder.totalTextView.text = "Total: ${frequencies["total"]}"
        holder.yearTextView.text = "This Year: ${frequencies["year"]}"
        holder.monthTextView.text = "This Month: ${frequencies["month"]}"
        holder.weekTextView.text = "This Week: ${frequencies["week"]}"
        holder.dayTextView.text = "Today: ${frequencies["day"]}"
    }

    override fun getItemCount(): Int {
        return callLogs.size
    }
}