# ● NothingCall

**Privacy-first, open-source phone app for Android, in Nothing OS style.**
Jetpack Compose · Material 3 · true-black OLED · dot-matrix design · GNU GPLv3

[🇩🇪 Deutsch](#-deutsch) · [🇬🇧 English](#-english)

---

## 🇩🇪 Deutsch

NothingCall ist eine vollständige Telefon-App (Standard-Dialer) mit tiefschwarzem OLED-Design, Dot-Matrix-Typografie und roten Akzenten. Datenschutz hat oberste Priorität.

### Funktionen
- **Wähltastatur** mit Dot-Matrix-Ziffern, DTMF-Tönen, Kontaktvorschlägen, `+` per langem Druck auf `0` und SIM-Auswahl bei Dual-SIM
- **Anrufbildschirm**: Annehmen/Ablehnen, Stumm, Lautsprecher, Halten, Wechseln zwischen zwei Anrufen, DTMF-Tastatur, Näherungssensor und Anzeige auf dem Sperrbildschirm
- **Anrufaufnahme**: manuell oder automatisch, AES-256-GCM-verschlüsselt, Wiedergabeliste mit Suchleiste und Export
- **In-Call-Soundboard** (inspiriert von One UI): 8 integrierte, synthetisch erzeugte Clips sowie eigene importierte Clips (verschlüsselt gespeichert)
- **Kontakte**: Systemkontakte (nur lesend) und **private, verschlüsselte Kontakte**, die nur in NothingCall existieren
- **Anrufverlauf** mit verpassten Anrufen in Rot
- **Deutsch & Englisch**: folgt automatisch der Systemsprache oder lässt sich in den Einstellungen umschalten

### Datenschutz
| | |
|---|---|
| 🌐 Keine `INTERNET`-Berechtigung | Die App kann technisch keine Daten senden (per `tools:node="remove"` erzwungen). |
| 📵 Keine Telemetrie | Keine Analytics, kein Crash-Reporting, keine Werbung, keine Google-Dienste |
| 🔐 Verschlüsselung | AES-256-GCM; der Datenschlüssel ist durch einen nicht exportierbaren Android-Keystore-Schlüssel geschützt |
| 💾 Keine Backups | `allowBackup=false` und Data-Extraction-Rules schließen alles aus |
| 🧹 Löschen | „Alle App-Daten löschen“ entfernt Aufnahmen, Clips und private Kontakte und vernichtet die Schlüssel |

### Wichtige Hinweise (technische Grenzen von Android)
- **Anrufaufnahme:** Seit Android 9 dürfen nur System-Apps die Audioquelle `VOICE_CALL` (beide Seiten) nutzen. NothingCall versucht diese Quelle zuerst und weicht dann auf Sprachkommunikation bzw. Mikrofon aus. Je nach Gerät wird daher eventuell nur deine eigene Stimme aufgenommen. Mit eingeschaltetem Lautsprecher ist die Gegenseite meist mit drauf. Die Audioquelle lässt sich in den Einstellungen wählen.
- **Soundboard:** Normale Apps können kein Audio direkt in den Mobilfunk-Uplink einspeisen. Die Clips werden deshalb über den Lautsprecher abgespielt (optional schaltet die App ihn automatisch ein). Ob das Gegenüber sie hört, hängt von der Echounterdrückung des Geräts ab.
- **Rechtliches:** Das Aufzeichnen von Gesprächen ohne Einwilligung aller Beteiligten kann strafbar sein (z. B. § 201 StGB). Die App zeigt vor der ersten Aufnahme einen Hinweis.

### Bauen
Voraussetzungen: JDK 17 und das Android SDK (API 35).
```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/
./gradlew assembleRelease      # app/build/outputs/apk/release/
```
Jeder Push baut über GitHub Actions (`.github/workflows/build.yml`) eine Debug- und eine Release-APK sowie ein Quellcode-ZIP als Artefakte. Bei Tags `v*` wird ein GitHub-Release erstellt.
Optionale Signierung über die Repository-Secrets `NC_KEYSTORE_BASE64`, `NC_KEYSTORE_PASSWORD`, `NC_KEY_ALIAS` und `NC_KEY_PASSWORD`. Ohne sie wird die Release-APK mit dem Debug-Schlüssel signiert.

Nach der Installation: **Einstellungen → Standard-Telefon-App → NothingCall**.

---

## 🇬🇧 English

NothingCall is a full replacement phone app (default dialer) with a true-black OLED theme, dot-matrix typography and fine red accents. Privacy comes first.

### Features
- **Dialer** with dot-matrix digits, DTMF tones, contact suggestions, long-press `0` for `+`, and dual-SIM picker
- **In-call screen**: answer/decline, mute, speaker, hold, swap between two calls, DTMF keypad, proximity sensor and lock-screen support
- **Call recording**: manual or automatic, AES-256-GCM encrypted, playlist with seek bar and export
- **In-call soundboard** (inspired by One UI): 8 built-in procedurally synthesized clips plus your own imported clips (stored encrypted)
- **Contacts**: system contacts (read-only) plus **private encrypted contacts** that exist only inside NothingCall
- **Call history** with missed calls in red
- **German & English**: follows the system language automatically or can be switched in settings

### Privacy
- No `INTERNET` permission (stripped via `tools:node="remove"`), so the app technically cannot send data
- No telemetry, analytics, crash reporting, ads or Google services
- AES-256-GCM encryption with a data key wrapped by a non-exportable Android Keystore key
- Backups and device transfer disabled
- One-tap "Delete all app data", which also destroys the keys

### Known platform limitations
- **Call recording:** Since Android 9, only system apps may use `VOICE_CALL` (both sides). NothingCall tries it first and then falls back to voice communication or the microphone, so on many devices only your own voice is captured unless the speaker is on. The audio source can be chosen in settings.
- **Soundboard:** Regular apps cannot inject audio into the cellular uplink. Clips are played through the loudspeaker, which can be turned on automatically. Whether the other party hears them depends on the device's echo cancellation.
- **Legal:** Recording calls without everyone's consent may be illegal where you live. A notice is shown before the first recording.

### Build
Requires JDK 17 and the Android SDK (API 35): `./gradlew assembleDebug assembleRelease`.
GitHub Actions builds debug and release APKs plus a source ZIP on every push, and publishes a release for `v*` tags.

---

## ☕ Support
NothingCall is free software. If you like it: **[Buy me a coffee](https://www.buymeacoffee.com/3rr0r303yt)**

## License
Copyright © 2026 **Real3rr0r303YT**

This program is free software: you can redistribute it and/or modify it under the terms of the **GNU General Public License v3.0** as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. See [`LICENSE`](LICENSE).

Material icons © Google, Apache License 2.0.
