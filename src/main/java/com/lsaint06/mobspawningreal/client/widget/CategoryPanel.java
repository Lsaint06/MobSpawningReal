package com.lsaint06.mobspawningreal.client.widget;
import com.lsaint06.mobspawningreal.dashboard.DashboardModels.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.*;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.util.*;

/** A reusable scrollable read-only category view, ready for future field editors. */
public final class CategoryPanel extends AbstractWidget {
    private RuleRow rule;
    private Category category=Category.SPAWN;
    private int scroll;
    private int contentHeight;
    public CategoryPanel(int x,int y,int w,int h) { super(x,y,w,h,Component.literal("Rule details")); }
    public void show(RuleRow rule,Category category) {
        if(this.rule==null||rule==null||!this.rule.key().equals(rule.key())||this.category!=category)scroll=0;
        this.rule=rule;this.category=category;
    }
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float a) {
        var font=Minecraft.getInstance().font;
        DashboardTheme.panel(g,getX(),getY(),width,height,DashboardTheme.PANEL);
        g.enableScissor(getX()+1,getY()+1,getRight()-1,getBottom()-1);
        List<String> lines=new ArrayList<>();
        if(rule==null)lines.add("Select a rule to inspect its configuration.");
        else {
            lines.add(rule.id());lines.add("Source: "+rule.source()+" | Priority: "+rule.priority());
            lines.add(rule.stateSource());
            if(!rule.errors().isEmpty()) { lines.add("Validation errors");lines.addAll(rule.errors()); }
            lines.add(category.title);
            var values=rule.categories().getOrDefault(category.name(),List.of());
            lines.addAll(values.isEmpty()?List.of("No settings configured in this category."):values);
            lines.add("Changes affect new processing. Existing attributes and equipment are not reverted.");
        }
        int offset=8;
        for(int i=0;i<lines.size();i++) {
            int color=i==0?DashboardTheme.TEXT:rule!=null&&rule.errors().contains(lines.get(i))?DashboardTheme.RED:DashboardTheme.MUTED;
            if(lines.get(i).equals(category.title)) {offset+=8;color=DashboardTheme.GREEN;}
            for(var wrapped:font.split(Component.literal(lines.get(i)),Math.max(8,width-22))) {
                int y=getY()+offset-scroll;
                if(y+10>=getY()&&y<getBottom())g.text(font,wrapped,getX()+10,y,color,false);
                offset+=12;
            }
            offset+=4;
        }
        contentHeight=offset+4;
        scroll=Math.max(0,Math.min(scroll,Math.max(0,contentHeight-height)));
        if(contentHeight>height) {
            int thumb=Math.max(10,(height-8)*height/contentHeight);
            int top=getY()+4+(height-8-thumb)*scroll/Math.max(1,contentHeight-height);
            g.fill(getRight()-6,top,getRight()-3,top+thumb,DashboardTheme.MUTED);
        }
        g.disableScissor();
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(!isMouseOver(x,y))return false;scroll=Math.max(0,Math.min(scroll-(int)(vertical*24),Math.max(0,contentHeight-height)));return true;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if(event.key()==264||event.key()==265||event.key()==266||event.key()==267) {
            int delta=event.key()==264?24:event.key()==265?-24:event.key()==266?-height:height;
            scroll=Math.max(0,Math.min(scroll+delta,Math.max(0,contentHeight-height)));return true;
        }return false;
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE,Component.literal(rule==null?"Select a rule":rule.id()+", "+category.title));
        if(rule!=null)output.add(NarratedElementType.HINT,Component.literal(String.join(". ",rule.categories().getOrDefault(category.name(),List.of()))));
    }
}
