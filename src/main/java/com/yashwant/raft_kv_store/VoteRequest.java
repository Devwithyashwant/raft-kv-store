package com.yashwant.raft_kv_store;


public class VoteRequest {
    private int candidateTerm;
    private int candidateLogLength;
    private String candidateId;

    public VoteRequest() {
    }

    public VoteRequest(int candidateTerm , String candidateId , int candidateLogLength){
        this.candidateTerm = candidateTerm;
        this.candidateId = candidateId;
        this.candidateLogLength = candidateLogLength;


    }
    public int getCandidateTerm(){
        return candidateTerm;

    }
    public void setCandidateTerm(int candidateTerm){
        this.candidateTerm = candidateTerm;
    }
    public String getCandidateId(){
        return candidateId;
    }
    public void setCandidateId(String candidateId){
        this.candidateId = candidateId;
    }
    public int getCandidateLogLength(){
        return candidateLogLength;
    }
    public void setCandidateLogLength(int candidateLogLength){
        this.candidateLogLength = candidateLogLength;
    }
}

