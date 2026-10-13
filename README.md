# Mob Spawning Real

Universal rule-driven mob spawning, attributes, equipment, loot and AI control for Minecraft **26.2**, NeoForge **26.2.0.7-beta**, and **Java 25**.

## Visual mob editor (1.4.0)

Install the same mod version on the client and server. Join as an administrator (or enable cheats in single-player) and run **`/msrgui`**.

- Search rules by ID, mob, filename or validation error; scroll and select a rule.
- Toggle rules individually or pause/resume all processing with **Mod: ON/OFF**.
- Browse registered vanilla and compatible modded mobs; search by translated name or registry ID and filter configured/unconfigured mobs.
- Create, duplicate, edit and delete individual rules. Changes remain in a draft until **Save Changes**; **Discard** restores saved values. Leaving a dirty draft requires confirmation.
- Edit Spawn Settings, Attributes, Equipment, Loot and AI Behaviors with numeric fields, toggles, searchable registry pickers, and settings pages for advanced fields. Blank/Unset differs from explicit zero or false.
- Use **Use JSON** to clear a selected rule's runtime override.
- **Reload** reads server rules and saved runtime state; **Refresh** fetches current server state.
- Changes become visible after server confirmation and are synchronized with other open dashboards, including changes from existing `/msr` commands.

Runtime switches are saved in `config/mobspawningreal/runtime-state.json`. Definition edits are saved under `config/mobspawningreal/rules/`; unknown properties and unrelated entries survive. Existing-file edits/deletions create exact original backups under `rules/backups/` and require atomic replacement support. Conflicting server revisions or changed file fingerprints reject the save and retain the draft. Unchanged definitions are not rewritten. Invalid entries remain visible in Diagnostics and cannot execute or be force-enabled.

Disabling processing stops future runtime actions. Attributes and equipment already applied to existing mobs are not undone. Global pause preserves individual rule settings.

See [visual editor guide](docs/VISUAL_EDITOR.md), [configuration reference](docs/CONFIG.md), and [testing checklist](docs/DASHBOARD_TESTING.md).

## Building

Set `JAVA_HOME` to a Java 25 JDK and run:

```powershell
.\gradlew.bat compileJava test build
```

The installable jar is produced in `build/libs/`. Automated tests cover rule loading, validation diagnostics, persistence, request policy, filtering, layout bounds, and snapshot paging. Live rendering and multiplayer interactions require Minecraft testing.
