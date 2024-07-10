package com.example.appcompatactivity

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

// Define a custom adapter class that extends ArrayAdapter<String>
class CustomSpinnerAdapter(
    context: Context,
    resource: Int,
    private val items: List<String>
) : ArrayAdapter<String>(context, resource, items) {

    // Override the getView method to customize the view displayed in the spinner
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get the default view from the ArrayAdapter
        val view = super.getView(position, convertView, parent)
        // Set the text of the view to "Filter by: " followed by the item at the given position
        (view as TextView).text = "Filter by: ${items[position]}"
        return view
    }

    // Override the getDropDownView method to customize the view displayed in the dropdown list
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get the default dropdown view from the ArrayAdapter
        val view = super.getDropDownView(position, convertView, parent)
        // Set the text of the view to "Filter by: " followed by the item at the given position
        (view as TextView).text = "Filter by: ${items[position]}"
        return view
    }
}