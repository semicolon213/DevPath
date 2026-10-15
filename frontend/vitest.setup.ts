import "@testing-library/jest-dom/vitest";
import { vi } from "vitest";

// Pin the API base URL so developer environment overrides cannot affect tests.
vi.stubEnv("VITE_DEVPATH_API_BASE_URL", "http://localhost:8080");
