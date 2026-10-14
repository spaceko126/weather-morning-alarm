# Weather Morning Alarm Privacy Policy

Last updated: 2026-06-18

Weather Morning Alarm helps users wake earlier when rain or snow is forecast.

## Information Stored On The Device

The app stores the following information on the user's device:

- Alarm time and repeat days.
- Weather trigger settings.
- Selected region name and coordinates.
- Alarm decision history.
- Developer KMA service key, only when manually configured for local testing.
- Anonymous backend proxy client id, only for backend proxy rate limiting.

App data is excluded from Android backup.

## Information Sent For Weather Checks

To fetch weather forecasts, the app may send:

- Latitude and longitude.
- Target forecast time.
- Locale.
- Anonymous install id when backend proxy mode is used.

In backend proxy mode, this information is sent to the Weather Morning Alarm
backend. The backend may request forecast data from weather providers such as
Open-Meteo or the Korea Meteorological Administration.

In direct provider mode, coordinates may be sent directly from the app to the
selected weather provider.

## How Information Is Used

Information is used for:

- Weather-based alarm decisions.
- Forecast retrieval.
- Backend rate limiting and abuse prevention.
- Local alarm history display.

## Advertising And Sale Of Data

The app does not use advertising identifiers in the current design.

The app does not sell personal data.

## Backend Logs

The backend is designed to log operational events such as cache hits, provider
success/failure, and rate-limit events. It is designed not to log raw
coordinates, provider API keys, or anonymous client ids.

## Data Deletion

Users can delete locally stored app data by deleting the app or clearing app
storage in Android settings.

## Contact

spaceko126@naver.com

## Korean Summary

날씨 아침 알람은 비나 눈 예보가 있을 때 사용자가 더 일찍 일어날 수 있도록 돕는 앱입니다.

앱 설정, 알람 기록, 선택한 지역 정보는 기기에 저장되며 Android 백업에서 제외됩니다. 날씨 확인을 위해 위도와 경도, 예보 대상 시간, 언어 설정이 날씨 제공자 또는 백엔드 프록시로 전송될 수 있습니다. 백엔드 프록시 모드에서는 사용량 제한을 위해 익명 설치 ID가 전송될 수 있습니다.

현재 설계에서 앱은 광고 식별자를 사용하지 않으며 개인정보를 판매하지 않습니다.

사용자는 Android 설정에서 앱 데이터를 삭제하거나 앱을 삭제해 로컬 데이터를 지울 수 있습니다.
