package com.lsaint06.mobspawningreal.client;
import com.lsaint06.mobspawningreal.client.widget.*;
import com.lsaint06.mobspawningreal.dashboard.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import static com.lsaint06.mobspawningreal.dashboard.DashboardModels.*;
import static com.lsaint06.mobspawningreal.client.widget.DashboardTheme.*;

public final class DashboardScreen extends Screen {
    private final UUID session;
    private DashboardLayout layout;
    private Snapshot snapshot;
    private String query="",selected="",message="Loading server configuration...";
    private boolean error,details;
    private Category category=Category.SPAWN;
    private long pending=-1, pendingAt;
    private RuleList list;
    private CategoryPanel panel;
    private DashboardButton global,reload,refresh,view,ruleToggle,reset;
    private EditBox search;
    private final List<DashboardButton> categories=new ArrayList<>();
    private List<RuleRow> filtered=List.of();
    public DashboardScreen(UUID session) { super(Component.literal("Mob Spawning Real"));this.session=session; }
    public UUID session(){return session;}
    @Override protected void init() {
        double previousScroll=list==null?0:list.scrollAmount();
        layout=DashboardLayout.of(width,height);categories.clear();
        int x=layout.x()+10,y=layout.y(),w=layout.width()-20;
        int gap=5,b=(w-3*gap)/4;
        global=addRenderableWidget(new DashboardButton(x,y+36,b,"Mod: ON",()->act(Action.GLOBAL,"",snapshot!=null&&!snapshot.enabled())));
        global.setTooltip(Tooltip.create(Component.literal("Pause or resume all Mob Spawning Real processing. Individual settings are preserved.")));
        reload=addRenderableWidget(new DashboardButton(x+b+gap,y+36,b,"Reload",()->act(Action.RELOAD,"",false)));
        reload.setTooltip(Tooltip.create(Component.literal("Read server JSON rules and runtime state again. Your rule files are never rewritten.")));
        refresh=addRenderableWidget(new DashboardButton(x+2*(b+gap),y+36,b,"Refresh",()->act(Action.REFRESH,"",false)));
        addRenderableWidget(new DashboardButton(x+3*(b+gap),y+36,b,"Done",this::onClose));
        int searchWidth=layout.wide()?w:w-85;
        search=addRenderableWidget(new EditBox(font,x,y+64,Math.max(20,searchWidth),20,Component.literal("Search rules, mobs, files or errors")));
        search.setMaxLength(256);search.setHint(Component.literal("Search rules, mobs, files..."));search.setValue(query);
        search.setResponder(value->{query=value;updateRows();});search.setTextColor(TEXT);
        if(!layout.wide())view=addRenderableWidget(new DashboardButton(x+w-80,y+64,80,details?"Rules":"Details",()->{details=!details;rebuildWidgets();}));
        else view=null;
        int bodyY=layout.contentY(),h=layout.contentHeight();
        int listWidth=layout.wide()?(w-10)/2:w;
        list=addRenderableWidget(new RuleList(x,bodyY,listWidth,h,this::select,row->act(Action.RULE,row.id(),!row.enabled())));
        int detailX=layout.wide()?x+listWidth+10:x,detailWidth=layout.wide()?w-listWidth-10:w;
        String[] labels={"Spawn","Attrs","Gear","Loot","AI"};
        int tabWidth=(detailWidth-4*3)/5;
        for(int i=0;i<Category.values().length;i++) {
            Category current=Category.values()[i];
            var button=addRenderableWidget(new DashboardButton(detailX+i*(tabWidth+3),bodyY,tabWidth,labels[i],()->{category=current;updateRows();}));
            button.setTooltip(Tooltip.create(Component.literal(current.title+" — configuration summary")));categories.add(button);
        }
        panel=addRenderableWidget(new CategoryPanel(detailX,bodyY+24,detailWidth,Math.max(1,h-49)));
        ruleToggle=addRenderableWidget(new DashboardButton(detailX,bodyY+h-20,(detailWidth-5)/2,"Enable rule",()->{var row=selectedRow();if(row!=null)act(Action.RULE,row.id(),!row.enabled());}));
        reset=addRenderableWidget(new DashboardButton(detailX+(detailWidth+5)/2,bodyY+h-20,(detailWidth-5)/2,"Use JSON",()->{var row=selectedRow();if(row!=null)act(Action.RESET,row.id(),false);}));
        reset.setTooltip(Tooltip.create(Component.literal("Remove the runtime override and follow this rule's enabled setting in JSON.")));
        updateRows(); list.scrollAmount(previousScroll); setInitialFocus(search);
    }
    public void updateSnapshot(Snapshot value) {
        if(value==null||snapshot!=null&&value.revision()<snapshot.revision())return;
        snapshot=value;
        if(value.request()==pending)pending=-1;
        if(pending<0||value.request()!=0) {message=value.message();error=value.error();}
        updateRows();
    }
    public void feedback(String text,boolean isError,long request) {
        if(request==pending||request==0)pending=-1;
        message=text;error=isError;updateRows();
    }
    private void act(Action action,String id,boolean enabled) {
        if(pending>=0||snapshot==null&&action!=Action.REFRESH)return;
        pending=DashboardClient.send(session,snapshot==null?0:snapshot.revision(),action,id,enabled);
        if(pending<0) {feedback("Disconnected from server.",true,0);return;}
        pendingAt=System.nanoTime();message=action==Action.RELOAD?"Reloading server configuration...":action==Action.REFRESH?"Refreshing from server...":"Saving to server...";error=false;updateRows();
    }
    private void select(RuleRow row) {selected=row.key();updateRows();}
    private RuleRow selectedRow() {return filtered.stream().filter(r->r.key().equals(selected)).findFirst().orElse(null);}
    private void updateRows() {
        if(list==null)return;
        filtered=snapshot==null?List.of():snapshot.rules().stream().filter(r->r.matches(query)).toList();
        if(filtered.stream().noneMatch(r->r.key().equals(selected)))selected=filtered.isEmpty()?"":filtered.getFirst().key();
        boolean busy=pending>=0;
        boolean showDetails=layout.wide()||details;
        list.visible=!details||layout.wide();list.update(filtered,selected,snapshot==null||snapshot.enabled(),busy);
        panel.visible=showDetails;panel.show(selectedRow(),category);
        for(int i=0;i<categories.size();i++){var button=categories.get(i);button.visible=showDetails;button.accent(Category.values()[i]==category);}
        var row=selectedRow();
        ruleToggle.visible=reset.visible=showDetails;
        ruleToggle.active=!busy&&row!=null&&row.valid();
        reset.active=ruleToggle.active&&row.override()!=null;
        ruleToggle.setMessage(Component.literal(row!=null&&row.enabled()?"Disable rule":"Enable rule"));ruleToggle.accent(row!=null&&row.enabled());
        global.active=reload.active=!busy&&snapshot!=null;
        refresh.active=!busy;
        global.setMessage(Component.literal(snapshot==null?"Mod: ...":snapshot.enabled()?"Mod: ON":"Mod: OFF"));global.accent(snapshot!=null&&snapshot.enabled());
        if(snapshot!=null)global.switchState(snapshot.enabled());
    }
    @Override public void tick() {
        if(minecraft.getConnection()==null){minecraft.gui.setScreen(null);return;}
        if(pending>=0&&System.nanoTime()-pendingAt>10_000_000_000L)feedback("No response yet. Refresh to check server state.",true,0);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float a) {
        g.fillGradient(0,0,width,height,0xEE101719,0xF0182022);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float a) {
        int x=layout.x(),y=layout.y(),w=layout.width(),h=layout.height();
        panel(g,x,y,w,h,BG);
        g.fill(x+1,y+1,x+w-1,y+29,PANEL);
        g.fill(x+11,y+10,x+25,y+24,0xFF385B3E);g.fill(x+14,y+13,x+22,y+21,GREEN);
        g.text(font,fit(font,"Mob Spawning Real",w-85),x+32,y+9,TEXT,false);
        g.text(font,"RULES",x+w-43,y+10,MUTED,false);
        String count=snapshot==null?"Connecting to server...":filtered.size()+" / "+snapshot.rules().size()+" rules"+(snapshot.enabled()?"":"  |  Globally paused");
        g.text(font,fit(font,count,w-20),x+10,y+88,snapshot!=null&&!snapshot.enabled()?AMBER:MUTED,false);
        g.horizontalLine(x+10,x+w-11,y+h-24,BORDER);
        g.text(font,fit(font,message,w-20),x+10,y+h-16,error?RED:pending>=0?AMBER:GREEN,false);
        super.extractRenderState(g,mx,my,a);
        if(my>=y+h-23 && my<y+h && mx>=x&&mx<x+w)
            g.setTooltipForNextFrame(font,Component.literal(message),mx,my);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void removed(){DashboardClient.close(session);}
}

