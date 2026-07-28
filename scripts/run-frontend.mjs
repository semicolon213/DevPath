import { spawnSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import process from "node:process";

const args = process.argv.slice(2);
const npmCliCandidates = [
  process.env.npm_execpath,
  process.platform === "win32" ? "C:\\Program Files\\nodejs\\node_modules\\npm\\bin\\npm-cli.js" : undefined
].filter(Boolean);

const npmCli = npmCliCandidates.find((candidate) => fs.existsSync(candidate));
const command = npmCli ? process.execPath : "npm";
const commandArgs = npmCli ? [npmCli, ...args] : args;

const result = spawnSync(command, commandArgs, {
  cwd: path.resolve("frontend"),
  stdio: "inherit",
  shell: false
});

process.exit(result.status ?? 1);

