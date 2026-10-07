# Delivery Roadmap

## Phase 0: Platform foundation

- Confirm privacy/consent, retention, branch boundaries, fee policy, and identity recovery requirements.
- CI, local Compose, migrations, secrets, environments, logs/metrics, backups and restore rehearsal.
- Auth hardening, authorization policy tests, audit writer, user/branch management.

## Phase 1: School operations MVP

- Branch/class/academic year/staff management.
- Student admission, guardian links, medical/document access, transfer approvals.
- Attendance register and correction history, daily attendance reports.
- Parent accounts and child-scoped dashboard/API.

## Phase 2: Learning and communication

- Syllabus progress, daily activity, homework, albums, announcements.
- In-app parent-teacher conversations and notification preferences.
- Email/SMS/push adapters behind retryable provider ports and transactional outbox.

## Phase 3: Finance and analytics

- Fee structures, payment-provider integration, reconciliation and permissioned finance reports.
- Branch and school dashboards, export jobs, retention-aware audit search.
- Load tests, query tuning, read replicas/reporting projections where warranted.

## Phase 4: Production launch

- Threat model, security/privacy review, dependency/image scanning, penetration test, accessibility review.
- Staged rollout to one branch, data migration rehearsal, support runbooks, rollback and disaster-recovery exercise.
- Gradual enablement for seven branches with SLOs and incident ownership.

## Definition of done

Every API has authorization and branch-scope tests, validation/error contract, audit coverage for sensitive mutations, migration/backfill plan, observability, and UI loading/error/empty/success states. Production readiness additionally requires managed secrets, TLS, backups with tested restore, log redaction, rate limits, security scanning, and operational ownership.