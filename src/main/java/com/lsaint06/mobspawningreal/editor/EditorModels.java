package com.lsaint06.mobspawningreal.editor;

import java.util.List;

public final class EditorModels {
    private EditorModels() {}
    public record RegistryEntry(String id,String name,String kind) {}
    public record State(long revision,long request,String fingerprint,List<RuleDocumentStore.Document> documents,
                        List<RegistryEntry> registry,String message,boolean success,String selectedKey) {}
}
