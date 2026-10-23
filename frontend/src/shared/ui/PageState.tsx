import { useLayoutEffect, useRef } from "react";
import { Link } from "react-router-dom";
import { ApiError } from "../api/apiClient";
import { PageHero } from "./PageHero";

type BaseProps = {
  title: string;
  description?: string;
  backTo?: string;
  backLabel?: string;
};

type PageStateProps = BaseProps & (
  | { state: "loading"; message: string }
  | { state: "empty"; actionTo: string; actionLabel: string }
  | { state: "error"; error: Error | null; errorTitle?: string; retry?: () => void }
);

export function PageState(props: PageStateProps) {
  const loadingFocused = useRef(false);
  useLayoutEffect(() => () => {
    if (props.state !== "loading" || !loadingFocused.current) return;
    queueMicrotask(() => {
      const heading = document.querySelector<HTMLElement>("main h1");
      if (heading) { heading.tabIndex = -1; heading.focus(); }
    });
  }, [props.state]);

  if (props.state === "loading") {
    return <>
      <PageHero title={props.title} description={props.description} onFocusCapture={() => { loadingFocused.current = true; }} onBlurCapture={() => { loadingFocused.current = false; }} />
      <section className="page-state page-state--loading page-state-skeleton" role="status">
        <span>{props.message}</span><i aria-hidden="true" /><i aria-hidden="true" /><i aria-hidden="true" />
      </section>
    </>;
  }

  if (props.state === "empty") return <>
    <PageHero eyebrow="다음 단계" title={props.title} description={props.description} />
    <section className="state-panel page-state page-state--empty"><Link className="button-link button-primary" to={props.actionTo}>{props.actionLabel}</Link></section>
  </>;

  const unauthorized = props.error instanceof ApiError && props.error.status === 401;
  const hidden = props.error instanceof ApiError && (props.error.status === 403 || props.error.status === 404);
  // fetch rejects with TypeError before an HTTP response exists, so treat it as a network failure.
  const network = props.error instanceof TypeError;
  const title = unauthorized ? "로그인이 필요합니다." : hidden ? props.title : network ? "서버에 연결할 수 없습니다." : props.errorTitle ?? props.title;
  const description = unauthorized
    ? "DevPath에 로그인한 뒤 다시 확인해 주세요."
    : hidden
      ? "요청한 정보가 없거나 현재 계정으로 접근할 수 없습니다."
      : network
        ? "네트워크 연결을 확인한 뒤 다시 시도해 주세요."
        : props.description ?? "일시적인 문제로 정보를 표시하지 못했습니다. 잠시 후 다시 시도해 주세요.";
  const requestId = props.error instanceof ApiError ? props.error.requestId : null;

  return <>
    <PageHero eyebrow={unauthorized || hidden ? "접근 안내" : "문제가 발생했습니다"} title={title} description={description} />
    <section className={`state-panel page-state page-state--${unauthorized || hidden ? "forbidden" : "error"}`} role="alert">
      <p className="visually-hidden">{title}. {description}</p>
      {!unauthorized && !hidden && requestId ? <p>오류 참조 <code className="page-state-reference">{requestId}</code></p> : null}
      {unauthorized ? <Link className="button-link button-primary" to="/">홈으로 이동</Link>
        : hidden || !props.retry ? <Link className="button-link button-primary" to={props.backTo ?? "/"}>{props.backLabel ?? "돌아가기"}</Link>
          : <button className="button-primary" type="button" onClick={props.retry}>다시 시도</button>}
    </section>
  </>;
}
