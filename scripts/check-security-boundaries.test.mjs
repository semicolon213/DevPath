import test from "node:test";
import assert from "node:assert/strict";
import { inspectFile } from "./check-security-boundaries.mjs";

test("detects high-confidence embedded credentials and private keys", () => {
  const githubToken = ["ghp", "abcdefghijklmnopqrstuvwxyz1234567890"].join("_");
  const privateKeyHeader = ["-----BEGIN", " PRIVATE KEY-----"].join("");
  assert.deepEqual(inspectFile("config.txt", `token=${githubToken}`), [
    { line: 1, rule: "embedded-GitHub-token" }
  ]);
  assert.equal(inspectFile("fixture.txt", privateKeyHeader).length, 1);
});

test("rejects unsafe schema ownership and browser credential handling", () => {
  const unsafeSchemaMode = ["ddl-auto:", " create-drop"].join("");
  assert.deepEqual(inspectFile("backend/src/main/resources/application.yml", unsafeSchemaMode), [
    { line: 1, rule: "unsafe-jpa-schema-mode" }
  ]);
  assert.deepEqual(inspectFile("frontend/src/session.ts", "localStorage.setItem('token', value)"), [
    { line: 1, rule: "browser-credential-storage" }
  ]);
  assert.deepEqual(inspectFile("frontend/src/github.ts", "fetch('https://api.github.com/user')"), [
    { line: 1, rule: "browser-provider-call" }
  ]);
});

test("allows environment references, examples, and backend provider adapters", () => {
  assert.deepEqual(inspectFile("backend/.env.example", "GITHUB_CLIENT_SECRET=replace-me"), []);
  assert.deepEqual(inspectFile("backend/src/main/resources/application.yml", "password: ${DEVPATH_DB_PASSWORD}"), []);
  assert.deepEqual(inspectFile("backend/src/main/java/GitHubAdapter.java", "https://api.github.com/user"), []);
});
