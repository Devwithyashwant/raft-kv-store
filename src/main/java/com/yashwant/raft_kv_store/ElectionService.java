package com.yashwant.raft_kv_store;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class ElectionService {

    private final RaftNode node;
    private final RaftPeerClient peerClient;
    private final List<String> peerUrls;

    public ElectionService(RaftNode node, RaftPeerClient peerClient,
                           @Value("${raft.node.peers}") List<String> peerUrls) {
        this.node = node;
        this.peerClient = peerClient;
        this.peerUrls = peerUrls;
    }

    public void runElection() throws IOException {
        node.startElection();

        int votesReceived = 1; // self-vote, already counted inside startElection()

        VoteRequest request = new VoteRequest(
                node.getCurrentTerm(),
                node.getNodeId(),
                node.getLog().size()
        );

        for (String peerUrl : peerUrls) {
            try {
                VoteResponse response = peerClient.requestVote(peerUrl, request);
                if (response.isVoteGranted()) {
                    votesReceived++;
                }
            } catch (Exception e) {
                System.out.println("Failed to reach peer " + peerUrl + ": " + e.getMessage());
            }
        }

        int clusterSize = peerUrls.size() + 1; // peers + this node
        int majorityNeeded = (clusterSize / 2) + 1;

        if (votesReceived >= majorityNeeded) {
            node.setRole(NodeRole.LEADER);
            System.out.println(node.getNodeId() + " won the election with " + votesReceived + " votes, is now LEADER for term " + node.getCurrentTerm());
        } else {
            System.out.println(node.getNodeId() + " lost the election with only " + votesReceived + " votes");
        }
    }
}