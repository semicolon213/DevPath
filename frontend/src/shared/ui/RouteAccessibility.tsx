import { useEffect, useRef } from "react";
import { useLocation } from "react-router-dom";

const MAIN_CONTENT_ID = "main-content";

export function RouteAccessibility() {
  const { pathname } = useLocation();
  const previousPathname = useRef(pathname);

  useEffect(() => {
    const main = document.querySelector<HTMLElement>("main");
    if (main === null) return;

    main.id = MAIN_CONTENT_ID;
    main.tabIndex = -1;

    if (previousPathname.current === pathname) return;
    previousPathname.current = pathname;

    const focusHeading = () => {
      const heading = main.querySelector<HTMLElement>("h1");
      if (heading === null) return false;
      heading.tabIndex = -1;
      heading.focus();
      return true;
    };

    if (focusHeading()) return;

    let fallback = 0;
    const observer = new MutationObserver(() => {
      if (focusHeading()) {
        observer.disconnect();
        window.clearTimeout(fallback);
      }
    });
    observer.observe(main, { childList: true, subtree: true });

    fallback = window.setTimeout(() => {
      observer.disconnect();
      if (!focusHeading()) main.focus();
    }, 1_000);

    return () => {
      observer.disconnect();
      window.clearTimeout(fallback);
    };
  }, [pathname]);

  return (
    <a className="skip-link" href={`#${MAIN_CONTENT_ID}`}>
      본문으로 건너뛰기
    </a>
  );
}
