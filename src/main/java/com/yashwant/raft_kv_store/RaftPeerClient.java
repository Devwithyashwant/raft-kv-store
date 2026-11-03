package com.yashwant.raft_kv_store;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RaftPeerClient {

    private final RestClient restClient = RestClient.create();

    public VoteResponse requestVote(String peerBaseUrl, VoteRequest request) {
        return restClient.post()
                .uri(peerBaseUrl + "/requestVote")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(VoteResponse.class);
    }
    public VoteResponse sendHeartbeat(String peerBaseUrl, VoteRequest request) {
        return restClient.post()
                .uri(peerBaseUrl + "/heartbeat")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(VoteResponse.class);
    }
}