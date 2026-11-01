package com.yashwant.raft_kv_store;

import  org.junit.jupiter.api.Test;
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
        assertEquals(NodeRole.CANDIDATE, node.getRole(), "Mode should become a candidate");
        assertEquals("node-1", node.getVotedFor(), "Node should vote for itself");


    }

    @Test
    void multipleElectionsKeepIncrementingTerm(){
        RaftNode node = new RaftNode("node-1");
        node.startElection();
        node.startElection();
        node.startElection();

        assertEquals(3,node.getCurrentTerm(), "Term should increment once per election call");
    }
}
