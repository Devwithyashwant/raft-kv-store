package com.yashwant.raft_kv_store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class RaftNodeTest {

    @TempDir
    Path tempDir;

    private RaftNode newNode(String nodeId) throws IOException {
        return new RaftNode(nodeId, tempDir.resolve(nodeId + ".log"), tempDir.resolve(nodeId + ".state"));
    }

    @Test
    void newNodeStartsAsFollowerWithTermZero() throws IOException {
        RaftNode node = newNode("node-1");

        assertEquals(0, node.getCurrentTerm());
        assertEquals(NodeRole.FOLLOWER, node.getRole());
        assertNull(node.getVotedFor());
    }

    @Test
    void startElectionIncrementsTermAndBecomesCandidate() throws IOException {
        RaftNode node = newNode("node-1");

        node.startElection();

        assertEquals(1, node.getCurrentTerm(), "Term should increment by 1");
        assertEquals(NodeRole.CANDIDATE, node.getRole(), "Node should become a candidate");
        assertEquals("node-1", node.getVotedFor(), "Node should vote for itself");
    }

    @Test
    void multipleElectionsKeepIncrementingTerm() throws IOException {
        RaftNode node = newNode("node-1");
        node.startElection();
        node.startElection();
        node.startElection();

        assertEquals(3, node.getCurrentTerm(), "Term should increment once per election call");
    }

    @Test
    void grantsVoteWhenNoPriorVoteAndSameOrHigherTerm() throws IOException {
        RaftNode node = newNode("node-1");

        boolean granted = node.receiveVoteRequest(1, "node-2", 0);

        assertTrue(granted);
        assertEquals("node-2", node.getVotedFor());
        assertEquals(1, node.getCurrentTerm());
    }

    @Test
    void rejectsVoteFromOlderTerm() throws IOException {
        RaftNode node = newNode("node-1");
        node.setCurrentTerm(5);

        boolean granted = node.receiveVoteRequest(3, "node-2", 0);

        assertFalse(granted);
    }

    @Test
    void rejectsSecondVoteRequestInSameTermFromDifferentCandidate() throws IOException {
        RaftNode node = newNode("node-1");

        node.receiveVoteRequest(1, "node-2", 0);
        boolean secondGranted = node.receiveVoteRequest(1, "node-3", 0);

        assertFalse(secondGranted);
        assertEquals("node-2", node.getVotedFor(), "Vote should remain with the first candidate");
    }

    @Test
    void candidateBecomesLeaderWithMajorityVotes() throws IOException {
        RaftNode candidate = newNode("node-1");
        RaftNode follower1 = newNode("node-2");
        RaftNode follower2 = newNode("node-3");

        candidate.startElection();

        int votesReceived = 1;

        boolean vote1 = follower1.receiveVoteRequest(candidate.getCurrentTerm(), candidate.getNodeId(), 0);
        if (vote1) votesReceived++;

        boolean vote2 = follower2.receiveVoteRequest(candidate.getCurrentTerm(), candidate.getNodeId(), 0);
        if (vote2) votesReceived++;

        int clusterSize = 3;
        int majorityNeeded = (clusterSize / 2) + 1;

        boolean wonElection = votesReceived >= majorityNeeded;
        if (wonElection) {
            candidate.setRole(NodeRole.LEADER);
        }

        assertTrue(wonElection, "Candidate should win with a majority of votes");
        assertEquals(NodeRole.LEADER, candidate.getRole());
        assertEquals(3, votesReceived, "Candidate should receive all 3 votes (itself + 2 followers)");
    }

    @Test
    void candidateLosesElectionIfVotesAlreadyCastElsewhere() throws IOException {
        RaftNode candidate = newNode("node-1");
        RaftNode follower1 = newNode("node-2");
        RaftNode follower2 = newNode("node-3");

        follower1.receiveVoteRequest(1, "node-4", 0);
        follower2.receiveVoteRequest(1, "node-4", 0);

        candidate.startElection();

        int votesReceived = 1;

        boolean vote1 = follower1.receiveVoteRequest(candidate.getCurrentTerm(), candidate.getNodeId(), 0);
        if (vote1) votesReceived++;

        boolean vote2 = follower2.receiveVoteRequest(candidate.getCurrentTerm(), candidate.getNodeId(), 0);
        if (vote2) votesReceived++;

        int clusterSize = 3;
        int majorityNeeded = (clusterSize / 2) + 1;

        boolean wonElection = votesReceived >= majorityNeeded;

        assertFalse(wonElection, "Candidate should lose - both other votes already committed elsewhere");
        assertEquals(1, votesReceived, "Only the candidate's own self-vote should count");
    }

    @Test
    void rejectsVoteWhenCandidateLogIsShorterThanVoterLog() throws IOException {
        RaftNode node = newNode("node-1");
        node.appendEntry(new LogEntry(1, "x=1"));
        node.appendEntry(new LogEntry(1, "x=2"));

        boolean granted = node.receiveVoteRequest(1, "node-2", 1);

        assertFalse(granted, "Should reject candidate whose log is shorter than voter's own");
    }

    @Test
    void leaderStepsDownWhenReceivingHigherTerm() throws IOException {
        RaftNode node = newNode("node-1");
        node.startElection();
        node.setRole(NodeRole.LEADER);

        boolean granted = node.receiveVoteRequest(5, "node-2", 0);

        assertEquals(NodeRole.FOLLOWER, node.getRole(), "Leader should step down to follower on seeing a higher term");
        assertEquals(5, node.getCurrentTerm(), "Node should adopt the higher term");
        assertTrue(granted, "Node should grant the vote that revealed the higher term, since it has no prior vote this term");
        assertEquals("node-2", node.getVotedFor(), "Node should vote for the candidate that prompted the step-down");
    }

    @Test
    void nodeRecoversLogFromDiskAfterRestart() throws IOException {
        Path logFile = tempDir.resolve("recovery-test.log");
        Path stateFile = tempDir.resolve("recovery-test.state");

        RaftNode firstInstance = new RaftNode("node-1", logFile, stateFile);
        firstInstance.appendEntry(new LogEntry(1, "x=5"));
        firstInstance.appendEntry(new LogEntry(1, "y=10"));

        RaftNode restartedInstance = new RaftNode("node-1", logFile, stateFile);

        assertEquals(2, restartedInstance.getLog().size(), "Restarted node should recover both entries from disk");
        assertEquals("x=5", restartedInstance.getLog().get(0).getCommand());
        assertEquals("y=10", restartedInstance.getLog().get(1).getCommand());
    }

    @Test
    void nodeRecoversTermAndVoteFromDiskAfterRestart() throws IOException {
        Path logFile = tempDir.resolve("state-recovery.log");
        Path stateFile = tempDir.resolve("state-recovery.state");

        RaftNode firstInstance = new RaftNode("node-1", logFile, stateFile);
        firstInstance.receiveVoteRequest(5, "node-2", 0);

        RaftNode restartedInstance = new RaftNode("node-1", logFile, stateFile);

        assertEquals(5, restartedInstance.getCurrentTerm(), "Restarted node should recover its term from disk");
        assertEquals("node-2", restartedInstance.getVotedFor(), "Restarted node should recover its vote from disk");
    }
    @Test
    void receivingHeartbeatResetsLastHeartbeatTime() throws IOException, InterruptedException {
        RaftNode node = newNode("node-1");

        long initialTime = node.getLastHeartbeatTime();

        Thread.sleep(200);

        node.receiveHeartbeat(1, "node-leader");

        long afterHeartbeatTime = node.getLastHeartbeatTime();

        assertTrue(afterHeartbeatTime > initialTime, "lastHeartbeatTime should be updated after receiving a heartbeat");

        long elapsedSinceHeartbeat = System.currentTimeMillis() - node.getLastHeartbeatTime();
        assertTrue(elapsedSinceHeartbeat < 100, "Elapsed time since last heartbeat should be very small immediately after receiving one, was: " + elapsedSinceHeartbeat);
    }
}