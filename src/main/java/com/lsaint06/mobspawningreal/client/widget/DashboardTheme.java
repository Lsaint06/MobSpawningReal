package com.lsaint06.mobspawningreal.client.widget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class DashboardTheme {
    public static final int BG=0xFF121719, PANEL=0xFF1D2528, CARD=0xFF263034, BORDER=0xFF3A484D,
            TEXT=0xFFEAF0EB, MUTED=0xFF9BAAAE, GREEN=0xFF8ACB8C, RED=0xFFFF9292, AMBER=0xFFEBC781;
    private DashboardTheme() {}
    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x,y,x+w,y+h,color);
        g.horizontalLine(x,x+w-1,y,BORDER); g.horizontalLine(x,x+w-1,y+h-1,BORDER);
        g.verticalLine(x,y,y+h-1,BORDER); g.verticalLine(x+w-1,y,y+h-1,BORDER);
    }
    public static String fit(Font font, String text, int width) {
        if (width <= 0) return "";
        return font.width(text) <= width ? text : font.plainSubstrByWidth(text, Math.max(0,width-font.width("..."))) + "...";
    }
    public static void switchControl(GuiGraphicsExtractor g, Font font, int x, int y, int w, boolean on, boolean active) {
        int color = !active ? MUTED : on ? GREEN : MUTED;
        g.fill(x,y,x+26,y+12,on && active ? 0xFF385B3E : 0xFF3C4549);
        g.fill(x+(on?15:2),y+2,x+(on?24:11),y+10,color);
        g.text(font,on?"ON":"OFF",x+32,y+2,color,false);
    }
}
