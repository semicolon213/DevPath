import { execFileSync } from "node:child_process";
import { readFileSync } from "node:fs";
import { dirname, extname, relative, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const scriptDirectory = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(scriptDirectory, "..");
const textExtensions = new Set([
  "", ".css", ".gradle", ".html", ".java", ".js", ".json", ".jsx", ".md", ".mjs", ".properties",
  ".sql", ".ts", ".tsx", ".txt", ".yaml", ".yml"
]);

const secretPatterns = [
  ["private key", /-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----/],
  ["GitHub token", /\bgh[pousr]_[A-Za-z0-9]{30,}\b/],
  ["GitHub fine-grained token", /\bgithub_pat_[A-Za-z0-9_]{40,}\b/],
  ["OpenAI API key", /\bsk-(?:proj-)?[A-Za-z0-9_-]{20,}\b/],
  ["AWS access key", /\bAKIA[0-9A-Z]{16}\b/]
];

export function inspectFile(relativePath, content) {
  const normalizedPath = relativePath.replaceAll("\\", "/");
  const findings = [];
  const sensitivePath = /(^|\/)\.env(?:\.|$)/.test(normalizedPath) && !normalizedPath.endsWith(".env.example")
    || /\.(?:jks|key|keystore|p12|pem|pfx)$/.test(normalizedPath);
  if (sensitivePath) findings.push({ line: 1, rule: "tracked-sensitive-file" });

  const lines = content.split(/\r?\n/);
  lines.forEach((line, index) => {
    for (const [name, pattern] of secretPatterns) {
      if (pattern.test(line)) findings.push({ line: index + 1, rule: `embedded-${name.replaceAll(" ", "-")}` });
    }
    if (/ddl-auto\s*:\s*(?:update|create|create-drop)\b/i.test(line)) {
      findings.push({ line: index + 1, rule: "unsafe-jpa-schema-mode" });
    }
    if (normalizedPath.startsWith("frontend/src/")
      && /(?:localStorage|sessionStorage).*(?:token|session|credential)|(?:token|session|credential).*(?:localStorage|sessionStorage)/i.test(line)) {
      findings.push({ line: index + 1, rule: "browser-credential-storage" });
    }
    if (normalizedPath.startsWith("frontend/src/")
      && /https:\/\/(?:api\.github\.com|api\.notion\.com|api\.openai\.com)\b/i.test(line)) {
      findings.push({ line: index + 1, rule: "browser-provider-call" });
    }
  });
  return findings;
}

export function inspectRepository(root = repositoryRoot) {
  const output = execFileSync("git", ["ls-files", "--cached", "--others", "--exclude-standard", "-z"], {
    cwd: root,
    encoding: "utf8"
  });
  const findings = [];
  for (const file of output.split("\0").filter(Boolean)) {
    if (!textExtensions.has(extname(file).toLowerCase())) continue;
    const absolutePath = resolve(root, file);
    let content;
    try {
      content = readFileSync(absolutePath, "utf8");
    } catch {
      continue;
    }
    if (content.includes("\0")) continue;
    for (const finding of inspectFile(relative(root, absolutePath), content)) {
      findings.push({ file: file.replaceAll("\\", "/"), ...finding });
    }
  }
  return findings;
}

function main() {
  const findings = inspectRepository();
  if (findings.length > 0) {
    console.error("Security boundary verification failed:");
    for (const finding of findings) console.error(`- ${finding.file}:${finding.line} ${finding.rule}`);
    process.exitCode = 1;
    return;
  }
  console.log("Security boundary verification passed.");
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) main();
