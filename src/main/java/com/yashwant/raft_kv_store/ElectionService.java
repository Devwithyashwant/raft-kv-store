package com.yashwant.raft_kv_store;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Random;

@Service
public class ElectionService {

    private final RaftNode node;
    private final RaftPeerClient peerClient;
    private final List<String> peerUrls;
    private final long electionTimeoutMillis;

    public ElectionService(RaftNode node, RaftPeerClient peerClient,
                           @Value("${raft.node.peers}") List<String> peerUrls) {
        this.node = node;
        this.peerClient = peerClient;
        this.peerUrls = peerUrls;
        this.electionTimeoutMillis = 3000 + new Random().nextInt(2000);
        System.out.println(node.getNodeId() + " configured with peers: " + peerUrls + ", election timeout: " + electionTimeoutMillis + "ms");
    }

    public void runElection() throws IOException {
        node.startElection();

        int votesReceived = 1;

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

        int clusterSize = peerUrls.size() + 1;
        int majorityNeeded = (clusterSize / 2) + 1;

        if (votesReceived >= majorityNeeded) {
            node.setRole(NodeRole.LEADER);
            System.out.println(node.getNodeId() + " won the election with " + votesReceived + " votes, is now LEADER for term " + node.getCurrentTerm());
        } else {
            node.setRole(NodeRole.FOLLOWER);
            System.out.println(node.getNodeId() + " lost the election with only " + votesReceived + " votes");
        }
    }

    @Scheduled(fixedRate = 1000)
    public void sendHeartbeatsIfLeader() {
        if (node.getRole() != NodeRole.LEADER) {
            return;
        }

        VoteRequest heartbeat = new VoteRequest(node.getCurrentTerm(), node.getNodeId(), node.getLog().size());

        for (String peerUrl : peerUrls) {
            try {
                peerClient.sendHeartbeat(peerUrl, heartbeat);
            } catch (Exception e) {
                System.out.println("Failed to send heartbeat to " + peerUrl + ": " + e.getMessage());
            }
        }
    }

    @Scheduled(fixedRate = 500)
    public void checkElectionTimeout() throws IOException {
        if (node.getRole() == NodeRole.LEADER) {
            return;
        }

        long timeSinceLastHeartbeat = System.currentTimeMillis() - node.getLastHeartbeatTime();

        if (timeSinceLastHeartbeat > electionTimeoutMillis) {
            System.out.println(node.getNodeId() + " detected leader timeout (" + timeSinceLastHeartbeat + "ms since last heartbeat), starting election");
            runElection();
        }
    }
}