# Project Rules & Development Guidelines

**MANDATORY RULE 1 (Documentation Review)**: Before initiating any new development, architectural changes, refactoring, or bug fixes on this project (`CloudAudioPlayer`), you **MUST** thoroughly review the project documentation located in the `docs/` directory:
- [docs/architecture.md](docs/architecture.md) — Architecture, Onion Architecture boundaries, and package specifications.
- [docs/development_notes.md](docs/development_notes.md) — Technical implementation details, cloud adapters, authentication, and known patterns.

**MANDATORY RULE 2 (Documentation Upkeep)**: Whenever architectural changes, new features, or refactoring are implemented, you **MUST** automatically and concurrently update [docs/architecture.md](docs/architecture.md) and [docs/development_notes.md](docs/development_notes.md) to reflect the changes.

**MANDATORY RULE 3 (Git Tracking for New Files)**: Whenever any new class, file, or resource is created in the project, you **MUST** ensure it is staged/tracked in Git (`git add`) so it is never left untracked.

Always maintain strict adherence to **Clean Architecture**, **Separation of Concerns**, and the established coding standards defined in these documents.
