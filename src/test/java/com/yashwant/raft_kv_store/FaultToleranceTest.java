package com.yashwant.raft_kv_store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FaultToleranceTest {

    @TempDir
    Path tempDir;

    @Test
    void clusterElectsNewLeaderWhenOriginalLeaderBecomesUnreachable() throws IOException {
        // Set up 3 nodes. node-2's "peer URL" for the others will point to a dead port,
        // simulating it being killed/unreachable after it becomes leader.
        RaftNode node1 = new RaftNode("node-1", tempDir.resolve("n1.log"), tempDir.resolve("n1.state"));
        RaftNode node2 = new RaftNode("node-2", tempDir.resolve("n2.log"), tempDir.resolve("n2.state"));
        RaftNode node3 = new RaftNode("node-3", tempDir.resolve("n3.log"), tempDir.resolve("n3.state"));

        // Simulate node-2 winning an initial election (manually, since we're not running real HTTP here)
        node2.startElection();
        node1.receiveVoteRequest(node2.getCurrentTerm(), node2.getNodeId(), 0);
        node3.receiveVoteRequest(node2.getCurrentTerm(), node2.getNodeId(), 0);
        node2.setRole(NodeRole.LEADER);

        assertEquals(NodeRole.LEADER, node2.getRole());

        // node-2 sends heartbeats, node-1 and node-3 stay followers
        node1.receiveHeartbeat(node2.getCurrentTerm(), node2.getNodeId());
        node3.receiveHeartbeat(node2.getCurrentTerm(), node2.getNodeId());

        assertEquals(NodeRole.FOLLOWER, node1.getRole());
        assertEquals(NodeRole.FOLLOWER, node3.getRole());

        // --- Simulate node-2 "dying": no more heartbeats arrive ---
        // node-1 detects the timeout and starts a new election
        node1.startElection();

        int votesForNode1 = 1;
        if (node3.receiveVoteRequest(node1.getCurrentTerm(), node1.getNodeId(), node1.getLog().size())) {
            votesForNode1++;
        }
        // node-2 is "dead" - we don't ask it for a vote

        int clusterSize = 3;
        int majorityNeeded = (clusterSize / 2) + 1;

        boolean node1WonElection = votesForNode1 >= majorityNeeded;

        if (node1WonElection) {
            node1.setRole(NodeRole.LEADER);
        }

        assertTrue(node1WonElection, "node-1 should win the election using only reachable nodes (itself + node-3)");
        assertEquals(NodeRole.LEADER, node1.getRole(), "node-1 should become the new leader after node-2 becomes unreachable");
        assertTrue(node1.getCurrentTerm() > node2.getCurrentTerm(), "The new leader's term should be higher than the old leader's term");
    }
    @Test
    void committedDataPersistsAcrossLeaderChange() throws IOException {
        RaftNode node1 = new RaftNode("node-1", tempDir.resolve("p1.log"), tempDir.resolve("p1.state"));
        RaftNode node2 = new RaftNode("node-2", tempDir.resolve("p2.log"), tempDir.resolve("p2.state"));
        RaftNode node3 = new RaftNode("node-3", tempDir.resolve("p3.log"), tempDir.resolve("p3.state"));

        // node-2 becomes leader and replicates one entry to a majority
        node2.startElection();
        node1.receiveVoteRequest(node2.getCurrentTerm(), node2.getNodeId(), 0);
        node3.receiveVoteRequest(node2.getCurrentTerm(), node2.getNodeId(), 0);
        node2.setRole(NodeRole.LEADER);

        LogEntry committedEntry = new LogEntry(node2.getCurrentTerm(), "x=5");
        node2.appendEntry(committedEntry);
        node1.receiveAppendEntries(node2.getCurrentTerm(), node2.getNodeId(), committedEntry.getTerm(), committedEntry.getCommand());
        node3.receiveAppendEntries(node2.getCurrentTerm(), node2.getNodeId(), committedEntry.getTerm(), committedEntry.getCommand());

        // node-2 "dies" - node-1 takes over, using its own (now up-to-date) log
        node1.startElection();
        node3.receiveVoteRequest(node1.getCurrentTerm(), node1.getNodeId(), node1.getLog().size());
        node1.setRole(NodeRole.LEADER);

        assertEquals(1, node1.getLog().size(), "The new leader should still have the previously committed entry");
        assertEquals("x=5", node1.getLog().get(0).getCommand(), "The committed data should be preserved after leader change");
    }
}