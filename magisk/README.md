# NothingCall – System Mode (Magisk)

Regular Android apps cannot record both sides of a call or play audio into the call uplink.
This Magisk module installs NothingCall into `/system/priv-app` and grants exactly two
privileged permissions:

| Permission | Unlocks |
|---|---|
| `CAPTURE_AUDIO_OUTPUT` | Call recording of **both sides** (`AudioSource.VOICE_CALL`) |
| `CALL_AUDIO_INTERCEPTION` | Soundboard clips go **directly into the call** (Android 14+) |

Nothing else changes – still no internet permission, no telemetry.

## Install
1. Uninstall any normally installed NothingCall first (signatures must match otherwise).
2. Download `NothingCall-<version>-magisk.zip` from the GitHub release / CI artifacts.
3. Magisk → Modules → Install from storage → reboot.
4. Set NothingCall as default phone app. *Settings → System mode* now shows **Active**.

## Build manually
```bash
./gradlew assembleRelease
mkdir -p magisk/system/priv-app/NothingCall
cp app/build/outputs/apk/release/*.apk magisk/system/priv-app/NothingCall/NothingCall.apk
(cd magisk && zip -r ../NothingCall-magisk.zip . -x README.md)
```
