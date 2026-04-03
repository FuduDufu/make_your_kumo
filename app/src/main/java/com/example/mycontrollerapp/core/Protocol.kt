package com.example.mycontrollerapp.core

class Protocol {

    // SET command (0x53 | 0x80 = 0xD3)
    private val CMD_SET = (0x53 or 0x80).toByte()

    // GET command (0x47 | 0x80 = 0xC7)
    private val CMD_GET = (0x47 or 0x80).toByte()

    /**
     * Set consecutive pins
     * startIndex = pin index
     * values = 14-bit values
     */
    fun encodeSet(startIndex: Int, values: IntArray): ByteArray {
        val out = ByteArray(3 + values.size * 2)
        out[0] = CMD_SET
        out[1] = (startIndex and 0x7F).toByte()
        out[2] = (values.size and 0x7F).toByte()

        var o = 3
        for (v in values) {
            out[o++] = (v and 0x7F).toByte()
            out[o++] = ((v shr 7) and 0x7F).toByte()
        }
        return out
    }

    /**
     * Get consecutive pins
     */
    fun encodeGet(startIndex: Int, count: Int): ByteArray {
        return byteArrayOf(
            CMD_GET,
            (startIndex and 0x7F).toByte(),
            (count and 0x7F).toByte()
        )
    }

    /**
     * Decode 14-bit value
     */
    fun decode14(low: Byte, high: Byte): Int {
        return (low.toInt() and 0x7F) or
                ((high.toInt() and 0x7F) shl 7)
    }
}