package com.yashwant.raft_kv_store;

public class AppendEntriesRequest {
    private int leaderTerm;
    private String leaderId;
    private int entryTerm;
    private String command;

    public AppendEntriesRequest() {
    }

    public AppendEntriesRequest(int leaderTerm, String leaderId, int entryTerm, String command) {
        this.leaderTerm = leaderTerm;
        this.leaderId = leaderId;
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