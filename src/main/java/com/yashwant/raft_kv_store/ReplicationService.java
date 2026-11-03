package com.yashwant.raft_kv_store;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class ReplicationService {

    private final RaftNode node;
    private final RaftPeerClient peerClient;
    private final List<String> peerUrls;

    public ReplicationService(RaftNode node, RaftPeerClient peerClient,
                              @Value("${raft.node.peers}") List<String> peerUrls) {
        this.node = node;
        this.peerClient = peerClient;
        this.peerUrls = peerUrls;
    }

    public boolean replicate(String command) throws IOException {
        LogEntry entry = new LogEntry(node.getCurrentTerm(), command);
        node.appendEntry(entry);

        int acknowledgedCount = 1; // leader's own log counts

        AppendEntriesRequest request = new AppendEntriesRequest(
                node.getCurrentTerm(),
                node.getNodeId(),
                entry.getTerm(),
                entry.getCommand()
        );

        for (String peerUrl : peerUrls) {
            try {
                AppendEntriesResponse response = peerClient.sendAppendEntries(peerUrl, request);
                if (response.isSuccess()) {
                    acknowledgedCount++;
                }
            } catch (Exception e) {
                System.out.println("Failed to replicate to " + peerUrl + ": " + e.getMessage());
            }
        }

        int clusterSize = peerUrls.size() + 1;
        int majorityNeeded = (clusterSize / 2) + 1;

        boolean committed = acknowledgedCount >= majorityNeeded;

        System.out.println(node.getNodeId() + " replicated '" + command + "' - acknowledged by " + acknowledgedCount + "/" + clusterSize + " nodes, committed: " + committed);

        return committed;
    }
}