package com.example.mycontrollerapp.ui

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.mycontrollerapp.R
import com.example.mycontrollerapp.core.ControllerEngine
import com.example.mycontrollerapp.core.Mode
import com.example.mycontrollerapp.usb.UsbSerialManager
import com.example.mycontrollerapp.ui.NetworkInfo.getLocalIp
import com.example.mycontrollerapp.ui.view.InfoSurfaceView
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import com.serenegiant.widget.UVCCameraTextureView


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private lateinit var engine: ControllerEngine
    private lateinit var usb: UsbSerialManager

    private lateinit var uvcView: UVCCameraTextureView

    private var usbMonitor: USBMonitor? = null
    private var camera: UVCCamera? = null

    private var cameraStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen)

        usb = UsbSerialManager(this)
        usb.register()

        engine = ControllerEngine(applicationContext, usb)
        engine.start()

        val info = findViewById<InfoSurfaceView>(R.id.infoSurfaceView)

        val ip = getLocalIp(this) ?: "---"

        engine.listener = { t ->
            runOnUiThread {
                info.setTouchStates(t.touch)
                info.setTelemetry(
                    voltage = t.voltage,
                    current = t.current,
                    bps = null,
                    ip = ip
                )
            }
        }

        info.setTelemetry(null, null, null, ip)

        findViewById<Button>(R.id.buttonBlock).setOnClickListener {
            engine.setMode(Mode.BLOCK)
        }

        findViewById<Button>(R.id.buttonTorque).setOnClickListener {
            engine.setMode(Mode.TORQUE)
        }

        findViewById<Button>(R.id.buttonExit).setOnClickListener {
            finishAffinity()
        }

        uvcView = findViewById(R.id.uvcView)

        usbMonitor = USBMonitor(this, deviceListener)

        val buttonCamera = findViewById<Button>(R.id.buttonCamera)

        buttonCamera.setOnClickListener {

            if (!cameraStarted) {

                usbMonitor?.register()
                cameraStarted = true

            } else {

                camera?.stopPreview()
                camera?.destroy()
                camera = null

                cameraStarted = false
            }
        }
    }

    private val deviceListener = object : USBMonitor.OnDeviceConnectListener {

        override fun onAttach(device: android.hardware.usb.UsbDevice?) {
            Log.d(TAG, "USB attached")
        }

        override fun onConnect(
            device: android.hardware.usb.UsbDevice?,
            ctrlBlock: USBMonitor.UsbControlBlock?,
            createNew: Boolean
        ) {

            Log.d(TAG, "Camera connect")

            camera = UVCCamera()
            camera!!.open(ctrlBlock)

            camera!!.setPreviewSize(640, 480)

            camera!!.setPreviewTexture(uvcView.surfaceTexture)

            camera!!.startPreview()

        }

        override fun onDisconnect(
            device: android.hardware.usb.UsbDevice?,
            ctrlBlock: USBMonitor.UsbControlBlock?
        ) {
            camera?.destroy()
            camera = null
        }

        override fun onDettach(device: android.hardware.usb.UsbDevice?) {}

        override fun onCancel(device: android.hardware.usb.UsbDevice?) {}
    }

    override fun onDestroy() {
        engine.stop()
        usb.unregister()
        usb.disconnect()

        usbMonitor?.unregister()

        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {

        supportActionBar?.hide()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            window.setDecorFitsSystemWindows(false)

            window.insetsController?.let {

                it.hide(
                    WindowInsets.Type.statusBars() or
                            WindowInsets.Type.navigationBars()
                )

                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

        }
    }
}