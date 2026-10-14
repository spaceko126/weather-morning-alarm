# 구조

README 의 구조도를 구성 요소별로 풀어 쓴 문서입니다. 모든 이름은 실제 클래스·파일입니다.

## 앱 구성 요소

| 구성 요소 | 파일 | 책임 |
|---|---|---|
| 화면 | `presentation/MainActivity.kt`, `presentation/WeatherMorningApp.kt`, `presentation/settings/*` | 알람 설정·기록·지역·권한 화면 (Jetpack Compose) |
| 알람 예약 | `alarm/AndroidAlarmScheduler.kt`, `alarm/NextAlarmCalculator.kt` | 다음 알람 시각 계산(요일·공휴일), 정확한 알람 3개 예약 |
| 사전 확인 시작 | `alarm/WeatherPrecheckReceiver.kt` | OS 알람을 받아 WorkManager 작업을 예약 |
| 사전 확인 작업 | `worker/WeatherCheckWorker.kt` | 날씨 조회 → 판정 → 판정·기록 저장, 실패 시 재시도 |
| 예보 제공자 선택 | `data/repository/WeatherRepositoryResolver.kt` | 설정에 따라 제공자 순서를 정하고 차례로 시도 |
| 예보 제공자 | `BackendProxyWeatherRepository.kt`, `OpenMeteoWeatherRepository.kt`, `KmaWeatherRepository.kt`, `StubWeatherRepository.kt` | 프록시 / Open-Meteo 직접 / 기상청 직접(키가 있을 때) / 테스트용 고정값(디버그 빌드만) |
| 판정 | `domain/usecase/DecideAlarmUseCase.kt`, `DecideWeatherWindowUseCase.kt` | 한 시각 또는 앞뒤 시간 범위의 예보로 울림 여부 결정 |
| 알람 시각 처리 | `alarm/AlarmReceiver.kt` | 저장된 판정을 읽어 울리거나 건너뛰고, 다음 알람 예약 |
| 울림 | `alarm/AlarmService.kt`, `presentation/AlarmActivity.kt` | 포그라운드 서비스로 소리·진동, 전체 화면 알람, 스누즈 |
| 저장 | `data/local/*`, `data/local/db/*` | 알람 설정, 판정, Room 알람 기록 |
| 재부팅 | `alarm/AlarmRescheduleReceiver.kt` | 부팅 후 알람 다시 예약 |

## 서버 구성 요소 (`backend/weather-proxy`)

| 파일 | 책임 |
|---|---|
| `src/index.js` | 라우팅, 요청 제한, 캐시, 제공자 순서(Open-Meteo → 기상청 → 만료 캐시) |
| `src/weather.js` | 요청 검증, Open-Meteo·기상청 응답 변환, 캐시·요청 제한 키 |
| `src/holidays.js` | 한국천문연구원 공휴일 조회 |
| `src/privacy.js` | 개인정보처리방침 페이지 |

## 데이터가 머무는 곳

| 데이터 | 위치 |
|---|---|
| 알람 설정, 저장된 판정 | 앱 로컬 저장소 |
| 울림·건너뜀 기록 | 앱 Room DB |
| 예보 캐시 | Workers 캐시 (좌표를 소수점 둘째 자리로 묶은 키) |

앱은 예보 요청에 좌표와 목표 시각을 보내고, 요청 제한 구분용 임의 식별자(`x-weather-client-id`)를 헤더로 보냅니다.
