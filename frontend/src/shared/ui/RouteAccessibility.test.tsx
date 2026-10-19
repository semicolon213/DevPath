import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { useEffect, useState } from "react";
import { Link, MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { RouteAccessibility } from "./RouteAccessibility";

describe("RouteAccessibility hash navigation", () => {
  const scrollIntoView = vi.fn();

  beforeEach(() => {
    Object.defineProperty(Element.prototype, "scrollIntoView", { configurable: true, value: scrollIntoView });
    vi.stubGlobal("matchMedia", vi.fn(() => ({ matches: true })));
  });

  afterEach(() => {
    scrollIntoView.mockClear();
    vi.useRealTimers();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("scrolls and focuses an evidence target on the same page, including a repeated click", async () => {
    vi.mocked(window.matchMedia).mockReturnValue({ matches: false } as MediaQueryList);
    render(<TestRouter initialEntry="/analyses/analysis-id" element={<>
      <Link to="#evidence-evidence-id">분석 근거 1</Link>
      <article id="evidence-evidence-id" tabIndex={-1}>근거 내용</article>
    </>} />);

    const link = screen.getByRole("link", { name: "분석 근거 1" });
    fireEvent.click(link);
    await waitFor(() => expect(screen.getByText("근거 내용")).toHaveFocus());
    expect(scrollIntoView).toHaveBeenCalledWith({ behavior: "smooth" });

    scrollIntoView.mockClear();
    fireEvent.click(link);
    await waitFor(() => expect(scrollIntoView).toHaveBeenCalledOnce());
    expect(screen.getByText("근거 내용")).toHaveFocus();
  });

  it("waits for an asynchronously loaded evidence target after cross-page navigation", async () => {
    render(<MemoryRouter initialEntries={["/skills"]}>
      <RouteAccessibility />
      <Routes>
        <Route path="/skills" element={<main><Link to="/analyses/analysis-id#evidence-evidence-id">분석 근거 1</Link></main>} />
        <Route path="/analyses/:analysisId" element={<AsyncEvidence />} />
      </Routes>
    </MemoryRouter>);

    fireEvent.click(screen.getByRole("link", { name: "분석 근거 1" }));

    expect(await screen.findByText("비동기 근거 내용")).toHaveFocus();
    expect(scrollIntoView).toHaveBeenCalledWith({ behavior: "auto" });
  });

  it("tracks pathname changes through hash navigation before returning to the previous page", async () => {
    render(<MemoryRouter initialEntries={["/a"]}>
      <RouteAccessibility />
      <Routes>
        <Route path="/a" element={<main><h1>Page A</h1><Link to="/b#target">Go to B target</Link></main>} />
        <Route path="/b" element={<main><h1>Page B</h1><Link to="/a">Return to A</Link><section id="target">Target</section></main>} />
      </Routes>
    </MemoryRouter>);

    fireEvent.click(screen.getByRole("link", { name: "Go to B target" }));
    await waitFor(() => expect(screen.getByText("Target")).toHaveFocus());
    fireEvent.click(screen.getByRole("link", { name: "Return to A" }));
    await waitFor(() => expect(screen.getByRole("heading", { name: "Page A" })).toHaveFocus());
  });

  it("adds a temporary tab index to a non-focusable hash target", async () => {
    render(<HashNavigation target={<section id="target">Target section</section>} />);

    fireEvent.click(screen.getByRole("link", { name: "Open target" }));

    const target = await screen.findByText("Target section");
    await waitFor(() => expect(target).toHaveFocus());
    expect(target).toHaveAttribute("tabindex", "-1");
  });

  it("falls back to the route heading and stops observing when the hash target is missing", async () => {
    vi.useFakeTimers();
    const disconnect = vi.spyOn(MutationObserver.prototype, "disconnect");
    render(<HashNavigation />);

    fireEvent.click(screen.getByRole("link", { name: "Open target" }));
    await act(() => vi.advanceTimersByTimeAsync(1_000));

    expect(screen.getByRole("heading", { name: "Destination" })).toHaveFocus();
    expect(disconnect).toHaveBeenCalled();
  });

  it("focuses the target of the existing profile settings hash link", async () => {
    render(<MemoryRouter initialEntries={["/careers"]}>
      <RouteAccessibility />
      <Routes>
        <Route path="/careers" element={<main><Link to="/settings/profile#targets">목표 직무 설정</Link></main>} />
        <Route path="/settings/profile" element={<main><h1>프로필과 평가 목표</h1><section id="targets">평가 목표</section></main>} />
      </Routes>
    </MemoryRouter>);

    fireEvent.click(screen.getByRole("link", { name: "목표 직무 설정" }));

    await waitFor(() => expect(screen.getByText("평가 목표")).toHaveFocus());
  });

  it("does not break the page for a malformed hash", () => {
    render(<TestRouter initialEntry="/analyses/analysis-id#%" element={<h1>분석 상세</h1>} />);
    expect(screen.getByRole("heading", { name: "분석 상세" })).toBeInTheDocument();
  });
});

function TestRouter({ initialEntry, element }: { initialEntry: string; element: React.ReactNode }) {
  return <MemoryRouter initialEntries={[initialEntry]}><RouteAccessibility /><Routes>
    <Route path="/analyses/:analysisId" element={<main>{element}</main>} />
  </Routes></MemoryRouter>;
}

function AsyncEvidence() {
  const [loaded, setLoaded] = useState(false);
  useEffect(() => setLoaded(true), []);
  return <main>{loaded && <article id="evidence-evidence-id" tabIndex={-1}>비동기 근거 내용</article>}</main>;
}

function HashNavigation({ target }: { target?: React.ReactNode }) {
  return <MemoryRouter initialEntries={["/source"]}>
    <RouteAccessibility />
    <Routes>
      <Route path="/source" element={<main><Link to="/destination#target">Open target</Link></main>} />
      <Route path="/destination" element={<main><h1>Destination</h1>{target}</main>} />
    </Routes>
  </MemoryRouter>;
}
