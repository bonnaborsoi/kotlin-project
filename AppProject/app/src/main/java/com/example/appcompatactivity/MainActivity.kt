package com.example.appcompatactivity

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

// Define a main activity class that inherits from AppCompatActivity
class MainActivity : AppCompatActivity() {

    // Declare RecyclerView and NumberAdapter for displaying data
    private lateinit var recyclerView: RecyclerView
    private lateinit var numberAdapter: NumberAdapter
    // Define a request code for permission requests
    private val PERMISSIONS_REQUEST_CODE = 123
    // Declare a Spinner for selecting time periods
    private lateinit var spinner: Spinner
    // Define a list of periods for filtering call logs
    private val periods = listOf("total", "year", "month", "week", "day")
    // Map to store call logs with phone numbers as keys and another map for counts as values
    private val callLogs = mutableMapOf<String, MutableMap<String, Int>>()
    // Map to store contact names associated with phone numbers
    private val contactNames = mutableMapOf<String, String>()
    // Variable to keep track of the selected period
    private var periodAt = "total"

    // Called when the activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the RecyclerView and set its layout manager and item decoration
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        // Initialize the Spinner and set its adapter
        spinner = findViewById(R.id.spinner)
        val spinnerAdapter = CustomSpinnerAdapter(this, android.R.layout.simple_spinner_item, periods)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        // Set a listener for item selection on the Spinner
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Update the RecyclerView based on the selected period
                updateRecyclerView(periods[position])
                periodAt = periods[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Check and request necessary permissions, store missing ones in permissionsToRequest
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CALL_LOG)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
        }

        // Request permissions (in permissionsToRequest) if not already granted
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        } else {
            // If permissions are granted, fetch call logs
            fetchCallLogs(periodAt)
        }

        // Register a BroadcastReceiver to listen for phone state changes
        val intentFilter = IntentFilter().apply {
            addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        }
        registerReceiver(callReceiver, intentFilter)
    }

    // Called when the activity resumes
    override fun onResume() {
        super.onResume()
        fetchCallLogs(periodAt)
    }

    // Called when the activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(callReceiver)
    }

    // Define a BroadcastReceiver to handle phone state changes
    private val callReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                fetchCallLogs(periodAt)
            }
        }
    }

    // Handle the result of permission requests
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                fetchCallLogs(periodAt)
            } else {
                Toast.makeText(this, "Permissões necessárias não concedidas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Fetch call logs and update the RecyclerView
    private fun fetchCallLogs(period: String) {
        Log.d("MainActivity", "Fetching contact names")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            fetchContactNames()
        }

        // Clear previous call logs to avoid duplicates
        callLogs.clear()

        // Query the call log content provider
        val cursor: Cursor? = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            null,
            null,
            CallLog.Calls.DEFAULT_SORT_ORDER
        )

        // Process the query results
        cursor?.use {
            val numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            val dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE)

            val currentTime = System.currentTimeMillis()
            val calendar = Calendar.getInstance()

            while (cursor.moveToNext()) {
                val number = normalizePhoneNumber(cursor.getString(numberIndex))
                val date = cursor.getLong(dateIndex)

                calendar.timeInMillis = date
                val callYear = calendar.get(Calendar.YEAR)
                val callMonth = calendar.get(Calendar.MONTH)
                val callWeek = calendar.get(Calendar.WEEK_OF_YEAR)
                val callDay = calendar.get(Calendar.DAY_OF_YEAR)

                calendar.timeInMillis = currentTime
                val currentYear = calendar.get(Calendar.YEAR)
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)
                val currentDay = calendar.get(Calendar.DAY_OF_YEAR)

                // Initialize the call log entry for the number if not already present
                if (number !in callLogs) {
                    callLogs[number] = mutableMapOf(
                        "total" to 0,
                        "year" to 0,
                        "month" to 0,
                        "week" to 0,
                        "day" to 0
                    )
                }

                // Increment the appropriate counters based on the call date
                callLogs[number]!!["total"] = callLogs[number]!!.getOrDefault("total", 0) + 1

                if (callYear == currentYear) {
                    callLogs[number]!!["year"] = callLogs[number]!!.getOrDefault("year", 0) + 1
                    if (callMonth == currentMonth) {
                        callLogs[number]!!["month"] = callLogs[number]!!.getOrDefault("month", 0) + 1
                    }
                    if (callWeek == currentWeek) {
                        callLogs[number]!!["week"] = callLogs[number]!!.getOrDefault("week", 0) + 1
                    }
                    if (callDay == currentDay) {
                        callLogs[number]!!["day"] = callLogs[number]!!.getOrDefault("day", 0) + 1
                    }
                }
            }
        }

        // Update the RecyclerView with the filtered and sorted logs
        updateRecyclerView(period)
    }

    // Fetch contact names and store them in the contactNames map
    private fun fetchContactNames() {
        val resolver: ContentResolver = contentResolver
        val cursor: Cursor? = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
            null,
            null,
            null
        )

        cursor?.use {
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val number = normalizePhoneNumber(cursor.getString(numberIndex))
                val name = cursor.getString(nameIndex)

                contactNames[number] = name
            }
        }
        Log.d("MainActivity", "Contact names fetched: ${contactNames.size}")
    }

    // Normalize phone numbers by removing non-digit characters
    private fun normalizePhoneNumber(number: String): String {
        return number.replace(Regex("[^\\d]"), "")
    }

    // Update the RecyclerView with the call logs for the specified period
    private fun updateRecyclerView(period: String) {
        Log.d("MainActivity", "Updating RecyclerView for period: $period")

        // Filter and sort the call logs based on the selected period
        val filteredLogs = callLogs.mapValues { it.value[period] ?: 0 }.filter { it.value > 0 }
        val sortedLogs = filteredLogs.toList().sortedByDescending { it.second }.toMap()
        val displayLogs = sortedLogs.mapKeys { contactNames[it.key] ?: it.key }

        Log.d("MainActivity", "Filtered logs: $filteredLogs")
        Log.d("MainActivity", "Sorted logs: $sortedLogs")
        Log.d("MainActivity", "Display logs: $displayLogs")

        // Create a new adapter with the sorted logs and set it to the RecyclerView
        numberAdapter = NumberAdapter(displayLogs)
        recyclerView.adapter = numberAdapter
    }
}