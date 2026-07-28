import { Link, Route, Routes } from "react-router-dom";

import { HomePage } from "../pages/HomePage";
import { NotFoundPage } from "../pages/NotFoundPage";

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}

export function HomeLink() {
  return <Link to="/">Return to DevPath home</Link>;
}

