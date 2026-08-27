# Parking-Assistent

Ein Android-Prototyp für regelkonforme Parkier-Automatisierung.

## Zweck und Sicherheitsgrenzen

Die App automatisiert **ausschliesslich zulässige Aktionen** gemäss den Regeln des Parkplatzbetreibers und der Parkzone:
- Sie löst keine Gratis-Parkperioden wiederholt ein, um zeitliche oder kostenpflichtige Regeln zu umgehen.
- Jede Verlängerung wird gegen die Zonenrichtlinie geprüft (maximale Gesamtdauer, max. Verlängerungen, Erweiterungserlaubnis).
- Der Benutzer muss die Berechtigung für die Aktion explizit bestätigen, bevor etwas ausgeführt wird.

Es wird **keine Accessibility-Automation** auf Drittanbieter-Apps verwendet. Stattdessen werden ausschliesslich offizielle Interfaces verwendet (Intents/Deep Links).

## Module

| Modul | Beschreibung |
|-------|-------------|
| `app` | Parking-Assistent – Hauptapp (Kotlin/Jetpack Compose/Material 3) |
| `mockparking` | Mock-Parking-Anbieter – lokale Test-App mit explizitem Intent-Vertrag |

## Setup und Build

### Voraussetzungen
- Android Studio Koala (2024.1) oder neuer
- JDK 17
- Android SDK 35

### Bauen
```bash
./gradlew assembleDebug
```

### Installieren
```bash
# Mock-Provider zuerst installieren:
./gradlew :mockparking:installDebug

# Dann Hauptapp:
./gradlew :app:installDebug
```

### Tests lokal ausführen
```bash
# Unit-Tests (kein Gerät erforderlich):
./gradlew :app:testDebugUnitTest
./gradlew :mockparking:testDebugUnitTest

# Lint:
./gradlew :app:lint :mockparking:lint

# Instrumentierungs-Tests (Gerät/Emulator erforderlich):
./gradlew :app:connectedDebugAndroidTest
```

## Mock-Flow

1. `mockparking`-App auf dem Gerät installieren.
2. Parking-Assistent öffnen, Anbieter „Mock Parking" wählen.
3. Zone, Kontrollschild und Ticketdauer eingeben.
4. Bestätigung ankreuzen und „Parkvorgang starten" tippen.
5. Der Assistent öffnet die Mock-Parking-App via explizitem Intent mit vorausgefüllten Daten.
6. In der Mock-App: „Bestätigen" → die App gibt RESULT_OK zurück.
7. Der Assistent wechselt in den Status „Aktiv" und zeigt eine Benachrichtigung.
8. Optional: Verlängern oder Stoppen.

## Android-Planungseinschränkungen

- WorkManager persistiert geplante Arbeit über Prozessneustarts und Gerätestarts hinweg.
- Für exakte Zeitplanung (< 1 Min.) wird `AlarmManager` mit `USE_EXACT_ALARM` benötigt (Android 12+).
- Android beschränkt Hintergrundstarts; ein Foreground Service mit laufender Benachrichtigung wird verwendet.
- Nach einem Geräteneustart reaktiviert `BootReceiver` den WorkManager.

## Anbieter-Integration

| Anbieter | Integrationsart | Hinweis |
|----------|----------------|---------|
| Mock Parking | Expliziter Intent-Vertrag | Vollautomatisch möglich |
| EasyPark | Deep Link (`easypark://parking`) | Nur wenn offizielle App installiert; manuell fortfahren |
| TWINT Parking | Deep Link (`twint://parking`) | Nur wenn offizielle App installiert; manuell fortfahren |

EasyPark und TWINT bieten aktuell keine öffentliche API für automatisches Parkieren. Die Adapter öffnen lediglich die offizielle App via Deep Link. Eine vollständige Integration erfordert eine offizielle Kooperation mit den Anbietern.

## Zustandsautomat

```
Idle → Scheduled → LaunchingProvider → AwaitingUser → Active → ExtensionDue → Active (…)
                                                              ↓
                                                         Completed / Cancelled / Error
```

## Sicherheitshinweise

- Keine Accessibility-Automation auf fremden Apps.
- Keine undokumentierten APIs oder Netzwerk-Interception.
- Keine Credentials werden gespeichert.
- Freie Parkperioden können nicht unbegrenzt wiederholt werden (Policy-Validator lehnt dies ab).
