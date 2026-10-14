const WEATHER_CODE_TO_CONDITION = new Map([
  [56, "SLEET"],
  [57, "SLEET"],
  [66, "SLEET"],
  [67, "SLEET"],
  [71, "SNOW"],
  [73, "SNOW"],
  [75, "SNOW"],
  [77, "SNOW"],
  [85, "SNOW"],
  [86, "SNOW"],
  [51, "RAIN"],
  [53, "RAIN"],
  [55, "RAIN"],
  [61, "RAIN"],
  [63, "RAIN"],
  [65, "RAIN"],
  [80, "RAIN"],
  [81, "RAIN"],
  [82, "RAIN"],
  [95, "RAIN"],
  [96, "RAIN"],
  [99, "RAIN"],
]);

export function parseForecastRequest(url) {
  const latitude = Number.parseFloat(url.searchParams.get("latitude") || "");
  const longitude = Number.parseFloat(url.searchParams.get("longitude") || "");
  const targetRaw = url.searchParams.get("target") || "";
  const locale = url.searchParams.get("locale") || "en-US";

  if (!Number.isFinite(latitude) || latitude < -90 || latitude > 90) {
    return { ok: false, message: "latitude must be between -90 and 90." };
  }
  if (!Number.isFinite(longitude) || longitude < -180 || longitude > 180) {
    return { ok: false, message: "longitude must be between -180 and 180." };
  }
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/.test(targetRaw)) {
    return { ok: false, message: "target must be local ISO date-time." };
  }

  return {
    ok: true,
    value: {
      latitude,
      longitude,
      target: normalizeTargetHour(targetRaw),
      locale,
    },
  };
}

export function buildOpenMeteoUrl(baseUrl, request) {
  const url = new URL(baseUrl);
  url.searchParams.set("latitude", formatCoordinate(request.latitude));
  url.searchParams.set("longitude", formatCoordinate(request.longitude));
  url.searchParams.set("hourly", "precipitation_probability,precipitation,weather_code");
  url.searchParams.set("timezone", "auto");
  url.searchParams.set("forecast_days", "3");
  return url.toString();
}

export function buildKmaUrl(baseUrl, serviceKey, request, now = new Date()) {
  const grid = toKmaGrid(request.latitude, request.longitude);
  const base = calculateKmaBaseTime(now);
  const url = new URL(baseUrl);
  url.searchParams.set("serviceKey", serviceKey);
  url.searchParams.set("pageNo", "1");
  url.searchParams.set("numOfRows", "1000");
  url.searchParams.set("dataType", "JSON");
  url.searchParams.set("base_date", base.date);
  url.searchParams.set("base_time", base.time);
  url.searchParams.set("nx", String(grid.nx));
  url.searchParams.set("ny", String(grid.ny));
  return url.toString();
}

export function parseOpenMeteoForecast(body, target) {
  const hourly = body.hourly || {};
  const times = Array.isArray(hourly.time) ? hourly.time : [];
  const precipitation = Array.isArray(hourly.precipitation_probability)
    ? hourly.precipitation_probability
    : [];
  const precipitationAmount = Array.isArray(hourly.precipitation)
    ? hourly.precipitation
    : [];
  const weatherCodes = Array.isArray(hourly.weather_code) ? hourly.weather_code : [];
  const targetHour = normalizeTargetHour(target);
  const index = times.findIndex((time) => normalizeTargetHour(time) === targetHour);

  if (index < 0) {
    throw new Error(`Target hour not found in Open-Meteo response: ${targetHour}`);
  }

  const weatherCode = Number(weatherCodes[index] ?? 0);
  return {
    checkedAt: new Date().toISOString(),
    targetDateTime: targetHour,
    condition: WEATHER_CODE_TO_CONDITION.get(weatherCode) || "CLEAR",
    precipitationProbability: Number(precipitation[index] ?? 0),
    precipitationAmountMm: Number(precipitationAmount[index] ?? 0),
    source: "open-meteo:forecast",
  };
}

export function parseKmaForecast(body, target) {
  const response = body.response || {};
  const header = response.header || {};
  if (header.resultCode !== "00") {
    throw new Error(`KMA error ${header.resultCode || "UNKNOWN"}: ${header.resultMsg || "Unknown error"}`);
  }

  const itemContainer = response.body?.items;
  const items = Array.isArray(itemContainer?.item) ? itemContainer.item : [];
  const targetDate = normalizeTargetHour(target).slice(0, 10).replaceAll("-", "");
  const targetHour = `${normalizeTargetHour(target).slice(11, 13)}00`;
  let precipitationProbability = 0;
  let precipitationAmountMm = 0;
  let condition = "CLEAR";

  for (const item of items) {
    if (item.fcstDate !== targetDate || item.fcstTime !== targetHour) {
      continue;
    }
    if (item.category === "POP") {
      precipitationProbability = Number.parseInt(String(item.fcstValue), 10) || 0;
    }
    if (item.category === "PCP") {
      precipitationAmountMm = parseKmaPrecipitationAmountSafe(String(item.fcstValue));
    }
    if (item.category === "PTY") {
      condition = conditionFromKmaPty(String(item.fcstValue));
    }
  }

  return {
    checkedAt: new Date().toISOString(),
    targetDateTime: normalizeTargetHour(target),
    condition,
    precipitationProbability,
    precipitationAmountMm,
    source: "kma:getVilageFcst",
  };
}

