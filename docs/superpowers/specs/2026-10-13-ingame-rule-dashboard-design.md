# In-game rule dashboard design

## Goal and scope

Administrators configure Mob Spawning Real inside Minecraft 26.2 with NeoForge 26.2.0.7-beta and Java 25. Version one opens through `/msrgui` and supports searching configured rules, inspecting their status, changing individual and global enabled states, and reloading configuration. Changes survive server restarts without rewriting rule JSON. Work remains on `feature/ingame-rule-dashboard`; do not modify or merge into main.

The conversational design was approved on October 13, 2026. This specification formalizes that design for review before implementation planning.

## Existing systems

`RuleManager` loads sorted JSON arrays from `config/mobspawningreal/rules`, normalizes rule objects, validates them, and publishes valid rules in priority order. It currently discards invalid rule objects and returns load errors only to the reload caller. `RuntimeControl` maintains global state and per-ID overrides in `config/mobspawningreal/runtime-state.json`. Runtime matching, active spawning, and spawn, combat, loot, leash, and AI event hooks already use this state.

Attributes, equipment, names, and persistent rule markers are applied to entities at spawn or join. Disabling processing does not reverse already applied attributes or equipment. The GUI must explain this behavior without changing it.

## Architecture and boundaries

Use a native Minecraft `Screen` and NeoForge custom payloads. Keep client screen and rendering classes in a client package and register their handlers only on the client distribution. Common payload models and server handlers must load on a dedicated server without resolving client classes.

Extend `RuleManager` with an immutable load snapshot containing valid runtime rules, dashboard entries, and diagnostics, published together after reload. The GUI consumes server-generated summaries; it never reads client configuration files or executes mob rules locally. Keep existing runtime and command consumers working.

Reuse `RuntimeControl` for every mutation, including existing commands. Make persistence report failure, preserve the previous file on failed writes, and leave in-memory state unchanged when a requested mutation cannot be saved. Write a temporary file in the same directory and replace the state file atomically where supported, with a replacement fallback. Preserve existing runtime-state format and rule override semantics.

Introduce a server dashboard service that handles authorized subscriptions, snapshots, mutation requests, revisions, and feedback. Separate screen layout, rule-list rows, toggle controls, category presentation, and transport models so future editors can be added without restructuring the screen.

## User interface

The screen uses charcoal panels, subtle borders, Minecraft's font, readable spacing, and restrained green, gray, and red accents. Labels accompany colors: Enabled, Disabled, Invalid, and Paused by global switch. The header includes the mod title and a global switch; reload and close controls remain visible.

An edit box filters rule ID, entity IDs, source filename, and diagnostics case-insensitively. A scrollable list shows rule ID, priority, source, targets, status, and an enable/disable switch. Keep selection and search across updates; preserve scroll position where possible and clamp it after filtering. Include empty-list and no-search-results messages. Support mouse scrolling, keyboard focus, and Escape to close; the dashboard does not pause the server simulation.

Selecting a rule displays read-only summaries under these categories: Spawn Settings, Attributes, Equipment, Loot, and AI Behaviors. These are reusable category components, prepared for future editors. Version one does not create rules or edit action values. Invalid rows show readable validation errors and have no active mutation control.

Use available scaled screen dimensions for layout. At wide sizes, show list and details side by side; at narrow sizes, use a compact list with a separately selectable details view. Clip scrollable content, wrap diagnostics, and shorten long row labels with full text available in details. Verify at multiple window sizes and GUI scales.

Individual switches represent JSON enabled state plus runtime override independently of global enable. When global processing is disabled, keep individual settings visible and editable and mark otherwise enabled rules as paused. Display whether state comes from JSON or an override. Include a reset-to-JSON action for a selected valid rule so overrides are reversible.

## Diagnostics and identity

Retain validation failures with source filename and array index. Malformed files become diagnostic entries even when no rule ID can be parsed. Null entries, missing or blank IDs, and duplicate IDs must produce diagnostics rather than ambiguous controls. Duplicate IDs are invalid for all conflicting entries, because overrides address rules by ID. Validate raw identity before normalization can replace a missing ID with `unnamed`.

Invalid entries never reach the runtime rule engine and cannot be force-enabled from the GUI. Only IDs from the current valid server snapshot are accepted for rule mutations. Diagnostic identity is separate from a mutable rule ID. Reload reflects repairs or newly invalid rules immediately.

## Permissions, networking, and concurrency

Register `/msrgui` using the same `Commands.LEVEL_ADMINS` permission predicate as existing administration commands. Require a player source and return a clear console message otherwise. Check permission again on every serverbound request, including read, reload, mutation, and subscription operations. Permission loss closes or disables the dashboard and removes its subscription; unauthorized requests disclose no configuration.

Handle requests on the server game thread and apply snapshots on the client game thread. Use a versioned protocol with bounded decoding for strings and collections. Page snapshots if the full configured list would exceed safe payload size; the UI only accepts a complete revision assembled from matching pages. Do not accept client-provided paths, JSON, entity objects, or authoritative state.

Opening subscribes the player and sends a snapshot. Each request carries an expected revision and requested state rather than a blind toggle. Reject stale mutation/reload requests with explanatory feedback and the current snapshot. Increment revisions when published rules or runtime settings change, including changes made through existing commands. Broadcast snapshots to authorized subscribed players. Rate-limit expensive reload requests; reject repeated requests with readable feedback.

The screen disables conflicting actions while waiting for confirmation and shows pending, saved, reload-result, stale-state, permission, and persistence-error feedback. A timeout restores interaction and offers refresh; late responses cannot reopen a closed screen. Closing, disconnecting, switching worlds, and server shutdown clean up session state. Client snapshots remain distinct from shared static server configuration in single-player.

## Persistence and reload behavior

Rule toggles update only `runtime-state.json`; unknown existing override entries remain preserved. Reload reads server rule files and republishes rules and diagnostics without clearing overrides. Retain the current behavior of creating a disabled example only when its file is absent. Do not overwrite existing rule files. Report persistence failure as failure, without a success toast or a changed switch.

Keep per-server dashboard sessions and revision lifecycle isolated. Integrated-server restarts reload server state and cannot inherit a previous world's GUI session. Clients and server need the compatible mod version for the dashboard.

## Validation and delivery

Add meaningful automated tests for rule loading and diagnostics, duplicate identity rejection, overrides independent of global pause, persistence round trips and failed writes, snapshot/request policy, stale requests, and payload limits or pagination. Use temporary directories for configuration tests, preserving real user files.

Run the available Gradle compilation, tests, and build under Java 25. Resolve compilation errors against the actual Minecraft/NeoForge APIs. Record any dependency, network, Java, or graphics limitation explicitly; do not claim live Minecraft verification unless performed.

Manual acceptance covers single-player with cheats, a dedicated server with two administrators, non-admin access and permission loss, command-driven updates, reload after malformed JSON, failed state writes, restart persistence, search and scrolling with many/long rule names, window resize and GUI scale, disconnect/reconnect, and global pause while preserving rule settings. Deliver the built jar if compilation succeeds and document installation on both clients and server, `/msrgui`, and the remaining manual checks. Do not merge into main.
