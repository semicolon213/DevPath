import { HomeLink } from "../routes/AppRoutes";

export function NotFoundPage() {
  return (
    <main className="shell">
      <section className="shell-card">
        <p className="eyebrow">Route placeholder</p>
        <h1>Page not found</h1>
        <p>The requested local scaffold route does not exist.</p>
        <HomeLink />
      </section>
    </main>
  );
}

