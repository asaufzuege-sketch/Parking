# Parking-Assistent

Ein Android-Prototyp für regelkonforme Parkier-Automatisierung.

## APK herunterladen

Die installierbaren Debug-APKs findest du unter **Actions → neuester erfolgreicher Lauf von „CI – Lint, Test & Build" → Artifacts**:

- [`parking-assistant-debug-apk`](../../actions) – Haupt-App (Parking-Assistent)
- [`mockparking-debug-apk`](../../actions) – Mock-Parking-Anbieter

> **Troubleshooting:** Wenn kein Download sichtbar ist, war der Lauf nicht grün. APK-Artefakte erscheinen nur bei einem erfolgreichen Lauf im Abschnitt **Artifacts** ganz unten auf der Workflow-Seite.

> Die Website unter GitHub Pages ist **nicht** die App. Lade die APKs oben herunter, entpacke die ZIP-Dateien und installiere zuerst `mockparking-debug.apk`, danach `app-debug.apk` auf deinem Android-Gerät.

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
./gradlew :app:lintDebug :mockparking:lintDebug

# Instrumentierungs-Tests (Gerät/Emulator erforderlich):
./gradlew :app:connectedDebugAndroidTest
```

## CI-Artefakte herunterladen und installieren

Nach jedem erfolgreichen CI-Durchlauf (Push auf `main` oder einen `copilot/**`-Branch) werden zwei Debug-APKs als GitHub-Actions-Artefakte hochgeladen:

| Artefakt-Name | Inhalt |
|---------------|--------|
| `parking-assistant-debug-apk` | Parking-Assistent (Haupt-App) |
| `mockparking-debug-apk` | Mock-Parking-Anbieter |

### Download

1. Gehe zu **Actions** im Repository auf GitHub.
2. Wähle den gewünschten Workflow-Durchlauf aus.
3. Scrolle zum Abschnitt **Artifacts** am Ende der Seite.
4. Lade `parking-assistant-debug-apk` und `mockparking-debug-apk` herunter.
   **Hinweis:** GitHub verpackt Artefakte als ZIP-Dateien. Entpacke die ZIP-Datei, um die eigentliche `.apk`-Datei zu erhalten.

### Installation via ADB

```bash
# Mock-Provider zuerst installieren (wird von der Haupt-App benötigt):
adb install -r mockparking-debug.apk

# Dann Haupt-App installieren:
adb install -r app-debug.apk
```

> **Voraussetzung:** USB-Debugging oder Wireless-ADB muss auf dem Gerät aktiviert sein (Einstellungen → Entwickleroptionen → USB-Debugging).
>
> **Manuelle Installation ohne ADB:** Übertrage die `.apk`-Datei auf das Gerät (z. B. per USB oder Cloud-Speicher). Aktiviere vorher **Unbekannte Quellen** (Einstellungen → Sicherheit → Unbekannte Apps zulassen) für deinen Dateimanager oder Browser. Installiere zuerst `mockparking-debug.apk`, danach `app-debug.apk`.

### Mock-Flow starten

1. Öffne **Parking-Assistent** auf dem Gerät.
2. Wähle als Anbieter **Mock Parking**.
3. Gib Zone, Kontrollschild und Ticketdauer ein.
4. Bestätige und tippe **Parkvorgang starten**.
5. Die Mock-Parking-App öffnet sich mit vorausgefüllten Daten.
6. Tippe in der Mock-App auf **Bestätigen**.
7. Der Assistent wechselt in den Status **Aktiv** und zeigt eine Benachrichtigung.

### Probleme melden

Falls die App auf einem bestimmten Gerät oder einer Android-Version nicht korrekt funktioniert, bitte folgende Informationen im Issue angeben:
- Gerätehersteller und Modell
- Android-Version (z. B. Android 13 / API 33)
- Logcat-Ausgabe: `adb logcat -s ParkingAssistant:V MockParking:V`

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
