package ch.parkassist.app.domain.model

enum class Provider(val displayName: String) {
    MOCK("Mock Parking"),
    PARKINGPAY("Parkingpay (manuell)"),
    TWINT("TWINT Parking (manuell)"),
}
