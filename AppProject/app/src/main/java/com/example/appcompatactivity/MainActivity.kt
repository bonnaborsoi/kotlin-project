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

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var numberAdapter: NumberAdapter
    private val PERMISSIONS_REQUEST_CODE = 123
    private lateinit var spinner: Spinner
    private val periods = listOf("total", "year", "month", "week", "day")
    private val callLogs = mutableMapOf<String, MutableMap<String, Int>>()
    private val contactNames = mutableMapOf<String, String>()
    private var periodAt = "total"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        spinner = findViewById(R.id.spinner)
        val spinnerAdapter = CustomSpinnerAdapter(this, android.R.layout.simple_spinner_item, periods)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateRecyclerView(periods[position])
                periodAt = periods[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

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

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        } else {
            fetchCallLogs(periodAt)
        }

        val intentFilter = IntentFilter().apply {
            addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        }
        registerReceiver(callReceiver, intentFilter)
    }

    override fun onResume() {
        super.onResume()
        fetchCallLogs(periodAt)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(callReceiver)
    }

    private val callReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                fetchCallLogs(periodAt)
            }
        }
    }

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

    private fun fetchCallLogs(period: String) {
        Log.d("MainActivity", "Fetching contact names")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            fetchContactNames()
        }

        // Clear previous call logs to avoid duplicates
        callLogs.clear()

        val cursor: Cursor? = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            null,
            null,
            CallLog.Calls.DEFAULT_SORT_ORDER
        )

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

                if (number !in callLogs) {
                    callLogs[number] = mutableMapOf(
                        "total" to 0,
                        "year" to 0,
                        "month" to 0,
                        "week" to 0,
                        "day" to 0
                    )
                }

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

        updateRecyclerView(period)
    }

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

    private fun normalizePhoneNumber(number: String): String {
        return number.replace(Regex("[^\\d]"), "")
    }

    private fun updateRecyclerView(period: String) {
        Log.d("MainActivity", "Updating RecyclerView for period: $period")

        val filteredLogs = callLogs.mapValues { it.value[period] ?: 0 }.filter { it.value > 0 }
        val sortedLogs = filteredLogs.toList().sortedByDescending { it.second }.toMap()
        val displayLogs = sortedLogs.mapKeys { contactNames[it.key] ?: it.key }

        Log.d("MainActivity", "Filtered logs: $filteredLogs")
        Log.d("MainActivity", "Sorted logs: $sortedLogs")
        Log.d("MainActivity", "Display logs: $displayLogs")

        numberAdapter = NumberAdapter(displayLogs)
        recyclerView.adapter = numberAdapter
    }
}
