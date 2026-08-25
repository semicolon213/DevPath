CREATE TABLE companies (
    company_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    localized_name VARCHAR(120) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('SUPPORTED', 'DEPRECATED', 'FUTURE_CANDIDATE')),
    active_profile_version_id UUID,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE company_profile_versions (
    company_profile_version_id UUID PRIMARY KEY,
    company_id VARCHAR(64) NOT NULL REFERENCES companies(company_id),
    version_label VARCHAR(32) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('ACTIVE', 'SUPERSEDED', 'DEPRECATED')),
    technology_focus JSONB NOT NULL,
    engineering_culture VARCHAR(500) NOT NULL,
    preferred_competencies JSONB NOT NULL,
    recommendation_priorities JSONB NOT NULL,
    skill_emphasis JSONB NOT NULL,
    weight_overrides JSONB NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL,
    UNIQUE (company_id, version_label)
);

ALTER TABLE companies ADD CONSTRAINT fk_companies_active_profile
    FOREIGN KEY (active_profile_version_id) REFERENCES company_profile_versions(company_profile_version_id);
CREATE UNIQUE INDEX uq_company_profile_active ON company_profile_versions(company_id) WHERE status = 'ACTIVE';

INSERT INTO companies (company_id, name, localized_name, status, created_at) VALUES
('google','Google','구글','SUPPORTED','2026-08-12T00:00:00Z'),
('amazon','Amazon','아마존','SUPPORTED','2026-08-12T00:00:00Z'),
('naver','Naver','네이버','SUPPORTED','2026-08-12T00:00:00Z'),
('kakao','Kakao','카카오','SUPPORTED','2026-08-12T00:00:00Z'),
('toss','Toss','토스','SUPPORTED','2026-08-12T00:00:00Z'),
('coupang','Coupang','쿠팡','SUPPORTED','2026-08-12T00:00:00Z');

INSERT INTO company_profile_versions (company_profile_version_id, company_id, version_label, status, technology_focus, engineering_culture, preferred_competencies, recommendation_priorities, skill_emphasis, weight_overrides, effective_at) VALUES
('42000000-0000-0000-0000-000000000001','google','company-v1','ACTIVE','["범용 언어","확장 가능한 시스템","테스트"]','기술적 깊이와 엔지니어링 엄격성을 강조합니다.','["아키텍처","알고리즘 인접 프로젝트","테스트","명료성"]','["아키텍처","면접 준비","프로젝트 복잡도"]','["언어 깊이","아키텍처","테스트"]','{"ARCHITECTURE":"INCREASE","TESTING":"INCREASE","PROJECT_COMPLEXITY":"INCREASE"}','2026-08-12T00:00:00Z'),
('42000000-0000-0000-0000-000000000002','amazon','company-v1','ACTIVE','["백엔드 서비스","데이터베이스","DevOps","신뢰성"]','오너십과 운영 우수성을 강조합니다.','["서비스 신뢰성","배포","문서화","백엔드 깊이"]','["프로젝트","DevOps","아키텍처","포트폴리오"]','["백엔드","DevOps","개발 활동"]','{"DEVOPS":"INCREASE","DATABASE":"INCREASE","RELIABILITY_DOCUMENTATION":"INCREASE"}','2026-08-12T00:00:00Z'),
('42000000-0000-0000-0000-000000000003','naver','company-v1','ACTIVE','["웹 서비스","백엔드와 프론트엔드","데이터와 검색 인접 시스템"]','제품 품질의 웹 엔지니어링을 강조합니다.','["문서화","유지보수성","웹 서비스 깊이"]','["포트폴리오","문서화","프로젝트"]','["웹 엔지니어링","문서화","데이터"]','{"DOCUMENTATION":"INCREASE","WEB_ENGINEERING":"INCREASE","DATA":"INCREASE"}','2026-08-12T00:00:00Z'),
('42000000-0000-0000-0000-000000000004','kakao','company-v1','ACTIVE','["제품 서비스","웹과 모바일 시스템","협업"]','사용자 중심 제품 전달을 강조합니다.','["협업","신뢰성","프론트엔드와 백엔드 품질"]','["포트폴리오","협업","프로젝트"]','["제품 엔지니어링","협업"]','{"COLLABORATION":"INCREASE","MAINTAINABILITY":"INCREASE"}','2026-08-12T00:00:00Z'),
('42000000-0000-0000-0000-000000000005','toss','company-v1','ACTIVE','["핀테크 인접 신뢰성","테스트","백엔드와 프론트엔드 품질"]','빠른 반복과 정확성을 함께 강조합니다.','["테스트","신뢰성","영향 명료성","유지보수성"]','["테스트","포트폴리오","아키텍처"]','["테스트","신뢰성","영향 근거"]','{"TESTING":"INCREASE","MAINTAINABILITY":"INCREASE","RELIABILITY":"INCREASE"}','2026-08-12T00:00:00Z'),
('42000000-0000-0000-0000-000000000006','coupang','company-v1','ACTIVE','["커머스 규모 백엔드","데이터","DevOps","운영"]','규모와 운영 규율을 강조합니다.','["확장성","데이터 시스템","배포","개발 활동"]','["DevOps","데이터","아키텍처","프로젝트"]','["확장성","데이터","DevOps"]','{"DEVOPS":"INCREASE","DATA":"INCREASE","ARCHITECTURE":"INCREASE","ACTIVITY":"INCREASE"}','2026-08-12T00:00:00Z');

UPDATE companies c SET active_profile_version_id = p.company_profile_version_id
FROM company_profile_versions p WHERE p.company_id = c.company_id AND p.status = 'ACTIVE';
