package com.yashwant.raft_kv_store;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RaftNode {

    private final String nodeId;
    private int currentTerm;
    private String votedFor;
    private NodeRole role;
    private final List<LogEntry> log;
    private final WriteAheadLog writeAheadLog;
    private final NodeState nodeState;
    private long lastHeartbeatTime;

    public RaftNode(String nodeId, Path logFilePath, Path stateFilePath) throws IOException {
        this.nodeId = nodeId;
        this.role = NodeRole.FOLLOWER;

        this.writeAheadLog = new WriteAheadLog(logFilePath);
        this.log = new ArrayList<>(writeAheadLog.readAll());

        this.nodeState = new NodeState(stateFilePath);
        this.currentTerm = nodeState.loadTerm();
        this.votedFor = nodeState.loadVotedFor();
        this.lastHeartbeatTime = System.currentTimeMillis();
    }

    public void appendEntry(LogEntry entry) throws IOException {
        writeAheadLog.append(entry);
        log.add(entry);
    }

    private void persistState() throws IOException {
        nodeState.save(currentTerm, votedFor);
    }

    public String getNodeId() {
        return nodeId;
    }

    public int getCurrentTerm() {
        return currentTerm;
    }

    public NodeRole getRole() {
        return role;
    }

    public long getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }
    public List<LogEntry> getLog() {
        return log;
    }

    public String getVotedFor() {
        return votedFor;
    }

    public void setCurrentTerm(int currentTerm) throws IOException {
        this.currentTerm = currentTerm;
        persistState();
    }

    public void setVotedFor(String votedFor) throws IOException {
        this.votedFor = votedFor;
        persistState();
    }

    public void setRole(NodeRole role) {
        this.role = role;
    }

    public void startElection() {
        this.currentTerm = this.currentTerm + 1;
        this.votedFor = this.nodeId;
        this.role = NodeRole.CANDIDATE;
        this.lastHeartbeatTime = System.currentTimeMillis();
    }

    public boolean receiveVoteRequest(int candidateTerm, String candidateId, int candidateLogLength) throws IOException {
        if (candidateTerm < this.currentTerm) {
            return false;
        }

        if (candidateTerm > this.currentTerm) {
            this.currentTerm = candidateTerm;
            this.votedFor = null;
            this.role = NodeRole.FOLLOWER;
        }

        boolean alreadyVotedForSomeoneElse = this.votedFor != null && !this.votedFor.equals(candidateId);
        boolean candidateLogIsUpToDate = candidateLogLength >= this.log.size();

        if (!alreadyVotedForSomeoneElse && candidateLogIsUpToDate) {
            this.votedFor = candidateId;
            persistState();
            return true;
        }

        persistState();
        return false;
    }
    public boolean receiveHeartbeat(int leaderTerm, String leaderId) throws IOException {
        if (leaderTerm < this.currentTerm) {
            System.out.println(this.nodeId + " rejected heartbeat from " + leaderId + " (stale term " + leaderTerm + ", current term is " + this.currentTerm + ")");
            return false;
        }

        if (leaderTerm > this.currentTerm) {
            this.currentTerm = leaderTerm;
            this.votedFor = null;
            persistState();
        }

        this.role = NodeRole.FOLLOWER;
        this.lastHeartbeatTime = System.currentTimeMillis();
        System.out.println(this.nodeId + " received heartbeat from " + leaderId + " for term " + leaderTerm);
        return true;
    }

    @Override
    public String toString() {
        return "RaftNode{id='" + nodeId + "', term=" + currentTerm + ", role=" + role + ", logSize=" + log.size() + "}";
    }
}