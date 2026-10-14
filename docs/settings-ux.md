# Settings UX

## Current MVP Controls

- Enable or disable alarm.
- Set alarm time with Material time picker.
- Toggle repeat weekdays.
- Pick region from presets.
- Toggle rain and snow triggers.
- Adjust precipitation threshold.
- Choose weather API failure policy.
- Open exact alarm permission settings.
- Request notification permission.
- Choose Open-Meteo weather provider in normal settings.
- Choose Backend proxy in normal settings when the app is built with a proxy
  URL.
- Expand advanced weather settings for Stub, KMA, and local KMA key entry.
- Reset to MVP default.
- Launch test alarm.

## Region Presets

The region picker supports search and current-location fallback. It stores
region name plus latitude and longitude.

Included regions:

- Seoul
- Incheon
- Suwon
- Busan
- Daegu
- Daejeon
- Gwangju
- Ulsan
- Jeju

Current location is available through `LocationManager` last-known location
after runtime location permission is granted.

## Known UX Gaps

- Current-location lookup uses last-known location only.
- Controls are functional but visually plain.
- No settings validation message when no repeat day is selected.
- Test alarm starts alarm activity directly; it does not yet exercise the full
  receiver/notification path.
- Backend proxy is build-configured only; there is no in-app proxy URL editor.
