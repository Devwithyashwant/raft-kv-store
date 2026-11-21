package com.yashwant.raft_kv_store;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReplicationService {

    private final RaftNode node;
    private final RaftPeerClient peerClient;
    private final List<String> peerUrls;

    // Tracks, per peer, the next log index (1-based) we believe that peer needs.
    // Starts empty - the first attempt for a peer just assumes it's caught up
    // except for the newest entry, and corrects itself via rejections if wrong.
    private final Map<String, Integer> nextIndex = new ConcurrentHashMap<>();

    public ReplicationService(RaftNode node, RaftPeerClient peerClient,
                              @Value("${raft.node.peers}") List<String> peerUrls) {
        this.node = node;
        this.peerClient = peerClient;
        this.peerUrls = peerUrls;
    }

    public boolean replicate(String command) throws IOException {
        LogEntry entry = new LogEntry(node.getCurrentTerm(), command);
        node.appendEntry(entry);
        int targetIndex = node.getLog().size(); // 1-based index of the entry we just appended

        int acknowledgedCount = 1; // leader's own log counts

        for (String peerUrl : peerUrls) {
            try {
                if (replicateToPeer(peerUrl, targetIndex)) {
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

    /**
     * Sends the entry at targetIndex to a single peer. If the peer rejects it
     * because its log doesn't match (missing or conflicting previous entry),
     * walks backward one index at a time until the peer accepts, then replays
     * forward entry-by-entry until the peer has caught up to targetIndex.
     * Returns true once the peer has confirmed it has the entry at targetIndex.
     */
    private boolean replicateToPeer(String peerUrl, int targetIndex) {
        List<LogEntry> log = node.getLog();
        int indexToSend = nextIndex.getOrDefault(peerUrl, targetIndex);

        // Bounded so a persistently broken peer can't loop forever.
        int maxAttempts = log.size() + 1;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            if (indexToSend < 1) {
                indexToSend = 1;
            }

            int prevLogIndex = indexToSend - 1;
            int prevLogTerm = prevLogIndex == 0 ? 0 : log.get(prevLogIndex - 1).getTerm();
            LogEntry entryToSend = log.get(indexToSend - 1);

            AppendEntriesRequest request = new AppendEntriesRequest(
                    node.getCurrentTerm(),
                    node.getNodeId(),
                    prevLogIndex,
                    prevLogTerm,
                    entryToSend.getTerm(),
                    entryToSend.getCommand()
            );

            AppendEntriesResponse response = peerClient.sendAppendEntries(peerUrl, request);

            if (response.getCurrentTerm() > node.getCurrentTerm()) {
                // This peer is on a higher term than us - we shouldn't still be
                // acting as leader. Stop trying to replicate to it; the next
                // election-timeout check will see the higher term elsewhere.
                System.out.println(node.getNodeId() + " saw higher term " + response.getCurrentTerm() + " from " + peerUrl + " during replication");
                return false;
            }

            if (response.isSuccess()) {
                nextIndex.put(peerUrl, indexToSend + 1);
                if (indexToSend >= targetIndex) {
                    return true;
                }
                indexToSend++; // peer is now caught up one more - send the next entry
            } else {
                // Log mismatch at this index - back off and try an earlier entry.
                indexToSend--;
            }
        }

        return false;
    }
}