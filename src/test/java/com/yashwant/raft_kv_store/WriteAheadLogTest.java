package com.yashwant.raft_kv_store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WriteAheadLogTest {

    @TempDir
    Path tempDir;

    @Test
    void appendAndReadAllReturnsCorrectEntries() throws IOException {
        Path logFile = tempDir.resolve("test.log");
        WriteAheadLog wal = new WriteAheadLog(logFile);

        wal.append(new LogEntry(1, "x=5"));
        wal.append(new LogEntry(1, "y=10"));
        wal.append(new LogEntry(2, "x=7"));

        List<LogEntry> entries = wal.readAll();

        assertEquals(3, entries.size());
        assertEquals(1, entries.get(0).getTerm());
        assertEquals("x=5", entries.get(0).getCommand());
        assertEquals(2, entries.get(2).getTerm());
        assertEquals("x=7", entries.get(2).getCommand());
    }

    @Test
    void readAllReturnsEmptyListWhenFileDoesNotExist() throws IOException {
        Path logFile = tempDir.resolve("nonexistent.log");
        WriteAheadLog wal = new WriteAheadLog(logFile);

        List<LogEntry> entries = wal.readAll();

        assertTrue(entries.isEmpty());
    }

    @Test
    void entriesSurviveAcrossSeparateWriteAheadLogInstances() throws IOException {
        Path logFile = tempDir.resolve("persist.log");

        WriteAheadLog firstInstance = new WriteAheadLog(logFile);
        firstInstance.append(new LogEntry(1, "x=1"));

        // Simulate a restart: a brand new WriteAheadLog object pointing at the same file
        WriteAheadLog secondInstance = new WriteAheadLog(logFile);
        List<LogEntry> entries = secondInstance.readAll();

        assertEquals(1, entries.size());
        assertEquals("x=1", entries.get(0).getCommand());
    }
}