# Dashboard testing

## Installation

Use Java 25, Minecraft 26.2 and NeoForge 26.2.0.7-beta. Put `mobspawningreal-1.3.0.jar` into both client and dedicated-server `mods` folders, removing the older mod jar. Keep your existing configuration. Start a test world/server and run `/msrgui` with administrator permissions. No resource pack is needed.

## Manual Minecraft acceptance checks

These checks require a running game; compilation and JUnit tests do not establish that they pass.

1. Single-player: enable cheats, run `/msrgui`, and check opening, Escape/Done closing, switch labels, pending/saved feedback, button sounds and keyboard focus. Tab reaches controls; arrows select rules; Enter toggles the selected rule. The simulation continues while the dashboard is open.
2. Search by a rule ID, `minecraft:zombie`, a filename and an error phrase. Check empty results, mouse wheel, scrollbar dragging, long names, rule selection and all five summary categories. At narrow GUI widths use Rules/Details navigation. Resize the window and change GUI scale; verify panels, text, scrolling and controls stay usable.
3. Toggle a valid rule, disable the global switch, and check the individual selection is retained with Paused/Disabled status. Change a rule while globally paused, then resume. Use JSON restores its configured enabled state.
4. Close and restart the server/world. Confirm saved state is restored. Compare original rule JSON before and after; it should be unchanged. Only `runtime-state.json` contains overrides.
5. With a configuration backup in a test instance, introduce an invalid rule, malformed JSON file, missing ID, and duplicate IDs. Reload; check red Invalid rows and diagnostics, inactive mutation controls, and valid rules still running. Repair the files and reload again.
6. Test saving with the runtime-state directory read-only. A failed save must show an error and leave the switch unchanged. Repair permissions and retry. Test malformed runtime state; the file must be preserved, saving blocked, and repair plus Reload should recover.
7. Dedicated server: connect two administrators with the same mod jar. Both open the dashboard. Change a rule/global state in one client; the other should update. Also change state through `/msr` commands. Simultaneous stale edits must be rejected with current server state rather than overwriting an administrator's newer decision.
8. Join as a non-admin: `/msrgui` must be unavailable. Revoke an administrator's permission while their screen is open; the screen should close and further requests must not mutate configuration. Try `/msrgui` from the console; expect a player-only message.
9. Disconnect/reconnect or switch worlds with the screen open. Check that old snapshots never reopen it and that the next session displays the current server's state.
10. Use many rules and large summaries to exercise paging and scrolling. Repeat Reload quickly; expect a brief rate-limit message. Interrupt a response; after ten seconds Refresh should recover interaction.

## Automated verification

Run `gradlew.bat compileJava test build` with Java 25. Tests use temporary directories and do not edit real rule files. Their scope includes JSON loading and source identity, duplicate rejection, strict state parsing, persistence round trips/failure, global-independent overrides, permission/revision/identity policy, search, screen layout bounds, complete snapshot assembly, and Unicode-safe paging. Dedicated-server startup and actual client-server/game-thread behavior need separate runtime verification.

For this development checkout, OneDrive marked generated output directories read-only, preventing repeated Gradle cleanup. Verification used an ignored local Gradle init script to direct build output to a temporary directory; normal project build configuration was preserved.

Development verification on October 13, 2026: `compileJava`, `test`, and `build` succeeded with **38 tests, zero failures**. An isolated localhost dedicated server loaded the mod, reached `Done`, executed global disable/enable, a persistent rule override, list and reload commands, rejected console `/msrgui` with its player-only message, and stopped cleanly. This validates server bootstrap and command integration; it does not establish live GUI or two-client behavior. The automated console harness waits until the world is loaded before submitting commands.
