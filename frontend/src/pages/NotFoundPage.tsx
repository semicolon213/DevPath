import { Link } from "react-router-dom";
import { PageHero } from "../shared/ui/PageHero";

export function NotFoundPage() {
  return <main className="shell">
    <PageHero eyebrow="잘못된 경로" title="페이지를 찾을 수 없습니다" description="요청한 페이지가 존재하지 않습니다." />
    <section className="state-panel page-state page-state--empty">
      <Link className="button-link button-primary" to="/">DevPath 홈으로 돌아가기</Link>
    </section>
  </main>;
}
