package com.lsaint06.mobspawningreal.client.widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Native keyboard focus, sounds and narration with dashboard styling. */
public final class DashboardButton extends Button {
    private boolean accent;
    private Boolean switchState;
    public DashboardButton(int x, int y, int w, String text, Runnable action) {
        super(x,y,w,20,Component.literal(text), b -> action.run(), DEFAULT_NARRATION);
    }
    public DashboardButton accent(boolean value) { accent=value; return this; }
    public DashboardButton switchState(boolean value) { switchState=value; return this; }
    @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        DashboardTheme.panel(g,getX(),getY(),width,height,isHoveredOrFocused()?0xFF3C4B50:DashboardTheme.CARD);
        if (isFocused()) g.horizontalLine(getX()+1,getRight()-2,getBottom()-2,DashboardTheme.GREEN);
        var font = Minecraft.getInstance().font;
        if(switchState!=null) {
            String label=getMessage().getString().split(":")[0];
            g.text(font,DashboardTheme.fit(font,label,width-65),getX()+6,getY()+6,active?DashboardTheme.TEXT:DashboardTheme.MUTED,false);
            DashboardTheme.switchControl(g,font,getRight()-58,getY()+4,54,switchState,active);
        } else g.centeredText(font,DashboardTheme.fit(font,getMessage().getString(),width-8),getX()+width/2,getY()+6,
                !active ? 0xFF68777D : accent ? DashboardTheme.GREEN : DashboardTheme.TEXT);
    }
}
