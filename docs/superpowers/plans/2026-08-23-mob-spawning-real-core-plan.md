# MobSpawningReal Core Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish a buildable NeoForge 26.2 project with deterministic config parsing, rule compilation, matching, resolution, zones, atomic reload, and administrative inspection commands that later spawn/entity/AI modules can consume.

**Architecture:** A server-first NeoForge mod compiles JSON configuration into immutable rule predicates/actions. A `RuleEngine` owns an atomic `RuleSet` snapshot and exposes context evaluation APIs; feature modules depend only on these APIs. Configuration parsing and rule resolution are kept Minecraft-light enough for unit testing.

**Tech Stack:** Java, NeoForge 26.2, Gson, JUnit 5, Minecraft/NeoForge registries and command API.

**Spec:** `docs/superpowers/specs/2026-08-23-mob-spawning-real-design.md`

## Global Constraints
- Target Minecraft Java Edition 26.2 and NeoForge 26.2.
- Dedicated-server safe; server-only features must not require client installation.
- Address vanilla and modded content by registry IDs/tags.
- Preserve vanilla behavior when no rule explicitly changes it.
- Invalid individual rules are disabled and reported; reload swaps active configuration atomically only after successful global validation.
- No proprietary Mob Control code may be copied.

---

### Task 1: NeoForge project shell and test harness

**Files:**
- Create: `settings.gradle`
- Create: `build.gradle`
- Create: `gradle.properties`
- Create: `src/main/java/com/lsaint06/mobspawningreal/MobSpawningReal.java`
- Create: `src/main/resources/META-INF/neoforge.mods.toml`
- Create: `src/main/resources/pack.mcmeta`
- Create: `src/test/java/com/lsaint06/mobspawningreal/ProjectSmokeTest.java`

**Interfaces:**
- Produces: `MobSpawningReal.MOD_ID = "mobspawningreal"`
- Produces: a JUnit 5 test task usable by all later tasks.

- [ ] **Step 1: Write the failing smoke test**

```java
package com.lsaint06.mobspawningreal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectSmokeTest {
    @Test
    void modIdIsStable() {
        assertEquals("mobspawningreal", MobSpawningReal.MOD_ID);
    }
}
```

- [ ] **Step 2: Run test and verify RED**

Run: `./gradlew test --tests com.lsaint06.mobspawningreal.ProjectSmokeTest`
Expected: compilation failure because `MobSpawningReal` does not exist.

- [ ] **Step 3: Create minimal NeoForge main class and build metadata**

```java
package com.lsaint06.mobspawningreal;

import net.neoforged.fml.common.Mod;

@Mod(MobSpawningReal.MOD_ID)
public final class MobSpawningReal {
    public static final String MOD_ID = "mobspawningreal";

    public MobSpawningReal() {}
}
```

Configure the project from a known working NeoForge 26.2 MDK/reference build and enable JUnit 5 in `test`.

- [ ] **Step 4: Run tests and verify GREEN**

Run: `./gradlew test`
Expected: PASS, 1 test.

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "build: scaffold NeoForge 26.2 project"
```

### Task 2: Core rule schema and deterministic ordering

**Files:**
- Create: `src/main/java/com/lsaint06/mobspawningreal/rules/RuleDefinition.java`
- Create: `src/main/java/com/lsaint06/mobspawningreal/rules/MatchDefinition.java`
- Create: `src/main/java/com/lsaint06/mobspawningreal/rules/SpawnActionDefinition.java`
- Create: `src/main/java/com/lsaint06/mobspawningreal/rules/RuleSet.java`
- Test: `src/test/java/com/lsaint06/mobspawningreal/rules/RuleSetTest.java`

**Interfaces:**
- Produces: `RuleDefinition(String id, boolean enabled, int priority, int sourceOrder, MatchDefinition match, SpawnActionDefinition spawn)`
- Produces: `RuleSet(List<RuleDefinition> rules)` with `List<RuleDefinition> orderedRules()`.

- [ ] **Step 1: Write failing ordering tests**

```java
@Test
void ordersHigherPriorityFirstAndKeepsSourceOrderForTies() {
    var low = rule("low", 10, 0);
    var tieA = rule("tieA", 20, 1);
    var tieB = rule("tieB", 20, 2);
    var set = new RuleSet(List.of(low, tieB, tieA));
    assertEquals(List.of("tieA", "tieB", "low"),
        set.orderedRules().stream().map(RuleDefinition::id).toList());
}
```

- [ ] **Step 2: Verify RED**
Run: `./gradlew test --tests '*RuleSetTest'`
Expected: FAIL because schema classes do not exist.

- [ ] **Step 3: Implement immutable records and deterministic sort**
Use descending priority, ascending `sourceOrder`; filter `enabled=false` from active ordered rules.

- [ ] **Step 4: Verify GREEN**
Run: `./gradlew test --tests '*RuleSetTest'`
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add src/main src/test
git commit -m "feat: add deterministic rule schema"
```

### Task 3: Primitive condition predicates

