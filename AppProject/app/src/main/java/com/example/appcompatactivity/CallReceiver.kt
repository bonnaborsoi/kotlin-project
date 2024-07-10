package com.example.appcompatactivity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val state = intent?.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent?.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        if (state == TelephonyManager.EXTRA_STATE_RINGING) {
            Log.d("CallReceiver", "Incoming call from: $incomingNumber")
            // Envie um broadcast informando que houve uma chamada recebida
            val broadcastIntent = Intent("com.example.appcompatactivity.CALL_RECEIVED")
            context?.sendBroadcast(broadcastIntent)
        } else if (state == TelephonyManager.EXTRA_STATE_OFFHOOK) {
            Log.d("CallReceiver", "Outgoing call to: $incomingNumber")
            // Envie um broadcast informando que houve uma chamada efetuada
            val broadcastIntent = Intent("com.example.appcompatactivity.CALL_RECEIVED")
            context?.sendBroadcast(broadcastIntent)
        }
    }
}
