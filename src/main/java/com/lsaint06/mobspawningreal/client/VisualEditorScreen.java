package com.lsaint06.mobspawningreal.client;

import com.google.gson.*;
import com.lsaint06.mobspawningreal.client.widget.*;
import com.lsaint06.mobspawningreal.dashboard.DashboardModels.*;
import com.lsaint06.mobspawningreal.editor.*;
import com.lsaint06.mobspawningreal.editor.EditorModels.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import static com.lsaint06.mobspawningreal.client.widget.DashboardTheme.*;

/** Draft forms never touch disk. All saved changes are validated by the logical server. */
public final class VisualEditorScreen extends Screen {
    private static final String[] SECTIONS={"Dashboard","Mob Browser","Rules","Spawn Settings","Attributes","Equipment","Loot","AI Behaviors","Diagnostics"};
    private final UUID session;
    private State state;private Snapshot snapshot;
    private List<RegistryEntry> registry=List.of();private final Map<String,String> names=new HashMap<>();
    private RuleDraft draft;private String selectedKey="",selectedMob="",fingerprint="",section="Dashboard",query="",picker="",pickerPath="",filter="All";
    private long baseRevision,pending=-1,pendingAt;private boolean creating,compactDetails,reverse,error,conflict;
    private final Map<String,Integer> mobCounts=new HashMap<>(),enabledCounts=new HashMap<>();
    private boolean smallForm;
    private int fieldPage;private String message="Loading server configuration...";
    private Runnable confirmation;private String confirmText="";
    private final Map<String,String> invalid=new HashMap<>();
    private final List<Label> labels=new ArrayList<>();
    private record Label(int x,int y,int width,String text,int color,String tooltip) {}
    private EditorChoiceList choices;private int formX,formY,formW,bodyH;private boolean wide;
    private double browserScroll;
    public VisualEditorScreen(UUID session){super(Component.literal("Mob Spawning Real — Visual Editor"));this.session=session;}
    public UUID session(){return session;}
    private boolean dirty(){return draft!=null&&(draft.dirty()||creating)||!invalid.isEmpty();}
    @Override protected void init(){
        if(choices!=null)browserScroll=choices.scroll();
        labels.clear();wide=width>=800&&height>=360;int x=10,y=10,w=width-20;
        button(x,y+25,Math.max(55,(w-15)/4),snapshot==null?"Mod: ...":snapshot.enabled()?"Mod: ON":"Mod: OFF",()->runtime(Action.GLOBAL,"",snapshot!=null&&!snapshot.enabled()),snapshot!=null,"Pause all processing; rule settings are preserved.");
        int bw=(w-15)/4;
        if(!wide&&compactDetails&&draft!=null){
            button(x+bw+5,y+25,bw,"Save Changes",this::save,dirty()&&invalid.isEmpty()&&!conflict,"Save this draft to the server.");
            button(x+2*(bw+5),y+25,bw,"Discard",this::discard,dirty()||conflict,"Load saved server values.");
        }else{
            button(x+bw+5,y+25,bw,"Reload",()->guard(()->runtime(Action.RELOAD,"",false)),snapshot!=null,"Reload JSON files and runtime overrides from the server.");
            button(x+2*(bw+5),y+25,bw,"Refresh",()->runtime(Action.REFRESH,"",false),true,"Fetch saved server state. Unsaved drafts are retained.");
        }
        button(x+3*(bw+5),y+25,bw,"Done",this::onClose,true,"Close the editor.");
        int top=y+54;bodyH=Math.max(30,height-top-47);int sidebar=wide?128:0;
        if(wide){
            String[] icons={"D","M","R","S","A","E","L","B","!"};
            for(int i=0;i<SECTIONS.length;i++){String s=SECTIONS[i];var b=button(x,top+i*24,sidebar-6,"["+icons[i]+"] "+s,()->navigate(s),true,"Open "+s);b.accent(section.equals(s));}
        }else{
            button(x,top,w/2-3,section+" >",()->{picker="section";query="";compactDetails=false;rebuildWidgets();},true,"Choose a section; your draft remains intact.");
            button(x+w/2+3,top,w/2-3,compactDetails?"Browse mobs / rules":draft==null?"Create Rule":"Edit selected rule",()->{compactDetails=!compactDetails;rebuildWidgets();},state!=null,"Switch between browser and editor on small windows.");
            top+=25;bodyH-=25;
        }
        int centerX=x+sidebar,centerW=wide?Math.max(210,(w-sidebar)/3):w;
        formX=wide?centerX+centerW+8:centerX;formW=wide?w-sidebar-centerW-8:w;formY=top;
        boolean browse=wide||!compactDetails;
        if(browse){
            var search=addRenderableWidget(new EditBox(font,centerX,top,centerW,20,Component.literal("Search mobs, rules, items")));
            search.setMaxLength(256);search.setHint(Component.literal("Search name or registry ID"));search.setValue(query);search.setResponder(v->{query=v;updateChoices();});
            String label=picker.isEmpty()?filter:"Picking "+picker;
            button(centerX,top+24,centerW/2-2,label,()->cycleFilter(),true,"Filter mobs or rules. Picker results use the server registry.");
            button(centerX+centerW/2+2,top+24,centerW/2-2,reverse?"Sort: Z–A":"Sort: A–Z",()->{reverse=!reverse;updateChoices();},true,"Sort by display name.");
            choices=addRenderableWidget(new EditorChoiceList(centerX,top+48,centerW,Math.max(1,bodyH-48),this::choose));updateChoices();choices.scroll(browserScroll);
        }else choices=null;
        if(wide||compactDetails)form();
        if(confirmation!=null)modal();
    }
    private DashboardButton button(int x,int y,int w,String text,Runnable action,boolean active,String tip){
        var b=addRenderableWidget(new DashboardButton(x,y,Math.max(15,w),text,action));b.active=active&&pending<0;
        b.setTooltip(Tooltip.create(Component.literal(tip)));return b;
    }
    private void label(int x,int y,int w,String text,int color,String tip){labels.add(new Label(x,y,w,text,color,tip));}
    private void navigate(String target){section=target;picker="";filter="All";query="";fieldPage=0;rebuildWidgets();}
    private String name(RegistryEntry entry){return names.getOrDefault(entry.id(),entry.id());}
    private void updateChoices(){
        if(choices==null)return;List<EditorChoiceList.Choice> rows=new ArrayList<>();String q=query.toLowerCase(Locale.ROOT);
        if(picker.equals("section")){
            for(String s:SECTIONS)if(s.toLowerCase(Locale.ROOT).contains(q))rows.add(new EditorChoiceList.Choice(s,s,"Navigation","Draft retained",GREEN));
        }else if(!picker.isEmpty()||section.equals("Mob Browser")){
            String kind=picker.isEmpty()?"entity":picker;
            for(var entry:registry)if(entry.kind().equals(kind)){
                String name=name(entry),id=entry.id();if(!(name+" "+id).toLowerCase(Locale.ROOT).contains(q))continue;
                long count=mobCounts.getOrDefault(id,0)+mobCounts.getOrDefault("*",0);
                long enabled=enabledCounts.getOrDefault(id,0)+enabledCounts.getOrDefault("*",0);
                if(picker.isEmpty()&&((filter.equals("Configured")&&count==0)||(filter.equals("Unconfigured")&&count>0)))continue;
                String origin=id.startsWith("minecraft:")?"Vanilla":id.contains(":")?"Mod: "+id.substring(0,id.indexOf(':')):"Server option";
                rows.add(new EditorChoiceList.Choice(id,name,id,origin+(kind.equals("entity")?" | "+count+" rules | "+enabled+" enabled":""),enabled>0?GREEN:MUTED));
            }
        }else if(snapshot!=null){
            for(var row:snapshot.rules()){
                if(!row.matches(query))continue;
                if(filter.equals("Enabled")&&(!row.enabled()||!row.valid())||filter.equals("Disabled")&&row.enabled()||filter.equals("Invalid")&&row.valid())continue;
                if(section.equals("Diagnostics")&&row.valid())continue;
                rows.add(new EditorChoiceList.Choice(row.key(),row.id(),row.targets(),row.status(snapshot.enabled())+" | Priority "+row.priority()+" | "+row.source(),!row.valid()?RED:row.enabled()?GREEN:MUTED));
            }
        }
        Comparator<EditorChoiceList.Choice> comparator=Comparator.comparing(EditorChoiceList.Choice::title,String.CASE_INSENSITIVE_ORDER);
        rows.sort(reverse?comparator.reversed():comparator);choices.update(rows,picker.isEmpty()&& !section.equals("Mob Browser")?selectedKey:selectedMob);
    }
    private void cycleFilter(){
        if(!picker.isEmpty()){picker="";query="";rebuildWidgets();return;}
        String[] filters=section.equals("Mob Browser")?new String[]{"All","Configured","Unconfigured"}:new String[]{"All","Enabled","Disabled","Invalid"};
        filter=filters[(Arrays.asList(filters).indexOf(filter)+1)%filters.length];rebuildWidgets();
    }
    private void choose(String key){
        if(pending>=0)return;
        if(picker.equals("section")){picker="";compactDetails=!key.equals("Mob Browser")&&draft!=null;navigate(key);return;}
        if(!picker.isEmpty()){pick(key);return;}
        if(section.equals("Mob Browser")){
            guard(()->{selectedMob=key;var doc=state==null?Optional.<RuleDocumentStore.Document>empty():state.documents().stream().filter(d->targets(d.json(),key)).findFirst();if(doc.isPresent())load(doc.get());else {draft=null;selectedKey="";creating=false;}fieldPage=0;rebuildWidgets();});
        }else if(!key.equals(selectedKey))guard(()->{selectedKey=key;loadSelected();fieldPage=0;rebuildWidgets();});
    }
    private static boolean targets(String json,String id){try{var v=JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("when");if(v==null||!v.has("entities")||v.get("entities").isJsonNull())return true;var entities=v.getAsJsonArray("entities");if(entities.isEmpty())return true;for(var e:entities)if(e.isJsonPrimitive()&&(e.getAsString().equals(id)||e.getAsString().equals("*")))return true;}catch(Exception ignored){}return false;}
    private void load(RuleDocumentStore.Document doc){draft=new RuleDraft(doc.json());selectedKey=doc.key();fingerprint=doc.fingerprint();baseRevision=state.revision();creating=false;invalid.clear();conflict=false;}
    private void loadSelected(){if(state==null)return;var doc=state.documents().stream().filter(d->d.key().equals(selectedKey)).findFirst();if(doc.isPresent())load(doc.get());else {draft=null;creating=false;invalid.clear();}}
    private Set<String> ids(){var ids=new HashSet<String>();if(snapshot!=null)for(var r:snapshot.rules())ids.add(r.id());return ids;}
    private void create(boolean duplicate){
        if(state==null)return;
        JsonObject root=duplicate&&draft!=null?draft.root().deepCopy():new JsonObject();
        String mob=selectedMob.isEmpty()?"minecraft:zombie":selectedMob;
        String base=duplicate&&root.has("id")&&root.get("id").isJsonPrimitive()?root.get("id").getAsString()+"_copy":mob.substring(mob.indexOf(':')+1);
        root.addProperty("id",RuleDraft.uniqueId(base,ids()));if(!duplicate)root.addProperty("enabled",true);
        if(!root.has("priority"))root.addProperty("priority",100);
        if(!duplicate){var when=new JsonObject();var entities=new JsonArray();entities.add(mob);when.add("entities",entities);root.add("when",when);}
        draft=new RuleDraft(root.toString());creating=true;selectedKey="";fingerprint=state.fingerprint();baseRevision=state.revision();invalid.clear();conflict=false;
        section="Rules";fieldPage=0;compactDetails=true;message="New rule draft — set an ID and review settings, then Save Changes.";rebuildWidgets();
    }
    private void form(){
        int x=formX,y=formY,w=formW;
        smallForm=!wide;
        label(x+6,y+4,w-12,section, GREEN,"Choose a section in navigation; your current draft is retained.");
        if(section.equals("Diagnostics")){
            int dy=y+23;boolean found=false;
            if(snapshot!=null)for(var row:snapshot.rules())if(row.key().equals(selectedKey))for(var e:row.errors()){
                found=true;label(x+6,dy,w-12,e,RED,e);dy+=15;if(dy>y+bodyH-35)break;
            }
            if(!found)label(x+6,dy,w-12,"No validation errors for this selection.",GREEN,"");
            return;
        }
        if(draft==null){
            label(x+6,y+23,w-12,selectedMob.isEmpty()?"Select a rule or browse available mobs.":selectedMob,TEXT,selectedMob);
            button(x+5,y+43,w-10,"Create Rule",()->create(false),state!=null,"Creates a draft; no files change until Save Changes.");
            label(x+6,y+71,w-12,"New rules affect matching mobs after saving.",MUTED,"");return;
        }
        String id=draft.get("id")==null?"New rule":draft.get("id").isJsonPrimitive()?draft.get("id").getAsString():"Invalid rule ID";
        String related=selectedKey;
        if(!selectedMob.isEmpty()&&snapshot!=null){var titles=snapshot.rules().stream().filter(r->r.valid()&&r.enabled()&&(r.targets().equals("All mobs")||Arrays.asList(r.targets().split(",\\s*")).contains(selectedMob)||r.targets().contains("*"))).map(RuleRow::id).toList();related+="\nPotential enabled rules for "+selectedMob+": "+String.join(", ",titles)+". Conditions and priority still determine which apply.";}
        label(x+6,y+20,w-12,id+(dirty()?" • Unsaved changes":""),dirty()?AMBER:TEXT,related);
        int mini=(w-15)/2;
        if(!smallForm){
            button(x+5,y+36,mini,"New",()->guard(()->create(false)),state!=null,"Create a fresh mob rule.");
            button(x+10+mini,y+36,mini,"Duplicate",()->guard(()->create(true)),state!=null,"Copy this saved rule into a new uniquely named draft.");
        }
        if(section.equals("Diagnostics")){
            int dy=y+(smallForm?38:66);if(snapshot!=null)for(var row:snapshot.rules())if(row.key().equals(selectedKey))for(var e:row.errors()){label(x+6,dy,w-12,e,RED,e);dy+=15;if(dy>y+bodyH-(smallForm?24:90))break;}
        }else{
            String formSection=section.equals("Mob Browser")?"Rules":section;
            var fields=new ArrayList<>(EditorSchema.fields(draft.root(),formSection));
            if(smallForm&&(formSection.equals("Rules")||formSection.equals("Dashboard")))for(String action:new String[]{"New rule","Duplicate rule","Toggle runtime","Reset override","Delete rule"})fields.add(new EditorSchema.Field(action,action,"action","",action+" for this rule."));
            int available=Math.max(1,(bodyH-(smallForm?62:132))/43);int pages=Math.max(1,(fields.size()+available-1)/available);fieldPage=Math.max(0,Math.min(fieldPage,pages-1));
            int start=fieldPage*available;
            for(int i=start;i<Math.min(fields.size(),start+available);i++)field(fields.get(i),x+5,y+(smallForm?34:64)+(i-start)*43,w-10);
            int fy=y+bodyH-(smallForm?21:66);
            button(x+5,fy,45,"<",()->{fieldPage--;rebuildWidgets();},fieldPage>0,"Previous settings page.");
            label(x+56,fy+6,w-111,(fieldPage+1)+" / "+pages+" settings",MUTED,formSection+" fields, including advanced settings.");
            button(x+w-50,fy,45,">",()->{fieldPage++;rebuildWidgets();},fieldPage<pages-1,"Next settings page.");
        }
        if(smallForm)return;
        int fy=y+bodyH-42;
        button(x+5,fy,mini,"Save Changes",this::save,dirty()&&invalid.isEmpty()&&!conflict,"Validate and save this draft on the server.");
        button(x+10+mini,fy,mini,"Discard",this::discard,dirty()||conflict,"Discard your draft and load current server values.");
        var row=snapshot==null?null:snapshot.rules().stream().filter(r->r.key().equals(selectedKey)).findFirst().orElse(null);
        int third=(w-20)/3;
        button(x+5,fy+24,third,row!=null&&row.enabled()?"Disable":"Enable",()->{if(row!=null)runtime(Action.RULE,row.id(),!row.enabled());},row!=null&&row.valid()&&!dirty(),"Save a runtime override, separate from the definition's Enabled setting.");
        button(x+10+third,fy+24,third,"Use JSON",()->{if(row!=null)runtime(Action.RESET,row.id(),false);},row!=null&&row.override()!=null&&!dirty(),"Reset the runtime override.");
        button(x+15+2*third,fy+24,third,"Delete",()->confirm("Delete this rule? A backup will be created.",()->sendEdit("DELETE","")),!creating&&draft!=null&&!dirty(),"Delete only this rule, preserving siblings in its source file.");
    }
    private void field(EditorSchema.Field field,int x,int y,int w){
        label(x+2,y,w-4,field.label(),invalid.containsKey(field.path())?RED:MUTED,field.help());
        JsonElement value=draft.get(field.path());String path=field.path();
        if(field.kind().equals("unsetGroup")){
            button(x,y+13,w,"Clear this equipment slot",()->{draft.unset(path);message="Equipment slot removed from draft.";rebuildWidgets();},value!=null,field.help());return;
        }
        if(field.kind().equals("action")){
            var row=snapshot==null?null:snapshot.rules().stream().filter(r->r.key().equals(selectedKey)).findFirst().orElse(null);
            boolean can=path.equals("New rule")||path.equals("Duplicate rule")||row!=null&&!dirty();
            button(x,y+13,w,field.label(),()->{switch(path){
                case "New rule"->guard(()->create(false));case "Duplicate rule"->guard(()->create(true));
                case "Toggle runtime"->{if(row!=null&&row.valid())runtime(Action.RULE,row.id(),!row.enabled());}
                case "Reset override"->{if(row!=null)runtime(Action.RESET,row.id(),false);}
                case "Delete rule"->confirm("Delete this rule? A backup will be created.",()->sendEdit("DELETE",""));
            }},can,field.help());return;
        }
        if(field.kind().equals("array")||field.kind().equals("map")){
            button(x,y+13,w,field.label(),()->startPicker(field.registry(),path),true,field.help());return;
        }
        if(field.kind().equals("remove")){
            button(x,y+13,w,"Remove this drop",()->{int split=path.lastIndexOf('/');draft.get(path.substring(0,split)).getAsJsonArray().remove(Integer.parseInt(path.substring(split+1)));message="Unsaved changes";rebuildWidgets();},true,field.help());return;
        }
        int pickerW=field.registry().isEmpty()?0:48;int unsetW=38;int controlW=Math.max(20,w-unsetW-pickerW-8);
        if(field.kind().equals("boolean")){
            boolean valid=value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isBoolean();
            String text=value==null?"Unset":!valid?"Invalid value":value.getAsBoolean()?"Enabled":"Disabled";
            button(x,y+13,controlW,text,()->{draft.set(path,new JsonPrimitive(!valid||!value.getAsBoolean()));message="Unsaved changes";rebuildWidgets();},true,field.help());
        }else{
            var box=addRenderableWidget(new EditBox(font,x,y+13,controlW,20,Component.literal(field.label())));box.setMaxLength(2048);
            box.setHint(Component.literal("Unset / default"));box.setTooltip(Tooltip.create(Component.literal(field.help())));
            String text=invalid.getOrDefault(path,value==null?"":field.kind().equals("list")&&value.isJsonArray()?join(value):value.isJsonPrimitive()?value.getAsString():value.toString());box.setValue(text);
            box.setResponder(v->{
                try{
                    if(v.isBlank())draft.unset(path);
                    else switch(field.kind()){
                        case "number"->{double n=Double.parseDouble(v);if(!Double.isFinite(n))throw new NumberFormatException();draft.set(path,new JsonPrimitive(n));}
                        case "integer"->draft.set(path,new JsonPrimitive(Integer.parseInt(v)));
                        case "list"->{var array=new JsonArray();for(var s:v.split(","))if(!s.trim().isEmpty())array.add(s.trim());draft.set(path,array);}
                        default->draft.set(path,new JsonPrimitive(v));
                    }
                    invalid.remove(path);box.setTextColor(TEXT);message="Unsaved changes — choose Save Changes when ready.";error=false;
                }catch(Exception e){invalid.put(path,v);box.setTextColor(RED);message=field.label()+": enter a valid "+field.kind()+".";error=true;}
                updateSaveButtons();
            });box.active=pending<0;
        }
        button(x+controlW+3,y+13,unsetW,"Unset",()->{draft.unset(path);invalid.remove(path);message="Setting reset to unset in draft.";rebuildWidgets();},true,"Remove this explicit setting; retain default behavior.");
        if(pickerW>0)button(x+w-pickerW,y+13,pickerW,"Pick",()->startPicker(field.registry(),path),true,"Search registered "+field.registry()+" entries.");
    }
    private void updateSaveButtons(){
        // EditBox responders must not rebuild focused widgets while typing.
        for(var widget:renderables)if(widget instanceof DashboardButton b){
            String title=b.getMessage().getString();if(title.equals("Save Changes"))b.active=pending<0&&dirty()&&invalid.isEmpty()&&!conflict;
            if(title.equals("Discard"))b.active=pending<0&&(dirty()||conflict);
            if(title.equals("Delete")||title.equals("Enable")||title.equals("Disable")||title.equals("Use JSON"))b.active=false;
        }
    }
    private static String join(JsonElement value){List<String> values=new ArrayList<>();for(var e:value.getAsJsonArray())values.add(e.isJsonPrimitive()?e.getAsString():e.toString());return String.join(", ",values);}
    private void startPicker(String kind,String path){picker=kind;pickerPath=path;query="";compactDetails=false;message="Choose a registered "+kind+"; draft changes are not saved yet.";rebuildWidgets();}
    private void pick(String id){
        String path=pickerPath;
        if(path.equals("then/loot/add")){
            var value=draft.get(path);var array=value==null||!value.isJsonArray()?new JsonArray():value.getAsJsonArray();var drop=new JsonObject();drop.addProperty("item",id);drop.addProperty("min",1);drop.addProperty("max",1);drop.addProperty("chance",1.0);array.add(drop);draft.set(path,array);
        }else if(path.equals("then/attributes/custom")){String attributePath=path+"/"+RuleDraft.encode(id);if(draft.get(attributePath)==null)draft.set(attributePath,new JsonPrimitive(0));}
        else if(path.startsWith("when/")||path.endsWith("/hostileTo")||path.endsWith("/peacefulTo")){
            var value=draft.get(path);var array=value==null||!value.isJsonArray()?new JsonArray():value.getAsJsonArray();boolean found=false;for(var v:array)if(v.isJsonPrimitive()&&v.getAsString().equals(id))found=true;if(!found)array.add(id);draft.set(path,array);
        }else draft.set(path,new JsonPrimitive(id));
        picker="";query="";compactDetails=true;message="Unsaved changes";rebuildWidgets();
    }
    private void save(){
        if(draft==null||!invalid.isEmpty())return;var errors=RuleDocumentStore.validate(draft.root());
        if(!errors.isEmpty()){feedback(String.join("; ",errors),true,0);return;}sendEdit(creating?"CREATE":"SAVE",draft.json());
    }
    private void discard(){guard(()->{if(selectedKey.isEmpty()){draft=null;creating=false;invalid.clear();conflict=false;}else loadSelected();message="Draft discarded; saved values loaded.";rebuildWidgets();});}
    private void sendEdit(String operation,String json){
        if(pending>=0)return;pending=DashboardClient.edit(session,baseRevision,operation,selectedKey,fingerprint,json);pendingAt=System.nanoTime();
        if(pending<0){feedback("Disconnected from server.",true,0);return;}message="Saving…";error=false;rebuildWidgets();
    }
    private void runtime(Action action,String id,boolean enabled){
        if(pending>=0)return;pending=DashboardClient.send(session,snapshot==null?0:snapshot.revision(),action,id,enabled);pendingAt=System.nanoTime();message="Waiting for server…";error=false;rebuildWidgets();
    }
    public void updateSnapshot(Snapshot value){
        if(snapshot!=null&&value.revision()<snapshot.revision())return;snapshot=value;
        mobCounts.clear();enabledCounts.clear();for(var row:value.rules())for(String target:row.targets().split(",\\s*")){if(target.equals("All mobs"))target="*";mobCounts.merge(target,1,Integer::sum);if(row.valid()&&row.enabled())enabledCounts.merge(target,1,Integer::sum);}
        if(value.request()==pending){pending=-1;message=value.message();error=value.error();}
        updateChoices();if(pending<0)rebuildWidgets();
    }
    public void updateState(State value){
        if(state!=null&&value.revision()<state.revision())return;state=value;
        if(value.registry()!=null){registry=value.registry();names.clear();for(var r:registry)names.put(r.id(),Component.translatable(r.name()).getString());}
        boolean response=value.request()!=0&&value.request()==pending;
        if(response){pending=-1;message=value.message();error=!value.success();
            if(value.success()){selectedKey=value.selectedKey();creating=false;loadSelected();if(selectedKey.isEmpty())draft=null;}
            else if(value.message().contains("changed"))conflict=true;
        }else if(!dirty()){
            if(selectedKey.isEmpty()&&!value.documents().isEmpty())selectedKey=value.documents().getFirst().key();loadSelected();
            message=value.message();error=!value.success();
        }else if(value.revision()!=baseRevision||!value.fingerprint().equals(fingerprint)){conflict=true;message="Configuration changed on server. Draft retained; Discard loads saved values.";error=true;}
        rebuildWidgets();
    }
    public void feedback(String text,boolean isError,long request){if(request==pending||request==0)pending=-1;message=text;error=isError;rebuildWidgets();}
    private void guard(Runnable action){if(dirty())confirm("Discard unsaved changes before continuing?",()->{invalid.clear();loadSelected();action.run();});else action.run();}
    private void confirm(String text,Runnable action){confirmation=action;confirmText=text;rebuildWidgets();}
    private void modal(){
        // Disable the existing controls, then put a confirmation card on top.
        for(var widget:renderables)if(widget instanceof AbstractWidget b)b.active=false;
        int w=Math.min(340,width-24),x=(width-w)/2,y=Math.max(40,height/2-25);
        label(x+8,y-16,w-16,confirmText,AMBER,confirmText);
        var yes=addRenderableWidget(new DashboardButton(x,y,w/2-3,"Confirm",()->{var action=confirmation;confirmation=null;action.run();rebuildWidgets();}));
        var no=addRenderableWidget(new DashboardButton(x+w/2+3,y,w/2-3,"Cancel",()->{confirmation=null;rebuildWidgets();}));setInitialFocus(no);
    }
    @Override public void onClose(){if(pending>=0){message="Waiting for the server; close after the current request completes.";return;}if(confirmation!=null){confirmation=null;rebuildWidgets();return;}guard(()->minecraft.gui.setScreen(null));}
    @Override public void removed(){DashboardClient.close(session);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void tick(){if(minecraft.getConnection()==null){minecraft.gui.setScreen(null);return;}if(pending>=0&&System.nanoTime()-pendingAt>10_000_000_000L)feedback("No response yet. Refresh to check server state; draft retained.",true,0);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float a){g.fillGradient(0,0,width,height,0xF0101719,0xFA182022);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float a){
        panel(g,7,7,width-14,height-14,BG);g.text(font,fit(font,"■ Mob Spawning Real 1.4 — Visual Editor",width-36),16,17,GREEN,false);
        if(wide||compactDetails)panel(g,formX,formY,formW,bodyH,PANEL);
        for(var label:labels){g.text(font,fit(font,label.text(),label.width()),label.x(),label.y(),label.color(),false);if(mx>=label.x()&&mx<label.x()+label.width()&&my>=label.y()&&my<label.y()+11&&!label.tooltip().isEmpty())g.setTooltipForNextFrame(font,Component.literal(label.tooltip()),mx,my);}
        String status=dirty()?"Unsaved changes | "+message:message;
        g.text(font,fit(font,status,width-32),16,height-22,error?RED:dirty()||pending>=0?AMBER:GREEN,false);
        if(my>=height-28)g.setTooltipForNextFrame(font,Component.literal(status),mx,my);
        super.extractRenderState(g,mx,my,a);
        if(confirmation!=null){
            g.fill(0,0,width,height,0xDD0A1012);int w=Math.min(340,width-24),x=(width-w)/2,y=Math.max(40,height/2-25);
            panel(g,x-5,y-38,w+10,70,CARD);g.text(font,fit(font,confirmText,w-16),x+8,y-25,AMBER,false);
            if(my>=y-35&&my<y)g.setTooltipForNextFrame(font,Component.literal(confirmText),mx,my);
            for(int i=Math.max(0,renderables.size()-2);i<renderables.size();i++)renderables.get(i).extractRenderState(g,mx,my,a);
        }
    }
}
