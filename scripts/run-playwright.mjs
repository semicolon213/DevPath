import { spawnSync } from "node:child_process";
import path from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";
import { selectE2ePort } from "./select-e2e-port.mjs";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");

try {
  const port = await selectE2ePort();
  console.log(`[e2e] Using http://127.0.0.1:${port}`);

  const result = spawnSync(process.execPath, [
    path.join(root, "frontend", "node_modules", "@playwright", "test", "cli.js"),
    "test",
    ...process.argv.slice(2)
  ], {
    cwd: path.join(root, "frontend"),
    env: { ...process.env, DEVPATH_E2E_PORT: String(port) },
    stdio: "inherit",
    shell: false
  });

  process.exit(result.status ?? 1);
} catch (error) {
  console.error(`[e2e] ${error.message}`);
  process.exit(1);
}
