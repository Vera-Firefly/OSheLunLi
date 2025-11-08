package com.firefly.oshe.lunli.utils

object Iso8601Converter {

    fun toUtcZeroOffsetFormat(timeSource: Any): String {
        return when (timeSource) {
            is Long -> formatFromTimestamp(timeSource)
            is String -> formatFromString(timeSource)
            is java.util.Date -> formatFromTimestamp(timeSource.time)
            is java.util.Calendar -> formatFromTimestamp(timeSource.timeInMillis)
            is java.time.Instant -> formatFromTimestamp(timeSource.toEpochMilli())
            is java.time.LocalDateTime -> formatFromLocalDateTime(timeSource)
            else -> formatFromTimestamp(System.currentTimeMillis())
        }
    }

    fun nowAsUtcZeroOffset(): String {
        return formatFromTimestamp(System.currentTimeMillis())
    }

    fun toUtcZeroOffsetTimestamp(timeSource: Any): Long {
        return when (timeSource) {
            is Long -> timeSource
            is String -> parseToTimestamp(timeSource)
            is java.util.Date -> timeSource.time
            is java.util.Calendar -> timeSource.timeInMillis
            is java.time.Instant -> timeSource.toEpochMilli()
            is java.time.LocalDateTime -> timeSource.atZone(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            else -> System.currentTimeMillis()
        }
    }

    fun parseUtcZeroOffsetFormat(isoString: String): Long {
        return parseToTimestamp(isoString)
    }

    fun nowAsUtcZeroOffsetTimestamp(): Long {
        return System.currentTimeMillis()
    }

    private fun formatFromTimestamp(timestamp: Long): String {
        val instant = java.time.Instant.ofEpochMilli(timestamp)
        val offsetDateTime = instant.atOffset(java.time.ZoneOffset.UTC)

        return buildString {
            append(offsetDateTime.year)
            append("-")
            append(offsetDateTime.monthValue.toString().padStart(2, '0'))
            append("-")
            append(offsetDateTime.dayOfMonth.toString().padStart(2, '0'))
            append("T")
            append(offsetDateTime.hour.toString().padStart(2, '0'))
            append(":")
            append(offsetDateTime.minute.toString().padStart(2, '0'))
            append(":")
            append(offsetDateTime.second.toString().padStart(2, '0'))
            append(".")
            append((offsetDateTime.nano / 1000).toString().padStart(6, '0')) // 微秒
            append("+0000")
        }
    }

    private fun formatFromString(isoString: String): String {
        val timestamp = parseToTimestamp(isoString)
        return formatFromTimestamp(timestamp)
    }

    private fun formatFromLocalDateTime(localDateTime: java.time.LocalDateTime): String {
        val instant = localDateTime.atZone(java.time.ZoneId.systemDefault())
            .toInstant()
        return formatFromTimestamp(instant.toEpochMilli())
    }

    private fun parseToTimestamp(isoString: String): Long {
        return try {
            java.time.Instant.parse(isoString).toEpochMilli()
        } catch (e: Exception) {
            try {
                val processedString = preprocessTimeString(isoString)
                java.time.Instant.parse(processedString).toEpochMilli()
            } catch (e2: Exception) {
                try {
                    java.time.OffsetDateTime.parse(isoString).toInstant().toEpochMilli()
                } catch (e3: Exception) {
                    parseManually(isoString)
                }
            }
        }
    }

    private fun preprocessTimeString(isoString: String): String {
        return when {
            isoString.contains(Regex("\\+\\d{4}$")) -> {
                isoString.replace(Regex("(\\+)(\\d{2})(\\d{2})$"), "$1$2:$3")
            }
            isoString.contains(Regex("-\\d{4}$")) -> {
                isoString.replace(Regex("(-)(\\d{2})(\\d{2})$"), "$1$2:$3")
            }
            else -> isoString
        }
    }

    private fun parseManually(isoString: String): Long {
        return try {
            val pattern = Regex("""(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})\.(\d{6})([+-]\d{4})""")
            val match = pattern.find(isoString) ?: return System.currentTimeMillis()

            val (year, month, day, hour, minute, second, micros, offset) = match.destructured

            val localDateTime = java.time.LocalDateTime.of(
                year.toInt(),
                month.toInt(),
                day.toInt(),
                hour.toInt(),
                minute.toInt(),
                second.toInt(),
                micros.toInt() * 1000
            )

            val zoneOffset = java.time.ZoneOffset.of(offset)
            localDateTime.toInstant(zoneOffset).toEpochMilli()

        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}