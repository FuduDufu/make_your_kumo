package com.example.mycontrollerapp.core

import android.content.Context
import android.util.Log
import com.example.mycontrollerapp.usb.UsbSerialManager
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class ControllerEngine(
    private val appContext: Context,
    private val usb: UsbSerialManager? = null
) {
    private val tag = "ControllerEngine"
    private val protocol = Protocol()
    private val mapper = ServoMapper(ServoMapper.BoardScale.ANGLE_DEG_DIRECT)

    private val running = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor()
    private val rxBuf = ByteArray(256)

    data class TouchTelemetry(
        val touch: BooleanArray,
        val voltage: Double?,
        val current: Double?
    )

    var listener: ((TouchTelemetry) -> Unit)? = null

    private val config: RobotConfig by lazy {
        ConfigManager(appContext).load("config-2040.txt")
    }

    private val poseBuilder: RobotPoseBuilder by lazy {
        RobotPoseBuilder(config, mapper)
    }

    private val servoStartIndex: Int by lazy {
        config.servos
            .mapNotNull { parsePinIndex(it.pin) }
            .minOrNull() ?: 0
    }

    private val servoCount: Int by lazy {
        val minPin = config.servos.mapNotNull { parsePinIndex(it.pin) }.minOrNull() ?: 0
        val maxPin = config.servos.mapNotNull { parsePinIndex(it.pin) }.maxOrNull() ?: 17
        maxPin - minPin + 1
    }

    private val relayPinIndex: Int by lazy {
        parsePinIndex(config.relay?.pin ?: "P26") ?: 26
    }

    @Volatile
    var mode: Mode = Mode.BLOCK
        private set

    fun setMode(newMode: Mode) {
        mode = newMode
    }

    fun start() {
        if (running.getAndSet(true)) return

        executor.execute {
            Log.d(tag, "ENGINE LOOP START")
            usb?.connect(115200)

            var lastMode: Mode? = null

            while (running.get()) {
                val u = usb ?: run {
                    Thread.sleep(100)
                    continue
                }

                if (!u.isConnected()) {
                    u.connect(115200)
                    Thread.sleep(100)
                    continue
                }

                if (mode != lastMode) {
                    try {
                        // Relay ON
                        u.write(protocol.encodeSet(relayPinIndex, intArrayOf(1)), 200)

                        when (mode) {
                            Mode.BLOCK -> {
                                val blockAll = poseBuilder.buildBlockAllFromConfig()
                                val l1 = intArrayOf(
                                    blockAll[15],
                                    blockAll[16],
                                    blockAll[17]
                                )

                                u.write(protocol.encodeSet(15, l1), 200)

                                // u.write(protocol.encodeSet(0, blockAll), 200)
                            }

                            Mode.TORQUE -> {
                                val centerAll = poseBuilder.buildCenterAllFromConfig()
                                val l1 = intArrayOf(
                                    centerAll[15], // L11
                                    centerAll[16], // L12
                                    centerAll[17]  // L13
                                )
                                u.write(protocol.encodeSet(15, l1), 200)
                                //u.write(protocol.encodeSet(0, centerAll), 200)
                            }
                        }

                        lastMode = mode
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to send mode pose", e)
                    }
                }

                pollTelemetry(u)
                Thread.sleep(50)
            }

            Log.d(tag, "ENGINE LOOP STOP")
        }
    }

    fun stop() {
        running.set(false)
        try {
            usb?.disconnect()
        } catch (_: Exception) {
        }
        executor.shutdownNow()
    }

    private fun pollTelemetry(u: UsbSerialManager) {
        try {
            // P18..P25 => 6 touch + current + voltage
            u.write(protocol.encodeGet(18, 8), 200)

            val n = u.read(rxBuf, 50)
            if (n <= 0) return

            Log.d("RX", "n=$n hex=${hexDump(rxBuf, n)}")

            if ((rxBuf[0].toInt() and 0xFF) != 0xC7) return
            if (n < 19) return

            val start = rxBuf[1].toInt() and 0x7F
            val count = rxBuf[2].toInt() and 0x7F
            if (start != 18 || count != 8) return

            val values = IntArray(count)
            for (i in 0 until count) {
                val base = 3 + i * 2
                values[i] = protocol.decode14(rxBuf[base], rxBuf[base + 1])
            }

            Log.d(
                tag,
                "touchRaw=${values.slice(0..5)} cur=${values.getOrNull(6)} vol=${values.getOrNull(7)}"
            )

            val threshold = 500
            val touchStates = booleanArrayOf(
                values[5] > threshold, // L1 = P23
                values[3] > threshold, // L2 = P21
                values[1] > threshold, // L3 = P19
                values[4] > threshold, // R1 = P22
                values[2] > threshold, // R2 = P20
                values[0] > threshold  // R3 = P18
            )

            val current = values.getOrNull(6)?.toDouble()
            val voltage = values.getOrNull(7)?.toDouble()

            listener?.invoke(
                TouchTelemetry(
                    touch = touchStates,
                    voltage = voltage,
                    current = current
                )
            )
        } catch (e: Exception) {
            Log.e(tag, "pollTelemetry failed", e)
        }
    }

    private fun hexDump(buf: ByteArray, n: Int): String {
        val sb = StringBuilder(n * 3)
        for (i in 0 until min(n, buf.size)) {
            sb.append(String.format("%02X ", buf[i]))
        }
        return sb.toString()
    }

    private fun parsePinIndex(pin: String): Int? {
        if (!pin.startsWith("P")) return null
        return pin.drop(1).toIntOrNull()
    }
}

enum class Mode {
    BLOCK,
    TORQUE
}