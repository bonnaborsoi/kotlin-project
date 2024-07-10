package com.example.appcompatactivity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Define a custom adapter class for the RecyclerView that takes a map of call logs
class NumberAdapter(private val callLogs: Map<String, Int>) : RecyclerView.Adapter<NumberAdapter.ViewHolder>() {

    // Define a ViewHolder class that holds references to the views in each item
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val numberTextView: TextView = itemView.findViewById(R.id.numberTextView)
        val frequencyTextView: TextView = itemView.findViewById(R.id.frequencyTextView)
    }

    // Called when the RecyclerView needs a new ViewHolder to represent an item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate the layout for an item and create a ViewHolder with it
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_number, parent, false)
        return ViewHolder(view)
    }

    // Called by the RecyclerView to display data at the specified position
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Get the phone number and call frequency for the current position
        val number = callLogs.keys.elementAt(position)
        val frequency = callLogs[number]!!

        // Set the text of the TextViews to display the phone number and call frequency
        holder.numberTextView.text = number
        holder.frequencyTextView.text = "Calls: $frequency"
    }

    // Return the total number of items in the data set
    override fun getItemCount(): Int {
        return callLogs.size
    }
}