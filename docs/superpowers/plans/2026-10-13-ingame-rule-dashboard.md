# In-game rule dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

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
- [x] Add tests using temporary directories for malformed rules, missing IDs, all duplicate entries rejected, stable source identity, state round trips, global pause preserving overrides, and failed writes preserving live state/file.
- [x] Run targeted tests and observe failure before implementing new classes.
- [x] Implement catalog loading, immutable publication, durable temporary-file replacement, explicit save results, and command failure feedback.
- [x] Run targeted tests and full existing suite; commit the changes on the feature branch.

### Task 2: Server dashboard and protocol

**Files:** Create `dashboard/DashboardModels.java`, `dashboard/DashboardPolicy.java`, `dashboard/DashboardService.java`, `network/DashboardNetwork.java`, payload classes; modify mod initialization, command registration, server lifecycle hooks; add policy/protocol tests.
**Interfaces:** Service opens a player session, accepts revisioned actions, broadcasts changed snapshots, and removes sessions. Network models carry bounded paged JSON summaries, never executable rules or paths.
- [x] Add tests for permission/stale/unknown-ID rejection, global-independent rule state, paging/reassembly bounds, and category/search summaries; observe failures.
- [x] Register versioned client/server payloads with game-thread handlers, server-side checks, subscription cleanup, reload throttling, revisions covering command changes, and lifecycle resets.
- [x] Compile against cached NeoForge/Minecraft sources; run protocol/policy and full tests; commit.

### Task 3: Native client interface

**Files:** Create `client/DashboardClient.java`, `client/DashboardScreen.java`, `client/widget/DashboardButton.java`, `client/widget/RuleList.java`, `client/widget/CategoryPanel.java`, `client/widget/DashboardTheme.java`; add layout/filter tests where logic is independent of rendering.
**Interfaces:** Client accepts complete paged snapshots for the active session only; screen submits explicit desired states and current revisions through the network.
- [x] Add tests for filtering, clipping/layout bounds, and out-of-order/partial snapshot assembly; observe failures.
- [x] Implement charcoal panels, accessible labeled switches, search, scrolling rows, selection, category summaries, compact details navigation, wrapping, pending/error/saved feedback, refresh/reset controls, timeout recovery, and disconnect cleanup.
- [x] Compile and run the suite; commit.

### Task 4: Verification and delivery

**Files:** Update `docs/CONFIG.md`, `README.md`, build metadata and local artifact documentation as needed.
- [x] Run `gradlew.bat compileJava test build` with Java 25; expected exit 0 and no failed tests.
- [x] Review whole feature changes for dedicated-server class loading, save failures, synchronization/lifecycle, and UI bounds; resolve material findings and rerun affected checks.
- [x] Attempt a dedicated-server smoke launch if runtime resources permit; distinguish smoke verification from live GUI verification.
- [x] Document installation on both clients/server and a manual Minecraft checklist covering two admins, non-admins, restart persistence, invalid JSON, resizing, search, and command synchronization.
- [x] Commit verified work; leave the feature branch unmerged.

## Execution record

The user explicitly requested implementation immediately after planning; execute inline without another plan approval pause. Use the existing clean feature-branch checkout as requested. Record verification and deviations below during work.

Task 1 foundation verified: Java 25 compilation and all 29 current tests pass. New catalog/store tests first failed because classes were absent. Ruling: use an ignored Gradle init script and temporary build directory for local verification because OneDrive marks generated directories read-only. Normal Gradle build configuration is unchanged. Protocol model tests were added early while resolving test runtime dependencies.



Task 2: complete. Common payloads and server service compiled against actual NeoForge 26.2.0.7-beta APIs; permission/revision/identity, search, paging, and assembly tests pass. Clientbound handlers are registered only by the client-distribution entry point. Observing a revision does not consume pending broadcasts.

Task 3: complete. Native Minecraft 26.2 GUI uses GuiGraphicsExtractor and Minecraft.gui screen APIs. Layout and precise switch-hit tests pass. Scroll position is retained through resize and compact Rules/Details navigation. Live visual QA remains manual.

Final independent review: one fresh read-only reviewer inspected the full feature. Fixed malformed duplicate identity, nonfinite summary values, consumed command broadcasts, and failed missing-file reload resetting live state. Tests reproduced these conditions before the fixes. UI hit areas and array positions also have regression checks; scroll preservation was fixed by retaining the list offset.

Task 4: compileJava, test, and build pass with 38 tests and zero failures. An isolated localhost dedicated server reached Done and successfully executed msr status, disable, rule enable, list, reload, enable, and msrgui console rejection, then stop. The first automated command attempt supplied input before world loading and triggered vanilla's null-level console source error; delayed input after startup resolved the smoke harness issue. No changes to Minecraft command execution were needed.

Ruling: existing requested feature-branch checkout was retained instead of creating another worktree, matching explicit user scope; no main edits or merge occurred. Cost if wrong: isolation is by branch rather than a separate checkout.
Ruling: local verification used a temporary build directory and ignored init script because OneDrive prevented Java/Gradle from deleting read-only generated directories. Cost if wrong: repeat verification requires the same local override until the filesystem behavior is resolved; normal project build settings are preserved.

No deferred review findings. Manual Minecraft acceptance still covers rendering, input feel, single-player cheats, two-admin synchronization, permission revocation, disconnect/world transitions, and resize/GUI scales. docs/DASHBOARD_TESTING.md documents those checks.
