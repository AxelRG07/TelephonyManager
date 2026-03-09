package com.example.telephonymanager.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

            // Verificamos si el teléfono está sonando
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

                // Leer datos guardados
                val sharedPref = context.getSharedPreferences("AutoReplyPrefs", Context.MODE_PRIVATE)
                val targetNumber = sharedPref.getString("TARGET_NUMBER", "")
                val replyMessage = sharedPref.getString("REPLY_MESSAGE", "")

                Log.d("CallReceiver", "Llamada entrante de: $incomingNumber")

                // Si el número entrante coincide con el configurado, enviamos el SMS
                if (!incomingNumber.isNullOrEmpty() && incomingNumber == targetNumber) {
                    sendSms(context, targetNumber, replyMessage ?: "Mensaje automático")
                }
            }
        }
    }

    private fun sendSms(context: Context, phoneNumber: String, message: String) {
        try {
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d("CallReceiver", "SMS enviado con éxito a $phoneNumber")
            showNotification(context, phoneNumber)
        } catch (e: Exception) {
            Log.e("CallReceiver", "Error al enviar SMS: ${e.message}")
        }
    }

    private fun showNotification(context: Context, phoneNumber: String) {
        val channelId = "auto_reply_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Avisos de Respuesta Automática",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifica cuando se envía un SMS automático"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Respuesta automática enviada")
            .setContentText("Se respondió al número: $phoneNumber")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}