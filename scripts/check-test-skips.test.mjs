import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from "node:fs";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import { checkTestSkips } from "./check-test-skips.mjs";

function fixture({ testcontainersSkipped = 0, includeResult = true } = {}) {
  const repoRoot = mkdtempSync(path.join(os.tmpdir(), "devpath-test-skips-"));
  const sourceDirectory = path.join(repoRoot, "backend", "src", "test", "java", "example");
  const resultDirectory = path.join(repoRoot, "backend", "build", "test-results", "test");
  mkdirSync(sourceDirectory, { recursive: true });
  mkdirSync(resultDirectory, { recursive: true });
  writeFileSync(path.join(sourceDirectory, "ContainerTest.java"), "package example;\n@Testcontainers\nclass ContainerTest {}\n");
  if (includeResult) {
    writeFileSync(path.join(resultDirectory, "TEST-example.ContainerTest.xml"), `<testsuite name="example.ContainerTest" tests="1" skipped="${testcontainersSkipped}"/>`);
  }
  return repoRoot;
}

function withFixture(options, assertion) {
  const repoRoot = fixture(options);
  try {
    assertion(repoRoot);
  } finally {
    rmSync(repoRoot, { recursive: true, force: true });
  }
}

test("passes when Testcontainers tests run and only DEVPATH_DB_URL tests skip", () => withFixture({}, (repoRoot) => {
  const resultDirectory = path.join(repoRoot, "backend", "build", "test-results", "test");
  writeFileSync(
    path.join(resultDirectory, "TEST-com.devpath.career.adapter.out.persistence.CareerCatalogPersistenceIntegrationTest.xml"),
    '<testsuite name="com.devpath.career.adapter.out.persistence.CareerCatalogPersistenceIntegrationTest" tests="1" skipped="1"/>'
  );
  assert.deepEqual(checkTestSkips({ repoRoot }), { resultFiles: 2, testcontainersClasses: 1 });
}));

test("fails when a Testcontainers class skips", () => withFixture({ testcontainersSkipped: 1 }, (repoRoot) => {
  assert.throws(() => checkTestSkips({ repoRoot }), /example\.ContainerTest: 1 skipped/);
}));

test("fails when a Testcontainers result is missing", () => withFixture({ includeResult: false }, (repoRoot) => {
  writeFileSync(
    path.join(repoRoot, "backend", "build", "test-results", "test", "TEST-example.OtherTest.xml"),
    '<testsuite name="example.OtherTest" tests="1" skipped="0"/>'
  );
  assert.throws(() => checkTestSkips({ repoRoot }), /example\.ContainerTest: no test result/);
}));

test("fails when no test result files exist", () => withFixture({ includeResult: false }, (repoRoot) => {
  assert.throws(() => checkTestSkips({ repoRoot }), /No backend test result files found/);
}));
