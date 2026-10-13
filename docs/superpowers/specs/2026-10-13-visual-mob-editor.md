# Mob Spawning Real v1.4 — Complete Visual Mob Configuration Editor

## Project Overview

You are continuing development of **Mob Spawning Real**, an existing Minecraft 26.2 NeoForge mod using Java 25 and NeoForge 26.2.0.7-beta.

Repository: `Lsaint06/MobSpawningReal`

The current v1.3 implementation provides a working in-game administrative dashboard, `/msrgui`, featuring searchable rules, persistent runtime toggles, read-only categories, server synchronization, validation diagnostics, and administrator permissions.

The current dashboard compiles, passes 38 automated tests, and has successfully started on a dedicated server.

**Your mission is to transform the dashboard into a professional, intuitive, fully interactive visual mob configuration editor.**

This is a major feature release, not a minor cosmetic update.

### Critical development requirements

- Create a new branch named `feature/visual-mob-editor-v1.4` from the existing dashboard implementation, not the older main branch.
- Do not modify or merge into main.
- Preserve every working feature from v1.3.
- Read and understand the existing architecture before writing code.
- Inspect the existing JSON rule schema, validation, runtime systems, GUI, networking, and tests.
- Use the actual Minecraft 26.2 and NeoForge APIs. Do not guess method names.
- Implement working features rather than mockups or nonfunctional placeholder controls.
- Continue working through implementation, compilation, testing, and fixes without stopping at a design document.
- If a feature cannot be safely implemented, document it clearly rather than silently simulating functionality.

---

# 1. Overall Vision

I want the GUI to feel like a professionally developed Minecraft mod, not a developer debugging menu.

Imagine the quality of a polished Minecraft mod configuration screen combined with a modern management dashboard.

The user should be able to:

1. Open `/msrgui`.
2. Browse available Minecraft mobs.
3. Select a mob or an existing rule.
4. See its current configuration in clearly organized categories.
5. Edit settings using intuitive controls.
6. Create new rules without writing JSON.
7. Save changes directly to the server.
8. See changes reflected in gameplay without restarting where technically possible.

The interface must be understandable to someone who has never written a JSON configuration file.

**Prioritize functional editing first, then polish the GUI around those controls.**

---

# 2. Redesigned GUI Layout

Create a responsive three-region interface for sufficiently large windows.

## Left sidebar — Navigation

Include:

- Dashboard
- Mob Browser
- Rules
- Spawn Settings
- Attributes
- Equipment
- Loot
- AI Behaviors
- Diagnostics

Use compact icons with readable labels.

Clearly indicate the current section.

Do not let sidebar navigation erase unsaved changes without warning.

## Center panel — Mob and rule selection

Display a searchable list with useful information.

For mobs:

- Friendly mob name, such as Zombie
- Registry ID, such as `minecraft:zombie`
- Vanilla or modded origin when determinable
- Number of configured rules
- Enabled or disabled status

For rules:

- Rule name or ID
- Target entities
- Priority
- Enabled state
- Source configuration file
- Validation status

Add sorting and filtering where useful.

Examples:

- All mobs
- Configured mobs
- Unconfigured mobs
- Enabled rules
- Disabled rules
- Invalid rules

## Right panel — Configuration editor

Display settings relevant to the selected mob or rule.

Use grouped cards or sections with clear headings and brief descriptions.

Avoid overwhelming the user with raw configuration data.

For example:

**Zombie — Spawn Settings**

- Allow Spawning: Enabled
- Spawn Rate Multiplier: 2.0×
- Maximum Per Player: 30
- Minimum Light: 0
- Maximum Light: 7
- Dimension: Overworld

Use actual interactive controls rather than displaying these values as text.

For smaller Minecraft windows, collapse or reorganize panels so the screen remains usable.

---

# 3. Visual Mob Browser

Implement a searchable mob browser using registered entity types.

Include vanilla Minecraft mobs and compatible registered modded mob types.

Do not hardcode a list containing only vanilla mobs.

Use actual registered entity IDs and proper display names.

Support selecting mobs such as:

- Zombie
- Skeleton
- Creeper
- Spider
- Enderman
- Cow
- Pig
- Iron Golem
- Warden
- Registered modded mobs

Provide useful selection feedback.

If technically practical, include small mob icons or rendered entity previews. Entity rendering must be client-safe, efficiently cached where appropriate, and must not instantiate unsafe entities or disrupt world state.

Do not allow decorative rendering to block the working editor.

A reliable text-and-icon fallback is acceptable.

When selecting an unconfigured mob, provide a clear **Create Rule** action.

---

# 4. Rule Creation and Management

Implement functional rule management.

Required operations:

- Create Rule
- Edit Rule
- Duplicate Rule
- Delete Rule
- Enable Rule
- Disable Rule
- Reset Override
- Save Changes
- Discard Changes

Creating a rule should present a guided form.

For example:

Rule Name: Super Zombies

Target Mob: Zombie

Priority: 100

Enabled: Yes

Provide an explanation of priority and stop-processing behavior.

Generate valid unique rule identifiers.

