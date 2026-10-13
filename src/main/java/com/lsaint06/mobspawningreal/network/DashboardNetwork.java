package com.lsaint06.mobspawningreal.network;
import com.lsaint06.mobspawningreal.MobSpawningReal;
import com.lsaint06.mobspawningreal.dashboard.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.UUID;
import static com.lsaint06.mobspawningreal.dashboard.DashboardModels.*;

public final class DashboardNetwork {
    private DashboardNetwork() {}
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String name) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MobSpawningReal.MOD_ID, name));
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("dashboard-2");
        registrar.playToClient(Open.TYPE, Open.CODEC);
        registrar.playToClient(Page.TYPE, Page.CODEC);
        registrar.playToClient(Notice.TYPE, Notice.CODEC);
        registrar.playToClient(EditorPage.TYPE, EditorPage.CODEC);
        registrar.playToServer(EditRequest.TYPE, EditRequest.CODEC, (request, context) -> {
            if (context.player() instanceof ServerPlayer player) DashboardService.edit(player, request);
        });
        registrar.playToServer(Request.TYPE, Request.CODEC, (request, context) -> {
            if (context.player() instanceof ServerPlayer player) DashboardService.handle(player, request);
        });
    }
    public record Open(UUID session) implements CustomPacketPayload {
        public static final Type<Open> TYPE = DashboardNetwork.type("dashboard_open");
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.of((b,p) -> b.writeUUID(p.session), b -> new Open(b.readUUID()));
        @Override public Type<Open> type() { return TYPE; }
    }
    public record EditorPage(UUID session, UUID transfer, int index, int total, String json) implements CustomPacketPayload {
        public static final Type<EditorPage> TYPE = DashboardNetwork.type("editor_page");
        public static final StreamCodec<RegistryFriendlyByteBuf, EditorPage> CODEC = StreamCodec.of((b,p)->{
            b.writeUUID(p.session);b.writeUUID(p.transfer);b.writeVarInt(p.index);b.writeVarInt(p.total);b.writeUtf(p.json,SnapshotAssembler.CHUNK_SIZE);
        },b->new EditorPage(b.readUUID(),b.readUUID(),b.readVarInt(),b.readVarInt(),b.readUtf(SnapshotAssembler.CHUNK_SIZE)));
        @Override public Type<EditorPage> type(){return TYPE;}
    }
    public record EditRequest(UUID session,long request,long revision,String operation,String key,String fingerprint,String json) implements CustomPacketPayload {
        public static final Type<EditRequest> TYPE = DashboardNetwork.type("editor_request");
        public static final StreamCodec<RegistryFriendlyByteBuf, EditRequest> CODEC = StreamCodec.of((b,p)->{
            b.writeUUID(p.session);b.writeVarLong(p.request);b.writeVarLong(p.revision);b.writeUtf(p.operation,16);b.writeUtf(p.key,1024);b.writeUtf(p.fingerprint,64);b.writeUtf(p.json,com.lsaint06.mobspawningreal.editor.RuleDocumentStore.MAX_JSON);
        },b->new EditRequest(b.readUUID(),b.readVarLong(),b.readVarLong(),b.readUtf(16),b.readUtf(1024),b.readUtf(64),b.readUtf(com.lsaint06.mobspawningreal.editor.RuleDocumentStore.MAX_JSON)));
        @Override public Type<EditRequest> type(){return TYPE;}
    }
    public record Page(UUID session, UUID transfer, int index, int total, String json) implements CustomPacketPayload {
        public static final Type<Page> TYPE = DashboardNetwork.type("dashboard_page");
        public static final StreamCodec<RegistryFriendlyByteBuf, Page> CODEC = StreamCodec.of((b,p) -> {
            b.writeUUID(p.session); b.writeUUID(p.transfer); b.writeVarInt(p.index); b.writeVarInt(p.total); b.writeUtf(p.json, SnapshotAssembler.CHUNK_SIZE);
        }, b -> new Page(b.readUUID(), b.readUUID(), b.readVarInt(), b.readVarInt(), b.readUtf(SnapshotAssembler.CHUNK_SIZE)));
        @Override public Type<Page> type() { return TYPE; }
    }
    public record Notice(UUID session, long request, String message, boolean close) implements CustomPacketPayload {
        public static final Type<Notice> TYPE = DashboardNetwork.type("dashboard_notice");
        public static final StreamCodec<RegistryFriendlyByteBuf, Notice> CODEC = StreamCodec.of((b,p) -> {
            b.writeUUID(p.session); b.writeVarLong(p.request); b.writeUtf(p.message, 2048); b.writeBoolean(p.close);
        }, b -> new Notice(b.readUUID(), b.readVarLong(), b.readUtf(2048), b.readBoolean()));
        @Override public Type<Notice> type() { return TYPE; }
    }
    public record Request(UUID session, long request, long revision, Action action, String ruleId, boolean enabled) implements CustomPacketPayload {
        public static final Type<Request> TYPE = DashboardNetwork.type("dashboard_request");
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of((b,p) -> {
            b.writeUUID(p.session); b.writeVarLong(p.request); b.writeVarLong(p.revision); b.writeEnum(p.action); b.writeUtf(p.ruleId, 1024); b.writeBoolean(p.enabled);
        }, b -> new Request(b.readUUID(), b.readVarLong(), b.readVarLong(), b.readEnum(Action.class), b.readUtf(1024), b.readBoolean()));
        @Override public Type<Request> type() { return TYPE; }
    }
}
