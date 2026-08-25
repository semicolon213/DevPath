CREATE TABLE careers (
    career_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    localized_name VARCHAR(120) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('SUPPORTED', 'DEPRECATED', 'FUTURE_CANDIDATE')),
    active_profile_version_id UUID,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE career_profile_versions (
    career_profile_version_id UUID PRIMARY KEY,
    career_id VARCHAR(64) NOT NULL REFERENCES careers(career_id),
    version_label VARCHAR(32) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('ACTIVE', 'SUPERSEDED', 'DEPRECATED')),
    purpose VARCHAR(500) NOT NULL,
    core_technologies JSONB NOT NULL,
    required_competencies JSONB NOT NULL,
    preferred_competencies JSONB NOT NULL,
    evaluation_categories JSONB NOT NULL,
    priority_weights JSONB NOT NULL,
    roadmap_template JSONB NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL,
    UNIQUE (career_id, version_label)
);

ALTER TABLE careers ADD CONSTRAINT fk_careers_active_profile
    FOREIGN KEY (active_profile_version_id) REFERENCES career_profile_versions(career_profile_version_id);

CREATE UNIQUE INDEX uq_career_profile_active
    ON career_profile_versions(career_id) WHERE status = 'ACTIVE';

INSERT INTO careers (career_id, name, localized_name, status, created_at) VALUES
('backend', 'Backend Engineer', '백엔드 엔지니어', 'SUPPORTED', '2026-08-12T00:00:00Z'),
('frontend', 'Frontend Engineer', '프론트엔드 엔지니어', 'SUPPORTED', '2026-08-12T00:00:00Z'),
('ai-engineer', 'AI Engineer', 'AI 엔지니어', 'SUPPORTED', '2026-08-12T00:00:00Z'),
('devops', 'DevOps Engineer', 'DevOps 엔지니어', 'SUPPORTED', '2026-08-12T00:00:00Z'),
('security', 'Security Engineer', '보안 엔지니어', 'SUPPORTED', '2026-08-12T00:00:00Z'),
('game', 'Game Developer', '게임 개발자', 'SUPPORTED', '2026-08-12T00:00:00Z'),
('embedded', 'Embedded Engineer', '임베디드 엔지니어', 'SUPPORTED', '2026-08-12T00:00:00Z'),
('mobile', 'Mobile Developer', '모바일 개발자', 'SUPPORTED', '2026-08-12T00:00:00Z'),
('data-engineer', 'Data Engineer', '데이터 엔지니어', 'SUPPORTED', '2026-08-12T00:00:00Z');

INSERT INTO career_profile_versions (
    career_profile_version_id, career_id, version_label, status, purpose, core_technologies,
    required_competencies, preferred_competencies, evaluation_categories, priority_weights,
    roadmap_template, effective_at
) VALUES
('32000000-0000-0000-0000-000000000001', 'backend', 'career-v1', 'ACTIVE', '서버 측 서비스와 API를 설계하고 구현합니다.',
 '["Java","Spring Boot","SQL","Redis","Docker"]', '["API 설계","데이터베이스 연동","테스트","아키텍처","개발 활동"]',
 '["캐싱","메시징","CI/CD","관측 가능성"]', '["LANGUAGE","FRAMEWORK","TESTING","DOCUMENTATION","ACTIVITY"]',
 '{"FRAMEWORK":"HIGH","TESTING":"HIGH","LANGUAGE":"HIGH","DOCUMENTATION":"MEDIUM","ACTIVITY":"MEDIUM"}',
 '["언어","프레임워크","데이터베이스","테스트","배포"]', '2026-08-12T00:00:00Z'),
('32000000-0000-0000-0000-000000000002', 'frontend', 'career-v1', 'ACTIVE', '접근 가능하고 유지보수 가능한 웹 사용자 경험을 구현합니다.',
 '["TypeScript","React","React Query","CSS"]', '["컴포넌트 구조","상태 관리","접근성","테스트"]',
 '["성능","디자인 시스템","E2E 테스트"]', '["LANGUAGE","FRAMEWORK","TESTING","DOCUMENTATION"]',
 '{"FRAMEWORK":"HIGH","LANGUAGE":"HIGH","TESTING":"MEDIUM","DOCUMENTATION":"MEDIUM"}',
 '["TypeScript","React","상태와 데이터","테스트","포트폴리오 완성도"]', '2026-08-12T00:00:00Z'),
('32000000-0000-0000-0000-000000000003', 'ai-engineer', 'career-v1', 'ACTIVE', '데이터 근거와 평가 절차를 갖춘 AI 응용 시스템을 구현합니다.',
 '["Python","FastAPI","Vector Search","AI APIs"]', '["AI 프레임워크 활용","데이터 처리","근거 기반 컨텍스트","평가 인식"]',
 '["RAG","벡터 검색","MLOps"]', '["LANGUAGE","FRAMEWORK","TESTING","DOCUMENTATION"]',
 '{"FRAMEWORK":"HIGH","LANGUAGE":"HIGH","DOCUMENTATION":"MEDIUM","TESTING":"MEDIUM"}',
 '["Python","AI 프레임워크","데이터와 벡터","평가","응용 통합"]', '2026-08-12T00:00:00Z'),
