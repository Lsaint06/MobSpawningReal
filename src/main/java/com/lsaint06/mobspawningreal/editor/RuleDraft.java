package com.lsaint06.mobspawningreal.editor;
import com.google.gson.*;
import java.util.*;

/** Raw-tree drafts preserve omission, unknown fields, and exact boolean/number semantics. */
public final class RuleDraft {
    private JsonObject root,saved;
    public RuleDraft(String json){root=JsonParser.parseString(json).getAsJsonObject();saved=root.deepCopy();}
    public JsonObject root(){return root;}
    public boolean dirty(){return !root.equals(saved);}
    public String json(){return root.toString();}
    public void discard(){root=saved.deepCopy();}
    public JsonElement get(String path){
        JsonElement current=root;
        for(String raw:path.split("/")){
            String part=decode(raw);
            if(current==null||current.isJsonNull())return null;
            if(current.isJsonArray()){int i;try{i=Integer.parseInt(part);}catch(Exception e){return null;}if(i<0||i>=current.getAsJsonArray().size())return null;current=current.getAsJsonArray().get(i);}
            else if(current.isJsonObject())current=current.getAsJsonObject().get(part);else return null;
        }
        return current==null||current.isJsonNull()?null:current;
    }
    public void set(String path,JsonElement value){
        String[] parts=path.split("/");JsonElement current=root;
        for(int i=0;i<parts.length;i++)parts[i]=decode(parts[i]);
        for(int i=0;i<parts.length-1;i++){
            if(current.isJsonArray()){
                var array=current.getAsJsonArray();int index=Integer.parseInt(parts[i]);var child=array.get(index);
                if(child.isJsonNull()||!child.isJsonObject()&&!child.isJsonArray()){child=new JsonObject();array.set(index,child);}current=child;
            }
            else{
                var object=current.getAsJsonObject();var child=object.get(parts[i]);
                if(child==null||child.isJsonNull()||!child.isJsonObject()&&!child.isJsonArray()){child=new JsonObject();object.add(parts[i],child);}current=child;
            }
        }
        String leaf=parts[parts.length-1];
        if(current.isJsonArray())current.getAsJsonArray().set(Integer.parseInt(leaf),value);
        else current.getAsJsonObject().add(leaf,value);
    }
    public void unset(String path){
        int split=path.lastIndexOf('/');var parent=split<0?root:get(path.substring(0,split));
        if(parent!=null&&parent.isJsonObject())parent.getAsJsonObject().remove(decode(path.substring(split+1)));
    }
    public static String encode(String segment){return segment.replace("~","~0").replace("/","~1");}
    private static String decode(String segment){return segment.replace("~1","/").replace("~0","~");}
    public static String uniqueId(String name,Set<String> existing){
        String base=name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]+","_").replaceAll("^_+|_+$","");if(base.isEmpty())base="mob_rule";
        if(base.length()>110)base=base.substring(0,110);
        String id=base;for(int i=2;existing.contains(id);i++)id=base+"_"+i;return id;
    }
}
