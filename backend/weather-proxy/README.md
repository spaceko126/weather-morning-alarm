# Weather Morning Proxy

Cloudflare Worker backend for production weather checks.

## Endpoint

```text
GET /weather/forecast?latitude=37.5665&longitude=126.9780&target=2026-06-19T07:00:00&locale=ko-KR
```

Successful response matches the Android app proxy contract:

```json
{
  "checkedAt": "2026-06-18T12:10:00.000Z",
  "targetDateTime": "2026-06-19T07:00:00",
  "condition": "RAIN",
  "precipitationProbability": 65,
  "source": "open-meteo:forecast",
  "providerAttempts": [
    {
      "provider": "OPEN_METEO",
      "status": "SUCCESS",
      "cache": "MISS"
    }
  ]
}
```

Public privacy policy:

```text
GET /privacy
```

## Local Commands

```text
npm install
npm test
npm run dev
npm run dryrun:staging
npm run dryrun:production
```

## Deploy Commands

```text
npm run dryrun:staging
npm run dryrun:production
npx wrangler deploy --env staging
npx wrangler deploy --env production
```

## Current Scope

- Open-Meteo provider.
- KMA fallback for Korea coordinates when `KMA_SERVICE_KEY` is configured.
- Coarse cache key by lat/lon bucket and target hour.
- Cloudflare Cache API with 30 minute TTL.
- Stale cache fallback with 12 hour TTL when live providers fail.
- Cloudflare Rate Limiting binding at 1500 requests per 60 seconds.
- Structured JSON logs for cache, provider success/failure, and rate limiting.
- Public `/privacy` HTML endpoint for Play Console privacy policy URL.

## Secrets

Local development:

```text
cp .dev.vars.example .dev.vars
```

Production:

```text
wrangler secret put KMA_SERVICE_KEY --env staging
wrangler secret put KMA_SERVICE_KEY --env production
```

## Variables

`PRIVACY_CONTACT_EMAIL` is set to `<CONTACT_EMAIL>` in `wrangler.jsonc`.

## Next

- Deploy after Cloudflare login.
- Smoke test the production `/privacy` URL.
