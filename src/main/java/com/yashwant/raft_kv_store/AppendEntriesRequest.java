package com.yashwant.raft_kv_store;

public class AppendEntriesRequest {
    private int leaderTerm;
    private String leaderId;
    private int prevLogIndex;
    private int prevLogTerm;
    private int entryTerm;
    private String command;

    public AppendEntriesRequest() {
    }

    public AppendEntriesRequest(int leaderTerm, String leaderId, int prevLogIndex, int prevLogTerm, int entryTerm, String command) {
        this.leaderTerm = leaderTerm;
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entryTerm = entryTerm;
        this.command = command;
    }

    public int getLeaderTerm() {
        return leaderTerm;
    }

    public void setLeaderTerm(int leaderTerm) {
        this.leaderTerm = leaderTerm;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(String leaderId) {
        this.leaderId = leaderId;
    }

    public int getPrevLogIndex() {
        return prevLogIndex;
    }

    public void setPrevLogIndex(int prevLogIndex) {
        this.prevLogIndex = prevLogIndex;
    }

    public int getPrevLogTerm() {
        return prevLogTerm;
    }

    public void setPrevLogTerm(int prevLogTerm) {
        this.prevLogTerm = prevLogTerm;
    }

    public int getEntryTerm() {
        return entryTerm;
    }

    public void setEntryTerm(int entryTerm) {
        this.entryTerm = entryTerm;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}