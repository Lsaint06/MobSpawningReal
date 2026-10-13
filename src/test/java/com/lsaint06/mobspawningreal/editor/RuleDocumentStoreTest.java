package com.lsaint06.mobspawningreal.editor;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class RuleDocumentStoreTest {
    @TempDir Path dir;
    RuleDocumentStore store() { return new RuleDocumentStore(dir, json -> java.util.List.of()); }
    String rule(String id) { return "{\"id\":\""+id+"\",\"when\":{\"entities\":[\"minecraft:zombie\"]}}"; }
    @Test void createReloadDuplicateDeleteWithBackup() throws Exception {
        var s=store(); var created=s.apply("CREATE","",s.fingerprint(),rule("zombie"));
        assertTrue(created.success(),created.message());
        var document=s.read(created.key()); assertEquals("zombie",JsonParser.parseString(document.json()).getAsJsonObject().get("id").getAsString());
        var copy=s.apply("CREATE","",s.fingerprint(),rule("zombie_copy")); assertTrue(copy.success());
        assertTrue(s.apply("DELETE",created.key(),s.fingerprint(),"").success());
        assertEquals(1,s.documents().size()); assertTrue(Files.list(dir.resolve("backups")).findAny().isPresent());
    }
    @Test void editPreservesUnknownAndUnrelatedEntries() throws Exception {
        Files.writeString(dir.resolve("a.json"),"["+rule("a").replace("\"when\"","\"extra\":{\"future\":7},\"when\"")+",{\"id\":\"b\",\"odd\":[1,2]},null]");
        Files.writeString(dir.resolve("untouched.json"),"[ "+rule("c")+" ]\n");
        var s=store(); var d=s.read("a.json#0"); var json=JsonParser.parseString(d.json()).getAsJsonObject();json.addProperty("priority",99);
        assertTrue(s.apply("SAVE",d.key(),d.fingerprint(),json.toString()).success());
        var array=JsonParser.parseString(Files.readString(dir.resolve("a.json"))).getAsJsonArray();
        assertEquals(7,array.get(0).getAsJsonObject().getAsJsonObject("extra").get("future").getAsInt());
        assertEquals("{\"id\":\"b\",\"odd\":[1,2]}",array.get(1).toString()); assertTrue(array.get(2).isJsonNull());
        assertEquals("[ "+rule("c")+" ]\n",Files.readString(dir.resolve("untouched.json")));
    }
    @Test void staleExternalChangesRejected() throws Exception {
        var s=store();var r=s.apply("CREATE","",s.fingerprint(),rule("a"));var d=s.read(r.key());
        Files.writeString(dir.resolve("external.json"),"["+rule("b")+"]");
        assertFalse(s.apply("SAVE",d.key(),d.fingerprint(),rule("a")).success());
    }
    @Test void duplicateIdsAndBadTypesRejected() throws Exception {
        var s=store();assertTrue(s.apply("CREATE","",s.fingerprint(),rule("a")).success());
        assertFalse(s.apply("CREATE","",s.fingerprint(),rule("a")).success());
        assertFalse(s.apply("CREATE","",s.fingerprint(),"{\"id\":\"b\",\"enabled\":\"false\"}").success());
        assertFalse(s.apply("CREATE","",s.fingerprint(),"{\"id\":\"b\",\"then\":{\"spawn\":{\"maxPerMob\":1.5}}}").success());
        assertFalse(s.apply("CREATE","",s.fingerprint(),"{\"id\":\"b\",\"then\":{\"attributes\":{\"maxHealth\":-1}}}").success());
    }
    @Test void malformedJsonAndTraversalCannotWrite() throws Exception {
        var s=store(); assertFalse(s.apply("CREATE","",s.fingerprint(),"{").success());
        assertThrows(Exception.class,()->s.read("../outside.json#0"));
        assertFalse(s.apply("DELETE","../outside.json#0",s.fingerprint(),"").success());
    }
    @Test void writeFailureLeavesOriginalAndDraftUnsetSemantics() throws Exception {
        Files.writeString(dir.resolve("a.json"),"["+rule("a")+"]"); Files.writeString(dir.resolve("backups"),"blocked");
        var s=store();var d=s.read("a.json#0"); var raw=Files.readString(dir.resolve("a.json"));
        assertFalse(s.apply("SAVE",d.key(),d.fingerprint(),rule("a").replace("\"when\"","\"enabled\":false,\"priority\":0,\"when\"")).success());
        assertEquals(raw,Files.readString(dir.resolve("a.json")));
    }
    @Test void boundsAndRegistryErrorsCannotSave() throws Exception {
        var s=new RuleDocumentStore(dir,json->java.util.List.of("Unknown entity id"));
        assertFalse(s.apply("CREATE","",s.fingerprint(),rule("a")).success());
        assertFalse(store().apply("CREATE","",store().fingerprint()," ".repeat(RuleDocumentStore.MAX_JSON+1)).success());
    }
    @Test void strictJsonAndEditorRangesAreValidated() throws Exception {
        var s=store();
        assertFalse(s.apply("CREATE","",s.fingerprint(),"{'id':'a'}").success());
        assertFalse(s.apply("CREATE","",s.fingerprint(),"{\"id\":\"a\",\"when\":{\"light\":{\"max\":16}}}").success());
        assertFalse(s.apply("CREATE","",s.fingerprint(),"{\"id\":\"a\",\"when\":{\"playerDistance\":{\"min\":10,\"max\":2}}}").success());
        assertFalse(s.apply("CREATE","",s.fingerprint(),"{\"id\":\"a\",\"then\":{\"activeSpawn\":{\"intervalTicks\":0}}}").success());
    }
    @Test void duplicateInOversizedSiblingIsRejected() throws Exception {
        Files.writeString(dir.resolve("big.json"),"[{\"id\":\"a\",\"large\":\""+"x".repeat(RuleDocumentStore.MAX_JSON)+"\"}]");
        assertFalse(store().apply("CREATE","",store().fingerprint(),rule("a")).success());
    }
    @Test void concurrentDraftsAndRestartKeepOnlyAcceptedChange() throws Exception {
        var s=store();var created=s.apply("CREATE","",s.fingerprint(),rule("a"));var document=s.read(created.key());
        assertTrue(s.apply("SAVE",created.key(),document.fingerprint(),rule("a").replace("\"when\"","\"enabled\":false,\"priority\":0,\"when\"")).success());
        assertFalse(s.apply("SAVE",created.key(),document.fingerprint(),rule("a")).success());
        var reloaded=JsonParser.parseString(store().read(created.key()).json()).getAsJsonObject();
        assertFalse(reloaded.get("enabled").getAsBoolean());assertEquals(0,reloaded.get("priority").getAsInt());
    }
    @Test void backupCanRestoreExactOriginal() throws Exception {
        var original="[ "+rule("a")+" ]\n";Files.writeString(dir.resolve("a.json"),original);
        var s=store();assertTrue(s.apply("DELETE","a.json#0",s.fingerprint(),"").success());
        Path backup;try(var files=Files.list(dir.resolve("backups"))){backup=files.findFirst().orElseThrow();}
        assertEquals(original,Files.readString(backup));
        Files.copy(backup,dir.resolve("a.json"),StandardCopyOption.REPLACE_EXISTING);
        assertEquals(1,store().documents().size());
    }
    @Test void malformedNeighborAndNoopAreNotRewritten() throws Exception {
        Files.writeString(dir.resolve("bad.json"),"{broken");Files.writeString(dir.resolve("a.json"),"[ "+rule("a")+" ]\n");
        var s=store();var doc=s.read("a.json#0");String before=Files.readString(dir.resolve("a.json"));
        assertTrue(s.apply("SAVE",doc.key(),doc.fingerprint(),doc.json()).success());
        assertEquals(before,Files.readString(dir.resolve("a.json")));assertFalse(Files.exists(dir.resolve("backups")));
        assertTrue(s.apply("CREATE","",s.fingerprint(),rule("b")).success());assertEquals("{broken",Files.readString(dir.resolve("bad.json")));
    }
    @Test void existingLegacyIdentifierCanBeEditedWithoutRenaming() throws Exception {
        Files.writeString(dir.resolve("legacy.json"),"["+rule("Super Zombies")+"]");
        var s=store();var document=s.read("legacy.json#0");var json=JsonParser.parseString(document.json()).getAsJsonObject();json.addProperty("priority",22);
        assertTrue(s.apply("SAVE",document.key(),document.fingerprint(),json.toString()).success());
    }
}
