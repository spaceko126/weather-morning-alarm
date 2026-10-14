"""Gradle JUnit XML 결과를 합산해 GitHub Actions 작업 요약에 표로 쓴다 (권한·외부 액션 불필요)."""
import glob
import os
import xml.etree.ElementTree as ET

tests = failures = errors = skipped = 0
files = glob.glob("app/build/test-results/testDebugUnitTest/*.xml")
for path in files:
    root = ET.parse(path).getroot()
    tests += int(root.get("tests", 0))
    failures += int(root.get("failures", 0))
    errors += int(root.get("errors", 0))
    skipped += int(root.get("skipped", 0))

summary = os.environ.get("GITHUB_STEP_SUMMARY")
text = (
    "### Android unit tests\n\n"
    "| suites | tests | failures | errors | skipped |\n|---|---|---|---|---|\n"
    f"| {len(files)} | {tests} | {failures} | {errors} | {skipped} |\n"
)
print(text)
if summary:
    with open(summary, "a", encoding="utf-8") as out:
        out.write(text)
if not files:
    print("::warning::No JUnit XML found; see the Unit tests step for the failure cause.")
