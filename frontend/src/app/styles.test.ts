import { expect, it } from "vitest";

type NodeModules = [
  { readFileSync(path: string, encoding: "utf8"): string },
  { fileURLToPath(url: string): string }
];

// Vitest stubs `?raw` CSS imports to an empty string, so read the file next to this test directly.
// Non-literal specifiers keep type-checking independent of @types/node.
const nodeModuleSpecifiers = ["node:fs", "node:url"];

it("keeps raw colour literals inside the root token block", async () => {
  const [{ readFileSync }, { fileURLToPath }] = await Promise.all(
    nodeModuleSpecifiers.map(specifier => import(/* @vite-ignore */ specifier))
  ) as NodeModules;
  // Vite rewrites `new URL("./styles.css", import.meta.url)` to an asset URL, so derive the sibling path textually.
  const stylesPath = fileURLToPath(import.meta.url.replace(/[^/]+$/, "styles.css"));
  const css = readFileSync(stylesPath, "utf8").replace(/\/\*[\s\S]*?\*\//g, "");
  const rootStart = css.search(/(^|[\s,}]):root\s*\{/);
  expect(rootStart).toBeGreaterThanOrEqual(0);
  const rootEnd = closingBraceIndex(css, css.indexOf("{", rootStart));

  expect(rootEnd).toBeGreaterThan(rootStart);
  const outsideRoot = css.slice(0, rootStart) + css.slice(rootEnd + 1);
  expect(outsideRoot).not.toMatch(/#[0-9a-f]{3,8}\b/i);
  expect(outsideRoot).not.toMatch(/\b(?:rgba?|hsla?)\s*\(/i);
  const defaultButtonRule = css.match(/^button\s*\{[^}]*\}/m)?.[0];
  const defaultButtonLinkRule = css.match(/^\.button-link\s*\{[^}]*\}/m)?.[0];
  expect(defaultButtonRule).toBeDefined();
  expect(defaultButtonLinkRule).toBeDefined();
  expect(defaultButtonRule).not.toContain("var(--action-primary)");
  expect(defaultButtonLinkRule).not.toContain("var(--action-primary)");
});

function closingBraceIndex(css: string, openIndex: number) {
  let depth = 0;
  for (let index = openIndex; index < css.length; index += 1) {
    if (css[index] === "{") depth += 1;
    if (css[index] === "}" && --depth === 0) return index;
  }
  return -1;
}
