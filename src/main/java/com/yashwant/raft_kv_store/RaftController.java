package com.yashwant.raft_kv_store;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class RaftController {

    private final RaftNode node;

    @Autowired
    private RaftPeerClient peerClient;

    public RaftController(RaftNode node) {
        this.node = node;
    }

    @PostMapping("/requestVote")
    public VoteResponse requestVote(@RequestBody VoteRequest request) throws IOException {
        boolean granted = node.receiveVoteRequest(
                request.getCandidateTerm(),
                request.getCandidateId(),
                request.getCandidateLogLength()
        );

        return new VoteResponse(granted, node.getCurrentTerm());
    }

    @GetMapping("/testCallPeer")
    public VoteResponse testCallPeer(@RequestParam String peerUrl) {
        VoteRequest request = new VoteRequest(node.getCurrentTerm() + 1, node.getNodeId(), node.getLog().size());
        return peerClient.requestVote(peerUrl, request);
    }
}