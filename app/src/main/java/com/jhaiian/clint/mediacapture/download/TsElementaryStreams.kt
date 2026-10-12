package com.jhaiian.clint.mediacapture.download

internal class TsMarker(val position: Long, val pts: Long, val dts: Long)

internal abstract class TsElementaryStream {
    protected val buffer = GrowableBytes()
    protected var baseOffset = 0L
    private val markers = java.util.ArrayDeque<TsMarker>()

    fun append(src: ByteArray, offset: Int, length: Int, pts: Long, dts: Long) {
        if (pts >= 0L) markers.addLast(TsMarker(baseOffset + buffer.size, pts, dts))
        buffer.add(src, offset, length)
        process(false)
    }

    fun finish() {
        process(true)
    }

    protected abstract fun process(final: Boolean)

    protected fun takeMarker(absolutePosition: Long): TsMarker? {
        var found: TsMarker? = null
        while (true) {
            val head = markers.peekFirst() ?: break
            if (head.position > absolutePosition) break
            markers.pollFirst()
            found = head
        }
        return found
    }

    protected fun discard(count: Int) {
        buffer.dropFront(count)
        baseOffset += count
    }
}

internal class TsVideoStream(
    private val onAccessUnit: (data: ByteArray, offset: Int, length: Int, pts: Long, dts: Long) -> Unit
) : TsElementaryStream() {

    private var scanPosition = 0
    private var accessUnitStart = -1
    private var sawSlice = false

    override fun process(final: Boolean) {
        val data = buffer.data
        val size = buffer.size
        var i = scanPosition
        while (i + 2 < size) {
            if (data[i].toInt() == 0 && data[i + 1].toInt() == 0 && data[i + 2].toInt() == 1) {
                val header = i + 3
                if (header + 1 >= size && !final) break
                if (header >= size) {
                    i = size
                    break
                }
                val type = data[header].toInt() and 0x1F
                val firstMacroblockZero = header + 1 < size && (data[header + 1].toInt() and 0x80) != 0
                onNal(i, type, firstMacroblockZero)
                i = header + 1
            } else {
                i++
            }
        }
        scanPosition = i
        if (final && accessUnitStart >= 0 && sawSlice) {
            emit(accessUnitStart, size)
            accessUnitStart = -1
            sawSlice = false
        }
        if (accessUnitStart > 0) {
            val drop = accessUnitStart
            discard(drop)
            scanPosition -= drop
            accessUnitStart = 0
        } else if (accessUnitStart < 0) {
            discard(minOf(scanPosition, buffer.size))
            scanPosition = 0
        }
    }

    private fun onNal(position: Int, type: Int, firstMacroblockZero: Boolean) {
        if (accessUnitStart < 0) {
            accessUnitStart = position
        } else if (sawSlice && (type in 6..9 || type in 14..18 || (type in 1..5 && firstMacroblockZero))) {
            emit(accessUnitStart, position)
            accessUnitStart = position
            sawSlice = false
        }
        if (type in 1..5) sawSlice = true
    }

    private fun emit(start: Int, end: Int) {
        val marker = takeMarker(baseOffset + start)
        onAccessUnit(buffer.data, start, end - start, marker?.pts ?: -1L, marker?.dts ?: -1L)
    }
}

internal class AdtsParams(val profile: Int, val samplingIndex: Int, val channelConfig: Int)

internal class TsAdtsStream(
    private val onParams: (AdtsParams) -> Unit,
    private val onFrame: (data: ByteArray, offset: Int, length: Int, pts: Long) -> Unit
) : TsElementaryStream() {

    private var paramsReported = false

    private fun isSync(data: ByteArray, at: Int): Boolean =
        (data[at].toInt() and 0xFF) == 0xFF && (data[at + 1].toInt() and 0xF6) == 0xF0

    override fun process(final: Boolean) {
        val data = buffer.data
        val size = buffer.size
        var pos = 0
        while (pos + 7 <= size) {
            if (!isSync(data, pos)) {
                pos++
                continue
            }
            val protectionAbsent = data[pos + 1].toInt() and 1
            val profile = (data[pos + 2].toInt() shr 6) and 3
            val samplingIndex = (data[pos + 2].toInt() shr 2) and 0xF
            val channels = ((data[pos + 2].toInt() and 1) shl 2) or ((data[pos + 3].toInt() shr 6) and 3)
            val frameLength = ((data[pos + 3].toInt() and 3) shl 11) or
                ((data[pos + 4].toInt() and 0xFF) shl 3) or
                ((data[pos + 5].toInt() shr 5) and 7)
            val headerLength = if (protectionAbsent == 1) 7 else 9
            if (samplingIndex >= 13 || frameLength < headerLength) {
                pos++
                continue
            }
            if (pos + frameLength > size) {
                if (final) pos = size
                break
            }
            if (pos + frameLength + 2 <= size) {
                if (!isSync(data, pos + frameLength)) {
                    pos++
                    continue
                }
            } else if (!final) {
                break
            }
            if ((data[pos + 6].toInt() and 3) != 0) throw TsUnsupportedException("Multi-block ADTS frames")
            if (!paramsReported) {
                if (channels == 0) throw TsUnsupportedException("AAC channel configuration in stream")
                onParams(AdtsParams(profile, samplingIndex, channels))
                paramsReported = true
            }
            val marker = takeMarker(baseOffset + pos)
            onFrame(data, pos + headerLength, frameLength - headerLength, marker?.pts ?: -1L)
            pos += frameLength
        }
        discard(pos)
    }
}
