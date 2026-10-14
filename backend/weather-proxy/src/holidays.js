/**
 * Korean public holidays for one year.
 *
 * The app's own calendar only knows fixed-date holidays, so Lunar New Year,
 * Chuseok and substitute holidays were missing and "weekdays except holidays"
 * misled the user. data.go.kr publishes the authoritative list; this route
 * proxies it so the service key never ships inside the APK.
 */

const KASI_BASE_URL =
  "https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo";

const CACHE_TTL_SECONDS = 60 * 60 * 24 * 7;

export function parseHolidayRequest(url) {
  const year = Number.parseInt(url.searchParams.get("year") ?? "", 10);
  if (!Number.isInteger(year) || year < 2000 || year > 2100) {
    return { ok: false, message: "year must be between 2000 and 2100." };
  }
  const country = (url.searchParams.get("country") ?? "KR").toUpperCase();
  if (country !== "KR") {
    return { ok: false, message: "Only country=KR is supported." };
  }
  return { ok: true, value: { year, country } };
}

export function buildKasiUrl(serviceKey, year) {
  const url = new URL(KASI_BASE_URL);
  // The portal issues keys already percent-encoded; append raw so it is not
  // encoded a second time.
  url.searchParams.set("solYear", String(year));
  url.searchParams.set("numOfRows", "100");
  url.searchParams.set("_type", "json");
  return `${url.toString()}&serviceKey=${serviceKey}`;
}

/** KASI returns `locdate` as the number 20260101. */
export function parseKasiHolidays(body) {
  const parsed = JSON.parse(body);
  const items = parsed?.response?.body?.items?.item;
  if (!items) return [];
  const list = Array.isArray(items) ? items : [items];
  return list
    .filter((item) => String(item.isHoliday ?? "Y").trim() === "Y")
    .map((item) => {
      const raw = String(item.locdate ?? "").trim();
      if (!/^\d{8}$/.test(raw)) return null;
      return {
        date: `${raw.slice(0, 4)}-${raw.slice(4, 6)}-${raw.slice(6, 8)}`,
        name: String(item.dateName ?? "").trim(),
      };
    })
    .filter(Boolean)
    .sort((a, b) => a.date.localeCompare(b.date));
}

export function holidaysJson(year, holidays, source) {
  return new Response(
    JSON.stringify({ year, source, holidays }),
    {
      status: 200,
      headers: {
        "content-type": "application/json; charset=utf-8",
        "cache-control": `public, max-age=${CACHE_TTL_SECONDS}`,
      },
    },
  );
}

export async function fetchHolidays(env, year) {
  if (!env.KMA_SERVICE_KEY) {
    return { ok: false, reason: "NO_SERVICE_KEY" };
  }
  const response = await fetch(buildKasiUrl(env.KMA_SERVICE_KEY, year), {
    cf: { cacheTtl: CACHE_TTL_SECONDS },
  });
  if (!response.ok) {
    return { ok: false, reason: `HTTP_${response.status}` };
  }
  const body = await response.text();
  try {
    return { ok: true, holidays: parseKasiHolidays(body) };
  } catch (error) {
    return { ok: false, reason: "PARSE_FAILED" };
  }
}

export { CACHE_TTL_SECONDS };
