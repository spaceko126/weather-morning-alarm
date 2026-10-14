import assert from "node:assert/strict";
import test from "node:test";

import {
  buildKasiUrl,
  parseHolidayRequest,
  parseKasiHolidays,
} from "../src/holidays.js";

test("rejects a year outside the supported range", () => {
  const parsed = parseHolidayRequest(new URL("https://x/holidays?year=1800"));
  assert.equal(parsed.ok, false);
});

test("rejects countries other than KR", () => {
  const parsed = parseHolidayRequest(new URL("https://x/holidays?year=2026&country=JP"));
  assert.equal(parsed.ok, false);
});

test("accepts a valid request and defaults to KR", () => {
  const parsed = parseHolidayRequest(new URL("https://x/holidays?year=2026"));
  assert.equal(parsed.ok, true);
  assert.deepEqual(parsed.value, { year: 2026, country: "KR" });
});

test("appends the service key without re-encoding it", () => {
  const url = buildKasiUrl("abc%2Fdef%3D%3D", 2026);
  assert.ok(url.endsWith("&serviceKey=abc%2Fdef%3D%3D"));
  assert.ok(url.includes("solYear=2026"));
});

test("maps locdate numbers to ISO dates and drops non-holidays", () => {
  const body = JSON.stringify({
    response: {
      body: {
        items: {
          item: [
            { locdate: 20260217, dateName: "설날", isHoliday: "Y" },
            { locdate: 20260101, dateName: "1월1일", isHoliday: "Y" },
            { locdate: 20260305, dateName: "임시", isHoliday: "N" },
          ],
        },
      },
    },
  });

  assert.deepEqual(parseKasiHolidays(body), [
    { date: "2026-01-01", name: "1월1일" },
    { date: "2026-02-17", name: "설날" },
  ]);
});

test("handles a single-item response that is not an array", () => {
  const body = JSON.stringify({
    response: { body: { items: { item: { locdate: 20260101, dateName: "1월1일" } } } },
  });

  assert.deepEqual(parseKasiHolidays(body), [{ date: "2026-01-01", name: "1월1일" }]);
});

test("returns an empty list when the portal reports no items", () => {
  assert.deepEqual(parseKasiHolidays(JSON.stringify({ response: { body: {} } })), []);
});
