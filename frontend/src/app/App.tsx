import { AppProviders } from "./AppProviders";
import { ErrorBoundary } from "./ErrorBoundary";
import { AppRoutes } from "../routes/AppRoutes";

export function App() {
  return (
    <ErrorBoundary>
      <AppProviders>
        <AppRoutes />
      </AppProviders>
    </ErrorBoundary>
  );
}

