# Third-party notices

The project's own code is licensed under the MIT License (see `LICENSE`).
The components below are **not** relicensed and remain under their original licenses.

## Bundled in this repository

| Component | Location | License |
|---|---|---|
| Noto Sans CJK (Korean + Latin subset) | `app/src/main/res/font/app_sans_kr_regular.otf` | SIL Open Font License 1.1 |
| Material Icons (path data of several vector icons) | `app/src/main/res/drawable/ic_*.xml` | Apache License 2.0 |
| Gradle Wrapper | `gradle/wrapper/gradle-wrapper.jar` | Apache License 2.0 |

- Noto Sans CJK — Copyright 2014-2021 Adobe (http://www.adobe.com/). Full OFL 1.1 text: [`licenses/OFL-NotoSansCJK.txt`](licenses/OFL-NotoSansCJK.txt)
  The subset is a Modified Version under the OFL; it may be used and redistributed with this notice, and may not be sold by itself.
- Material Icons — Copyright Google LLC. Apache License 2.0: https://www.apache.org/licenses/LICENSE-2.0
- Gradle Wrapper — Copyright Gradle, Inc. Apache License 2.0.

## Resolved at build time (not bundled)

AndroidX (Activity, Compose, Core, Lifecycle, Room, WorkManager, Test), Kotlin and kotlinx-serialization,
Android Gradle Plugin, KSP — Apache License 2.0. JUnit 4 — Eclipse Public License 1.0. org.json — Public Domain (JSON License text).
Backend dev tool `wrangler` — MIT OR Apache-2.0.

## External data services (used at runtime, not distributed)

Open-Meteo forecast API (data CC BY 4.0, attribution required when displaying data), Korea Meteorological Administration
short-term forecast API and Korea Astronomy and Space Science Institute holiday API (public data, used under their terms).
