package com.cmc.flashcallingapp

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cmc.flashcallingapp.R

class MainActivity : AppCompatActivity() {

    private var isFlashOn = false
    private var isBlinking = false
    private var isFlashEnabled = false
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private lateinit var telephonyManager: TelephonyManager
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize CameraManager
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager.cameraIdList[0] // Use first camera (usually rear camera)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        // Initialize TelephonyManager and listen for phone state changes
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)

        // Switch to enable/disable flashlight blinking feature
        val flashlightSwitch: Switch = findViewById(R.id.flashlightSwitch)
        flashlightSwitch.setOnCheckedChangeListener { _, isChecked ->
            isFlashEnabled = isChecked
            val status = if (isChecked) "enabled" else "disabled"
            Toast.makeText(this, "Flashlight blink $status", Toast.LENGTH_SHORT).show()
        }
    }

    // PhoneStateListener to listen for incoming calls
    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    if (isFlashEnabled) {
                        startBlinkingFlashlight()
                    }
                }
                TelephonyManager.CALL_STATE_IDLE, TelephonyManager.CALL_STATE_OFFHOOK -> {
                    stopBlinkingFlashlight() // Stop blinking when the call is answered or finished
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
        turnOffFlashlight()
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

    override fun onDestroy() {
        super.onDestroy()
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE) // Unregister listener
        stopBlinkingFlashlight()
    }
}