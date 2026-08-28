import { spawnSync } from "node:child_process";

const result = spawnSync("docker", ["version", "--format", "{{.Server.Version}}"], {
  encoding: "utf8",
  shell: process.platform === "win32"
});
const serverVersion = result.stdout?.trim();

if ((result.status ?? 1) !== 0 || !serverVersion) {
  console.error("MVP verification requires a running Docker-compatible engine for PostgreSQL Testcontainers.");
  process.exit(1);
}

console.log(`Docker engine ${serverVersion} is ready for MVP verification.`);
