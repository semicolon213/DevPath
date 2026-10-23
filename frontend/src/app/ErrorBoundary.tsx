import { Component, type ErrorInfo, type ReactNode } from "react";
import { PageHero } from "../shared/ui/PageHero";

type ErrorBoundaryProps = { children: ReactNode };
type ErrorBoundaryState = { hasError: boolean };

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { hasError: false };

  static getDerivedStateFromError(): ErrorBoundaryState {
    return { hasError: true };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    console.error("DevPath frontend error boundary captured an error.", {
      message: error.message,
      componentStack: errorInfo.componentStack
    });
  }

  render() {
    return this.state.hasError ? <SafeErrorPage /> : this.props.children;
  }
}

export function SafeErrorPage({ embedded = false }: { embedded?: boolean }) {
  const content = <>
    <PageHero title="화면을 표시하지 못했습니다" description="일시적인 문제가 발생했습니다. 페이지를 새로고침해 주세요." />
    <section className="state-panel page-state page-state--error" role="alert">
      <p>화면을 그리는 중 오류가 발생했습니다. 새로고침한 뒤에도 문제가 계속되면 잠시 후 다시 시도해 주세요.</p>
      <button type="button" onClick={() => window.location.reload()}>페이지 새로고침</button>
    </section>
  </>;
  return embedded ? <div>{content}</div> : <main className="shell">{content}</main>;
}
