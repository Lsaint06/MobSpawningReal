package com.lsaint06.mobspawningreal.dashboard;

public record DashboardLayout(int x, int y, int width, int height, boolean wide) {
    public static DashboardLayout of(int screenWidth, int screenHeight) {
        int w = Math.max(1, Math.min(1040, screenWidth - 12));
        int h = Math.max(1, Math.min(680, screenHeight - 12));
        return new DashboardLayout((screenWidth-w)/2, (screenHeight-h)/2, w, h, screenWidth >= 640);
    }
    public int contentY() { return y + 100; }
    public int contentHeight() { return Math.max(1, height - 130); }
    public static boolean switchHit(int rowRight,int rowTop,double x,double y) {
        return x>=rowRight-65&&x<rowRight-5&&y>=rowTop+4&&y<rowTop+24;
    }
}
