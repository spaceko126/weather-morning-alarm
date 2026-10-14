# Release Notes

## 0.3.0

English:

```text
Reliability, multiple alarms and a much smaller app.

- The alarm now rings from a foreground service, so it is no longer silent when
  the phone is awake and the full-screen alarm is shown as a notification.
- Snooze, from the alarm screen or its notification.
- Alarms survive a Play Store update, a time-zone change and a clock change.
- A total weather outage now applies your fail policy instead of being replaced
  by placeholder weather.
- The forecast check runs as expedited work with retries and a backup check an
  hour before the alarm.
- Multiple alarms, each with its own name, time, days, location and rules.
- Rebuilt time picker: flick, tap a number, or type the time.
- Dark theme, and the alarm screen stays dark for early mornings.
- Permission states refresh when you come back from system settings.
- First-run walkthrough covering the alarm permissions and battery optimisation.
- Region search covers anywhere, not nine preset cities.
- Lunar New Year, Chuseok and substitute holidays count as holidays.
- History keeps 60 entries instead of 5.
- Download size dropped from about 21 MB to about 2.4 MB.
```

Korean:

```text
신뢰성 개선, 다중 알람, 그리고 훨씬 가벼워진 앱입니다.

- 알람이 포그라운드 서비스로 울립니다. 폰을 쓰는 중이라 전체화면 알람이
  알림으로만 뜨는 상황에서도 이제 소리가 납니다.
- 다시 알림(스누즈)을 알람 화면과 알림에서 쓸 수 있습니다.
- 스토어 업데이트, 시간대 변경, 시각 변경 후에도 알람이 사라지지 않습니다.
- 날씨 조회가 모두 실패하면 임의의 날씨로 대체하지 않고 설정한 실패 정책을
  따릅니다.
- 예보 확인이 가속 작업으로 실행되고 재시도하며, 알람 1시간 전 백업 확인이
  추가됐습니다.
- 알람을 여러 개 만들 수 있습니다. 각각 이름, 시각, 요일, 지역, 조건이 따로입니다.
- 시간 선택을 다시 만들었습니다. 튕겨서 넘기거나, 숫자를 누르거나, 직접
  입력할 수 있습니다.
- 다크 테마를 지원하고, 알람 화면은 새벽 사용을 고려해 항상 어둡습니다.
- 시스템 설정에서 권한을 바꾸고 돌아오면 상태가 바로 갱신됩니다.
- 첫 실행에서 알람 권한과 배터리 최적화를 안내합니다.
- 지역 검색이 9개 도시 제한 없이 전 세계를 지원합니다.
- 설날, 추석, 대체공휴일이 공휴일로 반영됩니다.
- 기록이 5건에서 60건으로 늘었습니다.
- 다운로드 크기가 약 21MB에서 약 2.4MB로 줄었습니다.
```

Known limitations:

```text
Forecast accuracy depends on weather providers and network state.
The active alarm screen shows the configured alarm check range, not a full-day
weather report.
Holiday data needs the backend's data.go.kr key to be subscribed to the KASI
holiday service; without it only fixed-date holidays apply.
Release APK installation over an existing debug build requires uninstalling the
debug build first.
```


## 0.1.3

English:

```text
Weather Morning Alarm APK release.

- Fixed the precipitation threshold rule.
- If the user sets 60%, rain/snow/sleet alarms now ring only at 60% or higher.
- Forecast rows below the selected threshold, such as 59% when set to 60%, now skip correctly.
- Weather-based early morning alarm for rain or snow forecasts.
- Forecast range setting: alarm hour only or around alarm time.
- Optional rain amount threshold: no limit, drizzle+, light rain+, heavy rain+.
- Samsung-style alarm time picker and repeat-day controls.
- Alarm tone opens the Android/Samsung sound picker.
- Vibration can be enabled or disabled.
- Full-screen alarm shows weather status and up to 3 hourly forecast rows.
- Large Stop button for easier dismissal while the alarm is ringing.
- Bottom Test alarm button schedules a real 5-second alarm permission check.
- Open-Meteo global weather support with production backend proxy.
- In-app privacy notice and hosted privacy policy.
```

Korean:

```text
Weather Morning Alarm APK 릴리스입니다.

- 강수확률 기준 동작을 수정했습니다.
- 사용자가 60%로 설정하면 비/눈/진눈깨비 알람은 60% 이상일 때만 울립니다.
- 60% 설정 상태에서 59% 예보처럼 기준보다 낮은 경우는 이제 정상적으로 건너뜁니다.
- 비나 눈 예보가 있을 때만 이른 아침 알람을 울릴 수 있습니다.
- 예보 확인 범위는 알람 시간만 또는 알람 전후 시간대로 선택할 수 있습니다.
- 선택 강수량 기준은 없음, 부슬비 이상, 약한 비 이상, 강한 비 이상을 지원합니다.
- 삼성 알람 앱에 가까운 시간 선택 UI와 반복 요일 설정을 제공합니다.
- 알림음은 Android/Samsung 소리 선택 화면에서 고를 수 있습니다.
- 진동은 켜거나 끌 수 있습니다.
- 전체화면 알람은 날씨 상태와 최대 3개의 시간별 예보를 보여줍니다.
- 알람 중지를 쉽게 누를 수 있도록 큰 중지 버튼을 제공합니다.
- 하단 알람 테스트 버튼으로 5초 뒤 실제 테스트 알람을 울려 권한 상태를 확인할 수 있습니다.
- Open-Meteo 글로벌 날씨와 production backend proxy를 사용합니다.
- 앱 안 개인정보 안내와 공개 개인정보처리방침을 제공합니다.
```

Known limitations:

```text
Forecast accuracy depends on weather providers and network state.
The active alarm screen shows the configured alarm check range, not a full-day weather report.
Release APK installation over an existing debug build requires uninstalling the debug build first.
```