export function buildCacheKey(request, cacheKind = "fresh") {
  return [
    `cache=${cacheKind}`,
    "provider=open-meteo",
    `lat=${roundBucket(request.latitude)}`,
    `lon=${roundBucket(request.longitude)}`,
    `target=${normalizeTargetHour(request.target)}`,
  ].join("&");
}

export function buildRateLimitKey(request) {
  const clientId = request.headers.get("x-weather-client-id");
  if (clientId && /^[A-Za-z0-9_-]{8,128}$/.test(clientId)) {
    return `client:${clientId}`;
  }
  return `route:${new URL(request.url).pathname}`;
}

export function isLikelyKoreaCoordinate(request) {
  return request.latitude >= 33 && request.latitude <= 39.5 &&
    request.longitude >= 124 && request.longitude <= 132;
}

export function toKmaGrid(latitude, longitude) {
  const re = 6371.00877;
  const grid = 5.0;
  const slat1 = toRadians(30.0);
  const slat2 = toRadians(60.0);
  const olon = toRadians(126.0);
  const olat = toRadians(38.0);
  const xo = 43.0;
  const yo = 136.0;

  const sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) /
    Math.log(Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5));
  const sf = (Math.tan(Math.PI * 0.25 + slat1 * 0.5) ** sn * Math.cos(slat1)) / sn;
  const ro = re / grid * sf / Math.tan(Math.PI * 0.25 + olat * 0.5) ** sn;
  const ra = re / grid * sf / Math.tan(Math.PI * 0.25 + toRadians(latitude) * 0.5) ** sn;
  let theta = toRadians(longitude) - olon;
  if (theta > Math.PI) theta -= 2.0 * Math.PI;
  if (theta < -Math.PI) theta += 2.0 * Math.PI;
  theta *= sn;

  return {
    nx: Math.floor(ra * Math.sin(theta) + xo + 0.5),
    ny: Math.floor(ro - ra * Math.cos(theta) + yo + 0.5),
  };
}

export function calculateKmaBaseTime(now = new Date()) {
  const korea = toKoreaParts(new Date(now.getTime() - 20 * 60 * 1000));
  const baseTimes = ["0200", "0500", "0800", "1100", "1400", "1700", "2000", "2300"];
  const current = `${String(korea.hour).padStart(2, "0")}${String(korea.minute).padStart(2, "0")}`;
  const time = [...baseTimes].reverse().find((baseTime) => baseTime <= current) || "2300";
  const date = time === "2300" && current < "0200"
    ? formatDate(addKoreaDays(korea, -1))
    : formatDate(korea);
  return { date, time };
}

export function forecastJson(forecast, providerAttempts) {
  return json({
    ...forecast,
    providerAttempts,
  });
}

export function errorJson(error, message, status = 500, providerAttempts = []) {
  return json(
    {
      error,
      message,
      providerAttempts,
    },
    status,
  );
}

function json(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
    },
  });
}

function normalizeTargetHour(target) {
  return target.slice(0, 13) + ":00:00";
}

function conditionFromKmaPty(value) {
  if (value === "1" || value === "5") return "RAIN";
  if (value === "2" || value === "6") return "SLEET";
  if (value === "3" || value === "7") return "SNOW";
  return "CLEAR";
}

function parseKmaPrecipitationAmountSafe(value) {
  const trimmed = value.trim();
  if (trimmed === "강수없음" || trimmed === "없음" || trimmed === "-") return 0;
  if (trimmed.startsWith("1mm") && trimmed.includes("미만")) return 0.1;
  const match = trimmed.match(/\d+(\.\d+)?/);
  return match ? Number(match[0]) : 0;
}

function toRadians(value) {
  return value / 180.0 * Math.PI;
}

function toKoreaParts(date) {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Seoul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).formatToParts(date);
  const value = (type) => Number(parts.find((part) => part.type === type)?.value || 0);
  return {
    year: value("year"),
    month: value("month"),
    day: value("day"),
    hour: value("hour"),
    minute: value("minute"),
  };
}

function addKoreaDays(parts, days) {
  const utc = Date.UTC(parts.year, parts.month - 1, parts.day + days, 12, 0, 0);
  return toKoreaParts(new Date(utc));
}

function formatDate(parts) {
  return `${parts.year}${String(parts.month).padStart(2, "0")}${String(parts.day).padStart(2, "0")}`;
}

function formatCoordinate(value) {
  return value.toFixed(6);
}

function roundBucket(value) {
  return (Math.round(value * 100) / 100).toFixed(2);
}
