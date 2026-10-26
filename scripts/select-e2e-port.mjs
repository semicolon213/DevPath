import net from "node:net";

const HOST = "127.0.0.1";

function bind(port) {
  return new Promise((resolve, reject) => {
    const server = net.createServer();
    server.once("error", reject);
    server.listen({ host: HOST, port, exclusive: true }, () => {
      const address = server.address();
      server.close((error) => error ? reject(error) : resolve(address.port));
    });
  });
}

export async function selectE2ePort({ env = process.env, defaultPort = 4173 } = {}) {
  const configuredPort = env.DEVPATH_E2E_PORT;
  if (configuredPort !== undefined) {
    if (!/^\d+$/.test(configuredPort) || Number(configuredPort) < 1 || Number(configuredPort) > 65535) {
      throw new Error(`DEVPATH_E2E_PORT must be an integer between 1 and 65535; received "${configuredPort}".`);
    }

    try {
      await bind(Number(configuredPort));
      return Number(configuredPort);
    } catch (error) {
      throw new Error(`DEVPATH_E2E_PORT ${configuredPort} cannot be bound on ${HOST}: ${error.code ?? error.message}.`);
    }
  }

  try {
    await bind(defaultPort);
    return defaultPort;
  } catch {
    return bind(0);
  }
}
