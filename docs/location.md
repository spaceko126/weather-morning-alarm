# Location and Region Selection

## Current Scope

The app supports two location paths:

- Region preset search.
- Current device location from Android `LocationManager`.

## Region Search

The location section has a search field. It filters preset regions by stable
internal key, localized display label, and aliases.

Implementation detail:

- `RegionPreset.key` stays stable and romanized for persistence and internal
  comparisons.
- `RegionPreset.displayNameRes` controls user-facing localized text.
- `RegionPreset.aliases` supports Korean and alternate romanized search input.

Included presets:

- Seoul
- Incheon
- Suwon
- Busan
- Daegu
- Daejeon
- Gwangju
- Ulsan
- Jeju

## Current Location

The `Use current location` button:

1. Checks `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION`.
2. Requests both permissions if needed.
3. Reads the newest last-known location from enabled providers.
4. If no last-known location exists, requests a one-shot provider update.
5. Saves latitude and longitude to alarm settings.

Implementation:

```text
app/src/main/java/com/weathermorning/alarm/location/CurrentLocationReader.kt
```

## Known Limits

- Active one-shot update uses Android `LocationManager` provider APIs.
- No reverse geocoding; saved label is `Current location (provider)`.
- If provider update times out, user must try again with location services on.
- No map UI.
- Previously saved region labels can stay romanized until the user selects a
  region again.
