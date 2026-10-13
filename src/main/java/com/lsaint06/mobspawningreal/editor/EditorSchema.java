package com.lsaint06.mobspawningreal.editor;
import com.google.gson.*;
import com.lsaint06.mobspawningreal.rule.*;
import java.lang.reflect.*;
import java.util.*;

/** Enumerates the actual runtime schema rather than maintaining a second invented rule model. */
public final class EditorSchema {
    private EditorSchema() {}
    public record Field(String path,String label,String kind,String registry,String help) {}
    public static List<Field> fields(JsonObject root,String section){
        var result=new ArrayList<Field>();
        switch(section){
            case "Rules","Dashboard"->{
                add(result,"id",String.class);add(result,"priority",int.class);add(result,"enabled",boolean.class);add(result,"stopProcessing",boolean.class);
                group(result,root,"when/entities",new TypeTokenList().type());
                group(result,root,"then/name",NameActions.class);add(result,"then/outgoingDamage",Double.class);
            }
            case "Spawn Settings"->{group(result,root,"when",Conditions.class);group(result,root,"then/spawn",SpawnActions.class);group(result,root,"then/activeSpawn",ActiveSpawnActions.class);}
            case "Attributes"->group(result,root,"then/attributes",AttributeActions.class);
            case "Equipment"->{for(String slot:new String[]{"mainhand","offhand","head","chest","legs","feet"}){
                result.add(new Field("then/equipment/"+slot,"Clear "+slot,"unsetGroup","","Remove this slot assignment from the draft."));
                group(result,root,"then/equipment/"+slot,EquipmentEntry.class);
            }}
            case "Loot"->group(result,root,"then/loot",LootActions.class);
            case "AI Behaviors"->group(result,root,"then/behavior",BehaviorActions.class);
        }
        return result;
    }
    private static class TypeTokenList {public List<String> values;Type type(){try{return getClass().getField("values").getGenericType();}catch(Exception e){throw new IllegalStateException(e);}}}
    private static void group(List<Field> result,JsonObject root,String path,Type type){
        if(type instanceof ParameterizedType p){
            var value=new RuleDraft(root.toString()).get(path);
            if(p.getRawType()==List.class){
                if(p.getActualTypeArguments()[0]==String.class){add(result,path,type);return;}
                result.add(new Field(path,"Add drop","array", "item","Add a drop, then edit its amount and probability."));
                if(value!=null&&value.isJsonArray())for(int i=0;i<value.getAsJsonArray().size();i++){
                    result.add(new Field(path+"/"+i,"Remove drop "+(i+1),"remove","","Removes only this draft drop."));
                    group(result,root,path+"/"+i,p.getActualTypeArguments()[0]);
                }
            }else if(p.getRawType()==Map.class){
                result.add(new Field(path,"Add custom attribute","map","attribute","Includes armor, armor toughness and compatible modded attributes."));
                if(value!=null&&value.isJsonObject())for(var e:value.getAsJsonObject().entrySet())add(result,path+"/"+RuleDraft.encode(e.getKey()),Double.class);
            }return;
        }
        Class<?> c=(Class<?>)type;
        if(c==String.class||c==Boolean.class||c==boolean.class||c==Integer.class||c==int.class||c==Double.class){add(result,path,type);return;}
        for(var field:c.getFields()){
            // These legacy schema fields are not evaluated by the current matcher; preserve but do not advertise them.
            if(c==Conditions.class&&Set.of("entityTags","biomeTags").contains(field.getName()))continue;
            group(result,root,path+"/"+field.getName(),field.getGenericType());
        }
    }
    private static void add(List<Field> result,String path,Type type){
        String name=path.substring(path.lastIndexOf('/')+1);
        String kind=type instanceof ParameterizedType?"list":type==Boolean.class||type==boolean.class?"boolean":type==int.class||type==Integer.class?"integer":type==Double.class?"number":"text";
        String registry=path.equals("when/entities")||path.endsWith("/hostileTo")||path.endsWith("/peacefulTo")?"entity":name.equals("item")?"item":"";
        registry=switch(path){case "when/dimensions"->"dimension";case "when/biomes"->"biome";case "when/structures"->"structure";case "when/spawnReasons"->"reason";case "when/weather"->"weather";case "then/behavior/pillarBlock","then/behavior/treeBlock","then/behavior/flowerBlock"->"block";default->registry;};
        String label=name.replaceAll("([a-z])([A-Z])","$1 $2");label=Character.toUpperCase(label.charAt(0))+label.substring(1);
        String[] parts=path.split("/");
        if(parts.length>1&&(Set.of("min","max","start","end","item","count","chance","dropOnDeath").contains(name)||path.startsWith("then/activeSpawn/"))){
            String parent=parts[parts.length-2];if(parent.matches("[0-9]+"))parent="Drop "+(Integer.parseInt(parent)+1);
            parent=parent.replaceAll("([a-z])([A-Z])","$1 $2");label=Character.toUpperCase(parent.charAt(0))+parent.substring(1)+" • "+label;
        }
        String help=path.replace('/','.')+". Unset keeps runtime defaults or allows lower-priority rules to fill this setting.";
        if(name.equals("priority"))help="Higher priority wins. Lower priority rules fill fields left unset.";
        if(name.equals("stopProcessing"))help="Stop evaluating lower-priority rules after this rule matches.";
        if(name.equals("chance"))help="Probability from 0 to 1: 0.25 means 25%.";
        if(name.equals("maxHealth"))help="Health in HP; 2 HP = one heart. Affects newly spawned mobs.";
        if(name.endsWith("Ticks"))help="Ticks: 20 ticks is one second at normal server speed.";
        result.add(new Field(path,label,kind,registry,help));
    }
}
