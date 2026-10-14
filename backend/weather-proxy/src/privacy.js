export function privacyPolicyHtml(contactEmail = "") {
  const contact = contactEmail && contactEmail.trim().length > 0
    ? contactEmail.trim()
    : "Contact email must be added before public release.";

  return `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Weather Morning Alarm Privacy Policy</title>
  <style>
    body { font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; line-height: 1.6; margin: 0; color: #17151f; background: #ffffff; }
    main { max-width: 820px; margin: 0 auto; padding: 32px 20px 48px; }
    h1, h2 { line-height: 1.25; }
    h1 { font-size: 2rem; margin-bottom: 0.25rem; }
    h2 { font-size: 1.25rem; margin-top: 2rem; }
    ul { padding-left: 1.4rem; }
    .updated { color: #5f5a68; margin-top: 0; }
  </style>
</head>
<body>
  <main>
    <h1>Weather Morning Alarm Privacy Policy</h1>
    <p class="updated">Last updated: 2026-06-18</p>
    <p>Weather Morning Alarm helps users wake earlier when rain or snow is forecast.</p>

    <h2>Information Stored On The Device</h2>
    <ul>
      <li>Alarm time and repeat days.</li>
      <li>Weather trigger settings.</li>
      <li>Selected region name and coordinates.</li>
      <li>Alarm decision history.</li>
      <li>Developer KMA service key, only when manually configured for local testing.</li>
      <li>Anonymous backend proxy client id, only for backend proxy rate limiting.</li>
    </ul>
    <p>App data is excluded from Android backup.</p>

    <h2>Information Sent For Weather Checks</h2>
    <p>To fetch weather forecasts, the app may send latitude and longitude, target forecast time, locale, and an anonymous install id when backend proxy mode is used.</p>
    <p>In backend proxy mode, this information is sent to the Weather Morning Alarm backend. The backend may request forecast data from weather providers such as Open-Meteo or the Korea Meteorological Administration.</p>
    <p>In direct provider mode, coordinates may be sent directly from the app to the selected weather provider.</p>

    <h2>How Information Is Used</h2>
    <ul>
      <li>Weather-based alarm decisions.</li>
      <li>Forecast retrieval.</li>
      <li>Backend rate limiting and abuse prevention.</li>
      <li>Local alarm history display.</li>
    </ul>

    <h2>Advertising And Sale Of Data</h2>
    <p>The app does not use advertising identifiers in the current design.</p>
    <p>The app does not sell personal data.</p>

    <h2>Backend Logs</h2>
    <p>The backend is designed to log operational events such as cache hits, provider success/failure, and rate-limit events. It is designed not to log raw coordinates, provider API keys, or anonymous client ids.</p>

    <h2>Data Deletion</h2>
    <p>Users can delete locally stored app data by deleting the app or clearing app storage in Android settings.</p>

    <h2>Contact</h2>
    <p>${escapeHtml(contact)}</p>

    <h2>한국어 요약</h2>
    <p>날씨 아침 알람은 비나 눈 예보가 있을 때 사용자가 더 일찍 일어날 수 있도록 돕는 앱입니다.</p>
    <p>앱 설정, 알람 기록, 선택한 지역 정보는 기기에 저장되며 Android 백업에서 제외됩니다. 날씨 확인을 위해 위도와 경도, 예보 대상 시간, 언어 설정이 날씨 제공자 또는 백엔드 프록시로 전송될 수 있습니다. 백엔드 프록시 모드에서는 사용량 제한을 위해 익명 설치 ID가 전송될 수 있습니다.</p>
    <p>현재 설계에서 앱은 광고 식별자를 사용하지 않으며 개인정보를 판매하지 않습니다. 사용자는 Android 설정에서 앱 데이터를 삭제하거나 앱을 삭제해 로컬 데이터를 지울 수 있습니다.</p>
  </main>
</body>
</html>`;
}

export function privacyHtmlResponse(contactEmail) {
  return new Response(privacyPolicyHtml(contactEmail), {
    status: 200,
    headers: {
      "content-type": "text/html; charset=utf-8",
      "cache-control": "public, max-age=3600",
    },
  });
}

function escapeHtml(value) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}
