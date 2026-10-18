import { QueryClient, QueryClientProvider, useQueryClient } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import type { ReactElement } from "react";
import { createMemoryRouter, RouterProvider } from "react-router-dom";
import { appRoutes } from "../routes/AppRoutes";

export function renderWithProviders(ui?: ReactElement, initialEntries = ["/"]) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false
      }
    }
  });

  const router = createMemoryRouter(ui ? [{ path: "*", element: ui }] : appRoutes, { initialEntries });
  return { ...render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  ), router };
}

export function QueryClientProbe() {
  const queryClient = useQueryClient();
  return <span data-testid="query-client-present">{String(Boolean(queryClient))}</span>;
}

