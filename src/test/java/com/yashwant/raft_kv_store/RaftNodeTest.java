package com.yashwant.raft_kv_store;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RaftNodeTest {

    @Test
    void newNodeStartsAsFollowerWithTermZero(){
        RaftNode node = new RaftNode("node-1");

        assertEquals(0, node.getCurrentTerm());
        assertEquals(NodeRole.FOLLOWER, node.getRole());
        assertNull(node.getVotedFor());
    }

    @Test
    void startElectionIncrementsTermAndBecomesCandidate() {
        RaftNode node = new RaftNode("node-1");

        node.startElection();

        assertEquals(1, node.getCurrentTerm(), "Term should increment by 1");
        assertEquals(NodeRole.CANDIDATE, node.getRole(), "Node should become a candidate");
        assertEquals("node-1", node.getVotedFor(), "Node should vote for itself");
    }

    @Test
    void multipleElectionsKeepIncrementingTerm(){
        RaftNode node = new RaftNode("node-1");
        node.startElection();
        node.startElection();
        node.startElection();

        assertEquals(3, node.getCurrentTerm(), "Term should increment once per election call");
    }

    @Test
    void grantsVoteWhenNoPriorVoteAndSameOrHigherTerm() {
        RaftNode node = new RaftNode("node-1");

        boolean granted = node.receiveVoteRequest(1, "node-2", 0);

        assertTrue(granted);
        assertEquals("node-2", node.getVotedFor());
        assertEquals(1, node.getCurrentTerm());
    }

    @Test
    void rejectsVoteFromOlderTerm() {
        RaftNode node = new RaftNode("node-1");
        node.setCurrentTerm(5);

        boolean granted = node.receiveVoteRequest(3, "node-2", 0);

        assertFalse(granted);
    }

    @Test
    void rejectsSecondVoteRequestInSameTermFromDifferentCandidate() {
        RaftNode node = new RaftNode("node-1");

        node.receiveVoteRequest(1, "node-2", 0);
        boolean secondGranted = node.receiveVoteRequest(1, "node-3", 0);

        assertFalse(secondGranted);
        assertEquals("node-2", node.getVotedFor(), "Vote should remain with the first candidate");
    }

    @Test
    void candidateBecomesLeaderWithMajorityVotes() {
        RaftNode candidate = new RaftNode("node-1");
        RaftNode follower1 = new RaftNode("node-2");
        RaftNode follower2 = new RaftNode("node-3");

        candidate.startElection();

        int votesReceived = 1; // candidate votes for itself

        boolean vote1 = follower1.receiveVoteRequest(candidate.getCurrentTerm(), candidate.getNodeId(), 0);
        if (vote1) {
            votesReceived++;
        }

        boolean vote2 = follower2.receiveVoteRequest(candidate.getCurrentTerm(), candidate.getNodeId(), 0);
        if (vote2) {
            votesReceived++;
        }

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
    void candidateLosesElectionIfVotesAlreadyCastElsewhere() {
        RaftNode candidate = new RaftNode("node-1");
        RaftNode follower1 = new RaftNode("node-2");
        RaftNode follower2 = new RaftNode("node-3");

        // Both followers already voted for a different candidate in term 1
        follower1.receiveVoteRequest(1, "node-4", 0);
        follower2.receiveVoteRequest(1, "node-4", 0);

        candidate.startElection(); // candidate is now term 1, voted for itself

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
    void rejectsVoteWhenCandidateLogIsShorterThanVoterLog() {
        RaftNode node = new RaftNode("node-1");
        node.getLog().add(new LogEntry(1, "x=1"));
        node.getLog().add(new LogEntry(1, "x=2"));

        // Candidate claims only 1 log entry, but voter already has 2
        boolean granted = node.receiveVoteRequest(1, "node-2", 1);

        assertFalse(granted, "Should reject candidate whose log is shorter than voter's own");
    }
}