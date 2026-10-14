# Backend Weather Proxy Plan

## Goal

Move production weather API access behind a backend service so mobile clients do
not carry provider keys and repeated users share cached forecast results.

## MVP Client Contract

Request:

```text
GET /weather/forecast
  ?latitude=37.5665
  &longitude=126.9780
  &target=2026-06-19T07:00:00
  &locale=ko-KR
```

Response:

```json
{
  "checkedAt": "2026-06-18T12:10:00Z",
  "targetDateTime": "2026-06-19T07:00:00",
  "condition": "CLEAR",
  "precipitationProbability": 5,
  "source": "open-meteo:forecast",
  "providerAttempts": [
    {
      "provider": "OPEN_METEO",
      "status": "SUCCESS",
      "cache": "HIT"
    }
  ]
}
```

Error response:

```json
{
  "error": "WEATHER_UNAVAILABLE",
  "message": "All weather providers failed",
  "providerAttempts": [
    {
      "provider": "OPEN_METEO",
      "status": "FAILED"
    },
    {
      "provider": "KMA",
      "status": "FAILED"
    }
  ]
}
```

## Provider Order

Global default:

```text
Open-Meteo -> KMA when inside Korea and key available -> cached stale result
```

Korea-focused mode:

```text
KMA -> Open-Meteo -> cached stale result
```

## Cache Keys

Open-Meteo:

```text
provider=open-meteo
latBucket=round(latitude, 2)
lonBucket=round(longitude, 2)
targetHour=yyyy-MM-ddTHH:00
```

KMA:

```text
provider=kma
nx=<grid x>
ny=<grid y>
baseDate=yyyyMMdd
baseTime=HHmm
targetHour=yyyyMMddHH00
```

## Cache TTL

- Open-Meteo fresh TTL: 30 minutes.
- KMA fresh TTL: until the next base-time availability window.
- Stale fallback TTL: 12 hours for alarm resilience.

## Abuse Controls

- Per-device anonymous rate limit.
- Per-IP rate limit.
- Coarse coordinate bucketing before cache lookup.
- No raw API keys returned to client.
- Server logs must redact provider keys.

## Mobile App Migration Path

1. Keep current in-app providers for local development.
2. Add `BACKEND_PROXY` provider. Done in WM-045.
3. Make `BACKEND_PROXY` default for production builds when a proxy URL is
   configured. Done in WM-045.
4. Keep Open-Meteo local provider as fallback/dev mode.
5. Hide KMA key UI behind developer settings. Done in WM-043.

## Worker Scaffold

Initial Cloudflare Worker scaffold lives in:

```text
backend/weather-proxy
```

Current implemented scope:

- `GET /weather/forecast`
- `GET /privacy`
- Open-Meteo upstream provider
- KMA fallback when `KMA_SERVICE_KEY` exists and coordinates are inside Korea
- App-compatible JSON response
- Coarse cache key by provider, lat/lon bucket, and target hour
- Cloudflare Cache API with 30 minute TTL
- Stale cache fallback with 12 hour TTL
- Cloudflare Rate Limiting binding
- Android `x-weather-client-id` header for proxy requests
- Public privacy policy HTML endpoint for Play Console
- Wrangler dry-run validation

Current test command:

```text
cd backend/weather-proxy
node --test
npx wrangler deploy --dry-run --env staging
npx wrangler deploy --dry-run --env production
```

Secret setup:

```text
cd backend/weather-proxy
wrangler secret put KMA_SERVICE_KEY
```

Public privacy URL after production deploy:

```text
https://<production-worker-url>/privacy
```

## Open Questions

- Hosting target: Cloudflare Workers, Fly.io, or another backend.
- Auth model: anonymous install id or account-based.
- Exact production quota and pricing thresholds.
- Production deploy URL and secret configuration.
