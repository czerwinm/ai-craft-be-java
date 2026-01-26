# Repository Copilot Instructions

Purpose: Provide concise, machine-readable instructions for AI assistants (Copilot-style agents) and developers. This file embeds the canonical `AGENTS.md` and explicitly marks the docs listed in it as authoritative project instructions.

Quick reference (top-of-mind):

- Build: `./gradlew build`
- Run all tests: `./gradlew test`
- Run single test: `./gradlew test --tests "ClassName"`
- Java: Java 21 (use `--enable-preview` where required)
- Key architecture principles: Domain Independence; Aggregate Encapsulation; Ports & Adapters; Immutability (use records); Event-Driven; Package-private by default.

#file:AGENTS.md

---

Authoritative files (treat these as instruction sources, consult them first)

- `AGENTS.md` (embedded above)
- `src/docs/domain-model.md` — Domain-Driven Design patterns: aggregates, value objects, domain events, and unit testing guidance.
- `src/docs/ports.md` — Ports & Adapters: primary (service) and secondary (repository/gateway) port definitions and examples.
- `src/docs/adapter-http.md` — HTTP adapter guidance: thin controllers, validation, error handling, and examples.
- `src/docs/adapter-kafka.md` — Message broker adapter guidance and best practices for Kafka integration.
- `src/docs/adapter-scheduler.md` — Scheduled jobs adapter and patterns (Spring `@Scheduled`, error handling, monitoring).
- `src/docs/adapter-persistence.md` — Persistence adapter: JPA + JSONB, optimistic locking, Liquibase schema guidance, and Testcontainers examples.
- `src/docs/adapter-rest-client.md` — External REST client adapter guidance and error handling patterns.
- `src/docs/e2e-test.md` — End-to-end testing guidance: Testcontainers, fixtures, test isolation, and best practices.
- `src/docs/architecture-and-patterns.md` — Comprehensive architecture guidance and repeated examples (reference for deeper reading).

Assistant rules (MANDATORY)

1. Treat `AGENTS.md` and every file listed in the "Authoritative files" section above as authoritative instructions for design, code style, testing, and architecture in this repository.
2. When answering architecture, design, or implementation questions for this repo, consult the authoritative files first. Use their wording and examples when suggesting code patterns.
3. If a requested change conflicts with the authoritative docs, explicitly point out the conflict and prefer the authoritative guidance unless the user asks to deviate. Propose a clear justification and a code/design alternative if deviating.
4. Follow the Quick Reference (build/test commands, Java version) when producing runnable scripts, CI workflows, or commands. Always use `./gradlew` for build/test steps unless the user instructs otherwise.
5. Enforce DDD/Ports-and-Adapters conventions from the docs:
   - Make aggregates package-private and enforce business rules inside aggregates.
   - Prefer Java records for value objects and domain events.
   - Keep controllers and adapters thin; business logic belongs in the domain layer.
6. When making edits, prefer minimal, non-invasive changes that follow existing patterns in the repo.
7. If you modify or generate code that requires build/test verification, run `./gradlew test` (or the specific test) locally and report results. Include commands to reproduce the run.

Developer usage notes

- Read `AGENTS.md` first for a short project summary, then read the related `src/docs/*.md` document for domain-specific rules before implementing changes.
- Use the quick-reference commands above for local development.
- Tests: unit tests live in `src/test/java/...` and follow the patterns from the docs (aggregate tests, value object tests, service tests, adapter integration tests using Testcontainers).
- Naming/layout: follow the example module layout from the docs: domain package, module package, aggregate classes (package-private), controllers (package-private), service (primary port), repository interfaces (ports), and adapter implementations.

CI usage notes (recommended)

- CI jobs should run at minimum:
  - `./gradlew test` (fail fast on test failures)
  - Static checks / linters (if configured)
  - Optionally, run a subset of integration tests using Testcontainers if runners allow it.
- If you add a GitHub Actions workflow, keep this file at `.github/copilot-instructions.md` so hosted agents and bots can read it during automation.
- CI should treat failures of `./gradlew test` as blocking; tests represent the contract for correctness.

Maintenance & governance

- Update process: Keep `AGENTS.md` as the canonical short-form guide. When updating `AGENTS.md`, also update this file's embedding (the `#file:AGENTS.md` marker) or the one-line summaries as needed.
- Who may update: changes to authoritative architecture guidance should be made via Pull Request and reviewed by module owners / maintainers.
- Changelog: add a short line each time the authoritative docs change. See "Changelog" below.

Changelog

- 2026-01-26 — Initial `.github/copilot-instructions.md` created. Embeds `AGENTS.md` and declares `src/docs/*` files as authoritative.

Contact / escalation

- If guidance is unclear or you need to change architectural rules, open a PR and request review from module owners listed in the repository OWNERS file (or add reviewers in the PR). If you don't know the owners, tag the repository maintainers in an issue.

-- end of file --
