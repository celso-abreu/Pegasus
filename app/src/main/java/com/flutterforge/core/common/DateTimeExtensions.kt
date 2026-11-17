package com.flutterforge.core.common

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {

    fun formatRelativeTimePt(isoTimestamp: String): String {
        return runCatching {
            val instant = Instant.parse(isoTimestamp)
            val now = Instant.now()
            val duration = Duration.between(instant, now)
            val minutes = duration.toMinutes()
            val hours = duration.toHours()
            val days = duration.toDays()

            when {
                minutes < 1 -> "agora"
                minutes < 60 -> "há ${minutes} min"
                hours < 24 -> "há ${hours} h"
                days == 1L -> "há 1 dia"
                days < 30 -> "há ${days} dias"
                days < 365 -> {
                    val months = days / 30
                    if (months == 1L) "há 1 mês" else "há $months meses"
                }
                else -> {
                    val years = days / 365
                    if (years == 1L) "há 1 ano" else "há $years anos"
                }
            }
        }.getOrElse { isoTimestamp }
    }

    fun formatEpochTime(epochSeconds: Long): String {
        return runCatching {
            val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
                .withZone(ZoneId.systemDefault())
            formatter.format(Instant.ofEpochSecond(epochSeconds))
        }.getOrElse { "--:--" }
    }

    fun formatMillisDate(epochMillis: Long): String {
        return runCatching {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())
                .withZone(ZoneId.systemDefault())
            formatter.format(Instant.ofEpochMilli(epochMillis))
        }.getOrElse { "--" }
    }

    fun formatCountdown(seconds: Int): String {
        val safe = seconds.coerceAtLeast(0)
        val mins = safe / 60
        val secs = safe % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format(Locale.US, "%.2f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f GB", gb)
    }
}
