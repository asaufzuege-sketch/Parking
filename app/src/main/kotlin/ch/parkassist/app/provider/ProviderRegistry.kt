package ch.parkassist.app.provider

import ch.parkassist.app.domain.model.Provider

object ProviderRegistry {
    private val adapters: Map<Provider, ProviderAdapter> = mapOf(
        Provider.MOCK to MockParkingAdapter,
        Provider.PARKINGPAY to ParkingpayManualAdapter,
        Provider.TWINT to TwintManualAdapter,
    )

    fun adapterFor(provider: Provider): ProviderAdapter =
        adapters[provider] ?: error("No adapter for $provider")

    /** Returns true only for MockParkingAdapter. Cannot be enabled for any other provider. */
    fun supportsAutomation(provider: Provider): Boolean =
        adapters[provider]?.capabilities?.supportsAutomation == true
}
