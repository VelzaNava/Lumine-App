package com.thesis.lumine.utils

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * "₱25,303.00" on every phone. Formatting with the device's own locale would
 * print "₱25.303,00" on a phone set to e.g. German or Indonesian.
 */
fun formatPeso(amount: Double): String = String.format(Locale.US, "₱%,.2f", amount)

private val DISPLAY_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

/**
 * A server timestamp as a date in the phone's time zone, e.g. "May 22, 2026".
 * Accepts ISO timestamps with an offset (current API) or without one (older
 * records, treated as UTC). Returns null for blank or unreadable input.
 */
fun formatDisplayDate(isoTimestamp: String?): String? {
    if (isoTimestamp.isNullOrBlank()) return null
    val instant = try {
        OffsetDateTime.parse(isoTimestamp).toInstant()
    } catch (e: DateTimeParseException) {
        try {
            LocalDateTime.parse(isoTimestamp).atZone(ZoneId.of("UTC")).toInstant()
        } catch (e: DateTimeParseException) {
            return null
        }
    }
    return instant.atZone(ZoneId.systemDefault()).format(DISPLAY_DATE)
}
