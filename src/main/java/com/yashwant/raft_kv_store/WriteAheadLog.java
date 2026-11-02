package com.yashwant.raft_kv_store;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class WriteAheadLog {

    private final Path filePath;

    public WriteAheadLog(Path filePath) throws IOException {
        this.filePath = filePath;
        Path parentDir = filePath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
    }

    public void append(LogEntry entry) throws IOException {
        String line = entry.getTerm() + "|" + entry.getCommand() + System.lineSeparator();
        Files.writeString(filePath, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public List<LogEntry> readAll() throws IOException {
        List<LogEntry> entries = new ArrayList<>();

        if (!Files.exists(filePath)) {
            return entries;
        }

        List<String> lines = Files.readAllLines(filePath);
        for (String line : lines) {
            String[] parts = line.split("\\|", 2);
            int term = Integer.parseInt(parts[0]);
            String command = parts[1];
            entries.add(new LogEntry(term, command));
        }

        return entries;
    }
}