('32000000-0000-0000-0000-000000000004', 'devops', 'career-v1', 'ACTIVE', '전달 과정과 운영 자동화를 구축합니다.',
 '["Docker","GitHub Actions","Nginx","Cloud"]', '["CI/CD","컨테이너화","배포","모니터링 기초"]',
 '["IaC","오케스트레이션","신뢰성 공학"]', '["TESTING","DOCUMENTATION","ACTIVITY"]',
 '{"ACTIVITY":"HIGH","DOCUMENTATION":"HIGH","TESTING":"MEDIUM"}',
 '["Docker","CI","배포","모니터링","신뢰성"]', '2026-08-12T00:00:00Z'),
('32000000-0000-0000-0000-000000000005', 'security', 'career-v1', 'ACTIVE', '안전한 기본값과 검증 가능한 보안 엔지니어링을 적용합니다.',
 '["Secure Configuration","Dependency Hygiene","Authentication"]', '["보안 관행","테스트","유지보수성","문서화"]',
 '["정적 분석","위협 모델링","안전한 인증"]', '["TESTING","DOCUMENTATION","FRAMEWORK"]',
 '{"TESTING":"HIGH","DOCUMENTATION":"HIGH","FRAMEWORK":"MEDIUM"}',
 '["안전한 코딩","의존성 위생","인증","보안 문서"]', '2026-08-12T00:00:00Z'),
('32000000-0000-0000-0000-000000000006', 'game', 'career-v1', 'ACTIVE', '구조화된 게임 클라이언트 또는 서버 시스템을 구현합니다.',
 '["C#","C++","Unity","Unreal"]', '["게임 루프","클라이언트 구조","코드 구성","문서화"]',
 '["물리","도구화","최적화"]', '["LANGUAGE","FRAMEWORK","DOCUMENTATION","ACTIVITY"]',
 '{"LANGUAGE":"HIGH","FRAMEWORK":"HIGH","ACTIVITY":"MEDIUM","DOCUMENTATION":"MEDIUM"}',
 '["언어","게임 엔진","게임 플레이","구조화","완성도"]', '2026-08-12T00:00:00Z'),
('32000000-0000-0000-0000-000000000007', 'embedded', 'career-v1', 'ACTIVE', '저수준 및 하드웨어 인접 소프트웨어를 안정적으로 구현합니다.',
 '["C","C++","Embedded Toolchains"]', '["저수준 언어","코드 구성","빌드 재현성","문서화"]',
 '["RTOS","장치 문서","하드웨어 가정"]', '["LANGUAGE","TESTING","DOCUMENTATION"]',
 '{"LANGUAGE":"HIGH","DOCUMENTATION":"HIGH","TESTING":"MEDIUM"}',
 '["C/C++","장치 기초","구조화된 모듈","테스트와 문서"]', '2026-08-12T00:00:00Z'),
('32000000-0000-0000-0000-000000000008', 'mobile', 'career-v1', 'ACTIVE', '플랫폼 규약과 테스트 가능성을 갖춘 모바일 앱을 구현합니다.',
 '["Kotlin","Swift","Mobile SDK"]', '["앱 구조","UI","API 연동","테스트"]',
 '["접근성","릴리스 자동화","플랫폼 심화"]', '["LANGUAGE","FRAMEWORK","TESTING","DOCUMENTATION"]',
 '{"FRAMEWORK":"HIGH","LANGUAGE":"HIGH","TESTING":"MEDIUM","DOCUMENTATION":"MEDIUM"}',
 '["플랫폼 언어","앱 구성 요소","API","테스트","릴리스"]', '2026-08-12T00:00:00Z'),
('32000000-0000-0000-0000-000000000009', 'data-engineer', 'career-v1', 'ACTIVE', '신뢰할 수 있는 데이터 파이프라인과 저장 구조를 구현합니다.',
 '["SQL","Python","PostgreSQL","Data Processing"]', '["데이터 모델링","ETL","데이터베이스","신뢰성","문서화"]',
 '["스트리밍","오케스트레이션","웨어하우징"]', '["LANGUAGE","TESTING","DOCUMENTATION","ACTIVITY"]',
 '{"LANGUAGE":"HIGH","TESTING":"HIGH","DOCUMENTATION":"HIGH","ACTIVITY":"MEDIUM"}',
 '["SQL","파이프라인","오케스트레이션","신뢰성","문서화"]', '2026-08-12T00:00:00Z');

UPDATE careers c SET active_profile_version_id = p.career_profile_version_id
FROM career_profile_versions p WHERE p.career_id = c.career_id AND p.status = 'ACTIVE';
