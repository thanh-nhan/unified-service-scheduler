# 10: GenAI in the design phase

This section describes how generative AI was used to produce the requirements and this design, how its output was verified, and where human judgment overrode it. Implementation-phase AI use (the coding model) is described in the README's AI Collaboration Narrative.

## 1. Operating model

| Role | Who | Responsibility |
|---|---|---|
| Product Owner and Engineering Manager | Human (the candidate) | Owns scope and decisions; approves stories and design at explicit gates |
| Planning AI | Claude (cloud) | Analysis, options and trade-offs, drafting stories, design, and tasks; researching tools and versions |
| Coding AI | qwen2.5-coder:14b via Aider and Ollama (local) | Implements small tasks against approved stories and this design |

Two principles shaped the process:

1. **Documents are the contract between humans and AIs.** The approved user stories (with concrete, numbered acceptance criteria) and this design are what the coding AI builds against, and what its output is verified against. Nothing is built from a chat message alone.
2. **Approval gates are human.** Stories v0.1 → v0.3 went through PO review before any design work; this design is approved by the EM before task breakdown. Every change is recorded in a change log.

## 2. How AI was used, step by step

| Step | AI contribution | Human decision or correction |
|---|---|---|
| Scenario selection | Compared the four scenarios; recommended A because it contains a real correctness problem (concurrent double-booking) rather than CRUD | Chose Scenario A and the backend layer |
| Clarifying ambiguity | Listed the ambiguous points (who assigns resources, duration source, interval semantics, opening hours) and proposed defaults | Decided: system auto-assigns technician and bay; one service per appointment; customer self-service |
| Concurrency strategy | Explained the check-then-insert race and compared exclusion constraints, row locking, advisory locks, and SERIALIZABLE | Asked for a MySQL comparison; the AI added row locking and a slot table, including the REPEATABLE READ snapshot trap. Decision: stay on PostgreSQL (ADR-002) |
| API and errors | Proposed the 400/404/409/422 taxonomy, RFC 7807 errors, idempotency keys, cancel as an action | Accepted |
| Process design | Proposed a layered contract (stories, AC, design, tasks, frozen acceptance tests) for a weaker local coding model | Adopted; reframed as the EM's workflow with the PO |
| Tool selection | Checked the model name and found "Qwen3.5 Coder" does not exist (`qwen3-coder:30b` does). Diagnosed why Cline made no file changes with a smaller model, and adapted the rules and tasks for Aider | Chose qwen3-coder:30b and Cline; when the 30B model did not fit in memory, switched to qwen2.5-coder:14b, then to Aider after the agent reported edits it never made |
| User stories | Drafted 12 stories with fixture data and exact expected values, so each AC maps to one test with no interpretation | Reviewed as PO (below) |
| This design | Drafted all sections and ADRs from the approved stories | Pending EM approval |

## 3. Where the human overrode the AI

| AI proposal | Human decision | Why it was better |
|---|---|---|
| A 409 conflict returns up to 3 alternative slots, with a next-day fallback | Removed. The UI shows availability; on conflict the customer just picks again (ADR-006) | Simpler API, one source of truth for availability, and the UX was already covered by the availability view |
| Multi-time-zone support: `timestamptz`, a dealership time zone, DST-safe slot stepping | Removed. Single time zone, Vietnam only (ADR-003) | The business has no cross-border customers. Removing it eliminated a whole class of bugs and test cases |
| Daily availability only | Added a weekly view (US-09), kept the daily one, and required `weekStart` to be a Monday | Matches how customers choose a time. Rejecting non-Monday dates keeps behavior explicit for the coding model |
| Aider as the coding harness | Cline first, then back to Aider | Cline was the candidate's preference; with the smaller model that fits the laptop, Aider's plain-text edits proved more reliable than Cline's tool calls. The stories, design, and tasks needed no change |
| Maven as the build tool | Gradle (Kotlin DSL) | EM preference; the design was updated in one change (v0.2) with no effect on behavior |

## 4. Gaps the AI surfaced that the human accepted

These issues were not in the original brief. Writing precise acceptance criteria and design steps with AI exposed them:

- **Vehicle double-booking:** the same car could be booked into two overlapping appointments at different bays. Added a third exclusion constraint and `VEHICLE_ALREADY_BOOKED`.
- **Validation order:** without a defined order, a request that is both in the past and off-grid could return either error depending on how the code is written. US-04 fixes the order.
- **Retry inside `@Transactional`:** a likely AI-generated bug; after a constraint violation the PostgreSQL transaction is aborted, so the retry loop must be outside it, in a separate bean (04 §4).
- **Concurrent duplicate requests:** the idempotency key and the vehicle constraint can both fire; checking the key first makes the outcome deterministic (04 §4 rule 5).
- **Jackson leniency:** default date handling can accept a trailing `Z`, which would silently break the single-time-zone rule; the design parses strictly (04 §8).
- **UUID ordering:** Java's `UUID.compareTo` differs from PostgreSQL's ordering, so "lowest id" is decided in SQL only (04 §3).

## 5. How AI output was verified

| Technique | Example |
|---|---|
| Check facts that change over time against current sources | Spring Boot 3.5 open-source support turned out to have ended on 2026-06-30; the AI's earlier "use Spring Boot 3.x" advice was revised to 4.1 (ADR-005). The coding model name was checked against current sources |
| Recompute every number in the acceptance criteria | Slot counts (35 per day for a 30-minute service; 31 and 21 for a 90-minute service with a busy technician; 26 after 10:05) were recalculated by hand |
| Cross-document consistency checks | After each PO change, all story files were searched for leftover references (offsets, `alternatives`, renumbered AC IDs) |
| Ask for alternatives and trade-offs, not just one answer | Concurrency (ADR-002), harness choice, MySQL vs PostgreSQL |
| Human approval gates with a change log | Stories v0.1 → v0.4; design v0.1 → v0.2 |

## 6. Lessons from the design phase

- Precise acceptance criteria are where most design gaps surface; high-level design discussion missed the vehicle and validation-order issues.
- The AI tends to add generality (time zones, alternatives) that the business does not need; the PO role is to cut it.
- Facts the AI "knows" about versions and tools need checking: two of its assumptions (a model name, a supported Spring Boot line) were wrong or outdated.
