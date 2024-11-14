package com.cmc.flashcallingapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import com.cmc.flashcallingapp.R

class FlashlightService : Service() {

    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private var isFlashOn = false
    private var isBlinking = false
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var telephonyManager: TelephonyManager

    override fun onCreate() {
        super.onCreate()

        // Initialize CameraManager
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList[0] // Use the first camera (usually back camera)

        // Set up the telephony manager to listen for phone state changes
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)

        // Start the foreground service
        startForegroundService()
    }

    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    // Incoming call detected, start blinking flashlight
                    if (isBlinking) startBlinkingFlashlight()
                }
                TelephonyManager.CALL_STATE_IDLE, TelephonyManager.CALL_STATE_OFFHOOK -> {
                    // Call ended or answered, stop blinking flashlight
                    stopBlinkingFlashlight()
                }
            }
        }
    }

    // Function to start blinking the flashlight
    private fun startBlinkingFlashlight() {
        isBlinking = true
        toggleFlashlight()
    }

    // Function to stop blinking the flashlight
    private fun stopBlinkingFlashlight() {
        isBlinking = false
        handler.removeCallbacksAndMessages(null) // Stop any ongoing toggling
        turnOffFlashlight() // Ensure flashlight is turned off
    }

    // Function to toggle the flashlight on and off every 0.4 seconds
    private fun toggleFlashlight() {
        if (!isBlinking) return

        if (isFlashOn) {
            turnOffFlashlight()
        } else {
            turnOnFlashlight()
        }

        handler.postDelayed({ toggleFlashlight() }, 400) // Toggle every 0.4 seconds
    }

    // Function to turn on the flashlight
    private fun turnOnFlashlight() {
        try {
            cameraManager.setTorchMode(cameraId, true)
            isFlashOn = true
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    // Function to turn off the flashlight
    private fun turnOffFlashlight() {
        try {
            cameraManager.setTorchMode(cameraId, false)
            isFlashOn = false
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    // Start the service as a foreground service with a notification
    private fun startForegroundService() {
        val notificationChannelId = "flashlight_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "Flashlight Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Flashlight Service")
            .setContentText("Flashlight will blink during incoming calls.")
            .setSmallIcon(R.drawable.ic_flashlight)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBlinkingFlashlight()
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}