# Privacy Disclosure

## Current App Copy

The app shows a privacy notice in the permissions section.

It tells the user:

- Saved location is stored on the device.
- Latitude and longitude are sent to the selected weather provider during
  weather checks.
- Alarm history stays on the device and is excluded from Android backup.

## Current Data Flow

- Alarm settings: local device storage.
- Region name and coordinates: local device storage.
- Weather checks: selected provider receives latitude and longitude.
- Alarm history: local Room database.
- KMA service key: local private app preferences, masked in UI, hidden behind
  advanced settings.
- Android backup: disabled for app data.

## Release Gap

Before public release, add:

- Store listing privacy policy URL.
- Play Console data safety answers.
- Provider list in public privacy policy.
- Backend proxy privacy wording if the backend proxy is enabled.
