# Visual editor testing (1.4.0)

Use Java 25, Minecraft 26.2 and NeoForge 26.2.0.7-beta. Install mobspawningreal-1.4.0.jar on client and server, removing older copies. Back up the test config. No resource pack is required.

## Manual Minecraft acceptance checklist

These checks are not established by automated tests. Run them in a disposable single-player world, then on a dedicated server with two administrators.

1. Open /msrgui with cheats/admin permissions. Check buttons, focus, tooltips, scrolling/dragging, arrow navigation, long text, status colors, loading, Escape/Done and continued world simulation.
2. Test scaled GUI sizes 320x240, 427x240, 640x360, 800x360 and 1280x720 where feasible. Change GUI scale while open. Compact navigation must expose every section, creation for an unconfigured mob, save/discard and rule management. Wide screens show three regions. Check clipping and modal overlays.
3. Search vanilla/modded mob names and IDs. Check configured/unconfigured and unrestricted-rule counts, filters/sort, category selection retention, and scroll position after row selection. Verify text/status fallback without entity instantiation.
4. Select an unconfigured mob, Create Rule, edit ID/priority/enabled/stop-processing and Save Changes. Check the new gui-*.json and the other administrator's view. Duplicate/save, then confirm deletion of only the copy and verify its exact backup.
5. Edit every spawn/condition/active-spawn field across settings pages. Pick dimensions, biomes, structures, weather and reasons. Compare unset versus explicit 0/false in JSON. Check actual spawning with low limits in a disposable world.
6. Edit health, damage, speed, scale, armor/custom attributes and a modded attribute ID containing a slash. Test registered bounds and invalid numbers. Check new mobs; do not expect attributes to retroactively update all existing mobs.
7. Pick vanilla/modded items for all equipment slots. Edit count/chance/drop-on-death, clear a whole slot, save and verify new applicable mobs.
8. Add/edit/remove loot rows; check ranges, 25% chance stored as 0.25 and vanilla-drop clearing. Verify probabilistic drops across many mobs and preservation of other rows.
9. Test supported AI targeting, hostility, climbing, breaking/pillaring/planting/eating and cooldowns in safe test environments. Check block pickers and behavior changes on subsequent events for tagged mobs.
10. Draft a change and navigate categories. Switching rule/mob, creating/duplicating, reloading or closing must warn before discarding. Cancel retains values; confirm/Discard restores saved values. Typing must not write files.
11. Save competing drafts from two administrators. The stale save must fail with its draft retained. Repeat after runtime commands and external JSON edits. Discard/review/reapply; automatic merging is not provided.
12. Test rule/global runtime toggles and Use JSON. Overrides must remain separate and take precedence over definition Enabled. Restart and confirm persistence. Pausing must not undo applied equipment/attributes.
13. Test read-only rule/backup paths and failed writes. Never show success on failure; preserve original bytes and live rules. Restore an exact backup while stopped. Test malformed runtime-state recovery independently.
14. Introduce duplicate/missing IDs, invalid registry IDs and malformed types (id:{}, enabled:{}, entities:[null]), whole files and non-object entries. Diagnostics must remain available; invalid rules must not run. Repair editable objects; repair whole-file errors manually. Oversized objects must fail clearly.
15. Non-admin /msrgui must be unavailable. Revoke permission while open: screen closes and further data/mutations are denied. Console /msrgui must show player-only feedback.
16. Disconnect, change worlds/servers and reopen. Late responses must not reopen closed screens or discard dirty drafts. Interrupt paging; ten-second timeout and explicit Refresh must recover controls and registry data.
17. Exercise a large modpack/catalog and rapid requests. Check rate limits, unloaded chunks, spawn caps, and absence of per-frame registry scans/disk writes.

## Automated verification performed

October 13, 2026: Java 25 compileJava, test and build passed with **55 tests, zero failures**, including all 38 v1.3 tests. New tests cover targeted CRUD, duplicate IDs including oversized siblings, strict JSON/types, unique draft IDs, unknown/unrelated preservation, failed backup writes, exact backup recovery, no-op saves, restart persistence, competing drafts, validator rejection, list/path/unset semantics and real schema coverage. Existing tests cover pure permission/revision policy and bounded Unicode paging. They do not prove connected-client permissions or gameplay.

An isolated dedicated server loaded 1.4.0, reached a loaded world, executed status/global disable-enable/rule override/list/reload, rejected console /msrgui, and stopped cleanly. This validates bootstrap and retained commands, not live editor sessions or picker interaction.

OneDrive locked generated outputs in this checkout. An ignored init script redirected Gradle outputs to the temporary mobspawningreal-dashboard-build directory; normal project build settings remain unchanged. The installable JAR was copied to build/libs/mobspawningreal-1.4.0.jar. Live GUI rendering, multiplayer interaction, and gameplay are still unverified.
