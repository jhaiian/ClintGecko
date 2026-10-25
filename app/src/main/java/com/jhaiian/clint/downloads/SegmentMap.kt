package com.jhaiian.clint.downloads

internal object SegmentMap {

    const val MAX_BUCKETS = 16384

    const val STRIP_BUCKETS = 128

    fun bucketCount(total: Int): Int = total.coerceIn(0, MAX_BUCKETS)

    fun build(total: Int, fraction: (Int) -> Float): List<Float> = build(total, bucketCount(total), fraction)

    fun downsample(fractions: List<Float>, buckets: Int): List<Float> {
        if (fractions.size <= buckets) return fractions
        return build(fractions.size, buckets) { fractions[it] }
    }

    private fun build(total: Int, buckets: Int, fraction: (Int) -> Float): List<Float> {
        if (buckets == total) return List(total) { fraction(it) }
        return List(buckets) { bucket ->
            val start = (bucket.toLong() * total / buckets).toInt()
            val end = ((bucket + 1).toLong() * total / buckets).toInt()
            var sum = 0f
            for (index in start until end) sum += fraction(index)
            if (end > start) sum / (end - start) else 0f
        }
    }
}
