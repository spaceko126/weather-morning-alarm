# Weather Morning Alarm

Galaxy-first conditional alarm app.

It rings only when the configured morning weather condition is met, such as rain
or snow near commute time.

## MVP Goal

- User sets early alarm time.
- User sets weekday repeat days.
- User sets weather trigger: rain, snow, or precipitation probability.
- App checks weather before alarm time.
- App rings at alarm time only if the saved weather decision says it should.
- App stores why it rang or skipped.

## Current Status

This repository contains the Android MVP, Cloudflare Worker backend scaffold,
Play release prep docs, and release verification scripts.

Current state board:

```text
docs/project-status.md
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release AAB:

```text
app/build/outputs/bundle/release/app-release.aab
```

## Key Commands

Debug validation:

```text
.\scripts\test-debug.ps1
```

Connected Galaxy validation:

```text
.\scripts\test-device.ps1
```

Backend validation:

```text
cd backend\weather-proxy
npm test
npx wrangler deploy --dry-run --env staging
npx wrangler deploy --dry-run --env production
```

Release preflight with the real production Worker URL:

```text
.\scripts\release-preflight.ps1 -VersionCode 2 -VersionName 0.1.1 -WeatherProxyBaseUrl https://<your-worker>.workers.dev
```

Play Console package check:

```text
.\scripts\check-play-console-package.ps1
```

## Development Order

1. Android project skeleton.
2. Domain models and decision engine.
3. Local settings and decision storage.
4. Exact alarm scheduling.
5. Weather precheck worker.
6. Full-screen alarm UI.
7. Weather API integration.
8. Room-based history.
9. Galaxy device testing.
10. iOS/server extension planning.

## Progress Tracking

Large work units are recorded in:

```text
docs/progress-log.md
```

User-owned deployment/signing steps:

```text
docs/computer-actions.md
```

Play Console steps:

```text
docs/play-console-release-steps.md
```

Play Console submission pack:

```text
docs/play-console-submission-pack.md
```

Release checklist:

```text
docs/release-checklist.md
```

Weather API notes:

```text
docs/weather-api.md
```

Settings UX notes:

```text
docs/settings-ux.md
```

Location notes:

```text
docs/location.md
```

Galaxy device test notes:

```text
docs/device-test.md
```

History notes:

```text
docs/history.md
```

Room history notes:

```text
docs/room-history.md
```

History ViewModel notes:

```text
docs/history-viewmodel.md
```

Settings ViewModel notes:

```text
docs/settings-viewmodel.md
```

Settings validation notes:

```text
docs/settings-validation.md
```

Scheduling test notes:

```text
docs/scheduling-tests.md
```

Decision engine test notes:

```text
docs/decision-tests.md
```

KMA parser test notes:

```text
docs/kma-parser-tests.md
```

Localization notes:

```text
docs/localization.md
```

UI structure notes:

```text
docs/ui-structure.md
```

Alarm signal notes:

```text
docs/alarm-signal.md
```
