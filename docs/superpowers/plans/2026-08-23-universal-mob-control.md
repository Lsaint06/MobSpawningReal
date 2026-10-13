# Universal Mob Control Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a universal NeoForge 26.2 rule-driven mob control mod supporting spawning, conditions, attributes, loot/equipment, hostility and configurable AI abilities.

**Architecture:** JSON rules are parsed into immutable rule models, matched against a runtime context, and applied by focused event handlers. Complex abilities are opt-in and tick only mobs that have matched behavior rules.

**Tech Stack:** Java 25, NeoForge 26.2, Gson supplied by Minecraft, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-08-23-universal-mob-control-design.md`

## Global Constraints
- NeoForge 26.2 only.
- Universal vanilla/modded registry-id support.
- No witch-specific or farm-specific logic.
- Preserve vanilla behavior when no rule matches.
- Config reload must not require a server restart.

---

### Task 1: Rule model and deterministic evaluation
- [ ] Add JSON rule model, priority ordering and condition semantics.
- [ ] Add unit tests for time windows, ranges, priorities and allow/deny decisions.

### Task 2: Config loading and commands
- [ ] Load and validate all JSON rule files from the server config directory.
- [ ] Add reload, list, validate and explain commands.

### Task 3: Spawn control and limits
- [ ] Match spawn reasons, dimensions, biomes, structures, day/weather/time/light/Y/player distance.
- [ ] Deny spawn attempts and support controlled extra spawn count/rate and local caps.

### Task 4: Attributes, names and equipment
- [ ] Apply health, speed, damage, follow range, knockback resistance, scale, names and equipment.

### Task 5: Loot and combat behavior
- [ ] Add replacement/additive loot drops, damage overrides, hostility, retaliation, sunlight and leash policy metadata.

### Task 6: Environmental AI abilities
- [ ] Add opt-in climb, block break, pillar, plant tree/flower, cut grass and eat/heal behavior with cooldowns and safety limits.

### Task 7: Dedicated-server hardening and docs
- [ ] Add example rules, config reference, one-click Windows build helper and validation output.
- [ ] Run tests/build and package the repository.
