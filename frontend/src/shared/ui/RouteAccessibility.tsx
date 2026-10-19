import { useEffect, useRef } from "react";
import { useLocation } from "react-router-dom";

const MAIN_CONTENT_ID = "main-content";

export function RouteAccessibility() {
  const { pathname, hash, key } = useLocation();
  const previousPathname = useRef(pathname);

  useEffect(() => {
    const pathnameChanged = previousPathname.current !== pathname;
    previousPathname.current = pathname;
    const main = document.querySelector<HTMLElement>("main");
    if (main === null) return;

    main.id = MAIN_CONTENT_ID;
    main.tabIndex = -1;

    const focusHeading = () => {
      const heading = main.querySelector<HTMLElement>("h1");
      if (heading === null) return false;
      heading.tabIndex = -1;
      heading.focus();
      return true;
    };
    let observer: MutationObserver | undefined;
    let fallback = 0;
    const focusRoute = () => {
      if (focusHeading()) return;
      observer = new MutationObserver(() => {
        if (focusHeading()) {
          observer?.disconnect();
          window.clearTimeout(fallback);
        }
      });
      observer.observe(main, { childList: true, subtree: true });
      fallback = window.setTimeout(() => {
        observer?.disconnect();
        if (!focusHeading()) main.focus();
      }, 1_000);
    };

    if (hash) {
      const targetId = decodeHash(hash);
      const focusTarget = () => {
        if (targetId === null) return false;
        const target = document.getElementById(targetId);
        if (target === null) return false;
        const addedTabIndex = !target.hasAttribute("tabindex");
        if (addedTabIndex) target.tabIndex = -1;
        target.focus({ preventScroll: true });
        if (document.activeElement !== target) {
          if (addedTabIndex) target.removeAttribute("tabindex");
          return false;
        }
        if (addedTabIndex) target.addEventListener("blur", () => target.removeAttribute("tabindex"), { once: true });
        target.scrollIntoView({ behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth" });
        return true;
      };

      if (focusTarget()) return;

      observer = new MutationObserver(() => {
        if (focusTarget()) {
          observer?.disconnect();
          window.clearTimeout(fallback);
        }
      });
      observer.observe(main, { childList: true, subtree: true });
      fallback = window.setTimeout(() => {
        observer?.disconnect();
        if (pathnameChanged) focusRoute();
      }, 1_000);
    } else if (pathnameChanged) {
      focusRoute();
    } else return;

    return () => {
      observer?.disconnect();
      window.clearTimeout(fallback);
    };
  }, [pathname, hash, key]);

  return (
    <a className="skip-link" href={`#${MAIN_CONTENT_ID}`}>
      본문으로 건너뛰기
    </a>
  );
}

function decodeHash(hash: string) {
  try {
    return decodeURIComponent(hash.slice(1));
  } catch {
    return null;
  }
}
