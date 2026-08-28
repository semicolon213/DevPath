# DevPath Contracts

This directory owns machine-readable contract artifacts.

Current scope:

- `openapi/devpath-openapi.yaml` contains the implemented health, identity, integration, repository, analysis,
  immutable analysis and Skill Matrix comparison, current skill detail/evidence, career catalog, Backend/Frontend career-readiness, deterministic recommendation
  current/history/detail/evidence, learning-roadmap current/history/detail/archive,
  company-catalog, owner-scoped onboarding-progress, dashboard-summary, GitHub connection-recovery, provider
  rate-limit recovery, terminal large-repository collection-limit reporting, and repository collaboration/document
  evidence plus current-snapshot activity-timeline and Notion workspace-source registration API subsets.
- Business APIs that are not implemented remain specified only in `docs/10_API_Specification.md` and must not be
  added to the machine-readable contract until their handlers exist.
