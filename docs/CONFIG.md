# Mob Spawning Real configuration

Rule files live in:

`config/mobspawningreal/rules/*.json`

Each file contains a JSON array of rules. Registry IDs work for vanilla and modded content (`minecraft:zombie`, `some_mod:custom_mob`, etc.). Omit any field you do not want to constrain or change.

## Rule shell

```json
[
  {
    "id": "my_rule",
    "enabled": true,
    "priority": 100,
    "stopProcessing": false,
    "when": {},
    "then": {}
  }
]
```

`id` must be unique enough for you to recognize it. `priority` controls conflict resolution. `enabled: false` keeps a rule as an inert template.

## `when` conditions

All supplied conditions must match.

| Field | Meaning |
|---|---|
| `entities` | Entity registry IDs. `*` means any mob. |
| `spawnReasons` | Spawn reason names, e.g. `natural`, `spawner`, `command`, `event`. Case-insensitive. |
| `dimensions` | Dimension IDs such as `minecraft:overworld`, `minecraft:the_nether`. |
| `biomes` | Biome IDs such as `minecraft:plains`. |
| `structures` | Structure IDs present at the spawn position. |
| `day` | `{ "min": 1, "max": 10 }`; world day 1 is the first 24000 ticks. |
| `weather` | Any of `clear`, `rain`, `thunder`. |
| `time` | `{ "start": 13000, "end": 23000 }`; supports wraparound (e.g. 22000→2000). |
| `light` | Inclusive raw local brightness range. |
| `y` | Inclusive Y range. |
| `playerDistance` | Inclusive `{ "min": ..., "max": ... }` distance to nearest non-spectator player. |
| `nearbyMobCountMin` / `Max` | Match only when the same entity type has a nearby count inside bounds. |
| `nearbyMobRadius` | Radius used by nearby-mob condition. |
| `chance` | 0.0–1.0 probability that this rule matches an otherwise valid attempt. |

## `then.spawn`

```json
"spawn": {
  "allow": true,
  "rateMultiplier": 2.5,
  "extraSpawnMin": 0,
  "extraSpawnMax": 2,
  "maxPerMob": 40,
  "maxPerPlayer": 25,
  "maxPerCondition": 30,
  "capRadius": 64.0
}
```

- `allow: false` blocks matching spawn attempts.
- `rateMultiplier` below 1 probabilistically suppresses base attempts; above 1 adds extra mobs from successful matching attempts.
- `extraSpawnMin/Max` explicitly add a random number of extra same-type mobs per matching successful spawn.
- `maxPerMob` caps the same entity type in `capRadius`.
- `maxPerPlayer` caps the same entity type around the nearest player.
- `maxPerCondition` caps mobs carrying this rule's internal tag.

## `then.activeSpawn`

Active spawning is independent of vanilla's natural spawn-attempt loop. It is useful when you want a rule to create spawn attempts itself while the relevant chunks are loaded. It works with any concrete entity IDs in `when.entities`.

```json
"activeSpawn": {
  "enabled": true,
  "intervalTicks": 20,
  "attempts": 4,
  "area": {
    "minX": -100, "maxX": -70,
    "minY": 64, "maxY": 72,
    "minZ": 200, "maxZ": 230
  },
  "maxNearby": 20,
  "capRadius": 64.0,
  "requireLoadedChunk": true,
  "requireEmptySpace": true,
  "requireSolidGround": true
}
```

- `intervalTicks`: how often this rule runs; 20 ticks is about one second.
- `attempts`: random candidate positions tried each interval.
- `area`: inclusive world-coordinate box. Corner order does not matter.
- `maxNearby`: optional cap for this entity type around each candidate position.
- `capRadius`: radius used by `maxNearby`.
- `requireLoadedChunk`: defaults to true. Active spawning never intentionally force-loads chunks.
- `requireEmptySpace`: defaults to true. A spawned mob that collides at its candidate position is discarded.
- `requireSolidGround`: defaults to false for universal compatibility; enable it for normal ground mobs/farm platforms.

All normal `when` conditions still apply to every candidate position: dimension, biome, structure, day, weather, time, light, Y, player distance, nearby-mob conditions and chance. For active spawning, omit `spawnReasons` or include `active`. Active spawn rules must list concrete entity IDs; `*` is not accepted because the scheduler needs to know which entity type to create.

## `then.attributes`

```json
"attributes": {
  "maxHealth": 40.0,
  "movementSpeed": 0.3,
  "attackDamage": 8.0,
  "followRange": 48.0,
  "knockbackResistance": 0.4,
  "scale": 1.25,
  "custom": {
    "minecraft:gravity": 0.07
  }
}
```

`custom` accepts registered attribute IDs from vanilla or other mods if the target mob actually has that attribute instance.

## `then.name`

```json
"name": {
  "text": "The Collector",
  "alwaysVisible": true
}
```

## `then.equipment`

Each slot accepts an item, stack count, chance and whether it should be guaranteed to drop.

```json
"equipment": {
  "mainhand": {"item":"minecraft:iron_sword", "chance":1.0, "dropOnDeath":false},
  "head": {"item":"minecraft:iron_helmet", "chance":0.5}
}
```

Available slots: `mainhand`, `offhand`, `head`, `chest`, `legs`, `feet`.

