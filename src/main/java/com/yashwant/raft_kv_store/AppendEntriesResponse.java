package com.yashwant.raft_kv_store;

public class AppendEntriesResponse {
    private boolean success;
    private int currentTerm;

    public AppendEntriesResponse() {
    }

    public AppendEntriesResponse(boolean success, int currentTerm) {
        this.success = success;
        this.currentTerm = currentTerm;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getCurrentTerm() {
        return currentTerm;
    }

    public void setCurrentTerm(int currentTerm) {
        this.currentTerm = currentTerm;
    }
}