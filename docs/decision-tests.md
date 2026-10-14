# Decision Engine Tests

## Goal

Lock the core "ring or skip" logic before Galaxy testing.

## Covered Cases

`DecideAlarmUseCaseTest` covers:

- disabled alarm skips
- rain trigger rings on rain
- snow trigger rings on snow
- precipitation threshold can ring even when direct rain/snow toggles are off
- low precipitation skips when no condition matches
- fail policy `RING` rings when weather is missing
- fail policy `SKIP` skips when weather is missing

## Validation

```text
.\scripts\test-debug.ps1
NextAlarmCalculatorTest: 5 tests, 0 failures, 0 errors
DecideAlarmUseCaseTest: 7 tests, 0 failures, 0 errors
assembleDebug: success
lintDebug: success
```

## Problems

- Tests cover decision logic only, not Android receiver behavior.
- Weather API parsing still lacks fixture tests.
- KMA live call still needs configured service key.

## Next

- Add KMA parsing fixture tests.
- Add receiver/instrumentation checks after Galaxy connection.
