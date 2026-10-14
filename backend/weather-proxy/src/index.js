import {
  buildCacheKey,
  buildKmaUrl,
  buildOpenMeteoUrl,
  buildRateLimitKey,
  errorJson,
  forecastJson,
  isLikelyKoreaCoordinate,
  parseKmaForecast,
  parseForecastRequest,
  parseOpenMeteoForecast,
} from "./weather.js";
import { privacyHtmlResponse } from "./privacy.js";
import { fetchHolidays, holidaysJson, parseHolidayRequest } from "./holidays.js";

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    if (request.method !== "GET") {
      return errorJson("METHOD_NOT_ALLOWED", "Use GET.", 405);
    }
    if (url.pathname === "/privacy") {
      return privacyHtmlResponse(env.PRIVACY_CONTACT_EMAIL || "");
    }
    if (url.pathname === "/holidays") {
      const parsedHolidays = parseHolidayRequest(url);
      if (!parsedHolidays.ok) {
        return errorJson("INVALID_REQUEST", parsedHolidays.message, 400);
      }
      const result = await fetchHolidays(env, parsedHolidays.value.year);
      if (!result.ok) {
        logInfo("holidays_unavailable", { reason: result.reason });
        return errorJson("HOLIDAYS_UNAVAILABLE", "Holiday data is not configured.", 503);
      }
      logInfo("holidays_success");
      return holidaysJson(parsedHolidays.value.year, result.holidays, "KASI");
    }
    if (url.pathname !== "/weather/forecast") {
      return errorJson("NOT_FOUND", "Unknown route.", 404);
    }

    const parsed = parseForecastRequest(url);
    if (!parsed.ok) {
      return errorJson("INVALID_REQUEST", parsed.message, 400);
    }

    const rateLimited = await enforceRateLimit(request, env);
    if (rateLimited) {
      logInfo("rate_limited");
      return rateLimited;
    }

    const freshCacheKey = buildCacheKey(parsed.value, "fresh");
    const staleCacheKey = buildCacheKey(parsed.value, "stale");
    const freshCacheRequest = new Request(`https://weather-morning-cache.local/${freshCacheKey}`);
    const staleCacheRequest = new Request(`https://weather-morning-cache.local/${staleCacheKey}`);
    const cache = caches.default;
    const cached = await cache.match(freshCacheRequest);
    if (cached) {
      logInfo("forecast_cache_hit", { cache: "HIT" });
      return withCacheHeader(cached, "HIT");
    }

    const providerAttempts = [];
    const openMeteoBaseUrl = env.OPEN_METEO_BASE_URL || "https://api.open-meteo.com/v1/forecast";
    const upstreamUrl = buildOpenMeteoUrl(openMeteoBaseUrl, parsed.value);
    const openMeteoResult = await fetchOpenMeteoForecast(upstreamUrl, parsed.value.target);
    if (openMeteoResult.ok) {
      providerAttempts.push({ provider: "OPEN_METEO", status: "SUCCESS", cache: "MISS" });
      logInfo("forecast_provider_success", { provider: "OPEN_METEO", cache: "MISS" });
      return cacheAndReturn(
        forecastJson(openMeteoResult.forecast, providerAttempts),
        cache,
        freshCacheRequest,
        staleCacheRequest,
        ctx,
        env,
      );
    }
    providerAttempts.push({ provider: "OPEN_METEO", status: "FAILED", cache: "MISS" });

    if (env.KMA_SERVICE_KEY && isLikelyKoreaCoordinate(parsed.value)) {
      const kmaBaseUrl = env.KMA_BASE_URL || "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst";
      const kmaUrl = buildKmaUrl(kmaBaseUrl, env.KMA_SERVICE_KEY, parsed.value);
      const kmaResult = await fetchKmaForecast(kmaUrl, parsed.value.target);
      if (kmaResult.ok) {
        providerAttempts.push({ provider: "KMA", status: "SUCCESS", cache: "MISS" });
        logInfo("forecast_provider_success", { provider: "KMA", cache: "MISS" });
        return cacheAndReturn(
          forecastJson(kmaResult.forecast, providerAttempts),
          cache,
          freshCacheRequest,
          staleCacheRequest,
          ctx,
          env,
        );
      }
      providerAttempts.push({ provider: "KMA", status: "FAILED", cache: "MISS" });
    }

    const stale = await cache.match(staleCacheRequest);
    if (stale) {
      logInfo("forecast_cache_hit", { cache: "STALE" });
      return withCacheHeader(stale, "STALE");
    }

    logInfo("forecast_provider_failed", {
      attempts: providerAttempts.map((attempt) => attempt.provider).join(","),
    });
    return errorJson(
      "WEATHER_UNAVAILABLE",
      openMeteoResult.message,
      502,
      providerAttempts,
    );
  },
};

async function fetchOpenMeteoForecast(url, target) {
  try {
    const upstream = await fetch(url, {
      headers: {
        "accept": "application/json",
      },
    });

    if (!upstream.ok) {
      return { ok: false, message: `Open-Meteo request failed: HTTP ${upstream.status}` };
    }

    const upstreamBody = await upstream.json();
    return { ok: true, forecast: parseOpenMeteoForecast(upstreamBody, target) };
  } catch (error) {
    return { ok: false, message: error instanceof Error ? error.message : "Open-Meteo request failed." };
  }
}

async function fetchKmaForecast(url, target) {
  try {
    const upstream = await fetch(url, {
      headers: {
        "accept": "application/json",
      },
    });

    if (!upstream.ok) {
      return { ok: false, message: `KMA request failed: HTTP ${upstream.status}` };
    }

    const upstreamBody = await upstream.json();
    return { ok: true, forecast: parseKmaForecast(upstreamBody, target) };
  } catch (error) {
    return { ok: false, message: error instanceof Error ? error.message : "KMA request failed." };
  }
}

async function enforceRateLimit(request, env) {
  if (!env.WEATHER_FORECAST_RATE_LIMITER) {
    return null;
  }

  const { success } = await env.WEATHER_FORECAST_RATE_LIMITER.limit({
    key: buildRateLimitKey(request),
  });
  if (success) {
    return null;
  }
  return errorJson("RATE_LIMITED", "Too many weather forecast requests.", 429);
}

function cacheAndReturn(response, cache, freshCacheRequest, staleCacheRequest, ctx, env) {
    const ttlSeconds = Number.parseInt(env.CACHE_TTL_SECONDS || "1800", 10);
    const staleTtlSeconds = Number.parseInt(env.STALE_CACHE_TTL_SECONDS || "43200", 10);
    const freshCacheable = withCacheControl(response.clone(), ttlSeconds);
    const staleCacheable = withCacheControl(response.clone(), staleTtlSeconds);
    ctx.waitUntil(Promise.all([
      cache.put(freshCacheRequest, freshCacheable.clone()),
      cache.put(staleCacheRequest, staleCacheable.clone()),
    ]));
    return withCacheHeader(freshCacheable, "MISS");
}

function withCacheControl(response, ttlSeconds) {
  const headers = new Headers(response.headers);
  headers.set("cache-control", `public, max-age=${ttlSeconds}`);
  return new Response(response.body, {
    status: response.status,
    headers,
  });
}

function withCacheHeader(response, cacheStatus) {
  const headers = new Headers(response.headers);
  headers.set("x-weather-cache", cacheStatus);
  return new Response(response.body, {
    status: response.status,
    headers,
  });
}

function logInfo(event, fields = {}) {
  console.log(JSON.stringify({
    event,
    level: "info",
    ...fields,
  }));
}
