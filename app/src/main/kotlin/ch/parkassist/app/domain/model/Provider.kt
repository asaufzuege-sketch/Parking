package ch.parkassist.app.domain.model

enum class Provider(val displayName: String) {
    MOCK("Mock Parking"),
    EASYPARK("EasyPark"),
    TWINT("TWINT Parking"),
}
