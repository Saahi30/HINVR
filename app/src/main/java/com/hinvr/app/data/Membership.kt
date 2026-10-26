package com.hinvr.app.data

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class MembershipTier { None, Darshan, Gold, Platinum, Nri }

val MembershipTier.hasDeskPass: Boolean
    get() = this == MembershipTier.Gold ||
        this == MembershipTier.Platinum ||
        this == MembershipTier.Nri

/** The metal card is a Platinum benefit; NRI is Platinum priced in dollars. */
val MembershipTier.hasPhysicalCard: Boolean
    get() = this == MembershipTier.Platinum || this == MembershipTier.Nri

enum class Audience { Me, Parents, Family }

/**
 * profiles.valid_until arrives as an ISO date ("2027-04-30") from PostgREST. Older
 * rows held a free-text label ("Apr 2027"); show those exactly as the desk wrote them.
 */
fun formatValidUntil(raw: String, locale: Locale): String {
    val text = raw.trim()
    if (text.isEmpty()) return ""
    val date = parseIsoDate(text) ?: return text
    return DateTimeFormatter.ofPattern("MMM yyyy", locale).format(date)
}

private fun parseIsoDate(text: String): LocalDate? {
    if (!Regex("""^\d{4}-\d{2}-\d{2}""").containsMatchIn(text)) return null
    return runCatching { LocalDate.parse(text.take(10)) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(text).toLocalDate() }.getOrNull()
}
