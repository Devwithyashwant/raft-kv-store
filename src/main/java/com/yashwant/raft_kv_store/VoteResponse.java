package com.yashwant.raft_kv_store;

public class VoteResponse {

    private boolean voteGranted;
    private int currentTerm;

    public VoteResponse(){

    }
    public VoteResponse(boolean voteGranted, int currentTerm){
        this.voteGranted = voteGranted;
        this.currentTerm = currentTerm;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }
    public void setVoteGranted(boolean voteGranted) {
        this.voteGranted = voteGranted;
    }
    public int getCurrentTerm() {
        return currentTerm;
    }
    public void setCurrentTerm(int currentTerm) {
        this.currentTerm = currentTerm;
    }
}