## `then.loot`

```json
"loot": {
  "clearVanilla": false,
  "add": [
    {"item":"minecraft:diamond", "min":1, "max":2, "chance":0.05},
    {"item":"minecraft:gunpowder", "min":2, "max":5, "chance":1.0}
  ]
}
```

Set `clearVanilla: true` to replace normal drops instead of adding to them.

## `then.outgoingDamage`

```json
"outgoingDamage": 12.0
```

Overrides the amount of incoming damage when the matching mob is the direct damage source entity.

## `then.behavior`

```json
"behavior": {
  "alwaysHostile": true,
  "peacefulToPlayers": false,
  "peacefulToMobs": false,
  "hostileTo": ["minecraft:player", "minecraft:villager"],
  "peacefulTo": ["minecraft:iron_golem"],
  "hostileWhenHurt": true,
  "alertOthersWhenHurt": true,
  "leashable": false,
  "burnsInSun": true,
  "knockbackTakenMultiplier": 1.75,

  "climb": true,
  "breakBlocks": true,
  "blockBreakHardnessMax": 2.0,
  "pillarUp": true,
  "pillarBlock": "minecraft:cobblestone",
  "plantTrees": false,
  "treeBlock": "minecraft:oak_sapling",
  "plantFlowers": false,
  "flowerBlock": "minecraft:dandelion",
  "cutGrass": false,
  "eatPlants": false,
  "healFromPlants": 2.0,
  "abilityCooldownTicks": 20
}
```

Notes:
- `alwaysHostile` makes an otherwise non-hostile mob acquire players when it has no target.
- `hostileTo` can point at arbitrary entity registry IDs, including modded mobs. `*` matches any living entity.
- `peacefulTo...` clears disallowed targets.
- `hostileWhenHurt` retaliates against the attacker.
- `knockbackTakenMultiplier` scales incoming knockback after vanilla/NeoForge calculates its base strength. `1.0` is normal, `1.75` is 75% more, `2.0` is double, `0.5` is half, and `0.0` removes knockback. Negative values clamp to `0.0`.
- `alertOthersWhenHurt` asks nearby same-type mobs to target the attacker.
- `climb` adds simple wall-climbing movement.
- `breakBlocks` only destroys blocks up to `blockBreakHardnessMax`; unbreakable blocks are protected.
- `pillarUp` uses `pillarBlock` when pursuing a higher target.
- Plant/eat/cut abilities are deliberately rate-limited by `abilityCooldownTicks`.

## Commands

- `/msrgui` — open the in-game administrator rule dashboard (requires the mod on the client).

- `/mobspawningreal reload` or `/msr reload` — reload all JSON rules.
- `/mobspawningreal validate` — report load/validation errors.
- `/mobspawningreal list` — list loaded rules and their effective runtime states.
- `/mobspawningreal explain <entity>` — show rules already applied to an entity selector.
- `/mobspawningreal disable` — globally stop Mob Spawning Real runtime processing.
- `/mobspawningreal enable` — globally resume runtime processing.
- `/mobspawningreal status` — show global state and runtime-state file.
- `/mobspawningreal rule disable <id>` — persistently disable one rule.
- `/mobspawningreal rule enable <id>` — persistently force-enable one rule.
- `/mobspawningreal rule status <id>` — show JSON, override, and effective state.
- `/mobspawningreal rule clear <id>` — clear the runtime override and follow JSON again.

Runtime toggles are stored separately in `config/mobspawningreal/runtime-state.json`; commands do not rewrite your rule JSON files. Global disable stops active spawning, spawn multipliers, loot/behavior hooks, and other new runtime processing. Changes already applied permanently to existing entities (for example equipment or base attributes) are not reversed.

The v1.4 editor offers persistent runtime toggles, a searchable mob/rule browser, retained diagnostics, and editable Spawn Settings, Attributes, Equipment, Loot, and AI categories. Definition changes use drafts and explicit Save Changes, preserving unrelated entries and unknown properties with original-file backups and atomic replacement. **Use JSON** clears a runtime override. Runtime-state saves remain separate from definitions; failures preserve live settings. If runtime state is malformed, repair it and Reload. Duplicate IDs, missing IDs and invalid rules do not execute. New editor IDs use at most 128 letters/numbers/underscore/dot/colon/hyphen; existing catalog IDs remain bounded to 1024 characters. See [editor guide](VISUAL_EDITOR.md) for limits, timing and conflict handling.

## Example: block one mob in one dimension

```json
[
  {
    "id": "no_creepers_nether",
    "priority": 100,
    "when": {
      "entities": ["minecraft:creeper"],
      "dimensions": ["minecraft:the_nether"]
    },
    "then": {"spawn":{"allow":false}}
  }
]
```

## Example: much stronger night zombies

```json
[
  {
    "id": "night_zombies",
    "priority": 50,
    "when": {
      "entities": ["minecraft:zombie"],
      "time": {"start":13000,"end":23000}
    },
    "then": {
      "spawn": {"rateMultiplier":2.0,"maxPerMob":60},
      "attributes": {"maxHealth":40,"attackDamage":10,"movementSpeed":0.32},
      "behavior": {"climb":true,"breakBlocks":true,"blockBreakHardnessMax":1.5}
    }
  }
]
```