Validate identifiers against existing rules.

For deletion, require confirmation.

Avoid accidental deletion of unrelated rules stored in the same JSON file.

Never directly mutate user files from the client.

All modifications must be authorized and applied by the server.

---

# 5. Spawn Settings Editor

Build a real editing interface for the existing spawn-rule schema.

Inspect the actual `SpawnActions`, `ActiveSpawnActions`, `SpawnArea`, and `Conditions` classes before implementing controls.

Support every existing spawn-related field that can be edited safely.

Depending on the existing schema, include controls for:

- Allow or block spawning
- Spawn rate multiplier
- Maximum mobs per player
- Spawn limits
- Active spawning
- Spawn attempts and intervals
- Dimension restrictions
- Biome restrictions
- Light restrictions
- Height restrictions
- Time restrictions
- Weather restrictions
- Player-distance conditions
- Spawn-reason conditions
- Spawn areas and radius settings

Use numeric inputs, sliders, toggles, and selection menus as appropriate.

Preserve the distinction between fields that are unset and fields explicitly set to zero or false.

Never invent configuration properties that the runtime does not recognize.

If any field requires more advanced editing, implement an expandable advanced section rather than dropping it.

---

# 6. Attributes Editor

Provide editable mob attributes using the existing rule architecture.

Support applicable fields such as:

- Maximum health
- Attack damage
- Movement speed
- Armor
- Armor toughness
- Knockback resistance
- Follow range
- Scale
- Existing supported custom attributes

Use validated numeric controls with appropriate bounds.

Display helpful units where meaningful.

For example:

Health: 40 HP

Movement Speed: 0.30

Attack Damage: 8

Include tooltips explaining what settings do.

Do not assume changing an attribute will automatically update existing mobs. Clearly distinguish changes affecting newly spawned mobs from changes that can be applied live.

---

# 7. Equipment Editor

Implement a visual equipment editor using the existing equipment schema.

Support applicable equipment slots:

- Main hand
- Offhand
- Helmet
- Chestplate
- Leggings
- Boots

Allow selecting items from registered Minecraft items.

Include modded items where supported.

Provide search by display name or registry ID.

Preserve existing supported fields such as equipment chances and other schema-defined properties.

Avoid generating invalid item IDs.

If the mod's equipment schema supports weighted choices, implement an appropriate editor.

---

# 8. Loot Editor

Create a visual interface for modifying mob drops.

Display configured drops with:

- Item name
- Item registry ID
- Quantity or range
- Drop chance
- Any additional supported loot properties

Allow:

- Adding drops
- Editing drops
- Removing drops

Use existing `LootActions` and `LootEntry` structures.

Use a searchable item picker instead of requiring users to memorize registry IDs.

Show probabilities in human-readable form without changing the stored semantics.

Validate all edited values.

---

# 9. AI Behavior Editor

Build controls for existing supported AI and behavior functionality.

Inspect the actual `BehaviorActions` schema and existing behavior controllers.

Represent supported features with switches, dropdowns, or target lists.

Potential examples include:

- Always hostile
- Passive behavior
- Target entity selection
- Climbing
- Block breaking
- Pillar building
- Environmental interaction
- Other already implemented behavior actions

Do not expose unsupported abilities as if they were implemented.

Explain that changing some behavior settings may only affect newly spawned entities or entities reevaluated by the current runtime.

Use tooltips or descriptions for complicated settings.

---

# 10. Configuration Persistence

This is one of the most important parts of the update.

The v1.3 dashboard already contains transactional persistence for runtime toggles.

Extend the safe-saving architecture to support actual rule editing.

### Required properties

- Rule edits are saved to the server's JSON configuration files.
- Existing unrelated rules remain intact.
- Existing unknown supported JSON fields are preserved where possible.
- Unmodified rules are not needlessly rewritten.
- Rule editing is validated before saving.
- Duplicate IDs are rejected.
- Invalid values are rejected with readable errors.
- Failed writes leave existing files unchanged.
- Saving a rule must not silently overwrite another administrator's recent changes.
- Create backups before destructive operations.
- Use safe temporary-file writes and atomic replacement when supported.
- Preserve valid JSON formatting.
- Keep runtime toggles in `runtime-state.json`, separate from edited rule definitions.
- Update the runtime rule snapshot after successful saves.

Build a clear distinction between draft changes and saved server state.

Use an explicit **Save Changes** button, with appropriate feedback:

- Unsaved Changes
- Saving…
- Saved Successfully
- Validation Error
- Save Failed
- Configuration Changed on Server

A failed save must never display success.

---

# 11. Multiplayer and Permissions

The server must remain authoritative.

The client should never directly access or modify server configuration files.

Reuse or extend the existing custom payload networking architecture.

Requirements:

- Administrator permission checks for every configuration request.
- No unauthorized configuration disclosure.
- Support dedicated servers.
- Synchronize changes between connected administrators.
- Reject stale edits.
- Refresh affected views after successful saves.
- Bound packet sizes and collection lengths.
- Paginate large mob/item/rule lists as necessary.
- Do not trust client-provided registry IDs, values, paths, or JSON.
- Rate-limit expensive requests.
- Correctly handle disconnects, world changes, permission loss, and screen closing.

