package com.lsaint06.mobspawningreal.client.widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.*;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

/** One virtualized widget for rules, entities, and registry picker results. */
public final class EditorChoiceList extends AbstractWidget {
    public record Choice(String key,String title,String subtitle,String status,int color) {}
    private List<Choice> choices=List.of();private String selected="";private double scroll;
    private final Consumer<String> select;
    private static final int ROW=48;
    public EditorChoiceList(int x,int y,int w,int h,Consumer<String> select){super(x,y,w,h,Component.literal("Selection list. Arrow keys select."));this.select=select;}
    public void update(List<Choice> choices,String selected){this.choices=choices;this.selected=selected;clamp();}
    public double scroll(){return scroll;}
    public void scroll(double value){scroll=value;clamp();}
    private void clamp(){scroll=Math.max(0,Math.min(scroll,Math.max(0,choices.size()*ROW-height)));}
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float a){
        var font=Minecraft.getInstance().font;DashboardTheme.panel(g,getX(),getY(),width,height,DashboardTheme.PANEL);
        g.enableScissor(getX()+1,getY()+1,getRight()-1,getBottom()-1);
        if(choices.isEmpty())g.text(font,"No matching entries",getX()+8,getY()+10,DashboardTheme.MUTED);
        int first=(int)scroll/ROW;
        for(int i=first;i<choices.size()&&i<=first+height/ROW+1;i++){
            var c=choices.get(i);int x=getX()+3,y=getY()+i*ROW-(int)scroll+2,w=width-13;
            boolean chosen=c.key().equals(selected),hovered=mx>=x&&mx<x+w&&my>=y&&my<y+ROW-3;
            g.fill(x,y,x+w,y+ROW-3,chosen?0xFF34463E:hovered?0xFF303C41:DashboardTheme.CARD);
            g.fill(x,y,x+2,y+ROW-3,chosen?DashboardTheme.GREEN:c.color());
            g.text(font,DashboardTheme.fit(font,c.title(),w-14),x+8,y+5,DashboardTheme.TEXT);
            g.text(font,DashboardTheme.fit(font,c.subtitle(),w-14),x+8,y+18,DashboardTheme.MUTED,false);
            g.text(font,DashboardTheme.fit(font,c.status(),w-14),x+8,y+31,c.color(),false);
            if(hovered&&my>=getY()&&my<getBottom())g.setTooltipForNextFrame(font,Component.literal(c.title()+"\n"+c.subtitle()+"\n"+c.status()),mx,my);
        }
        if(choices.size()*ROW>height){int track=height-8,thumb=Math.max(12,track*height/(choices.size()*ROW));int top=getY()+4+(int)((track-thumb)*scroll/Math.max(1,choices.size()*ROW-height));g.fill(getRight()-7,top,getRight()-4,top+thumb,DashboardTheme.MUTED);}
        g.disableScissor();
    }
    @Override public void onClick(MouseButtonEvent e,boolean twice){
        if(e.x()>=getRight()-10){scroll=(e.y()-getY())/height*Math.max(0,choices.size()*ROW-height);clamp();return;}
        int i=(int)((e.y()-getY()+scroll)/ROW);if(i>=0&&i<choices.size())select.accept(choices.get(i).key());
    }
    @Override protected void onDrag(MouseButtonEvent e,double dx,double dy){if(e.x()>=getRight()-12){scroll=(e.y()-getY())/height*Math.max(0,choices.size()*ROW-height);clamp();}}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(!isMouseOver(x,y))return false;scroll-=vertical*ROW;clamp();return true;}
    @Override public boolean keyPressed(KeyEvent e){
        if(choices.isEmpty())return false;int i=0;for(int n=0;n<choices.size();n++)if(choices.get(n).key().equals(selected))i=n;
        if(e.key()==264||e.key()==265){i=Math.max(0,Math.min(choices.size()-1,i+(e.key()==264?1:-1)));if(i*ROW<scroll)scroll=i*ROW;if((i+1)*ROW>scroll+height)scroll=(i+1)*ROW-height;clamp();select.accept(choices.get(i).key());return true;}return false;
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output){output.add(NarratedElementType.TITLE,Component.literal(choices.stream().filter(c->c.key().equals(selected)).map(Choice::title).findFirst().orElse("Selection list")));output.add(NarratedElementType.USAGE,Component.literal("Arrow keys choose an entry."));}
}
