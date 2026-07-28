import { spawnSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import process from "node:process";

const args = process.argv.slice(2);
const wrapper = process.platform === "win32" ? "gradlew.bat" : "./gradlew";
const backendDir = path.resolve("backend");
const localGradle = path.join(
  os.homedir(),
  ".gradle",
  "wrapper",
  "dists",
  "gradle-8.11.1-bin",
  "bpt9gzteqjrbo1mjrsomdt32c",
  "gradle-8.11.1",
  "bin",
  process.platform === "win32" ? "gradle.bat" : "gradle"
);

let result = spawnSync(wrapper, args, {
  cwd: backendDir,
  stdio: "inherit",
  shell: process.platform === "win32"
});

if ((result.status ?? 1) !== 0 && fs.existsSync(localGradle)) {
  console.warn("Gradle wrapper failed; retrying with cached local Gradle distribution.");
  result = spawnSync(localGradle, args, {
    cwd: backendDir,
    stdio: "inherit",
    shell: process.platform === "win32"
  });
}

process.exit(result.status ?? 1);
