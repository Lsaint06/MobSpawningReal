package com.lsaint06.mobspawningreal.client.widget;
import com.lsaint06.mobspawningreal.dashboard.DashboardModels.RuleRow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.*;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

/** Virtualized rows share one focusable list; arrows select, Enter changes state. */
public final class RuleList extends AbstractWidget {
    private static final int ROW=56;
    private List<RuleRow> rows=List.of();
    private String selected="";
    private double scroll;
    private boolean global=true, pending;
    private final Consumer<RuleRow> select, toggle;
    public RuleList(int x,int y,int w,int h,Consumer<RuleRow> select,Consumer<RuleRow> toggle) {
        super(x,y,w,h,Component.literal("Configured mob rules. Use arrows to select; Enter to toggle."));
        this.select=select;this.toggle=toggle;
    }
    public void update(List<RuleRow> rows,String selected,boolean global,boolean pending) {
        this.rows=rows;this.selected=selected;this.global=global;this.pending=pending;clamp();
    }
    private void clamp() { scroll=Math.max(0,Math.min(scroll,Math.max(0,rows.size()*ROW-height))); }
    public double scrollAmount() { return scroll; }
    public void scrollAmount(double value) { scroll=value;clamp(); }
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float a) {
        var font=Minecraft.getInstance().font;
        DashboardTheme.panel(g,getX(),getY(),width,height,DashboardTheme.PANEL);
        g.enableScissor(getX()+1,getY()+1,getRight()-1,getBottom()-1);
        if(rows.isEmpty()) { g.text(font,"No matching rules",getX()+12,getY()+12,DashboardTheme.MUTED); }
        int first=(int)scroll/ROW;
        for(int i=first;i<rows.size() && i<=first+height/ROW+1;i++) {
            RuleRow row=rows.get(i); int x=getX()+4,y=getY()+i*ROW-(int)scroll+3,w=width-14;
            boolean chosen=row.key().equals(selected), hovered=mx>=x&&mx<x+w&&my>=Math.max(y,getY())&&my<Math.min(y+ROW-4,getBottom());
            int status=!row.valid()?DashboardTheme.RED:!row.enabled()?DashboardTheme.MUTED:!global?DashboardTheme.AMBER:DashboardTheme.GREEN;
            g.fill(x,y,x+w,y+ROW-5,chosen?0xFF34423E:hovered?0xFF303C41:DashboardTheme.CARD);
            g.fill(x,y,x+2,y+ROW-5,chosen?DashboardTheme.GREEN:status);
            g.text(font,DashboardTheme.fit(font,row.id(),w-83),x+9,y+7,DashboardTheme.TEXT);
            g.text(font,DashboardTheme.fit(font,row.targets(),w-18),x+9,y+21,DashboardTheme.MUTED,false);
            g.text(font,DashboardTheme.fit(font,row.status(global)+"  |  "+row.source(),w-18),x+9,y+36,status,false);
            if(row.valid()) DashboardTheme.switchControl(g,font,x+w-65,y+7,60,row.enabled(),!pending);
            else g.text(font,"INVALID",x+w-58,y+7,DashboardTheme.RED,false);
        }
        if(rows.size()*ROW>height) {
            int track=height-8,thumb=Math.max(12,track*height/(rows.size()*ROW));
            int top=getY()+4+(int)((track-thumb)*scroll/Math.max(1,rows.size()*ROW-height));
            g.fill(getRight()-7,getY()+4,getRight()-4,getBottom()-4,DashboardTheme.BORDER);
            g.fill(getRight()-7,top,getRight()-4,top+thumb,DashboardTheme.MUTED);
        }
        g.disableScissor();
    }
    private RuleRow at(double y) {
        int index=(int)((y-getY()+scroll)/ROW);
        return index>=0&&index<rows.size()?rows.get(index):null;
    }
    @Override public void onClick(MouseButtonEvent event,boolean doubleClick) {
        if(event.x()>=getRight()-10) { scrollbar(event.y());return; }
        var row=at(event.y());if(row==null)return;
        selected=row.key();select.accept(row);
        int rowIndex=rows.indexOf(row),rowTop=getY()+rowIndex*ROW-(int)scroll+3;
        if(com.lsaint06.mobspawningreal.dashboard.DashboardLayout.switchHit(getRight()-10,rowTop,event.x(),event.y())&&row.valid()&&!pending)toggle.accept(row);
    }
    private void scrollbar(double y) { scroll=(y-getY())/height*Math.max(0,rows.size()*ROW-height);clamp(); }
    @Override protected void onDrag(MouseButtonEvent event,double dx,double dy) { if(event.x()>=getRight()-12)scrollbar(event.y()); }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(!isMouseOver(x,y))return false;scroll-=vertical*ROW;clamp();return true;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if(rows.isEmpty())return false;
        int index=0;for(int i=0;i<rows.size();i++)if(rows.get(i).key().equals(selected))index=i;
        if(event.key()==264||event.key()==265||event.key()==266||event.key()==267) {
            int delta=event.key()==264?1:event.key()==265?-1:event.key()==266?-Math.max(1,height/ROW):Math.max(1,height/ROW);
            index=Math.max(0,Math.min(rows.size()-1,index+delta));
            selected=rows.get(index).key();select.accept(rows.get(index));
            if(index*ROW<scroll)scroll=index*ROW;
            if((index+1)*ROW>scroll+height)scroll=(index+1)*ROW-height;
            clamp();return true;
        }
        if(event.isSelection()&&!pending) { var row=rows.get(index);if(row.valid())toggle.accept(row);return true; }
        return false;
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        var row=rows.stream().filter(r->r.key().equals(selected)).findFirst();
        output.add(NarratedElementType.TITLE,row.<Component>map(r->Component.literal(r.id()+", "+r.status(global)+", "+r.targets())).orElse(getMessage()));
        output.add(NarratedElementType.USAGE,Component.literal("Arrow keys select rules. Enter toggles the selected rule."));
    }
}
