package ch.parkassist.app.provider

import ch.parkassist.app.domain.model.Provider

object ProviderRegistry {
    private val adapters: Map<Provider, ProviderAdapter> = mapOf(
        Provider.MOCK to MockProviderAdapter,
        Provider.EASYPARK to EasyParkAdapter,
        Provider.TWINT to TwintAdapter,
    )

    fun adapterFor(provider: Provider): ProviderAdapter =
        adapters[provider] ?: error("No adapter for $provider")
}
