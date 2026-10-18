import { AppProviders } from "./AppProviders";
import { ErrorBoundary } from "./ErrorBoundary";
import { RouterProvider } from "react-router-dom";
import { appRouter } from "../routes/AppRoutes";

export function App() {
  return (
    <ErrorBoundary>
      <AppProviders>
        <RouterProvider router={appRouter} />
      </AppProviders>
    </ErrorBoundary>
  );
}

