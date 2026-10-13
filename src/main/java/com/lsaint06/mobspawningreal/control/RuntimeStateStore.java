package com.lsaint06.mobspawningreal.control;
import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Publishes live state only after successful file replacement. */
public final class RuntimeStateStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private volatile State state = new State(true, Map.of());
    private String loadError;
    public record State(boolean enabled, Map<String, Boolean> ruleOverrides) {
        public State { ruleOverrides = Map.copyOf(ruleOverrides); }
        public boolean ruleEnabled(String id, boolean configured) { return ruleOverrides.getOrDefault(id, configured); }
    }
    public record Result(boolean success, String message) {
        public static Result ok() { return new Result(true, "Saved successfully"); }
    }
    public RuntimeStateStore(Path file) { this.file = file; }
    public State state() { return state; }
    public synchronized Result load() {
        loadError = null;
        try {
            if (!Files.exists(file)) return update(true, Map.of());
            try (var reader = Files.newBufferedReader(file)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                boolean enabled = !json.has("enabled") || readBoolean(json.get("enabled"), "enabled");
                Map<String, Boolean> overrides = new LinkedHashMap<>();
                if (json.has("ruleOverrides") && !json.get("ruleOverrides").isJsonNull()) {
                    for (var entry : json.getAsJsonObject("ruleOverrides").entrySet()) {
                        if (!entry.getKey().isBlank() && !entry.getValue().isJsonNull()) overrides.put(entry.getKey().trim(), readBoolean(entry.getValue(), "ruleOverrides / " + entry.getKey()));
                    }
                }
                state = new State(enabled, overrides);
            }
            return Result.ok();
        } catch (Exception e) {
            loadError = "Cannot read runtime-state.json; repair the file before saving: " + e.getMessage();
            return new Result(false, loadError);
        }
    }
    private static boolean readBoolean(JsonElement value, String field) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
            throw new IllegalArgumentException(field + " must be a boolean");
        return value.getAsBoolean();
    }
    public synchronized Result update(boolean enabled, Map<String, Boolean> overrides) {
        if (loadError != null) return new Result(false, loadError);
        State candidate = new State(enabled, overrides);
        Path temporary = null;
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            temporary = Files.createTempFile(file.toAbsolutePath().getParent(), "runtime-state-", ".tmp");
            try (var channel = java.nio.channels.FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                var bytes = java.nio.ByteBuffer.wrap(GSON.toJson(candidate).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
            state = candidate;
            return Result.ok();
        } catch (Exception e) { return new Result(false, "Could not save runtime state: " + e.getMessage()); }
        finally { if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) {} }
    }
}
