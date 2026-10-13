# MobSpawningReal Design

## Goal
Build a server-first NeoForge 26.2 mod that gives administrators universal, rule-driven control over vanilla and modded mob spawning, post-spawn mutation, loot/equipment, hostility, and optional AI abilities without replacing vanilla behavior unless a rule explicitly asks it to.

## Platform
- Minecraft Java Edition 26.2
- NeoForge 26.2
- Dedicated-server safe; client installation should not be required for server-only features.
- Java version follows the NeoForge 26.2 toolchain used by the project template.
- Vanilla and modded entities/items/biomes/structures are addressed by registry IDs and tags rather than hard-coded enum lists.

## Configuration model
Rules are stored as JSON files under `config/mobspawningreal/rules/`. Each file may contain one rule or an array of rules. Files are loaded in lexical filename order and rules have an explicit integer `priority`. Higher priority evaluates first; equal priority preserves file/order deterministically.

A rule has:
- stable `id`
- `enabled`
- `priority`
- `match` conditions
- one or more action sections: `spawn`, `attributes`, `loot`, `equipment`, `behavior`, `abilities`, `name`

Unknown registry IDs are validation errors with file/line-path context. Invalid rules are disabled individually and logged; one bad rule must not crash the server or discard valid rules.

`/mobspawningreal reload` atomically validates and swaps the active rule set. If loading fails globally, the previous valid rule set remains active.

## Matching conditions
Rules can match:
- entity IDs and entity tags
- spawn reason/category
- dimensions and dimension tags when available
- biomes and biome tags
- structures and structure tags at the spawn position
- named coordinate zones (axis-aligned boxes) and direct min/max XYZ bounds
- Y range
- block light and sky light range
- day number and day ranges
- time of day ranges including wraparound ranges
- weather: clear, rain, thunder
- distance from nearest player, min/max
- player presence/count radius
- nearby mob/entity count radius
- chance/probability

All omitted conditions mean “match any.” Lists can support `include` and `exclude` semantics.

## Spawn controls
The spawn engine supports both interception and active spawn attempts.

Interception actions:
- allow, deny, or pass through
- acceptance probability / spawn-rate multiplier
- per-entity local cap
- per-player cap
- per-condition cap
- group-size override (min/max)
- min/max distance from players

Active spawning actions:
- optional configured additional spawn attempts per interval
- entity or weighted entity pool
- attempts per condition
- group size
- area/zone selection
- respects configured light/biome/dimension/structure/Y/player-distance conditions
- configurable whether to also respect vanilla placement/collision/spawn-rule checks
- configurable cap guards to prevent runaway spawning

Vanilla spawning remains unchanged when no rule matches.

## Spawn-rate semantics
`rate_multiplier` applies to matching natural spawn attempts:
- `0` denies matching attempts.
- `0 < x < 1` probabilistically accepts x fraction of attempts.
- `1` preserves vanilla attempt acceptance.
- `x > 1` preserves vanilla attempts and schedules bounded extra attempts using the active-spawn subsystem; it does not globally accelerate server ticks.

This avoids unsafe direct mutation of global spawning internals while still allowing increased rates.

## Limits
Rules may define:
- `max_nearby_same_entity`
- `max_nearby_matching_rule`
- `max_per_player`
- `count_radius`
- `spawn_count_min` / `spawn_count_max`

Counts are evaluated before extra spawn attempts and may also veto intercepted natural attempts if the rule requests hard-cap behavior.

## Entity mutation
On successful spawn/finalization, matched rules can mutate:
- max health and current-health policy
- attack damage
- movement speed
- follow range
- armor
- armor toughness
- knockback resistance
- attack knockback
- scale where a supported entity scale attribute/API exists
- persistent flag when explicitly configured

Values support absolute set, additive modifier, and multiplier modes where safe.

## Loot
Rules may:
- add weighted custom drops
- replace normal drops entirely
- clear selected vanilla drops by item/tag filters
- set min/max counts
- set drop chance
- set item NBT/components via a constrained data representation supported by 26.2 items

Loot is evaluated on entity death and only for matched entities. Player-kill-only conditions can be supported in loot rule predicates.

## Equipment
Rules can equip main hand, off hand, head, chest, legs, feet using registry IDs/tags and weighted alternatives. Each slot supports chance, count, durability/component data, and drop chance. Equipment can apply to vanilla and modded mobs that support living-entity equipment slots.

## Hostility and retaliation
Behavior rules support:
- force hostile toward players
- suppress hostility toward players
- force/suppress hostility toward selected entity IDs/tags
- retaliate when hurt
- group retaliation radius
- whether only the hurt mob retaliates or nearby matching mobs also acquire the attacker
- sunlight burning enabled/disabled override where the entity normally uses daylight burning
- leash allow/deny override where NeoForge exposes a leashability hook; otherwise a targeted compatibility hook may be used

