# AI collaboration log

A running record of how the AI agents were directed and corrected. It feeds the README's AI Collaboration Narrative. Add an entry whenever an agent does something wrong, surprising, or notably good, or when you change the process.

| Date | Task | Agent | What happened | How it was caught | Fix / decision |
|---|---|---|---|---|---|
| 2026-10-08 | Design | Claude (planner) | Proposed multi-time-zone support and alternative-slot suggestions | PO review of stories | Removed both (ADR-003, ADR-006) |
| 2026-10-08 | Design | Claude (planner) | Recommended Spring Boot 3.x for model familiarity | Version check against current support dates | Spring Boot 4.1 (ADR-005) |
| 2026-10-08 | Tooling | Claude (planner) | Model name "Qwen3.5 Coder 30B" did not exist | Web check | `qwen3-coder:30b` |
| 2026-10-09 | Planning | Claude (planner) | EM asked for User stories first before creating tasks for coding agent | Collaboration process review | Claude generated User Stories|
| 2026-10-09 | Planning | Claude (planner) | EM asked to not returning alternative booking time slots when having booking conflict. Instead, just return relevant error | User stories review | Claude updated UC-02|
| 2026-10-09 | Planning | Claude (planner) | EM asked to show available booking time slots for chosen service in week view so future UI can present the availability to users | User stories review | Claude created UC-09|
| 2026-10-09 | Planning | Claude (planner) | EM/PO approved UCs and asked to break into tasks | User stories review | Claude created tasks|
| 2026-10-09 | Planning | Claude (planner) | EM asked to switch to Gradle | Task review | Claude updated relevant tasks|
| | | | | | |

## Process changes

| Date | Change | Reason |
|---|---|---|
| 2026-10-08 | Switched coding harness from Aider to Cline | EM preference; added Plan-mode review and manual commits |
| 2026-10-09 | Switched build from Maven to Gradle (Kotlin DSL) | EM preference |
| 2026-10-09 | Add SpringBoot 4.1.1 and Gradle 9.7.1 | Framework setup |
| | | |

