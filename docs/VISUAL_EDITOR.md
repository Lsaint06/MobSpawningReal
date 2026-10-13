# Visual editor 1.4.0

Install the same 1.4.0 JAR on Minecraft 26.2 clients and the NeoForge 26.2.0.7-beta server, using Java 25. Remove older copies from both mods folders. Administrators (the existing vanilla admin command permission) can open `/msrgui`; single-player requires cheats. Non-admin requests receive no configuration data. The server checks permissions on every request and closes sessions after permission loss or disconnect.

## Creating and editing

Open Mob Browser, search a friendly name or registry ID, and select a mob. Entries come from server registries, including compatible modded living types with registered default attributes; no entities are instantiated for previews. Player and armor-stand types are excluded. Configured counts include unrestricted rules; these are potential matches, since dimensions, other conditions, priority and stop-processing still apply. Hover the selected rule title for the enabled-rule summary for the selected mob.

Choose Create Rule for an unconfigured mob, or New for another rule. On compact screens use Create Rule/Edit selected rule to switch panels. The section chooser opens navigation; rule-management actions are pages in Rules/Dashboard. Generated IDs are unique; edit the ID in Rules if desired. Higher priority wins and lower-priority rules fill unset fields. Stop Processing stops evaluation of lower-priority matching rules.

Use the category navigation to edit the retained draft. Numeric inputs require whole numbers or finite decimal numbers as appropriate. Probabilities are stored from 0 to 1; the tooltip explains that 0.25 is 25%. Lists accept comma-separated entries; Pick adds choices from the server registry without memorizing IDs. Dimension, biome, structure, spawn-reason, weather, item, block, entity, and custom-attribute pickers are supported. Values are validated again on the server, including registry membership and registered attribute ranges.

Spawn pages include conditions, limits, active-spawn intervals/attempts, absolute area coordinates and loaded-chunk/space/ground requirements. Attributes include health, attack damage, speed, range, knockback resistance, scale and custom attributes. Armor/armor toughness use Add custom attribute. Equipment pages cover main/offhand and all four armor slots, count, chance and drop-on-death; Clear removes the whole slot. Loot supports vanilla-drop clearing, adding/removing rows, quantity ranges and probabilities. AI exposes existing hostility, target lists, leash, sunlight, climbing, breaking, pillaring, planting, eating and cooldown settings. Naming and outgoing damage are in Rules/Dashboard.

Blank or Unset removes an explicit field; zero and false remain explicit values. Unset is not necessarily the same as resetting to a numerical default: other matching rules can fill gaps. Discard resets the entire draft to saved values. Navigation between categories keeps the draft. Switching rule/mob, creating/duplicating, or closing with unsaved edits asks whether to discard. Duplicate copies the saved definition into a new draft with a unique ID; no file is created until saved. Delete requires confirmation and removes only the selected array entry.

## Saving, conflicts and overrides

Save Changes validates the draft, sends it to the server and reports success only after persistence succeeds. New rules receive separate `gui-<UUID>.json` files. Editing a shared source file preserves other entries and unknown properties, although that changed file is pretty-printed. Unmodified files and no-op saves retain their exact bytes. No write occurs while typing.

Every edit compares both the authoritative dashboard revision and a SHA-256 fingerprint of the whole rule directory. Another administrator's save, a command change, or an external JSON edit rejects a stale save. A dirty draft is retained; Discard loads the current server version. This version does not merge conflicting drafts automatically. Copy important draft values before discarding, then reapply them to the newer definition. Refresh retains drafts; Reload warns before discarding a dirty draft and reads server rules/runtime state.

Enable/Disable and Use JSON operate on runtime overrides, immediately persisted separately in `runtime-state.json`. The definition's Enabled field only changes when its draft is saved. Runtime overrides take precedence. Editing a definition never resets its runtime override. Renaming an ID creates a new rule identity; old overrides remain retained rather than transferred silently.

For changed existing files, exact original backups are written to `rules/backups/<filename>.<UUID>.bak`. Backups do not load as rules and are never overwritten or automatically pruned. Save writes a temporary file, forces its bytes to disk, rechecks conflicts and atomically replaces the original. A failed validation, backup, or write reports an error and does not publish new live rules. Unsupported atomic replacement fails safely instead of risking a destructive fallback. To restore, stop the server, copy the desired backup over its original filename in rules/, then restart (or restore while stopped and subsequently reload). Keep the backup for audit/recovery.

## Gameplay timing

Successful saves reload RuleManager immediately. Spawn conditions/rates and active spawning affect subsequent spawn checks. Attributes, naming and equipment are applied at spawn/join and are not retroactively reapplied to all existing mobs. Loot, combat and AI controllers consult current definitions for rule IDs already tagged on existing mobs; those settings can change on subsequent runtime events. New targeting conditions do not automatically retag existing entities. Deleting/renaming/disabling a rule stops its later runtime actions; previously applied equipment/attributes are not undone. Global pause preserves individual settings.

## Limits and troubleshooting

Editable definitions are limited to 8,000 UTF-16 characters, individual files to 2 MB, the catalog to 16 MB/512 files/4,096 editable objects, lists/maps to 256 entries and nesting to 16 levels. Paged responses are bounded to approximately 4 million characters. Oversized definitions remain visible in diagnostics/summaries but require file editing. Registry metadata is cached per server and sent initially or on explicit Refresh. Expensive save requests are rate-limited. Compatible installed clients are required by protocol version.

Malformed whole files and non-object array entries remain visible in Diagnostics but require manual repair; the editor will not guess how to rewrite them. Structurally valid invalid rule objects can be corrected through forms. Entity/biome tag properties are preserved but not exposed as working controls because the existing runtime matcher does not evaluate them. Weighted equipment is not offered because the schema has one equipment entry per slot. Entity model previews, animations and automatic conflict merging are not implemented; the browser uses text and status icons.

If saving fails, read the full footer message by hovering it, check server file/backup-directory permissions, repair invalid registry IDs/values, or refresh after a conflict. A malformed runtime-state file blocks runtime-switch saves until repaired and reloaded. For a stalled response, wait for the ten-second timeout and use Refresh from the browse view. Keep test backups when verifying gameplay.

Compilation and automated tests do not verify in-game rendering, GUI scale behavior, connected-client synchronization, or actual gameplay effects. Follow the manual checklist before treating this release as visually/gameplay verified.
