package com.lsaint06.mobspawningreal.dashboard;
import com.google.gson.Gson;
import com.lsaint06.mobspawningreal.config.RuleManager;
import com.lsaint06.mobspawningreal.control.RuntimeControl;
import com.lsaint06.mobspawningreal.network.DashboardNetwork.*;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;
import static com.lsaint06.mobspawningreal.dashboard.DashboardModels.*;
import com.lsaint06.mobspawningreal.editor.*;

/** All access is on the logical server game thread. No client classes occur here. */
public final class DashboardService {
    private static final Gson GSON = new Gson();
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static DashboardRevision revisions = new DashboardRevision();
    private static long reloadAt;
    private static class Session {
        final UUID token = UUID.randomUUID();
        long lastRequest, nextRequestAt;
        boolean registrySent;
    }
    private DashboardService() {}
    public static boolean administrator(ServerPlayer player) {
        return Commands.<net.minecraft.commands.CommandSourceStack>hasPermission(Commands.LEVEL_ADMINS).test(player.createCommandSourceStack());
    }
    public static void reset() { SESSIONS.clear(); revisions = new DashboardRevision(); reloadAt = 0; EditorRegistries.reset(); }
    public static void remove(ServerPlayer player) { SESSIONS.remove(player.getUUID()); }
    public static void open(ServerPlayer player) {
        if (!administrator(player)) return;
        syncRevision();
        Session session = new Session(); SESSIONS.put(player.getUUID(), session);
        PacketDistributor.sendToPlayer(player, new Open(session.token));
        send(player, session, 0, RuntimeControl.lastError().isEmpty() ? "Server configuration ready" : RuntimeControl.lastError(), !RuntimeControl.lastError().isEmpty());
    }
    private static boolean syncRevision() {
        return revisions.observe(RuleManager.revision(), RuntimeControl.revision());
    }
    public static void tick(MinecraftServer server) {
        syncRevision();
        boolean changed = revisions.needsBroadcast();
        for (var entry : new ArrayList<>(SESSIONS.entrySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) { SESSIONS.remove(entry.getKey()); continue; }
            if (!administrator(player)) {
                PacketDistributor.sendToPlayer(player, new Notice(entry.getValue().token, 0, "Administrator permission is required.", true));
                SESSIONS.remove(entry.getKey());
            } else if (changed) send(player, entry.getValue(), 0, "Configuration updated on the server", false);
        }
        revisions.broadcasted();
    }
    public static void handle(ServerPlayer player, Request request) {
        Session session = SESSIONS.get(player.getUUID());
        if (!administrator(player)) {
            if (session != null) { PacketDistributor.sendToPlayer(player, new Notice(session.token, request.request(), "Administrator permission is required.", true)); remove(player); }
            return;
        }
        if (session == null || !session.token.equals(request.session())) return;
        if (request.action() == Action.CLOSE) { remove(player); return; }
        if (request.request() <= session.lastRequest) return;
        session.lastRequest = request.request();
        long now = System.nanoTime();
        if (now < session.nextRequestAt) { notice(player, session, request.request(), "Please wait a moment before trying again."); return; }
        session.nextRequestAt = now + 150_000_000L;
        syncRevision();
        if (request.action() == Action.REFRESH) { session.registrySent=false; send(player, session, request.request(), "Refreshed from server", false); return; }
        boolean valid = request.action() != Action.RULE && request.action() != Action.RESET || RuleManager.rules().stream().anyMatch(r -> r.id.equals(request.ruleId()));
        String denied = DashboardPolicy.check(true, request.revision(), revisions.current(), valid);
        if (!denied.isEmpty()) { send(player, session, request.request(), denied, true); return; }
        String message = "Saved successfully"; boolean error = false;
        switch (request.action()) {
            case GLOBAL -> { var result = RuntimeControl.setGloballyEnabled(request.enabled()); message = result.message(); error = !result.success(); }
            case RULE -> { var result = RuntimeControl.setRuleEnabled(request.ruleId(), request.enabled()); message = result.message(); error = !result.success(); }
            case RESET -> { var result = RuntimeControl.clearRuleOverride(request.ruleId()); message = result.message(); error = !result.success(); }
            case RELOAD -> {
                if (now < reloadAt) { notice(player, session, request.request(), "Reload is available every two seconds."); return; }
                reloadAt = now + 2_000_000_000L;
                RuntimeControl.load();
                var result = RuleManager.reload();
                message = "Reloaded " + result.loaded() + " rules; " + result.errors().size() + " validation errors.";
                error = !result.errors().isEmpty() || !RuntimeControl.lastError().isEmpty();
                if (!RuntimeControl.lastError().isEmpty()) message += " " + RuntimeControl.lastError();
            }
            default -> { return; }
        }
        boolean changed = syncRevision();
        send(player, session, request.request(), message, error);
        if (changed) {
            var server = player.level().getServer();
            for (var entry : SESSIONS.entrySet()) {
                if (entry.getKey().equals(player.getUUID())) continue;
                var other = server.getPlayerList().getPlayer(entry.getKey());
                if (other != null && administrator(other)) send(other, entry.getValue(), 0, "Configuration updated by an administrator", false);
            }
        }
    }
    private static void notice(ServerPlayer player, Session session, long request, String message) {
        PacketDistributor.sendToPlayer(player, new Notice(session.token, request, message, false));
    }
    public static void edit(ServerPlayer player, EditRequest request) {
        Session session=SESSIONS.get(player.getUUID());
        if(!administrator(player)){if(session!=null){PacketDistributor.sendToPlayer(player,new Notice(session.token,request.request(),"Administrator permission is required.",true));remove(player);}return;}
        if(session==null||!session.token.equals(request.session())||request.request()<=session.lastRequest)return;
        session.lastRequest=request.request();long now=System.nanoTime();
        if(now<session.nextRequestAt){notice(player,session,request.request(),"Please wait before saving again.");return;}
        session.nextRequestAt=now+500_000_000L;syncRevision();
        if(request.revision()!=revisions.current()){editor(player,session,request.request(),"Configuration changed on server. Your draft is retained.",false,"");return;}
        var store=new RuleDocumentStore(RuleManager.ruleDirectory(),EditorRegistries::validate);
        var result=store.apply(request.operation(),request.key(),request.fingerprint(),request.json());
        if(result.success()){RuleManager.reload();syncRevision();}
        editor(player,session,request.request(),result.message(),result.success(),result.key());
        if(result.success()){
            send(player,session,0,result.message(),false);
            var server=player.level().getServer();
            for(var e:SESSIONS.entrySet())if(!e.getKey().equals(player.getUUID())){var other=server.getPlayerList().getPlayer(e.getKey());if(other!=null&&administrator(other))send(other,e.getValue(),0,"Configuration updated by an administrator",false);}
        }
    }
    private static void editor(ServerPlayer player,Session session,long request,String message,boolean success,String key){
        if(!administrator(player))return;
        try{
            var store=new RuleDocumentStore(RuleManager.ruleDirectory(),EditorRegistries::validate);
            var state=new EditorModels.State(revisions.current(),request,store.fingerprint(),store.documents(),session.registrySent?null:EditorRegistries.entries(player.level().getServer()),message,success,key);
            var pages=SnapshotAssembler.pages(GSON.toJson(state));UUID transfer=UUID.randomUUID();
            for(int i=0;i<pages.size();i++)PacketDistributor.sendToPlayer(player,new EditorPage(session.token,transfer,i,pages.size(),pages.get(i)));
            session.registrySent=true;
        }catch(Exception e){notice(player,session,request,"Editor unavailable: "+Objects.toString(e.getMessage(),"Could not read rules."));}
    }
    private static void send(ServerPlayer player, Session session, long request, String message, boolean error) {
        if (!administrator(player)) return;
        editor(player,session,0,message,!error,"");
        var rows = RuleManager.catalog().entries().stream().map(e -> RuleSummaries.row(e, e.valid() && RuntimeControl.isRuleSelected(e.rule()), RuntimeControl.ruleOverride(e.id())))
                .sorted(Comparator.comparingInt(RuleRow::priority).reversed().thenComparing(RuleRow::id).thenComparing(RuleRow::key)).toList();
        String json = GSON.toJson(new Snapshot(revisions.current(), request, RuntimeControl.isGloballyEnabled(), message, error, rows));
        try {
            var pages = SnapshotAssembler.pages(json); UUID transfer = UUID.randomUUID();
            for (int i = 0; i < pages.size(); i++) PacketDistributor.sendToPlayer(player, new Page(session.token, transfer, i, pages.size(), pages.get(i)));
        } catch (IllegalArgumentException e) { notice(player, session, request, e.getMessage()); }
    }
}
