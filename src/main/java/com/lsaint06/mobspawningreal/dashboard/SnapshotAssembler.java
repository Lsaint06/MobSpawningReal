package com.lsaint06.mobspawningreal.dashboard;
import java.util.*;

/** Bounded assembly, independent of client classes and networking. */
public final class SnapshotAssembler {
    public static final int CHUNK_SIZE = 8000, MAX_PAGES = 512;
    private UUID transfer;
    private String[] chunks;
    private final Set<UUID> retired = new LinkedHashSet<>();
    public static List<String> pages(String json) {
        List<String> pages = new ArrayList<>();
        for (int start = 0; start < json.length();) {
            int end = Math.min(json.length(), start + CHUNK_SIZE);
            if (end < json.length() && Character.isHighSurrogate(json.charAt(end-1)) && Character.isLowSurrogate(json.charAt(end))) end--;
            pages.add(json.substring(start,end)); start=end;
            if (pages.size() > MAX_PAGES) throw new IllegalArgumentException("Dashboard exceeds 4 million characters; reduce configuration size.");
        }
        if (pages.isEmpty()) pages.add("");
        return List.copyOf(pages);
    }
    public String accept(UUID id, int index, int total, String text) {
        if (total < 1 || total > MAX_PAGES || index < 0 || index >= total || text.length() > CHUNK_SIZE) throw new IllegalArgumentException("Invalid dashboard page");
        if (retired.contains(id)) return null;
        if (!id.equals(transfer)) {
            if (transfer != null) retire(transfer);
            transfer = id; chunks = new String[total];
        }
        if (total != chunks.length) throw new IllegalArgumentException("Inconsistent page count");
        chunks[index] = text;
        if (Arrays.stream(chunks).anyMatch(Objects::isNull)) return null;
        String result = String.join("", chunks); retire(transfer); transfer = null; chunks = null;
        return result;
    }
    private void retire(UUID id) { retired.add(id); if (retired.size() > 32) retired.remove(retired.iterator().next()); }
}
