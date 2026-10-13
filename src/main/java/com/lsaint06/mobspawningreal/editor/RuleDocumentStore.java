package com.lsaint06.mobspawningreal.editor;

import com.google.gson.*;
import com.lsaint06.mobspawningreal.config.RuleValidator;
import com.lsaint06.mobspawningreal.rule.MobRule;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;

/** Server-owned identities; clients never supply filesystem paths. Whole-catalog optimistic concurrency. */
public final class RuleDocumentStore {
    public static final int MAX_JSON=8000, MAX_FILE=2_000_000, MAX_FILES=512;
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Gson STRICT=new GsonBuilder().setStrictness(Strictness.STRICT).create();
    private final Path directory;
    private final Function<JsonObject,List<String>> registryValidation;
    public record Document(String key,String fingerprint,String json) {}
    public record Result(boolean success,String message,String key) {}
    public RuleDocumentStore(Path directory,Function<JsonObject,List<String>> registryValidation) {
        this.directory=directory.toAbsolutePath().normalize();this.registryValidation=registryValidation;
    }
    private List<Path> files() throws Exception {
        Files.createDirectories(directory);
        try(var stream=Files.list(directory)) {
            var files=stream.filter(p->p.getFileName().toString().endsWith(".json")).sorted().toList();
            if(files.size()>MAX_FILES)throw new IllegalArgumentException("Too many rule files to edit safely (maximum 512).");
            long total=0;for(var file:files)total+=Files.size(file);
            if(total>16_000_000)throw new IllegalArgumentException("Rule catalog exceeds the 16 MB editor limit.");return files;
        }
    }
    private byte[] bytes(Path path) throws Exception {
        if(Files.isSymbolicLink(path)||Files.size(path)>MAX_FILE)throw new IllegalArgumentException("Rule file is too large or is a symbolic link.");
        return Files.readAllBytes(path);
    }
    public synchronized String fingerprint() throws Exception {
        var hash=MessageDigest.getInstance("SHA-256");
        for(var file:files()){hash.update(file.getFileName().toString().getBytes(StandardCharsets.UTF_8));hash.update((byte)0);hash.update(bytes(file));}
        return HexFormat.of().formatHex(hash.digest());
    }
    private JsonArray array(Path path) throws Exception {
        var value=JsonParser.parseString(new String(bytes(path),StandardCharsets.UTF_8));
        if(!value.isJsonArray())throw new IllegalArgumentException("Source file must contain an array of rules.");return value.getAsJsonArray();
    }
    public synchronized List<Document> documents() throws Exception {
        String fingerprint=fingerprint();List<Document> result=new ArrayList<>();
        for(var file:files()) {
            JsonArray array;
            try{array=array(file);}catch(Exception ignored){continue;}
            for(int i=0;i<array.size();i++)if(array.get(i).isJsonObject()) {
                String json=array.get(i).toString();if(json.length()<=MAX_JSON)result.add(new Document(file.getFileName()+"#"+i,fingerprint,json));
            }
        }
        if(result.size()>4096)throw new IllegalArgumentException("Too many editable rules (maximum 4096).");
        return List.copyOf(result);
    }
    private Path resolve(String key) throws Exception {
        int split=key.lastIndexOf('#');if(split<1)throw new IllegalArgumentException("Select an existing rule.");
        String name=key.substring(0,split);
        Path path=directory.resolve(name).normalize();
        if(!path.getParent().equals(directory)||!path.getFileName().toString().equals(name)||!name.endsWith(".json")||!files().contains(path))throw new IllegalArgumentException("Unknown rule identity.");
        return path;
    }
    private int index(String key,JsonArray array) {
        int index=Integer.parseInt(key.substring(key.lastIndexOf('#')+1));
        if(index<0||index>=array.size()||!array.get(index).isJsonObject())throw new IllegalArgumentException("Rule no longer exists.");return index;
    }
    public synchronized Document read(String key) throws Exception {
        var array=array(resolve(key));String json=array.get(index(key,array)).toString();
        if(json.length()>MAX_JSON)throw new IllegalArgumentException("Rule is too large for the editor.");return new Document(key,fingerprint(),json);
    }
    public static List<String> validate(JsonObject json) {
        List<String> errors=new ArrayList<>();
        try {
            bounded(json,0);typed(json,MobRule.class,"rule");
            if(!json.has("id")||!json.get("id").isJsonPrimitive()||!json.get("id").getAsJsonPrimitive().isString())throw new IllegalArgumentException("Rule identifier is required.");
            String id=json.get("id").getAsString();
            if(id.isBlank()||id.length()>1024||!id.equals(id.trim()))throw new IllegalArgumentException("Rule ID must be nonblank, at most 1024 characters, with no surrounding whitespace.");
            var rule=JSON.fromJson(json,MobRule.class);rule.normalize();errors.addAll(RuleValidator.validate(rule));
            if(rule.when.light.min!=null&&(rule.when.light.min<0||rule.when.light.min>15)||rule.when.light.max!=null&&(rule.when.light.max<0||rule.when.light.max>15))errors.add("Light values must be between 0 and 15.");
            var distance=rule.when.playerDistance;
            if(distance.min!=null&&distance.min<0||distance.max!=null&&distance.max<0||distance.min!=null&&distance.max!=null&&distance.min>distance.max)errors.add("Player distance must be nonnegative, with maximum >= minimum.");
            var active=rule.then.activeSpawn;
            if(active.intervalTicks!=null&&active.intervalTicks<1||active.attempts!=null&&active.attempts<1)errors.add("Active-spawn intervals and attempts must be at least 1, even when disabled.");
            if(active.maxNearby!=null&&active.maxNearby<0||active.capRadius!=null&&active.capRadius<=0)errors.add("Active-spawn caps must be nonnegative and radius must be positive.");
            if(rule.when.nearbyMobCountMin!=null&&rule.when.nearbyMobCountMax!=null&&rule.when.nearbyMobCountMin>rule.when.nearbyMobCountMax)errors.add("Nearby mob maximum must be >= minimum.");
            if(rule.then.outgoingDamage!=null&&rule.then.outgoingDamage<0||rule.then.behavior.knockbackTakenMultiplier!=null&&rule.then.behavior.knockbackTakenMultiplier<0)errors.add("Damage and knockback multipliers must be nonnegative.");
            for(var drop:rule.then.loot.add)if(drop!=null&&drop.min!=null&&drop.max!=null&&(long)drop.max-drop.min+1>Integer.MAX_VALUE)errors.add("Loot quantity range is too large.");
        }catch(Exception e){errors.add(Objects.toString(e.getMessage(),"Invalid rule."));}
        return errors;
    }
    private static void bounded(JsonElement json,int depth) {
        if(depth>16)throw new IllegalArgumentException("Settings nesting is too deep.");
        if(json.isJsonArray()) {if(json.getAsJsonArray().size()>256)throw new IllegalArgumentException("A list cannot exceed 256 entries.");for(var v:json.getAsJsonArray())bounded(v,depth+1);}
        if(json.isJsonObject()) {if(json.getAsJsonObject().size()>256)throw new IllegalArgumentException("Too many properties.");for(var v:json.getAsJsonObject().entrySet())bounded(v.getValue(),depth+1);}
        if(json.isJsonPrimitive()&&json.getAsJsonPrimitive().isString()&&json.getAsString().length()>2048)throw new IllegalArgumentException("Text value exceeds 2048 characters.");
    }
    private static void typed(JsonElement value,Type type,String path) throws Exception {
        if(value.isJsonNull()) {if(type==boolean.class||type==int.class)throw new IllegalArgumentException(path+" cannot be unset.");return;}
        if(type instanceof ParameterizedType p) {
            if(p.getRawType()==List.class){if(!value.isJsonArray())throw new IllegalArgumentException(path+" must be a list.");for(var v:value.getAsJsonArray())typed(v,p.getActualTypeArguments()[0],path);}
            else if(p.getRawType()==Map.class){if(!value.isJsonObject())throw new IllegalArgumentException(path+" must be a map.");for(var e:value.getAsJsonObject().entrySet())typed(e.getValue(),p.getActualTypeArguments()[1],path+"."+e.getKey());}
            return;
        }
        Class<?> c=(Class<?>)type;
        boolean primitive=c==String.class||c==Boolean.class||c==boolean.class||c==Double.class||c==Integer.class||c==int.class;
        if(primitive){
            if(!value.isJsonPrimitive())throw new IllegalArgumentException(path+" has an invalid value.");var p=value.getAsJsonPrimitive();
            boolean correct=c==String.class?p.isString():(c==Boolean.class||c==boolean.class)?p.isBoolean():p.isNumber();
            if(!correct)throw new IllegalArgumentException(path+" has the wrong value type.");
            if(c==Double.class||c==Integer.class||c==int.class){double n=p.getAsDouble();if(!Double.isFinite(n))throw new IllegalArgumentException(path+" must be finite.");if(c!=Double.class&&(n!=Math.rint(n)||n<Integer.MIN_VALUE||n>Integer.MAX_VALUE))throw new IllegalArgumentException(path+" must be a whole number.");}
        }else{
            if(!value.isJsonObject())throw new IllegalArgumentException(path+" must be a settings group.");
            for(Field field:c.getFields())if(value.getAsJsonObject().has(field.getName()))typed(value.getAsJsonObject().get(field.getName()),field.getGenericType(),path+"."+field.getName());
        }
    }
    public synchronized Result apply(String operation,String key,String expected,String draft) {
        Path temporary=null;
        try {
            if(!Set.of("CREATE","SAVE","DELETE").contains(operation))throw new IllegalArgumentException("Unknown editor operation.");
            if(!Objects.equals(expected,fingerprint()))throw new IllegalArgumentException("Configuration changed on server. Refresh and review your draft before saving again.");
            boolean create=operation.equals("CREATE"),delete=operation.equals("DELETE");
            Path path=create?directory.resolve("gui-"+UUID.randomUUID()+".json"):resolve(key);
            JsonArray array=create?new JsonArray():array(path);int index=create?0:index(key,array);
            JsonObject candidate=null;
            if(!delete){
                if(draft.length()>MAX_JSON)throw new IllegalArgumentException("Rule exceeds the editor packet size limit.");
                candidate=STRICT.fromJson(draft,JsonObject.class);if(candidate==null)throw new IllegalArgumentException("Rule must be an object.");
                var errors=validate(candidate);errors.addAll(registryValidation.apply(candidate));
                if(!errors.isEmpty())throw new IllegalArgumentException(String.join("; ",errors));
                String id=candidate.get("id").getAsString();
                var oldId=create?null:array.get(index).getAsJsonObject().get("id");
                boolean unchangedId=oldId!=null&&oldId.isJsonPrimitive()&&oldId.getAsJsonPrimitive().isString()&&id.equals(oldId.getAsString());
                if(!unchangedId&&(id.length()>128||!id.matches("[A-Za-z0-9_.:-]+")))throw new IllegalArgumentException("New rule IDs: use 1–128 letters, numbers, underscore, dot, colon or hyphen.");
                for(var file:files()){
                    JsonArray existing;try{existing=array(file);}catch(Exception ignored){continue;}
                    for(int i=0;i<existing.size();i++)if(existing.get(i).isJsonObject()&&!(file.equals(path)&&i==index&&!create)){
                        var other=existing.get(i).getAsJsonObject().get("id");
                        if(other!=null&&other.isJsonPrimitive()&&id.equals(other.getAsString()))throw new IllegalArgumentException("Duplicate rule ID: "+id);
                    }
                }
            }
            if(delete)array.remove(index);else if(create)array.add(candidate);else {
                if(array.get(index).equals(candidate))return new Result(true,"Already saved; no file changes.",key);
                array.set(index,candidate);
            }
            byte[] output=(JSON.toJson(array)+"\n").getBytes(StandardCharsets.UTF_8);
            if(output.length>MAX_FILE)throw new IllegalArgumentException("Source file exceeds the size limit.");
            temporary=Files.createTempFile(directory,".msr-edit-",".tmp");
            try(var channel=FileChannel.open(temporary,StandardOpenOption.WRITE)){var buffer=ByteBuffer.wrap(output);while(buffer.hasRemaining())channel.write(buffer);channel.force(true);}
            if(!create){Path backups=directory.resolve("backups");if(Files.isSymbolicLink(backups))throw new IllegalArgumentException("Backup directory cannot be a symbolic link.");Files.createDirectories(backups);Files.copy(path,backups.resolve(path.getFileName()+"."+UUID.randomUUID()+".bak"));}
            if(!Objects.equals(expected,fingerprint()))throw new IllegalArgumentException("Configuration changed during save. Nothing was replaced.");
            try{Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){throw new java.io.IOException("This filesystem does not support atomic rule replacement. Original file was preserved.",e);}
            return new Result(true,delete?"Rule deleted; backup created.":"Saved successfully",delete?"":path.getFileName()+"#"+index);
        }catch(Exception e){return new Result(false,Objects.toString(e.getMessage(),"Save failed."),key);}
        finally{if(temporary!=null)try{Files.deleteIfExists(temporary);}catch(Exception ignored){}}
    }
}
