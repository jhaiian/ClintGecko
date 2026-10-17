package com.jhaiian.clint.mediacapture

object MediaCodecNames {

    private val VIDEO_PREFIXES = listOf("avc1", "avc3", "hev1", "hvc1", "dvh1", "dvhe", "vp8", "vp9", "vp09", "vp08", "av01", "mp4v", "theora")
    private val AUDIO_PREFIXES = listOf("mp4a", "ac-3", "ec-3", "ac-4", "opus", "vorbis", "flac", "alac", "mp3")

    private fun parts(codecs: String?): List<String> =
        codecs?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    fun videoCodecs(codecs: String?): List<String> =
        parts(codecs).filter { part -> VIDEO_PREFIXES.any { part.lowercase().startsWith(it) } }

    fun audioCodecs(codecs: String?): List<String> =
        parts(codecs).filter { part -> AUDIO_PREFIXES.any { part.lowercase().startsWith(it) } }

    fun audioCodecString(codecs: String?): String? =
        audioCodecs(codecs).takeIf { it.isNotEmpty() }?.joinToString(",")

    fun videoLabel(codecs: String?): String? = videoCodecs(codecs).firstOrNull()?.let(::friendlyVideo)

    fun audioLabel(codecs: String?): String? = audioCodecs(codecs).firstOrNull()?.let(::friendlyAudio)

    private fun friendlyVideo(codec: String): String {
        val c = codec.lowercase()
        return when {
            c.startsWith("avc") -> "H.264"
            c.startsWith("hev1") || c.startsWith("hvc1") -> "H.265"
            c.startsWith("dvh") -> "Dolby Vision"
            c.startsWith("vp09") || c.startsWith("vp9") -> "VP9"
            c.startsWith("vp08") || c.startsWith("vp8") -> "VP8"
            c.startsWith("av01") -> "AV1"
            c.startsWith("mp4v") -> "MPEG-4"
            c.startsWith("theora") -> "Theora"
            else -> codec
        }
    }

    private fun friendlyAudio(codec: String): String {
        val c = codec.lowercase()
        return when {
            c == "mp4a.40.2" -> "AAC-LC"
            c == "mp4a.40.5" -> "HE-AAC"
            c == "mp4a.40.29" -> "HE-AACv2"
            c == "mp4a.6b" || c == "mp4a.69" || c == "mp3" -> "MP3"
            c.startsWith("mp4a") -> "AAC"
            c.startsWith("ac-3") -> "AC-3"
            c.startsWith("ec-3") -> "E-AC-3"
            c.startsWith("ac-4") -> "AC-4"
            c.startsWith("opus") -> "Opus"
            c.startsWith("vorbis") -> "Vorbis"
            c.startsWith("flac") -> "FLAC"
            c.startsWith("alac") -> "ALAC"
            else -> codec
        }
    }

    fun parseFrameRate(value: String?): Double? {
        val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val slash = raw.indexOf('/')
        val rate = if (slash > 0) {
            val num = raw.substring(0, slash).toDoubleOrNull()
            val den = raw.substring(slash + 1).toDoubleOrNull()
            if (num != null && den != null && den > 0.0) num / den else null
        } else raw.toDoubleOrNull()
        return rate?.takeIf { it > 0.0 }
    }
}
