import { existsSync, readdirSync, readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";

// These tests require a caller-supplied PostgreSQL URL and intentionally skip when DEVPATH_DB_URL is absent.
const ALLOWED_SKIPS = new Set([
  "com.devpath.career.adapter.out.persistence.CareerCatalogPersistenceIntegrationTest",
  "com.devpath.company.adapter.out.persistence.CompanyCatalogPersistenceIntegrationTest",
]);

function filesUnder(directory, suffix) {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const entryPath = path.join(directory, entry.name);
    return entry.isDirectory() ? filesUnder(entryPath, suffix) : entry.name.endsWith(suffix) ? [entryPath] : [];
  });
}

function testcontainersClasses(sourceDirectory) {
  return filesUnder(sourceDirectory, ".java").flatMap((file) => {
    const source = readFileSync(file, "utf8");
    if (!source.includes("@Testcontainers")) return [];

    const packageName = source.match(/^package\s+([\w.]+);/m)?.[1];
    const className = source.match(/\bclass\s+(\w+)/)?.[1];
    if (!packageName || !className) throw new Error(`Cannot determine test class from ${file}.`);
    return [`${packageName}.${className}`];
  });
}

export function checkTestSkips({ repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..") } = {}) {
  const resultDirectory = path.join(repoRoot, "backend", "build", "test-results", "test");
  const sourceDirectory = path.join(repoRoot, "backend", "src", "test", "java");
  const resultFiles = existsSync(resultDirectory) ? filesUnder(resultDirectory, ".xml").filter((file) => path.basename(file).startsWith("TEST-")) : [];
  if (resultFiles.length === 0) throw new Error(`No backend test result files found in ${resultDirectory}.`);

  const results = new Map(resultFiles.map((file) => {
    const xml = readFileSync(file, "utf8");
    const suite = xml.match(/<testsuite\b[^>]*\bname="([^"]+)"[^>]*>/)?.[0];
    const name = suite?.match(/\bname="([^"]+)"/)?.[1];
    const skipped = Number(suite?.match(/\bskipped="(\d+)"/)?.[1] ?? 0);
    if (!name) throw new Error(`Cannot read testsuite name from ${file}.`);
    return [name, skipped];
  }));

  const required = testcontainersClasses(sourceDirectory);
  const missing = required.filter((name) => !results.has(name));
  const unexpectedSkips = [...results].filter(([name, skipped]) => skipped > 0 && !ALLOWED_SKIPS.has(name));
  const failures = [
    ...missing.map((name) => `${name}: no test result`),
    ...unexpectedSkips.map(([name, skipped]) => `${name}: ${skipped} skipped`),
  ];
  if (failures.length > 0) throw new Error(`Backend test skip guard failed:\n${failures.join("\n")}`);

  return { resultFiles: resultFiles.length, testcontainersClasses: required.length };
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const checked = checkTestSkips();
    console.log(`Backend test skip guard passed (${checked.testcontainersClasses} Testcontainers classes, ${checked.resultFiles} result files).`);
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
