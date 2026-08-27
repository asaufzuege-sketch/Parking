package ch.parkassist.app.domain

import ch.parkassist.app.domain.model.ParkingSession
import ch.parkassist.app.domain.model.Provider
import ch.parkassist.app.domain.model.ZonePolicy
import ch.parkassist.app.domain.policy.PolicyResult
import ch.parkassist.app.domain.policy.PolicyValidator
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class PolicyValidatorTest {

    private val baseSession = ParkingSession(
        provider = Provider.MOCK,
        zone = "ZONE-A",
        licensePlate = "ZH 1234",
        ticketDurationMinutes = 60,
        maxExtensions = 2,
        startTime = Instant.now(),
        confirmedByUser = true,
    )

    private val basePolicy = ZonePolicy(
        zoneId = "ZONE-A",
        maxTotalMinutes = 480,
        minTicketMinutes = 10,
        maxTicketMinutes = 120,
        extensionAllowed = true,
        confirmationRequired = true,
    )

    @Test
    fun `valid session returns Ok`() {
        val result = PolicyValidator.validateStart(baseSession, basePolicy)
        assertEquals(PolicyResult.Ok, result)
    }

    @Test
    fun `ticket below minimum rejected`() {
        val session = baseSession.copy(ticketDurationMinutes = 5)
        val result = PolicyValidator.validateStart(session, basePolicy)
        assertTrue(result is PolicyResult.Rejected)
    }

    @Test
    fun `ticket above maximum rejected`() {
        val session = baseSession.copy(ticketDurationMinutes = 200)
        val result = PolicyValidator.validateStart(session, basePolicy)
        assertTrue(result is PolicyResult.Rejected)
    }

    @Test
    fun `total planned duration exceeds zone max rejected`() {
        // 60 min * (1 + 8 extensions) = 540 > 480
        val session = baseSession.copy(maxExtensions = 8)
        val result = PolicyValidator.validateStart(session, basePolicy)
        assertTrue(result is PolicyResult.Rejected)
    }

    @Test
    fun `free period cycling rejected`() {
        // Zone has free period of 10 min; session requests 10 min with 2 extensions
        val policy = basePolicy.copy(freePeriodMinutes = 10)
        val session = baseSession.copy(ticketDurationMinutes = 10, maxExtensions = 2)
        val result = PolicyValidator.validateStart(session, policy)
        assertTrue(result is PolicyResult.Rejected)
    }

    @Test
    fun `free period with zero extensions is allowed`() {
        val policy = basePolicy.copy(freePeriodMinutes = 10)
        val session = baseSession.copy(ticketDurationMinutes = 10, maxExtensions = 0)
        val result = PolicyValidator.validateStart(session, policy)
        assertEquals(PolicyResult.Ok, result)
    }

    @Test
    fun `extension allowed when within limits`() {
        val session = baseSession.copy(extensionsUsed = 1)
        val result = PolicyValidator.validateExtension(session, basePolicy, elapsedTotalMinutes = 120)
        assertEquals(PolicyResult.Ok, result)
    }

    @Test
    fun `extension rejected when not allowed by zone`() {
        val policy = basePolicy.copy(extensionAllowed = false)
        val result = PolicyValidator.validateExtension(baseSession, policy, elapsedTotalMinutes = 60)
        assertTrue(result is PolicyResult.Rejected)
    }

    @Test
    fun `extension rejected when max extensions reached`() {
        val session = baseSession.copy(extensionsUsed = 2, maxExtensions = 2)
        val result = PolicyValidator.validateExtension(session, basePolicy, elapsedTotalMinutes = 180)
        assertTrue(result is PolicyResult.Rejected)
    }

    @Test
    fun `extension rejected when would exceed zone max total`() {
        // elapsed=430, ticket=60 results_in_490 > 480 (zone max), so extension is rejected
        val session = baseSession.copy(extensionsUsed = 1)
        val result = PolicyValidator.validateExtension(session, basePolicy, elapsedTotalMinutes = 430)
        assertTrue(result is PolicyResult.Rejected)
    }
}
