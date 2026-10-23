import { expect, it } from "vitest";

type NodeModules = [
  {
    readFileSync(path: string, encoding: "utf8"): string;
    readdirSync(path: string): string[];
  },
  { fileURLToPath(url: string): string }
];

// Vitest stubs `?raw` CSS imports to an empty string, so read the files next to this test directly.
// Non-literal specifiers keep type-checking independent of @types/node.
const nodeModuleSpecifiers = ["node:fs", "node:url"];

it("keeps raw colour literals inside the root token block", async () => {
  const [{ readFileSync, readdirSync }, { fileURLToPath }] = await Promise.all(
    nodeModuleSpecifiers.map(specifier => import(/* @vite-ignore */ specifier))
  ) as NodeModules;
  const appPath = fileURLToPath(import.meta.url.replace(/[^/]+$/, ""));
  const partNames = readdirSync(`${appPath}styles`).filter(name => name.endsWith(".css"));
  const files = ["styles.css", ...partNames.map(name => `styles/${name}`)].map(name => ({
    name,
    css: readFileSync(`${appPath}${name}`, "utf8").replace(/\/\*[\s\S]*?\*\//g, "")
  }));
  const tokens = files.find(file => file.name === "styles/tokens.css");
  expect(tokens).toBeDefined();
  if (!tokens) return;
  const rootStart = tokens.css.search(/(^|[\s,}]):root\s*\{/);
  expect(rootStart).toBeGreaterThanOrEqual(0);
  const rootEnd = closingBraceIndex(tokens.css, tokens.css.indexOf("{", rootStart));

  expect(rootEnd).toBeGreaterThan(rootStart);
  for (const file of files) {
    const css = file === tokens
      ? file.css.slice(0, rootStart) + file.css.slice(rootEnd + 1)
      : file.css;
    expect(css, file.name).not.toMatch(/#[0-9a-f]{3,8}\b/i);
    expect(css, file.name).not.toMatch(/\b(?:rgba?|hsla?)\s*\(/i);
    expect(css, file.name).not.toContain("color-mix(");
  }

  const allCss = files.map(file => file.css).join("\n");
  const defaultButtonRule = allCss.match(/^button\s*\{[^}]*\}/m)?.[0];
  const defaultButtonLinkRule = allCss.match(/^\.button-link\s*\{[^}]*\}/m)?.[0];
  expect(defaultButtonRule).toBeDefined();
  expect(defaultButtonLinkRule).toBeDefined();
  expect(defaultButtonRule).not.toContain("var(--action-primary)");
  expect(defaultButtonLinkRule).not.toContain("var(--action-primary)");
});

it("imports every stylesheet part exactly once", async () => {
  const [{ readFileSync, readdirSync }, { fileURLToPath }] = await Promise.all(
    nodeModuleSpecifiers.map(specifier => import(/* @vite-ignore */ specifier))
  ) as NodeModules;
  const appPath = fileURLToPath(import.meta.url.replace(/[^/]+$/, ""));
  const entry = readFileSync(`${appPath}styles.css`, "utf8");
  const partNames = readdirSync(`${appPath}styles`).filter(name => name.endsWith(".css")).sort();
  const imports = [...entry.matchAll(/@import\s+["']\.\/styles\/([^"']+\.css)["'];/g)].map(match => match[1]);

  expect([...imports].sort()).toEqual(partNames);
  for (const partName of partNames) {
    expect(imports.filter(name => name === partName), partName).toHaveLength(1);
  }
});

function closingBraceIndex(css: string, openIndex: number) {
  let depth = 0;
  for (let index = openIndex; index < css.length; index += 1) {
    if (css[index] === "{") depth += 1;
    if (css[index] === "}" && --depth === 0) return index;
  }
  return -1;
}