These features are implemented with NeoForge events/goal APIs first and narrowly scoped mixins only when no stable hook exists.

## AI ability modules
Abilities are opt-in rule actions and are implemented as independent modules:
- `climb`: allows configured mobs to treat colliding horizontal surfaces as climbable movement assistance.
- `break_blocks`: periodically selects breakable blocks matching allow/deny block/tag filters within reach, with hardness limit, griefing gamerule policy, cooldown, and optional drops.
- `pillar_up`: places allowed configured blocks beneath/adjacent to the mob to reach a target when vertical pathing is blocked, with cooldown/max-height/material source policy.
- `plant_trees`: plants configured saplings/propagules on valid substrate near the mob, with cooldown and optional item-consumption policy.
- `plant_flowers`: plants configured flowers/plants on valid substrate.
- `cut_grass`: breaks grass/short vegetation matching tags, with cooldown and optional drops.
- `eat_plants`: consumes configured plant blocks/items in reach; optional heal amount or percentage.

Abilities never activate unless requested by a matching rule. Each ability has per-entity cooldowns to prevent tick-heavy scans.

## Names
Rules can set a literal or translatable custom name and choose visibility mode:
- never
- always
- when targeted/looked at where feasible with vanilla custom-name visibility constraints

If exact “looked at” visibility cannot be represented server-side without a client protocol addition, the complete implementation will use the closest server-safe behavior and document that limitation rather than silently requiring a client mod.

## Rule resolution
A spawn can match multiple rules. Resolution is field-based rather than “first rule wins” for all functionality:
- Spawn decision: first highest-priority rule that explicitly sets `allow`/`deny` wins.
- Scalar mutation properties: highest-priority explicit setting wins per property.
- Additive lists such as extra loot/abilities merge unless `replace=true` is set for that section.
- Exclusions at higher priority override lower-priority additions for the same target.

This makes specific overrides possible without duplicating complete rule definitions.

## Zones
Named zones live in `config/mobspawningreal/zones.json` and define `dimension`, `min`, and `max` coordinates. Rules can reference zones by name. Zones are immutable during a single evaluation and reload atomically with rules.

## Commands
- `/mobspawningreal reload`
- `/mobspawningreal validate`
- `/mobspawningreal debug on|off [player]`
- `/mobspawningreal explain entity <target>`: shows matching rules and applied actions for an entity.
- `/mobspawningreal explain position <x> <y> <z> [entity_id]`: shows spawn-relevant rule evaluation at a location.
- `/mobspawningreal rules list [filter]`

Commands require operator permission level appropriate for server administration.

## Debugging and observability
Debug mode emits concise, rate-limited explanations for denied/modified spawns. Validation reports exact file, rule id, and field path. Normal runtime must not spam logs.

## Performance
- Compile JSON rules into immutable predicate/action objects at reload time.
- Pre-index candidate rules by entity ID/tag-independent bucket and dimension where possible.
- Cache structure/biome lookups per spawn evaluation only, not indefinitely.
- Nearby entity scans use bounded AABBs and only run for rules that request count/AI functionality.
- AI block scans are cooldown-based and radius-capped.
- No full-world entity scans on tick.

## Compatibility policy
- Prefer NeoForge events and extension points.
- Add mixins only for capabilities with no reliable event/hook.
- Do not copy proprietary Mob Control code. Public documentation is behavior inspiration only.
- Modded registry content is supported through Minecraft registries/tags.
- If another mod cancels a spawn before this mod sees it, MobSpawningReal does not forcibly resurrect it unless an explicit active-spawn rule independently creates a new attempt.

## Persistence
The mod itself stores no per-mob permanent database. Required cooldown/state flags attach to entities via NeoForge data attachments or persistent entity data as appropriate. Rules/zones are configuration files.

## Initial example use cases
1. Witch-farm zone: allow/increase `minecraft:witch` only inside a named farm zone, deny other monsters there, cap witches, respect collision and configured light rules.
2. Nether gold farm: increase `minecraft:zombified_piglin` attempts in a specified zone while preserving hard caps.
3. Challenge zombies: after day 20, zombies receive extra health, weapon chance, group retaliation, and block-breaking ability.

## Testing strategy
- Pure unit tests for parsing, validation, priority, time/day/weather ranges, distance/Y/light conditions, merge semantics, limits, weighted selection, and config reload atomicity.
- Game tests for spawn allow/deny, active spawning, entity attributes/equipment, loot, hostility, and each AI ability where NeoForge/Minecraft test framework permits.
- Dedicated-server smoke test: mod loads with no client mod, generates example configs, reloads, and exercises representative commands.

## Non-goals
- No graphical configuration UI in the initial complete release; config + commands are the supported administration surface.
- No arbitrary Java/script execution from configs.
- No bypass of server permissions for commands.
