# In-game rule dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a polished administrator dashboard with reliable persistent runtime switches.
**Architecture:** Keep rule loading and runtime state authoritative on the server. Publish immutable diagnostics and bounded paged summaries over NeoForge payloads to a reusable native screen.
**Tech Stack:** Minecraft 26.2, NeoForge 26.2.0.7-beta, Java 25, Gson, JUnit 5, Gradle 9.2.1.
**Spec:** `docs/superpowers/specs/2026-10-13-ingame-rule-dashboard-design.md`

## Global Constraints

- Work remains on `feature/ingame-rule-dashboard`; do not modify or merge into main.
- Reuse RuleManager and RuntimeControl; preserve existing JSON and override format.
- Client rendering classes must not load on dedicated servers.
- Version one offers switches and read-only categories, with future editor extension points.

## Review Focus

- Failed saves preserve both the prior state file and live settings.
- Duplicate/missing IDs and malformed JSON remain visible but cannot execute.
- Global pause preserves individual selections.
- Stale requests and lost permissions never change state or disclose configuration.
- Large snapshots and long diagnostics stay within network and viewport bounds.

### Task 1: Rule catalog and durable runtime state

**Files:** Create `config/RuleCatalog.java`, `control/RuntimeStateStore.java`; modify `config/RuleManager.java`, `control/RuntimeControl.java`, `command/MobControlCommands.java`; add catalog/store tests.
**Interfaces:** `RuleCatalog.load(Path)` returns valid rules and diagnostic entries; `RuntimeStateStore` loads and transactionally writes runtime state; existing manager/control APIs remain available.
- [ ] Add tests using temporary directories for malformed rules, missing IDs, all duplicate entries rejected, stable source identity, state round trips, global pause preserving overrides, and failed writes preserving live state/file.
- [ ] Run targeted tests and observe failure before implementing new classes.
- [ ] Implement catalog loading, immutable publication, durable temporary-file replacement, explicit save results, and command failure feedback.
- [ ] Run targeted tests and full existing suite; commit the changes on the feature branch.

### Task 2: Server dashboard and protocol

**Files:** Create `dashboard/DashboardModels.java`, `dashboard/DashboardPolicy.java`, `dashboard/DashboardService.java`, `network/DashboardNetwork.java`, payload classes; modify mod initialization, command registration, server lifecycle hooks; add policy/protocol tests.
**Interfaces:** Service opens a player session, accepts revisioned actions, broadcasts changed snapshots, and removes sessions. Network models carry bounded paged JSON summaries, never executable rules or paths.
- [ ] Add tests for permission/stale/unknown-ID rejection, global-independent rule state, paging/reassembly bounds, and category/search summaries; observe failures.
- [ ] Register versioned client/server payloads with game-thread handlers, server-side checks, subscription cleanup, reload throttling, revisions covering command changes, and lifecycle resets.
- [ ] Compile against cached NeoForge/Minecraft sources; run protocol/policy and full tests; commit.

### Task 3: Native client interface

**Files:** Create `client/DashboardClient.java`, `client/DashboardScreen.java`, `client/widget/DashboardButton.java`, `client/widget/RuleList.java`, `client/widget/CategoryPanel.java`, `client/widget/DashboardTheme.java`; add layout/filter tests where logic is independent of rendering.
**Interfaces:** Client accepts complete paged snapshots for the active session only; screen submits explicit desired states and current revisions through the network.
- [ ] Add tests for filtering, clipping/layout bounds, and out-of-order/partial snapshot assembly; observe failures.
- [ ] Implement charcoal panels, accessible labeled switches, search, scrolling rows, selection, category summaries, compact details navigation, wrapping, pending/error/saved feedback, refresh/reset controls, timeout recovery, and disconnect cleanup.
- [ ] Compile and run the suite; commit.

### Task 4: Verification and delivery

**Files:** Update `docs/CONFIG.md`, `README.md`, build metadata and local artifact documentation as needed.
- [ ] Run `gradlew.bat compileJava test build` with Java 25; expected exit 0 and no failed tests.
- [ ] Review whole feature changes for dedicated-server class loading, save failures, synchronization/lifecycle, and UI bounds; resolve material findings and rerun affected checks.
- [ ] Attempt a dedicated-server smoke launch if runtime resources permit; distinguish smoke verification from live GUI verification.
- [ ] Document installation on both clients/server and a manual Minecraft checklist covering two admins, non-admins, restart persistence, invalid JSON, resizing, search, and command synchronization.
- [ ] Commit verified work; leave the feature branch unmerged.

## Execution record

The user explicitly requested implementation immediately after planning; execute inline without another plan approval pause. Use the existing clean feature-branch checkout as requested. Record verification and deviations below during work.
