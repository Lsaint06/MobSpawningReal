# Visual Mob Editor Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans. Track completed stages below.

**Goal:** Turn the v1.3 dashboard into an authoritative visual rule editor without sacrificing safe saves or compatibility.

**Architecture:** Client drafts contain JSON trees so omitted fields and unknown properties survive editing. A server-only rule store resolves opaque rule identities itself, validates against the actual schema and registries, compares file fingerprints, backs up originals, and atomically replaces a single changed file. Paged responses carry documents and cached registry metadata. Reusable typed forms expose existing fields; registries supply mob and item pickers.

**Tech Stack:** Java 25, Minecraft 26.2, NeoForge 26.2.0.7-beta, Gson, JUnit 5.

**Spec:** User-provided complete v1.4 specification, copied to ../specs/2026-10-13-visual-mob-editor.md.

## Global Constraints
- Branch feature/visual-mob-editor-v1.4 begins at v1.3 commit 690fd4c; never modify or merge main.
- Version 1.4.0; preserve existing runtime-state.json toggles.
- Server permission checks on every request, bounded transport, explicit save, stale-write rejection.
- No client file paths; no entity instantiation for decorative previews.

## Review Focus
- An external file edit must cause conflict rather than silent replacement.
- Malformed siblings and unknown fields must survive a valid targeted edit.
- False/zero and unset must remain distinct across draft/save/reload.
- Late network responses must not discard a dirty draft or reopen closed screens.
- File-write failure must leave both live rules and original bytes unchanged.

## Tasks
### 1. Safe persistence
- [x] Add RuleDocumentStoreTest for create/edit/duplicate/delete, conflicts, backups, malformed input, failed writes, unknown fields and unrelated preservation; observe failure.
- [x] Implement RuleDocumentStore with load, create, replace, duplicate, delete and typed bounded JSON validation; compare whole-directory fingerprints to catch cross-file ID changes.
- [x] Verify tests pass and original 38 tests remain green.
### 2. Authoritative editor transport
- [x] Extend admin sessions with paged editor documents/registry metadata and bounded edit requests; enforce permission, monotonic requests, rate limits, revisions and server registry validation.
- [x] Publish RuleManager reload only after successful mutation; broadcast updates without erasing dirty drafts.
### 3. Working forms and management
- [x] Build reusable schema field model and client draft model with typed controls, unset/reset semantics, array/map operations, item/entity pickers.
- [x] Implement create, duplicate, delete confirmation, save, discard and leave-dirty confirmation.
- [x] Expose all supported Spawn, Attributes, Equipment, Loot, AI and naming/damage settings.
### 4. Browser and polish
- [x] Registry browser (vanilla/modded), counts, search, filters, sort, responsive navigation and selection retained across categories.
- [x] Use dark panels, status colors, tooltips, clipping, keyboard focus and explicit loading/error states.
### 5. Verification and delivery
- [x] Compile, test, build; dedicated server smoke test; independent review and fixes.
- [x] Update docs/manual checklist and commit feature branch; build exact installable JAR.
- [ ] Push feature branch to GitHub (blocked by GitHub write authorization).

## Execution ledger
Ruling: user explicitly requests continuing through implementation without a design-document stop; execute inline using this plan and their supplied spec.
Ruling: stable text/icon registry entries replace entity previews; avoids instantiating modded entities and preserves dedicated-server safety.

### Results
All six user-specified stages implemented inline. Java 25 compilation, full build and 54 automated tests pass; isolated dedicated-server command smoke test passes. Independent review identified compact creation, equipment clearing, file diagnostics, raw-value rendering, escaped custom-attribute paths, unrestricted rule counts and list-scroll retention; these were fixed. The path regression was reproduced failing and now passes.

Ruling: editor writes require atomic replacement; if unsupported, fail with the original intact instead of using a risky non-atomic fallback.
Ruling: entityTags/biomeTags are preserved but not presented as working settings because the current matcher ignores them. Full-file malformed JSON and oversized rule objects require manual editing. Those limitations and unperformed visual/gameplay/multiplayer checks are explicit in docs/VISUAL_EDITOR.md and docs/DASHBOARD_TESTING.md.
Ruling: use the existing checkout on the requested new branch; main remains untouched. Reuse the ignored local TEMP build override because OneDrive prevents reliable Gradle output cleanup.

Delivery: feature implementation committed as 0bffc33. Git push rejected with HTTP 403 because local Git authenticates as yukonwarrior without write permission. The connected account is Lsaint06, but GitHub create-tree also returned 403 Resource not accessible by integration. No remote branch was created. Publishing requires fixing Git credentials or granting the installed integration repository Contents write permission. No main changes or merges occurred.
