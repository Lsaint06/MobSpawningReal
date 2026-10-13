package com.lsaint06.mobspawningreal.client;
import com.google.gson.Gson;
import com.lsaint06.mobspawningreal.MobSpawningReal;
import com.lsaint06.mobspawningreal.dashboard.*;
import com.lsaint06.mobspawningreal.network.DashboardNetwork.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import java.util.UUID;
import static com.lsaint06.mobspawningreal.dashboard.DashboardModels.*;

/** Loaded by FML only on the client distribution. */
@Mod(value=MobSpawningReal.MOD_ID, dist=Dist.CLIENT)
public final class DashboardClient {
    private static final Gson GSON=new Gson();
    private static UUID session;
    private static SnapshotAssembler assembler=new SnapshotAssembler();
    private static long request;
    public DashboardClient(IEventBus modBus) {
        modBus.addListener(DashboardClient::register);
        NeoForge.EVENT_BUS.addListener(DashboardClient::logout);
    }
    private static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(Open.TYPE,(payload,context)->open(payload));
        event.register(Page.TYPE,(payload,context)->page(payload));
        event.register(Notice.TYPE,(payload,context)->notice(payload));
    }
    private static void open(Open open) {
        var minecraft=Minecraft.getInstance();if(minecraft.getConnection()==null)return;
        session=open.session(); request=0; assembler=new SnapshotAssembler();
        minecraft.gui.setScreen(new DashboardScreen(session));
    }
    private static void page(Page page) {
        if(!page.session().equals(session))return;
        var minecraft=Minecraft.getInstance();
        if(!(minecraft.gui.screen() instanceof DashboardScreen screen)||!screen.session().equals(session))return;
        try {
            String json=assembler.accept(page.transfer(),page.index(),page.total(),page.json());
            if(json!=null)screen.updateSnapshot(GSON.fromJson(json,Snapshot.class));
        }catch(Exception e){screen.feedback("Could not read server response. Use Refresh to retry.",true,0);assembler=new SnapshotAssembler();}
    }
    private static void notice(Notice notice) {
        if(!notice.session().equals(session))return;
        var minecraft=Minecraft.getInstance();
        if(minecraft.gui.screen() instanceof DashboardScreen screen) {
            if(notice.close()) {
                if(minecraft.player!=null)minecraft.gui.chatListener().handleSystemMessage(Component.literal(notice.message()),false);
                minecraft.gui.setScreen(null);
            }else screen.feedback(notice.message(),true,notice.request());
        }
    }
    public static long send(UUID token,long revision,Action action,String id,boolean enabled) {
        if(!token.equals(session)||Minecraft.getInstance().getConnection()==null)return -1;
        long next=++request;
        ClientPacketDistributor.sendToServer(new Request(token,next,revision,action,id,enabled));
        return next;
    }
    public static void close(UUID token) {
        if(!token.equals(session))return;
        send(token,0,Action.CLOSE,"",false); session=null;assembler=new SnapshotAssembler();
    }
    private static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        session=null;request=0;assembler=new SnapshotAssembler();
        var minecraft=Minecraft.getInstance();
        if(minecraft.gui.screen() instanceof DashboardScreen)minecraft.gui.setScreen(null);
    }
}

