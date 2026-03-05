package com.folio.util

import android.content.Context
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

/**
 * Kotlin extension functions used throughout Folio.
 */

/**
 * Show a short toast message.
 */
fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/**
 * Show a long toast message.
 */
fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

/**
 * Format a timestamp to relative time string.
 * e.g. "Just now", "5 min ago", "2 hours ago", "Yesterday", "Mar 5"
 */
fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(this))
        }
    }
}

/**
 * Format bytes to human-readable file size.
 */
fun Long.toFileSize(): String {
    return when {
        this < 1024 -> "$this B"
        this < 1024 * 1024 -> "${this / 1024} KB"
        this < 1024 * 1024 * 1024 -> String.format("%.1f MB", this / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", this / (1024.0 * 1024.0 * 1024.0))
    }
}

/**
 * Validate a page range string like "1-3, 5, 8-12".
 * Returns true if format is valid, false otherwise.
 */
fun String.isValidPageRange(maxPage: Int): Boolean {
    if (this.isBlank()) return false
    val parts = this.split(",").map { it.trim() }
    return parts.all { part ->
        if (part.contains("-")) {
            val range = part.split("-").map { it.trim() }
            if (range.size != 2) return false
            val start = range[0].toIntOrNull() ?: return false
            val end = range[1].toIntOrNull() ?: return false
            start in 1..maxPage && end in 1..maxPage && start <= end
        } else {
            val page = part.toIntOrNull() ?: return false
            page in 1..maxPage
        }
    }
}

/**
 * Parse a page range string into a list of page indices (0-based).
 */
fun String.parsePageRange(): List<Int> {
    val pages = mutableSetOf<Int>()
    val parts = this.split(",").map { it.trim() }
    for (part in parts) {
        if (part.contains("-")) {
            val range = part.split("-").map { it.trim().toInt() }
            for (i in range[0]..range[1]) {
                pages.add(i - 1)  // Convert to 0-based
            }
        } else {
            pages.add(part.toInt() - 1)  // Convert to 0-based
        }
    }
    return pages.sorted()
}
