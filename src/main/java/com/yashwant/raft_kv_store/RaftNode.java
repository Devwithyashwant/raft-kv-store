package com.yashwant.raft_kv_store;

import java.util.ArrayList;
import java.util.List;

public class RaftNode {

    private final String nodeId;
    private int currentTerm;
    private String votedFor;
    private NodeRole role;
    private final List<LogEntry> log;


    public RaftNode(String nodeId){
        this.nodeId = nodeId;
        this.currentTerm = 0;
        this.votedFor = null;
        this.role = NodeRole.FOLLOWER;
        this.log = new ArrayList<>();
    }

    public String getNodeId(){
        return nodeId;
    }

    public int getCurrentTerm(){
        return currentTerm;
    }

    public NodeRole getRole(){
        return role;
    }
    public List<LogEntry> getLog(){
        return log;
    }

    @Override
    public String toString(){
        return "RaftNode{id='" + nodeId + "' , term=" +currentTerm + ", role =" + role + ", logSize=" +log.size() + "}";
    }
}

