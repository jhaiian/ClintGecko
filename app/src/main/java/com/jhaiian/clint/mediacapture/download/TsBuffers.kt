package com.jhaiian.clint.mediacapture.download

internal class TsUnsupportedException(message: String) : Exception(message)

internal class GrowableBytes(initialCapacity: Int = 1 shl 16) {
    var data = ByteArray(initialCapacity)
        private set
    var size = 0
        private set

    fun add(src: ByteArray, offset: Int, length: Int) {
        val needed = size + length
        if (needed > data.size) {
            var capacity = data.size
            while (capacity < needed) capacity = capacity shl 1
            data = data.copyOf(capacity)
        }
        System.arraycopy(src, offset, data, size, length)
        size += length
    }

    fun clear() {
        size = 0
    }

    fun dropFront(count: Int) {
        if (count <= 0) return
        if (count >= size) {
            size = 0
            return
        }
        System.arraycopy(data, count, data, 0, size - count)
        size -= count
    }
}

internal class IntList {
    private var data = IntArray(1024)
    var size = 0
        private set

    fun add(value: Int) {
        if (size == data.size) data = data.copyOf(size * 2)
        data[size++] = value
    }

    operator fun get(index: Int): Int = data[index]

    fun last(): Int = data[size - 1]

    fun setLast(value: Int) {
        data[size - 1] = value
    }
}

internal class LongList {
    private var data = LongArray(1024)
    var size = 0
        private set

    fun add(value: Long) {
        if (size == data.size) data = data.copyOf(size * 2)
        data[size++] = value
    }

    operator fun get(index: Int): Long = data[index]
}

internal class ByteBuf(capacity: Int = 1024) {
    var data = ByteArray(capacity)
        private set
    var size = 0
        private set

    private fun ensure(extra: Int) {
        val needed = size + extra
        if (needed > data.size) {
            var capacity = data.size
            while (capacity < needed) capacity = capacity shl 1
            data = data.copyOf(capacity)
        }
    }

    fun u8(value: Int) {
        ensure(1)
        data[size++] = value.toByte()
    }

    fun u16(value: Int) {
        u8(value shr 8)
        u8(value)
    }

    fun u32(value: Long) {
        u8((value shr 24).toInt())
        u8((value shr 16).toInt())
        u8((value shr 8).toInt())
        u8(value.toInt())
    }

    fun u64(value: Long) {
        u32(value ushr 32)
        u32(value and 0xFFFFFFFFL)
    }

    fun bytes(src: ByteArray, offset: Int = 0, length: Int = src.size) {
        ensure(length)
        System.arraycopy(src, offset, data, size, length)
        size += length
    }

    fun zeros(count: Int) {
        ensure(count)
        java.util.Arrays.fill(data, size, size + count, 0)
        size += count
    }

    fun fourcc(text: String) {
        for (ch in text) u8(ch.code)
    }

    fun clear() {
        size = 0
    }

    fun toByteArray(): ByteArray = data.copyOf(size)

    fun box(type: String, body: ByteBuf.() -> Unit) {
        val start = size
        u32(0L)
        fourcc(type)
        body()
        patchU32(start, (size - start).toLong())
    }

    fun fullBox(type: String, version: Int, flags: Int, body: ByteBuf.() -> Unit) {
        box(type) {
            u8(version)
            u8(flags shr 16)
            u8(flags shr 8)
            u8(flags)
            body()
        }
    }

    fun patchU32(at: Int, value: Long) {
        data[at] = (value shr 24).toByte()
        data[at + 1] = (value shr 16).toByte()
        data[at + 2] = (value shr 8).toByte()
        data[at + 3] = value.toByte()
    }
}

internal class SpsInfo(
    val profileIdc: Int,
    val constraintFlags: Int,
    val levelIdc: Int,
    val chromaFormat: Int,
    val bitDepthLuma: Int,
    val bitDepthChroma: Int,
    val width: Int,
    val height: Int,
    val sarWidth: Int,
    val sarHeight: Int
)

private class BitReader(private val data: ByteArray) {
    private var position = 0

    fun bit(): Int {
        val index = position shr 3
        if (index >= data.size) throw IllegalStateException("SPS truncated")
        val value = (data[index].toInt() shr (7 - (position and 7))) and 1
        position++
        return value
    }

    fun bits(count: Int): Int {
        var value = 0
        for (i in 0 until count) value = (value shl 1) or bit()
        return value
    }

    fun ue(): Int {
        var zeros = 0
        while (bit() == 0) {
            zeros++
            if (zeros > 30) throw IllegalStateException("Invalid exp-golomb")
        }
        return if (zeros == 0) 0 else (1 shl zeros) - 1 + bits(zeros)
    }