**Files:**
- Create: `src/main/java/com/lsaint06/mobspawningreal/rules/context/SpawnContextView.java`
- Create: `src/main/java/com/lsaint06/mobspawningreal/rules/condition/IntRange.java`
- Create: `src/main/java/com/lsaint06/mobspawningreal/rules/condition/DoubleRange.java`
- Create: `src/main/java/com/lsaint06/mobspawningreal/rules/condition/TimeRange.java`
- Create: `src/main/java/com/lsaint06/mobspawningreal/rules/condition/WeatherCondition.java`
- Test: `src/test/java/com/lsaint06/mobspawningreal/rules/condition/ConditionTest.java`

**Interfaces:**
- Produces: inclusive range `matches` methods.
- Produces: `TimeRange.matches(long dayTime)` with wraparound support over 0..23999.
- Produces: `WeatherCondition.matches(boolean raining, boolean thundering)`.

- [ ] **Step 1: Write failing tests for inclusive bounds and wraparound time**
```java
@Test void wraparoundNightRangeMatchesLateAndEarlyTimes() {
    var range = new TimeRange(13000, 1000);
    assertTrue(range.matches(18000));
    assertTrue(range.matches(500));
    assertFalse(range.matches(6000));
}
```

- [ ] **Step 2: Verify RED**
Run: `./gradlew test --tests '*ConditionTest'`
Expected: FAIL because condition classes do not exist.

- [ ] **Step 3: Implement minimal condition classes**
Normalize time with `Math.floorMod(dayTime, 24000)`; if start <= end use ordinary inclusive range, otherwise match `t >= start || t <= end`.

- [ ] **Step 4: Verify GREEN**
Run: `./gradlew test --tests '*ConditionTest'`
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add src/main src/test
git commit -m "feat: add core rule conditions"
```

### Task 4: Named coordinate zones

**Files:**
- Create: `src/main/java/com/lsaint06/mobspawningreal/zones/ZoneDefinition.java`
- Create: `src/main/java/com/lsaint06/mobspawningreal/zones/ZoneRegistry.java`
- Test: `src/test/java/com/lsaint06/mobspawningreal/zones/ZoneRegistryTest.java`

**Interfaces:**
- Produces: `ZoneDefinition(String id, String dimensionId, int minX, int minY, int minZ, int maxX, int maxY, int maxZ)` with normalized coordinates and `contains(...)`.
- Produces: immutable `ZoneRegistry` lookup by ID.

- [ ] **Step 1: Write failing normalization/containment tests**
```java
@Test void normalizesReversedCornersAndMatchesInclusiveEdges() {
    var zone = new ZoneDefinition("farm", "minecraft:overworld", 10, 80, 10, -10, 60, -10);
    assertTrue(zone.contains("minecraft:overworld", -10, 60, -10));
    assertTrue(zone.contains("minecraft:overworld", 10, 80, 10));
    assertFalse(zone.contains("minecraft:the_nether", 0, 70, 0));
}
```

- [ ] **Step 2: Verify RED**
Run: `./gradlew test --tests '*ZoneRegistryTest'`
Expected: FAIL.

- [ ] **Step 3: Implement normalized immutable zones and registry**
Reject duplicate IDs in `ZoneRegistry` construction.

- [ ] **Step 4: Verify GREEN**
Run: `./gradlew test --tests '*ZoneRegistryTest'`
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add src/main src/test
git commit -m "feat: add named spawn zones"
```

### Task 5: Rule matching and spawn-decision resolution

**Files:**
- Create: `src/main/java/com/lsaint06/mobspawningreal/rules/RuleMatcher.java`
- Create: `src/main/java/com/lsaint06/mobspawningreal/rules/SpawnDecision.java`
- Create: `src/main/java/com/lsaint06/mobspawningreal/rules/RuleEngine.java`
- Test: `src/test/java/com/lsaint06/mobspawningreal/rules/RuleEngineTest.java`

**Interfaces:**
- Produces: `RuleEngine.evaluateSpawn(SpawnContextView context) -> SpawnDecision`
- `SpawnDecision` contains `Decision {PASS, ALLOW, DENY}`, matched rule IDs, rate multiplier, limits/group settings as optional values.

- [ ] **Step 1: Write failing priority and pass-through tests**
```java
@Test void highestPriorityExplicitDecisionWins() {
    var engine = engineWith(
        spawnRule("deny", 100, "minecraft:witch", false),
        spawnRule("allow", 10, "minecraft:witch", true));
    assertEquals(SpawnDecision.Decision.DENY,
        engine.evaluateSpawn(context("minecraft:witch")).decision());
}

@Test void noMatchingExplicitRulePassesVanillaThrough() {
    assertEquals(SpawnDecision.Decision.PASS,
        engineWith().evaluateSpawn(context("minecraft:zombie")).decision());
}
```

- [ ] **Step 2: Verify RED**
Run: `./gradlew test --tests '*RuleEngineTest'`
Expected: FAIL.

- [ ] **Step 3: Implement matcher/resolver**
Evaluate ordered rules; collect matches; first explicit allow/deny sets decision; independent explicit scalar spawn fields resolve from highest priority.

