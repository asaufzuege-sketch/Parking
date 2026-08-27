# Parking-Assistent

Kotlin/Jetpack-Compose-Android-Projekt mit sicherer Provider-Architektur für Parkvorgänge.

## Sicherheitsmodell

- **Keine AccessibilityService-Implementierung vorhanden.** Drittanbieter-Apps werden nicht automatisiert.
- **Mock Parking** (`ch.parkassist.mockparking`) ist der einzige Adapter mit `supportsAutomation = true`.
- **Parkingpay** und **TWINT** werden ausschliesslich per sicherem manuellem Handoff geöffnet.
- Keine Credentials, Tokens oder Zahlungsdaten werden von den Adaptern verarbeitet oder geloggt.

## Provider-Architektur

Zentrale Erweiterungspunkte im Modul `app`:

- `ProviderAdapter` – einheitlicher Vertrag für Start/Extend/Stop, Dry-Run und Logging
- `ProviderCapabilities` – beschreibt Automatisierung vs. manueller Handoff
- `ProviderRegistry` – bindet konkrete Adapter an `Provider`
- `MockParkingAdapter` – repo-eigener Testadapter
- `ParkingpayManualAdapter` – sicherer manueller Parkingpay-Handoff
- `TwintManualAdapter` – sicherer manueller TWINT-Handoff
- `MockAutomationConfig` / `requireMockAutomation()` – Guardrails für den Mock-Dev-Flow

Um später eine offizielle Integration zu ergänzen, muss nur ein neuer `ProviderAdapter` implementiert und in `ProviderRegistry` ersetzt werden. State Machine und Compose-UI bleiben unverändert anschlussfähig.

## Provider-Verhalten

### Mock Parking

- Öffnet `ch.parkassist.mockparking` über den gemeinsamen Intent-Vertrag in `MockProviderContract`.
- Einziger Provider mit Automatisierungsfreigabe.
- `runMockAutomation()` ist bewusst begrenzt; `MockAutomationConfig.MAX_REPETITIONS = 5`.
- Kein `FLAG_ACTIVITY_NEW_TASK` auf Mock-Intents.

### Parkingpay / TWINT

- Öffnen nur offizielle App-/Web-Handoffs.
- Vor dem Start erscheint ein **experimenteller Warnhinweis**.
- Nach Rückkehr muss der Nutzer das Ergebnis explizit als **bestätigt**, **nicht abgeschlossen** oder **unklar** melden.
- Keine undokumentierten APIs, keine Accessibility-Automation, keine versteckte Hintergrundsteuerung.

## Dry-Run

Dry-Run kann in der Eingabemaske aktiviert werden.

- Es wird **kein externer Aufruf** ausgeführt.
- Stattdessen wird nur die Dry-Run-Beschreibung des Adapters geloggt.
- Der Flow dient ausschliesslich zum sicheren Prüfen von UI- und Statusübergängen.

## APK-Artefakte und Installation

GitHub Actions erzeugt zwei Debug-Artefakte:

1. `mockparking-debug-apk`
2. `parking-assistant-debug-apk`

Installation immer in dieser Reihenfolge:

1. `mockparking-debug.apk`
2. `app-debug.apk`

Beispiel mit ADB:

```bash
adb install -r mockparking-debug.apk
adb install -r app-debug.apk
```

## Lokaler Build

```bash
./gradlew :app:testDebugUnitTest :mockparking:testDebugUnitTest
./gradlew :app:lintDebug :mockparking:lintDebug
./gradlew :app:assembleDebug :mockparking:assembleDebug
```

## Mock-Developer-Flow

1. `mockparking` installieren.
2. In der Haupt-App **Mock Parking** wählen.
3. Zone, Kennzeichen, Dauer und Bestätigung setzen.
4. Optional Dry-Run aktivieren.
5. Starten und in der Mock-App bestätigen.
6. Verlängern/Stoppen zum Testen der Zustandsübergänge verwenden.

## Grenzen

- Keine Drittanbieter-Automation ausserhalb der repo-eigenen Mock-App.
- Keine Zahlungsabwicklung im Adapter-Layer.
- Keine Speicherung von Credentials oder Tokens.
- Wiederholte Gratisperioden-Umgehung wird weiterhin durch Policy-Logik begrenzt.
