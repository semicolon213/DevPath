import assert from "node:assert/strict";
import net from "node:net";
import test from "node:test";
import { selectE2ePort } from "./select-e2e-port.mjs";

function listen(port = 0) {
  return new Promise((resolve, reject) => {
    const server = net.createServer();
    server.once("error", reject);
    server.listen({ host: "127.0.0.1", port, exclusive: true }, () => resolve(server));
  });
}

function close(server) {
  return new Promise((resolve, reject) => server.close((error) => error ? reject(error) : resolve()));
}

test("uses the default port when it can be bound", async () => {
  const available = await listen();
  const port = available.address().port;
  await close(available);

  assert.equal(await selectE2ePort({ env: {}, defaultPort: port }), port);
});

test("falls back to another free port when the default is busy", async () => {
  const busy = await listen();
  const port = busy.address().port;

  try {
    const selected = await selectE2ePort({ env: {}, defaultPort: port });
    assert.notEqual(selected, port);
    assert.ok(selected > 0);
  } finally {
    await close(busy);
  }
});

test("respects an explicit DEVPATH_E2E_PORT", async () => {
  const available = await listen();
  const port = available.address().port;
  await close(available);

  assert.equal(await selectE2ePort({ env: { DEVPATH_E2E_PORT: String(port) } }), port);
});

test("fails clearly instead of replacing an unavailable explicit port", async () => {
  const busy = await listen();
  const port = busy.address().port;

  try {
    await assert.rejects(
      selectE2ePort({ env: { DEVPATH_E2E_PORT: String(port) } }),
      new RegExp(`DEVPATH_E2E_PORT ${port} cannot be bound on 127\\.0\\.0\\.1`)
    );
  } finally {
    await close(busy);
  }
});
