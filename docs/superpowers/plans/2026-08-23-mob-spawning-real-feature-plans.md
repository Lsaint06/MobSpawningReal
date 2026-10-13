# MobSpawningReal Feature Workstreams

This document decomposes the remaining complete product into independently testable implementation plans. Each workstream receives its own detailed TDD plan immediately before execution, building on the stable core rule engine.

1. **Spawn Interception + Limits** — NeoForge spawn events, allow/deny, probability, light/Y/player distance, biome/dimension/structure/day/weather/time, per-entity/per-player/per-rule caps, group controls.
2. **Active Spawn Scheduler** — bounded extra attempts for rate multipliers >1, explicit active rules, weighted entity pools, zones, interval/attempt controls, placement/collision policies, hard safety caps.
3. **Entity Attributes + Names** — health, damage, movement, follow range, knockback, scale, persistence, naming/visibility with documented server-only limits.
4. **Loot + Equipment** — additive/replacement drop policies, weighted item drops, equipment slots, chances/components/drop chances, modded registry IDs/tags.
5. **Hostility + Retaliation + Sunlight + Leash** — target/goal/event hooks and narrowly scoped compatibility mixins where required.
6. **AI Abilities** — climb, block break, pillar, plant trees, plant flowers, cut grass, eat plants + healing; each as an isolated cooldown-driven module with griefing/safety controls.
7. **Debugging + Game Tests + Dedicated Server Hardening** — game tests for all feature modules, performance counters, rate-limited debug tracing, generated examples, server-only compatibility verification, release packaging.

The product is complete only after all seven workstreams pass their unit/game/smoke tests and the dedicated-server acceptance suite.
