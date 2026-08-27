package ch.parkassist.app.domain.model

/**
 * Models the rules of a parking zone.
 *
 * @param zoneId            Identifier for the zone (e.g. "ZONE-A")
 * @param maxTotalMinutes   Maximum total parking duration allowed (null = no limit)
 * @param minTicketMinutes  Minimum single-ticket duration
 * @param maxTicketMinutes  Maximum single-ticket duration
 * @param extensionAllowed  Whether zone rules permit extension of an active ticket
 * @param confirmationRequired Whether the user must explicitly confirm the action
 * @param freePeriodMinutes If > 0, tickets up to this duration are "free"; cycling is rejected
 */
data class ZonePolicy(
    val zoneId: String,
    val maxTotalMinutes: Int? = null,
    val minTicketMinutes: Int = 1,
    val maxTicketMinutes: Int = 480,
    val extensionAllowed: Boolean = true,
    val confirmationRequired: Boolean = true,
    val freePeriodMinutes: Int = 0,
)
