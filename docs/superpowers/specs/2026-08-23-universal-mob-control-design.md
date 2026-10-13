# Universal Mob Control Design

Mob Spawning Real is a universal NeoForge 26.2 server-side rule engine for vanilla and modded mobs. Rules are JSON files loaded from `config/mobspawningreal/rules`, sorted by priority, and evaluated without hard-coding any entity type.

Rules can match entity ids/tags, spawn reasons, dimensions, biomes, structures, days, weather, time, light, Y range, player distance, and local population. Actions can deny spawning, increase spawn count/rate, change caps, attributes, names, equipment, loot, hostility/retaliation, sunlight burning, leash behavior, and opt-in environmental abilities including climbing, breaking blocks, pillaring, planting, cutting grass, eating plants, and healing from plants.

The engine preserves vanilla behavior when no rule matches. Rule evaluation, application, and AI abilities are separate modules. Commands provide reload, validation and explain/debug support. All selectors use registry identifiers so modded mobs/items/structures work without compile-time dependencies.
