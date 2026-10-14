# KMA Parser Tests

## Goal

Validate KMA JSON parsing without a live service key or network call.

## Current Structure

- `KmaWeatherRepository`
  - Builds URL.
  - Performs HTTP request.
  - Delegates JSON parsing to `KmaForecastParser`.
- `KmaForecastParser`
  - Parses `resultCode`.
  - Selects target `fcstDate` and `fcstTime`.
  - Reads `POP` precipitation probability.
  - Maps `PTY` precipitation type to app weather conditions.

## Unit Tests

`KmaForecastParserTest` covers:

- target hour `POP` and `PTY`
- ignoring non-target forecast hours
- snow and sleet PTY codes
- KMA error result handling

`KmaBaseTimeCalculatorTest` covers:

- latest base time older than 20 minutes
- previous-day `2300` before first base time
- same-day `2300` late at night
- using `0200` before `0500` is safely available

JVM tests use:

```text
testImplementation("org.json:json:20240303")
```

This is needed because Android's bundled `org.json` is only a mocked stub in
local JVM unit tests.

## Validation

```text
.\scripts\test-debug.ps1
KmaForecastParserTest: 4 tests, 0 failures, 0 errors
KmaBaseTimeCalculatorTest: 4 tests, 0 failures, 0 errors
total JVM tests: 20, 0 failures, 0 errors
assembleDebug: success
lintDebug: success
```

## Next

- Add live KMA smoke check once a service key is configured.
