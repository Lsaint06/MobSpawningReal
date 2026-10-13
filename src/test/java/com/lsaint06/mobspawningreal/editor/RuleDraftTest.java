package com.lsaint06.mobspawningreal.editor;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RuleDraftTest {
    @Test void preservesUnknownUnsetAndExplicitZeroFalse(){
        var draft=new RuleDraft("{\"id\":\"a\",\"unknown\":7}");
        draft.set("then/spawn/maxPerPlayer",new JsonPrimitive(0));draft.set("then/spawn/allow",new JsonPrimitive(false));
        assertTrue(draft.dirty()); assertEquals(0,draft.get("then/spawn/maxPerPlayer").getAsInt());
        assertFalse(draft.get("then/spawn/allow").getAsBoolean());
        draft.unset("then/spawn/maxPerPlayer");assertNull(draft.get("then/spawn/maxPerPlayer"));
        assertEquals(7,draft.root().get("unknown").getAsInt());draft.discard();assertFalse(draft.dirty());
    }
    @Test void schemaExposesActualFieldsAndLootEntries(){
        var draft=new RuleDraft("{\"id\":\"a\",\"then\":{\"loot\":{\"add\":[{\"item\":\"minecraft:apple\"}]}}}");
        var fields=EditorSchema.fields(draft.root(),"Loot");
        assertTrue(fields.stream().anyMatch(f->f.path().equals("then/loot/add/0/chance")));
        assertTrue(EditorSchema.fields(draft.root(),"Spawn Settings").stream().anyMatch(f->f.path().equals("then/activeSpawn/area/minX")));
        assertTrue(EditorSchema.fields(draft.root(),"AI Behaviors").stream().anyMatch(f->f.path().equals("then/behavior/pillarUp")));
    }
    @Test void uniqueIdsAndListEdit(){
        assertEquals("super_zombies_2",RuleDraft.uniqueId("Super Zombies",java.util.Set.of("super_zombies")));
        var draft=new RuleDraft("{\"id\":\"a\",\"then\":{\"loot\":{\"add\":[{\"item\":\"minecraft:apple\"}]}}}");
        draft.set("then/loot/add/0/chance",new JsonPrimitive(0.2));
        assertEquals(0.2,draft.get("then/loot/add/0/chance").getAsDouble());
    }
    @Test void registryPathsAndMalformedParentsAreHandled(){
        var draft=new RuleDraft("{\"id\":\"a\",\"then\":false}");
        draft.set("then/attributes/custom/mod:combat~1armor",new JsonPrimitive(4));
        assertEquals(4,draft.root().getAsJsonObject("then").getAsJsonObject("attributes").getAsJsonObject("custom").get("mod:combat/armor").getAsInt());
        draft.unset("then/attributes/custom/mod:combat~1armor");assertNull(draft.get("then/attributes/custom/mod:combat~1armor"));
    }
}
