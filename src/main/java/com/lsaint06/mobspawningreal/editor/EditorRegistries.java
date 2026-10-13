package com.lsaint06.mobspawningreal.editor;

import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import java.util.*;
import static com.lsaint06.mobspawningreal.editor.EditorModels.*;

/** Metadata only: never instantiate entities. Common-side registry access is safe on dedicated servers. */
public final class EditorRegistries {
    private static List<RegistryEntry> cached;
    private EditorRegistries() {}
    public static void reset(){cached=null;}
    public static List<RegistryEntry> entries(MinecraftServer server) {
        if(cached!=null)return cached;
        var entries=new ArrayList<RegistryEntry>();
        for(var type:BuiltInRegistries.ENTITY_TYPE)if(DefaultAttributes.hasSupplier(type)&&!Set.of("minecraft:player","minecraft:armor_stand").contains(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString())){
            var id=BuiltInRegistries.ENTITY_TYPE.getKey(type);entries.add(new RegistryEntry(id.toString(),type.getDescriptionId(),"entity"));
        }
        for(var item:BuiltInRegistries.ITEM)entries.add(new RegistryEntry(BuiltInRegistries.ITEM.getKey(item).toString(),item.getDescriptionId(),"item"));
        for(var attribute:BuiltInRegistries.ATTRIBUTE)entries.add(new RegistryEntry(BuiltInRegistries.ATTRIBUTE.getKey(attribute).toString(),BuiltInRegistries.ATTRIBUTE.getKey(attribute).toString(),"attribute"));
        for(var block:BuiltInRegistries.BLOCK)entries.add(new RegistryEntry(BuiltInRegistries.BLOCK.getKey(block).toString(),block.getDescriptionId(),"block"));
        for(var level:server.getAllLevels())entries.add(new RegistryEntry(level.dimension().identifier().toString(),level.dimension().identifier().toString(),"dimension"));
        server.registryAccess().lookupOrThrow(Registries.BIOME).keySet().forEach(id->entries.add(new RegistryEntry(id.toString(),id.toString(),"biome")));
        server.registryAccess().lookupOrThrow(Registries.STRUCTURE).keySet().forEach(id->entries.add(new RegistryEntry(id.toString(),id.toString(),"structure")));
        for(var reason:EntitySpawnReason.values())entries.add(new RegistryEntry(reason.name().toLowerCase(Locale.ROOT),reason.name().toLowerCase(Locale.ROOT),"reason"));
        entries.add(new RegistryEntry("unknown","unknown","reason"));
        for(String weather:List.of("clear","rain","thunder"))entries.add(new RegistryEntry(weather,weather,"weather"));
        cached=List.copyOf(entries);return cached;
    }
    public static List<String> validate(JsonObject json) {
        List<String> errors=new ArrayList<>();validate("",json,errors);return errors;
    }
    private static void validate(String path,JsonElement value,List<String> errors){
        if(value==null||value.isJsonNull())return;
        if(value.isJsonObject())for(var e:value.getAsJsonObject().entrySet()){
            String child=path.isEmpty()?e.getKey():path+"."+e.getKey();
            if(path.equals("then.attributes.custom")){check("attribute",e.getKey(),child,errors);attributeRange(e.getKey(),e.getValue(),child,errors);}
            if(path.equals("then.attributes")){
                String id=switch(e.getKey()){case "maxHealth"->"max_health";case "movementSpeed"->"movement_speed";case "attackDamage"->"attack_damage";case "followRange"->"follow_range";case "knockbackResistance"->"knockback_resistance";case "scale"->"scale";default->"";};
                if(!id.isEmpty())attributeRange("minecraft:"+id,e.getValue(),child,errors);
            }
            validate(child,e.getValue(),errors);
        }
        else if(value.isJsonArray()){for(var v:value.getAsJsonArray())validate(path,v,errors);}
        else if(value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString()){
            String id=value.getAsString();if(id.equals("*"))return;
            if(path.equals("when.entities")||path.equals("then.behavior.hostileTo")||path.equals("then.behavior.peacefulTo"))check("entity",id,path,errors);
            if(path.endsWith(".item")&&(path.startsWith("then.equipment.")||path.startsWith("then.loot.add")))check("item",id,path,errors);
            String kind=switch(path){case "when.dimensions"->"dimension";case "when.biomes"->"biome";case "when.structures"->"structure";case "when.spawnReasons"->"reason";case "when.weather"->"weather";default->"";};
            if(!kind.isEmpty()&&(cached==null||cached.stream().noneMatch(e->e.kind().equals(kind)&&e.id().equalsIgnoreCase(id))))errors.add(path+": unknown "+kind+" "+id);
            if(Set.of("then.behavior.pillarBlock","then.behavior.treeBlock","then.behavior.flowerBlock").contains(path))check("block",id,path,errors);
        }
    }
    private static void attributeRange(String id,JsonElement value,String path,List<String> errors){
        if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())return;
        try{var attribute=BuiltInRegistries.ATTRIBUTE.getOptional(Identifier.parse(id));if(attribute.isPresent()&&attribute.get() instanceof RangedAttribute ranged){double number=value.getAsDouble();if(number<ranged.getMinValue()||number>ranged.getMaxValue())errors.add(path+" must be between "+ranged.getMinValue()+" and "+ranged.getMaxValue()+".");}}catch(Exception e){errors.add(path+": invalid attribute value.");}
    }
    private static void check(String kind,String id,String path,List<String> errors){
        try{
            var key=Identifier.parse(id);
            boolean valid=switch(kind){case "entity"->BuiltInRegistries.ENTITY_TYPE.containsKey(key);case "item"->BuiltInRegistries.ITEM.containsKey(key);case "attribute"->BuiltInRegistries.ATTRIBUTE.containsKey(key);default->BuiltInRegistries.BLOCK.containsKey(key);};
            if(!valid)errors.add(path+": unknown "+kind+" "+id);
        }catch(Exception e){errors.add(path+": invalid "+kind+" identifier.");}
    }
}
