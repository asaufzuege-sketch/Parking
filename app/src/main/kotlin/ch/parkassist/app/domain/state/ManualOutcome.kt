package ch.parkassist.app.domain.state

/** Outcome explicitly confirmed by the user after returning from a manual provider handoff. */
enum class ManualOutcome {
    /** User confirmed success in the provider app. */
    CONFIRMED,
    /** User did not complete the action. */
    NOT_COMPLETED,
    /** Outcome is unclear; user will check again. */
    UNCLEAR,
}