- [ ] **Step 4: Verify GREEN**
Run: `./gradlew test --tests '*RuleEngineTest'`
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add src/main src/test
git commit -m "feat: add rule evaluation engine"
```

### Task 6: JSON loader, validation, and atomic reload

**Files:**
- Create: `src/main/java/com/lsaint06/mobspawningreal/config/ConfigLoader.java`
- Create: `src/main/java/com/lsaint06/mobspawningreal/config/ConfigProblem.java`
- Create: `src/main/java/com/lsaint06/mobspawningreal/config/LoadResult.java`
- Create: `src/main/java/com/lsaint06/mobspawningreal/config/RuleService.java`
- Test: `src/test/java/com/lsaint06/mobspawningreal/config/ConfigLoaderTest.java`
- Test: `src/test/java/com/lsaint06/mobspawningreal/config/RuleServiceTest.java`

**Interfaces:**
- Produces: `ConfigLoader.load(Path configDir) -> LoadResult`.
- Produces: `RuleService.reload(Path configDir)` that swaps an `AtomicReference<RuleEngine>` only when the new global result is usable.

- [ ] **Step 1: Write failing tests for deterministic file order, invalid-rule isolation, and previous-snapshot retention**
Create temporary `rules/10-base.json`, `rules/20-specific.json`, and malformed/invalid rule files using JUnit `@TempDir`.

- [ ] **Step 2: Verify RED**
Run: `./gradlew test --tests '*ConfigLoaderTest' --tests '*RuleServiceTest'`
Expected: FAIL.

- [ ] **Step 3: Implement Gson loading and validation**
Lexically sort `*.json`; assign monotonically increasing `sourceOrder`; report problems with file, rule ID when available, and JSON path. Invalid rules are excluded. Syntax failure in one file is a problem but does not erase valid files. Treat missing zones/rules directory as empty valid configuration.

- [ ] **Step 4: Verify GREEN**
Run: `./gradlew test --tests '*ConfigLoaderTest' --tests '*RuleServiceTest'`
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add src/main src/test
git commit -m "feat: load and atomically reload rule configs"
```

### Task 7: Minecraft context adapter and registry-aware matching

**Files:**
- Create: `src/main/java/com/lsaint06/mobspawningreal/minecraft/MinecraftSpawnContext.java`
- Create: `src/main/java/com/lsaint06/mobspawningreal/minecraft/RegistryMatcher.java`
- Create: `src/main/java/com/lsaint06/mobspawningreal/minecraft/StructureLookup.java`
- Test: `src/test/java/com/lsaint06/mobspawningreal/minecraft/RegistryIdTest.java`

**Interfaces:**
- Produces: adapter from `ServerLevel`, entity type, block position, spawn reason, and nearest-player data to `SpawnContextView`.
- Produces: registry/tag matching helpers that fail closed for invalid configured IDs during validation rather than at runtime.

- [ ] **Step 1: Write failing pure tests for namespaced-ID normalization/validation**
Test `minecraft:witch`, modded IDs, and rejection of malformed IDs.

- [ ] **Step 2: Verify RED**
Run: `./gradlew test --tests '*RegistryIdTest'`
Expected: FAIL.

- [ ] **Step 3: Implement registry-aware adapter**
Use Minecraft registries and holders/tags; compute biome/structure/light/time/weather lazily and memoize per context evaluation.

- [ ] **Step 4: Verify GREEN plus compile**
Run: `./gradlew test compileJava`
Expected: PASS.

- [ ] **Step 5: Commit**
```bash
git add src/main src/test
git commit -m "feat: connect rules to Minecraft registries"
```

### Task 8: Administrative commands and examples

**Files:**
- Create: `src/main/java/com/lsaint06/mobspawningreal/command/MobSpawningRealCommands.java`
- Create: `src/main/java/com/lsaint06/mobspawningreal/debug/RuleExplainer.java`
- Create: `src/main/resources/defaultconfigs/mobspawningreal/examples/witch_farm.json`
- Create: `README.md`
- Test: `src/test/java/com/lsaint06/mobspawningreal/debug/RuleExplainerTest.java`

**Interfaces:**
- Produces: reload, validate, list, and explain commands described in the spec.
- Produces: human-readable `RuleExplainer.explain(...)` from an evaluation result.

- [ ] **Step 1: Write failing explainer test**
Assert output names the matched rule, winning decision, and relevant conditions/scalars without depending on log formatting.

- [ ] **Step 2: Verify RED**
Run: `./gradlew test --tests '*RuleExplainerTest'`
Expected: FAIL.

- [ ] **Step 3: Implement commands, explainer, README, and example configuration**
Register through NeoForge command-registration event. Restrict administration commands to server operators. Example witch rule demonstrates zone, entity, deny-other-hostiles, rate multiplier, cap, light, and player-distance fields supported by the completed engine.

- [ ] **Step 4: Verify GREEN and full core build**
Run: `./gradlew clean test build`
Expected: PASS and mod jar under `build/libs/`.

- [ ] **Step 5: Commit**
```bash
git add .
git commit -m "feat: add mob spawning rule administration"
```
