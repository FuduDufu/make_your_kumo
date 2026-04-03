package com.example.mycontrollerapp.usb

import android.app.PendingIntent
import android.content.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber

class UsbSerialManager(
    private val context: Context
) {
    private val TAG = "UsbSerialManager"
    private val ACTION_USB_PERMISSION = "com.example.mycontrollerapp.USB_PERMISSION"

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private var connection: UsbDeviceConnection? = null
    private var port: UsbSerialPort? = null

    private val permissionIntent: PendingIntent by lazy {
        val intent = Intent(ACTION_USB_PERMISSION)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    // FIX 1: Permission granted болсны дараа auto-connect хийдэг болгов.
    // port == null шалгалтаар infinite loop-оос сэргийлнэ —
    // hasPermission() true болсон тул connect() шууд open руу орно, давтагдахгүй.
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action != ACTION_USB_PERMISSION) return
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            Log.d(TAG, "USB permission result granted=$granted")

            if (granted && port == null) {
                Log.d(TAG, "Permission granted → auto-connecting...")
                connect()
            }
        }
    }

    fun register() {
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(
                receiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    fun unregister() {
        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
    }

    /**
     * 1) device хайна
     * 2) permission байхгүй бол request
     * 3) port open + setParameters
     */
    fun connect(baudRate: Int = 115200): Boolean {
        if (port != null) {
            Log.d(TAG, "Already connected")
            return true
        }
        val deviceList = usbManager.deviceList
        for (d in deviceList.values) {
            Log.d(TAG, "USB DEVICE FOUND vid=${d.vendorId} pid=${d.productId}")
        }

        val driver = findFirstDriver() ?: run {
            Log.d(TAG, "No USB serial driver found")
            return false
        }

        val device = driver.device
        if (!usbManager.hasPermission(device)) {
            Log.d(TAG, "No permission. Requesting...")
            usbManager.requestPermission(device, permissionIntent)
            return false
        }

        val conn = usbManager.openDevice(device) ?: run {
            Log.d(TAG, "openDevice returned null")
            return false
        }

        val p = driver.ports.firstOrNull() ?: run {
            Log.d(TAG, "No ports")
            conn.close()
            return false
        }

        return try {
            p.open(conn)
            p.setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            p.setDTR(true)
            p.setRTS(true)

            connection = conn
            port = p
            Log.d(TAG, "Connected OK: ${device.vendorId}:${device.productId}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "connect failed", e)
            try { p.close() } catch (_: Exception) {}
            try { conn.close() } catch (_: Exception) {}
            port = null
            connection = null
            false
        }
    }

    fun isConnected(): Boolean = port != null

    fun write(data: ByteArray, timeoutMs: Int = 200): Int {
        val p: UsbSerialPort = port ?: return -1
        return try {
            p.write(data, timeoutMs)
            data.size
        } catch (e: Exception) {
            Log.e(TAG, "write failed", e)
            -1
        }
    }

    fun read(buffer: ByteArray, timeoutMs: Int = 200): Int {
        val p = port ?: return -1
        return try {
            p.read(buffer, timeoutMs)
        } catch (e: Exception) {
            Log.e(TAG, "read failed", e)
            -1
        }
    }

    fun disconnect() {
        try { port?.close() } catch (_: Exception) {}
        try { connection?.close() } catch (_: Exception) {}
        port = null
        connection = null
        Log.d(TAG, "Disconnected")
    }

    // FIX 2: Default prober-оор эхэлж хайна (илүү өргөн coverage),
    // олдохгүй бол Servo2040-ийн VID/PID-тай custom probe руу fallback хийнэ.
    private fun findFirstDriver(): UsbSerialDriver? {
        val defaultDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (defaultDrivers.isNotEmpty()) {
            Log.d(TAG, "Driver found via default prober: ${defaultDrivers.first().device.vendorId}:${defaultDrivers.first().device.productId}")
            return defaultDrivers.first()
        }

        Log.d(TAG, "Default prober found nothing. Trying custom probe table...")
        val probeTable = ProbeTable().apply {
            addProduct(8187, 137, CdcAcmSerialDriver::class.java)
            addProduct(8187, 138, CdcAcmSerialDriver::class.java)
            addProduct(8187, 139, CdcAcmSerialDriver::class.java)
            addProduct(8187, 140, CdcAcmSerialDriver::class.java)
        }

        val customDrivers = UsbSerialProber(probeTable).findAllDrivers(usbManager)
        if (customDrivers.isNotEmpty()) {
            Log.d(TAG, "Driver found via custom probe: ${customDrivers.first().device.vendorId}:${customDrivers.first().device.productId}")
        } else {
            Log.w(TAG, "No driver found. Check VID/PID in logs above.")
        }
        return customDrivers.firstOrNull()
    }
}