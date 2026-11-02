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
        return new RaftNode(nodeId, tempDir.resolve(nodeId + ".log"));
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
}