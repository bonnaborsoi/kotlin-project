package com.example.appcompatactivity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NumberAdapter(private val callLogs: Map<String, Int>) :
    RecyclerView.Adapter<NumberAdapter.NumberViewHolder>() {

    class NumberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val numberTextView: TextView = view.findViewById(R.id.numberTextView)
        val frequencyTextView: TextView = view.findViewById(R.id.frequencyTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NumberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_number, parent, false)
        return NumberViewHolder(view)
    }

    override fun onBindViewHolder(holder: NumberViewHolder, position: Int) {
        val number = callLogs.keys.toList()[position]
        val frequency = callLogs[number]
        holder.numberTextView.text = number
        holder.frequencyTextView.text = "Frequência: $frequency"
    }

    override fun getItemCount(): Int {
        return callLogs.size
    }
}