Preserve all functioning v1.3 networking behaviors.

Clients must have a compatible installed mod version.

---

# 12. Visual Polish

Once the editor functions correctly, improve the appearance.

Use a cohesive visual system.

### Theme

- Charcoal and near-black backgrounds
- Subtle borders and panel separation
- Restrained emerald-green accents
- Amber for warnings and unsaved changes
- Red for invalid settings and errors
- Gray for secondary information
- Minecraft-compatible text rendering

### Interaction quality

- Smooth list scrolling
- Clear selection states
- Hover highlights
- Helpful tooltips
- Accessible focus indication
- Consistent button sizing
- Keyboard navigation where practical
- Responsive layouts
- Clean spacing
- Proper clipping
- Long text truncation with full details accessible
- Clear disabled and loading states

Make the interface feel carefully designed, not crowded.

Test layout at multiple Minecraft GUI scales and window sizes.

Avoid elaborate animation if it compromises performance or readability.

---

# 13. Quality-of-Life Features

Include where feasible:

- Search that updates while typing
- Recently selected mob retained while navigating sections
- Unsaved-changes indicator
- Confirmation when leaving with unsaved edits
- Reset individual setting to default/unset
- Reset entire draft to saved values
- Helpful explanations of advanced settings
- Clear success/error messages
- A summary of active rules affecting the selected mob

Avoid repeatedly fetching unchanged registry data.

Keep the interface responsive with large registries and many configured rules.

---

# 14. Testing Requirements

Implement meaningful automated tests covering:

- Rule creation
- Rule duplication
- Rule deletion
- Rule editing
- Unique ID validation
- Invalid values
- Malformed JSON
- Saving and reloading
- Backup and recovery behavior
- Failed file writes
- Concurrent edits
- Stale revision rejection
- Permission checks
- Registry ID validation
- Preservation of unrelated rules
- Persistence after restart
- Packet size limits
- Server-side request validation

Retain and run all existing v1.3 tests.

Run Gradle compilation, automated tests, and full build under Java 25.

Resolve compilation failures against the actual Minecraft and NeoForge 26.2 APIs.

Test dedicated-server startup where possible.

Do not claim visual or gameplay verification unless those tests were actually performed.

---

# 15. Performance Requirements

Avoid expensive operations on every rendered frame.

Do not repeatedly scan the entire entity or item registry during rendering.

Cache stable registry metadata appropriately.

Keep the mod lightweight enough for dedicated servers.

Avoid unnecessary disk writes while dragging sliders.

Use draft edits and save only when explicitly requested.

Ensure large modpacks with many registered entities remain usable.

---

# 16. Documentation

Update the documentation to cover:

- How to open the GUI
- How to create a mob rule
- How to edit an existing rule
- How to save configuration
- How to reset overrides
- How runtime toggles differ from rule definitions
- What changes apply immediately
- What changes apply only to newly spawned mobs
- Required permissions
- Dedicated-server setup
- Backup behavior
- Troubleshooting

Create an updated manual testing checklist for Minecraft.

---

# 17. Versioning and Delivery

Target version: **Mob Spawning Real 1.4.0**

Work exclusively on `feature/visual-mob-editor-v1.4` based on the current v1.3 dashboard branch.

Do not merge into main automatically.

When finished:

1. Run all available tests.
2. Build the mod JAR.
3. Run a dedicated-server startup check where practical.
4. Review the changes for regressions.
5. Commit the completed work to the feature branch.
6. Push the branch to GitHub.
7. Report what is fully implemented and what remains incomplete.
8. Identify any remaining manual Minecraft tests.
9. Provide the exact local path to the compiled JAR.

Do not report success if compilation or tests fail.

---

# 18. Development Strategy

This is a substantial release. Work methodically.

Start by inspecting the current repository and writing an implementation plan.

Then implement in logical stages:

**Stage 1 — Safe rule editing infrastructure**

Rule identity, validation, saving, revisions, concurrency, and tests.

**Stage 2 — Rule creation and management**

Create, duplicate, delete, save, and discard functionality.

**Stage 3 — Editable settings**

Spawn, attributes, equipment, loot, and AI controls based on the existing schema.

**Stage 4 — Visual mob browser**

Searchable vanilla and modded mob selection, and item registry pickers.

**Stage 5 — Interface polish**

Improve layout, spacing, icons, responsiveness, tooltips, and navigation.

**Stage 6 — Verification**

Compile, run tests, review, test dedicated-server startup, fix regressions, and prepare the JAR.

Do not sacrifice correctness for visual complexity.

If time or tooling constraints prevent completion, finish the highest-priority working subset, commit it clearly as partial progress, and identify exactly what remains.

## Final expectation

I want Mob Spawning Real to evolve from a configuration-file mod into a visual Minecraft mob-management system.

The finished interface should make complex mob configuration approachable, without requiring the user to manually write JSON.

**Make it reliable, powerful, easy to navigate, and visually polished. Preserve the existing mod's functionality throughout development.**