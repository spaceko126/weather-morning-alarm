# Localization

## Goal

Prepare the MVP for Korean-first Galaxy testing while keeping English fallback
resources.

## Current Resources

- Default strings:
  - `app/src/main/res/values/strings.xml`
- Korean strings:
  - `app/src/main/res/values-ko/strings.xml`

## Localized Areas

- App name and subtitle.
- Main settings labels.
- Alarm time and repeat weekday labels.
- Location search and current location action.
- Weather trigger labels.
- Permission status labels.
- Weather provider labels.
- Action buttons.
- History empty/clear labels.
- Alarm screen title and Stop button.
- Test alarm reason.
- Notification channel name/description.
- Settings warning messages.
- Region preset display names.
- Region preset search aliases for Korean input.
- History type labels.
- History weather condition labels.
- History display mapping for saved English decision reasons.

## Not Fully Localized Yet

- Existing persisted values may remain in the old language until rewritten.
  - Example: a previously saved `Current location (gps)` label remains English
    until the current-location action is run again.
- Domain decision reasons are still stored as stable diagnostic strings, but
  the history UI maps known English values to localized display text.
- Previously saved region labels can remain romanized until the user selects
  the region again.

## Validation

```text
.\scripts\test-debug.ps1
NextAlarmCalculatorTest: 5 tests, 0 failures, 0 errors
KmaBaseTimeCalculatorTest: 4 tests, 0 failures, 0 errors
KmaForecastParserTest: 4 tests, 0 failures, 0 errors
DecideAlarmUseCaseTest: 7 tests, 0 failures, 0 errors
assembleDebug: success
lintDebug: success
```

Galaxy UI tree confirmed Korean strings:

```text
날씨 아침 알람
비나 눈 예보가 있으면 이른 알람이 울립니다.
알람 사용
시간 설정
반복 요일
지역 검색
```

Galaxy UI tree confirmed localized region labels:

```text
FOUND region search label
FOUND Seoul Korean label
FOUND Incheon Korean label
FOUND Suwon Korean label
FOUND Busan Korean label
```

Galaxy UI tree confirmed localized legacy history reasons:

```text
FOUND history label
FOUND test type label
FOUND localized receiver test reason
MISS Receiver test alarm
```

## Next

- Consider a language toggle only if product testing needs it.
