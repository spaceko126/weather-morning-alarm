import assert from "node:assert/strict";
import test from "node:test";
import worker from "../src/index.js";
import {
  buildCacheKey,
  buildKmaUrl,
  buildOpenMeteoUrl,
  buildRateLimitKey,
  calculateKmaBaseTime,
  isLikelyKoreaCoordinate,
  parseKmaForecast,
  parseForecastRequest,
  parseOpenMeteoForecast,
  toKmaGrid,
} from "../src/weather.js";
import { privacyPolicyHtml } from "../src/privacy.js";

test("parses valid forecast request", () => {
  const url = new URL("https://proxy.test/weather/forecast?latitude=37.5665&longitude=126.9780&target=2026-06-19T07:30:00&locale=ko-KR");

  const parsed = parseForecastRequest(url);

  assert.equal(parsed.ok, true);
  assert.equal(parsed.value.latitude, 37.5665);
  assert.equal(parsed.value.longitude, 126.978);
  assert.equal(parsed.value.target, "2026-06-19T07:00:00");
  assert.equal(parsed.value.locale, "ko-KR");
});

test("rejects invalid latitude", () => {
  const url = new URL("https://proxy.test/weather/forecast?latitude=100&longitude=126.9780&target=2026-06-19T07:00:00");

  const parsed = parseForecastRequest(url);

  assert.equal(parsed.ok, false);
  assert.match(parsed.message, /latitude/);
});

test("builds Open-Meteo URL", () => {
  const url = buildOpenMeteoUrl("https://api.open-meteo.com/v1/forecast", {
    latitude: 37.5665,
    longitude: 126.978,
  });

  assert.equal(url, "https://api.open-meteo.com/v1/forecast?latitude=37.566500&longitude=126.978000&hourly=precipitation_probability%2Cprecipitation%2Cweather_code&timezone=auto&forecast_days=3");
});

test("maps Open-Meteo rain response to app contract", () => {
  const forecast = parseOpenMeteoForecast(
    {
      hourly: {
        time: ["2026-06-19T06:00", "2026-06-19T07:00"],
        precipitation_probability: [10, 75],
        precipitation: [0, 2.5],
        weather_code: [0, 61],
      },
    },
    "2026-06-19T07:30:00",
  );

  assert.equal(forecast.targetDateTime, "2026-06-19T07:00:00");
  assert.equal(forecast.condition, "RAIN");
  assert.equal(forecast.precipitationProbability, 75);
  assert.equal(forecast.precipitationAmountMm, 2.5);
  assert.equal(forecast.source, "open-meteo:forecast");
});

test("builds coarse cache key", () => {
  const key = buildCacheKey({
    latitude: 37.5665,
    longitude: 126.978,
    target: "2026-06-19T07:30:00",
  });

  assert.equal(key, "cache=fresh&provider=open-meteo&lat=37.57&lon=126.98&target=2026-06-19T07:00:00");
  assert.equal(
    buildCacheKey({
      latitude: 37.5665,
      longitude: 126.978,
      target: "2026-06-19T07:30:00",
    }, "stale"),
    "cache=stale&provider=open-meteo&lat=37.57&lon=126.98&target=2026-06-19T07:00:00",
  );
});

test("builds rate limit key from client id header", () => {
  const request = new Request("https://proxy.test/weather/forecast", {
    headers: {
      "x-weather-client-id": "install_12345678",
    },
  });

  assert.equal(buildRateLimitKey(request), "client:install_12345678");
});

test("falls back to route rate limit key when client id is missing", () => {
  const request = new Request("https://proxy.test/weather/forecast");

  assert.equal(buildRateLimitKey(request), "route:/weather/forecast");
});

test("detects Korea coordinates", () => {
  assert.equal(isLikelyKoreaCoordinate({ latitude: 37.5665, longitude: 126.978 }), true);
  assert.equal(isLikelyKoreaCoordinate({ latitude: 40.7128, longitude: -74.006 }), false);
});

test("converts Seoul coordinate to KMA grid", () => {
  assert.deepEqual(toKmaGrid(37.5665, 126.978), { nx: 60, ny: 127 });
});

test("calculates KMA base time in Korea timezone", () => {
  const base = calculateKmaBaseTime(new Date("2026-06-17T01:30:00Z"));

  assert.deepEqual(base, { date: "20260617", time: "0800" });
});

test("builds KMA URL", () => {
  const url = buildKmaUrl(
    "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst",
    "test key",
    { latitude: 37.5665, longitude: 126.978 },
    new Date("2026-06-17T01:30:00Z"),
  );

  assert.equal(url, "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst?serviceKey=test+key&pageNo=1&numOfRows=1000&dataType=JSON&base_date=20260617&base_time=0800&nx=60&ny=127");
});

test("maps KMA forecast response to app contract", () => {
  const forecast = parseKmaForecast(
    {
      response: {
        header: { resultCode: "00", resultMsg: "NORMAL_SERVICE" },
        body: {
          items: {
            item: [
              { fcstDate: "20260619", fcstTime: "0700", category: "POP", fcstValue: "80" },
              { fcstDate: "20260619", fcstTime: "0700", category: "PCP", fcstValue: "1mm 미만" },
              { fcstDate: "20260619", fcstTime: "0700", category: "PTY", fcstValue: "3" },
            ],
          },
        },
      },
    },
    "2026-06-19T07:30:00",
  );

  assert.equal(forecast.targetDateTime, "2026-06-19T07:00:00");
  assert.equal(forecast.condition, "SNOW");
  assert.equal(forecast.precipitationProbability, 80);
  assert.equal(forecast.precipitationAmountMm, 0.1);
  assert.equal(forecast.source, "kma:getVilageFcst");
});

test("maps KMA Korean trace precipitation to drizzle amount", () => {
  const forecast = parseKmaForecast(
    {
      response: {
        header: { resultCode: "00", resultMsg: "NORMAL_SERVICE" },
        body: {
          items: {
            item: [
              { fcstDate: "20260619", fcstTime: "0700", category: "POP", fcstValue: "60" },
              { fcstDate: "20260619", fcstTime: "0700", category: "PCP", fcstValue: "1mm 미만" },
              { fcstDate: "20260619", fcstTime: "0700", category: "PTY", fcstValue: "1" },
            ],
          },
        },
      },
    },
    "2026-06-19T07:30:00",
  );

  assert.equal(forecast.precipitationAmountMm, 0.1);
});

test("builds public privacy policy html", () => {
  const html = privacyPolicyHtml("support@example.com");

  assert.match(html, /Weather Morning Alarm Privacy Policy/);
  assert.match(html, /support@example.com/);
  assert.match(html, /한국어 요약/);
});

test("escapes privacy contact email html", () => {
  const html = privacyPolicyHtml("<support@example.com>");

  assert.match(html, /&lt;support@example.com&gt;/);
  assert.doesNotMatch(html, /<support@example.com>/);
});

test("serves privacy policy route", async () => {
  const response = await worker.fetch(
    new Request("https://proxy.test/privacy"),
    { PRIVACY_CONTACT_EMAIL: "support@example.com" },
    { waitUntil() {} },
  );

  assert.equal(response.status, 200);
  assert.equal(response.headers.get("content-type"), "text/html; charset=utf-8");
  assert.match(await response.text(), /support@example.com/);
});
