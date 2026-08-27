package ch.parkassist.app.provider

import ch.parkassist.app.domain.model.Provider
import java.time.Instant

/**
 * Configuration for the automated Mock Parking developer/test flow.
 * This automation is ONLY available when provider == Provider.MOCK and
 * the target package is exactly ch.parkassist.mockparking.
 */
data class MockAutomationConfig(
    /** Maximum number of start→stop cycles. Must be > 0 and ≤ MAX_REPETITIONS. */
    val repetitions: Int = 1,
    /** Interval between cycles in seconds. */
    val intervalSeconds: Long = 5,
) {
    companion object {
        /** Hard cap to prevent normalization of recurring free-period cycling. */
        const val MAX_REPETITIONS = 5
    }

    init {
        require(repetitions in 1..MAX_REPETITIONS) {
            "repetitions must be between 1 and $MAX_REPETITIONS"
        }
        require(intervalSeconds >= 0) { "intervalSeconds must be >= 0" }
    }
}

/**
 * Result of a single automation step.
 */
sealed class AutomationStepResult {
    data class Success(val message: String, val timestamp: Instant = Instant.now()) : AutomationStepResult()
    data class Failure(val reason: String, val timestamp: Instant = Instant.now()) : AutomationStepResult()
    data class Cancelled(val timestamp: Instant = Instant.now()) : AutomationStepResult()
}

/**
 * Guard: enforces that automation is only possible for Mock Parking.
 * Throws IllegalStateException if invoked for any other provider.
 */
fun requireMockAutomation(provider: Provider) {
    check(provider == Provider.MOCK) {
        "Automation is only available for Provider.MOCK (ch.parkassist.mockparking). " +
            "Cannot enable automation for $provider."
    }
    check(MockProviderContract.PACKAGE == "ch.parkassist.mockparking") {
        "Mock package mismatch: expected ch.parkassist.mockparking"
    }
}
