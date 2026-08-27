package ch.parkassist.app.domain.policy

import ch.parkassist.app.domain.model.ParkingSession
import ch.parkassist.app.domain.model.ZonePolicy

sealed class PolicyResult {
    object Ok : PolicyResult()
    data class Rejected(val reason: String) : PolicyResult()
}

object PolicyValidator {

    /**
     * Validates a new parking session start against the zone policy.
     * Rejects if the ticket duration exceeds ticket bounds, total duration would be exceeded,
     * or a free-period cycle would be initiated.
     */
    fun validateStart(session: ParkingSession, policy: ZonePolicy): PolicyResult {
        if (session.ticketDurationMinutes < policy.minTicketMinutes) {
            return PolicyResult.Rejected(
                "Ticketdauer ${session.ticketDurationMinutes} Min. liegt unter dem Minimum " +
                    "${policy.minTicketMinutes} Min."
            )
        }
        if (session.ticketDurationMinutes > policy.maxTicketMinutes) {
            return PolicyResult.Rejected(
                "Ticketdauer ${session.ticketDurationMinutes} Min. überschreitet das Maximum " +
                    "${policy.maxTicketMinutes} Min."
            )
        }
        policy.maxTotalMinutes?.let { max ->
            val totalPlanned = session.ticketDurationMinutes * (1 + session.maxExtensions)
            if (totalPlanned > max) {
                return PolicyResult.Rejected(
                    "Geplante Gesamtdauer $totalPlanned Min. überschreitet Zonenmaximum $max Min."
                )
            }
        }
        if (policy.freePeriodMinutes > 0 && session.ticketDurationMinutes <= policy.freePeriodMinutes
            && session.maxExtensions > 0
        ) {
            return PolicyResult.Rejected(
                "Wiederholtes Einlösen von Gratisperioden (≤${policy.freePeriodMinutes} Min.) " +
                    "mit Verlängerungen ist nicht zulässig."
            )
        }
        return PolicyResult.Ok
    }

    /**
     * Validates whether an extension is lawful at the current point in a session.
     */
    fun validateExtension(session: ParkingSession, policy: ZonePolicy, elapsedTotalMinutes: Int): PolicyResult {
        if (!policy.extensionAllowed) {
            return PolicyResult.Rejected("Verlängerungen sind in dieser Zone nicht erlaubt.")
        }
        if (session.extensionsUsed >= session.maxExtensions) {
            return PolicyResult.Rejected(
                "Maximale Anzahl zulässiger Verlängerungen (${session.maxExtensions}) bereits erreicht."
            )
        }
        policy.maxTotalMinutes?.let { max ->
            val newElapsed = elapsedTotalMinutes + session.ticketDurationMinutes
            if (newElapsed > max) {
                return PolicyResult.Rejected(
                    "Verlängerung würde Zonenmaximum von $max Min. überschreiten."
                )
            }
        }
        return PolicyResult.Ok
    }
}