    fun se(): Int {
        val k = ue()
        return if (k and 1 == 1) (k + 1) / 2 else -(k / 2)
    }
}

internal object H264Sps {

    private val HIGH_PROFILES = setOf(100, 110, 122, 244, 44, 83, 86, 118, 128, 138, 139, 134, 135)

    private val SAR_TABLE = arrayOf(
        intArrayOf(1, 1), intArrayOf(12, 11), intArrayOf(10, 11), intArrayOf(16, 11),
        intArrayOf(40, 33), intArrayOf(24, 11), intArrayOf(20, 11), intArrayOf(32, 11),
        intArrayOf(80, 33), intArrayOf(18, 11), intArrayOf(15, 11), intArrayOf(64, 33),
        intArrayOf(160, 99), intArrayOf(4, 3), intArrayOf(3, 2), intArrayOf(2, 1)
    )

    private fun unescape(nal: ByteArray, from: Int): ByteArray {
        val out = ByteArray(nal.size - from)
        var length = 0
        var zeros = 0
        for (i in from until nal.size) {
            val value = nal[i].toInt() and 0xFF
            if (zeros >= 2 && value == 3) {
                zeros = 0
                continue
            }
            out[length++] = value.toByte()
            zeros = if (value == 0) zeros + 1 else 0
        }
        return out.copyOf(length)
    }

    private fun skipScalingList(reader: BitReader, size: Int) {
        var last = 8
        var next = 8
        for (j in 0 until size) {
            if (next != 0) {
                val delta = reader.se()
                next = (last + delta + 256) % 256
            }
            if (next != 0) last = next
        }
    }

    fun parse(nal: ByteArray): SpsInfo {
        val reader = BitReader(unescape(nal, 1))
        val profile = reader.bits(8)
        val constraints = reader.bits(8)
        val level = reader.bits(8)
        reader.ue()
        var chroma = 1
        var separateColour = 0
        var depthLuma = 8
        var depthChroma = 8
        if (profile in HIGH_PROFILES) {
            chroma = reader.ue()
            if (chroma == 3) separateColour = reader.bit()
            depthLuma = reader.ue() + 8
            depthChroma = reader.ue() + 8
            reader.bit()
            if (reader.bit() == 1) {
                val lists = if (chroma != 3) 8 else 12
                for (i in 0 until lists) {
                    if (reader.bit() == 1) skipScalingList(reader, if (i < 6) 16 else 64)
                }
            }
        }
        reader.ue()
        val pocType = reader.ue()
        if (pocType == 0) {
            reader.ue()
        } else if (pocType == 1) {
            reader.bit()
            reader.se()
            reader.se()
            val cycle = reader.ue()
            for (i in 0 until cycle) reader.se()
        }
        reader.ue()
        reader.bit()
        val widthMbs = reader.ue() + 1
        val heightMapUnits = reader.ue() + 1
        val frameMbsOnly = reader.bit()
        if (frameMbsOnly == 0) reader.bit()
        reader.bit()
        var cropLeft = 0
        var cropRight = 0
        var cropTop = 0
        var cropBottom = 0
        if (reader.bit() == 1) {
            cropLeft = reader.ue()
            cropRight = reader.ue()
            cropTop = reader.ue()
            cropBottom = reader.ue()
        }
        val chromaArrayType = if (separateColour == 1) 0 else chroma
        val cropUnitX = if (chromaArrayType == 0 || chromaArrayType == 3) 1 else 2
        val cropUnitY = (if (chromaArrayType == 1) 2 else 1) * (2 - frameMbsOnly)
        val width = widthMbs * 16 - cropUnitX * (cropLeft + cropRight)
        val height = (2 - frameMbsOnly) * heightMapUnits * 16 - cropUnitY * (cropTop + cropBottom)
        var sarWidth = 1
        var sarHeight = 1
        runCatching {
            if (reader.bit() == 1 && reader.bit() == 1) {
                val idc = reader.bits(8)
                if (idc == 255) {
                    sarWidth = reader.bits(16)
                    sarHeight = reader.bits(16)
                } else if (idc in 1..SAR_TABLE.size) {
                    sarWidth = SAR_TABLE[idc - 1][0]
                    sarHeight = SAR_TABLE[idc - 1][1]
                }
            }
        }
        if (width <= 0 || height <= 0) throw IllegalStateException("Invalid SPS dimensions")
        return SpsInfo(profile, constraints, level, chroma, depthLuma, depthChroma, width, height, sarWidth, sarHeight)
    }
}
