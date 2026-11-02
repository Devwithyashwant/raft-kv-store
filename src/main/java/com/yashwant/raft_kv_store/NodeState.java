package com.yashwant.raft_kv_store;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class NodeState {

    private final Path filePath;

    public NodeState(Path filePath) {
        this.filePath = filePath;
    }

    public void save(int currentTerm, String votedFor) throws IOException {
        String content = currentTerm + "|" + (votedFor == null ? "" : votedFor);
        Files.writeString(filePath, content);
    }

    public int loadTerm() throws IOException {
        if (!Files.exists(filePath)) {
            return 0;
        }
        String content = Files.readString(filePath);
        String[] parts = content.split("\\|", 2);
        return Integer.parseInt(parts[0]);
    }

    public String loadVotedFor() throws IOException {
        if (!Files.exists(filePath)) {
            return null;
        }
        String content = Files.readString(filePath);
        String[] parts = content.split("\\|", 2);
        return parts[1].isEmpty() ? null : parts[1];
    }
}