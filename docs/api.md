# 프록시 서버 API

Cloudflare Workers 에서 동작합니다. 모든 엔드포인트는 `GET` 만 받습니다 (그 외 `405 METHOD_NOT_ALLOWED`). 운영 주소는 공개하지 않으며, 문서에서는 `https://<your-worker>.workers.dev` 로 씁니다.

## GET /weather/forecast

| 파라미터 | 필수 | 설명 |
|---|---|---|
| `latitude` | 예 | -90 ~ 90 |
| `longitude` | 예 | -180 ~ 180 |
| `target` | 예 | 현지 시각 `YYYY-MM-DDTHH:mm` (정시 단위로 맞춤) |
| `locale` | 아니오 | 기본 `en-US` |

| 헤더 | 설명 |
|---|---|
| `x-weather-client-id` (요청) | 요청 제한을 기기별로 나누는 임의 식별자 (8~128자). 없으면 경로 단위로 제한 |
| `x-weather-cache` (응답) | `HIT` 신선 캐시 · `MISS` 새로 조회 · `STALE` 제공자 실패로 만료 대비 캐시 사용 |

**처리 순서**: 요청 검증 → 요청 제한(요청 제한 설정이 있을 때) → 신선 캐시 → Open-Meteo → (실패 시, 기상청 키가 있고 국내 좌표면) 기상청 → 만료 대비 캐시 → 오류.
캐시 유지 시간은 기본 신선 30분(`CACHE_TTL_SECONDS`), 만료 대비 12시간(`STALE_CACHE_TTL_SECONDS`)입니다.

**성공 응답 (200)**

```json
{
  "checkedAt": "2026-10-14T21:00:00.000Z",
  "targetDateTime": "2026-10-15T07:00:00",
  "condition": "RAIN",
  "precipitationProbability": 70,
  "precipitationAmountMm": 1.2,
  "source": "open-meteo:forecast",
  "providerAttempts": [{ "provider": "OPEN_METEO", "status": "SUCCESS", "cache": "MISS" }]
}
```

값은 형식 예시입니다 (실제 측정값 아님).

## GET /holidays

| 파라미터 | 설명 |
|---|---|
| `year` | 2000 ~ 2100 |
| `country` | `KR` 만 지원 (기본값) |

성공 시 `{ "year", "source": "KASI", "holidays": [...] }`. 공휴일 데이터를 가져오지 못하면 `503 HOLIDAYS_UNAVAILABLE`.

## GET /privacy

개인정보처리방침 HTML 페이지.

## 오류 형식

```json
{ "error": "INVALID_REQUEST", "message": "...", "providerAttempts": [] }
```

| 상태 | error | 상황 |
|---|---|---|
| 400 | `INVALID_REQUEST` | 파라미터 오류 |
| 404 | `NOT_FOUND` | 없는 경로 |
| 405 | `METHOD_NOT_ALLOWED` | GET 이 아님 |
| 429 | `RATE_LIMITED` | 요청 제한 초과 |
| 502 | `WEATHER_UNAVAILABLE` | 모든 제공자 실패, 캐시도 없음 |
| 503 | `HOLIDAYS_UNAVAILABLE` | 공휴일 데이터 미설정 |

## 로그

구조화된 JSON 로그(`event`, `level`, 캐시·제공자 상태)만 남기고 좌표와 비밀값은 로그에 넣지 않습니다.
