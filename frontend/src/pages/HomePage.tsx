import { SessionPanel } from "../features/session/ui/SessionPanel";

export function HomePage() {
  return (
    <main className="shell">
      <section className="shell-card" aria-labelledby="devpath-title">
        <p className="eyebrow">Developer career intelligence</p>
        <h1 id="devpath-title">DevPath</h1>
        <p>
          Start with a secure GitHub session. Repository analysis and career
          intelligence remain outside this first vertical slice.
        </p>
        <SessionPanel />
      </section>
    </main>
  );
}
