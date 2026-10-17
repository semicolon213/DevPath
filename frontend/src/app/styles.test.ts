import { expect, it } from "vitest";
// @ts-expect-error Node types are not installed; Vitest still runs in Node.
import { readFileSync } from "node:fs";

it("keeps raw hex colours inside the root token block", () => {
  const css = readFileSync("src/app/styles.css", "utf8");
  const rootEnd = css.indexOf("}");

  expect(rootEnd).toBeGreaterThan(0);
  expect(css.slice(rootEnd + 1)).not.toMatch(/#[0-9a-f]{3,8}\b/i);
});
