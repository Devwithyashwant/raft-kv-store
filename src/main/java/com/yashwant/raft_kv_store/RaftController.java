package com.yashwant.raft_kv_store;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class RaftController {

    private final RaftNode node;

    @Autowired
    private ElectionService electionService;

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

    @PostMapping("/triggerElection")
    public String triggerElection() throws IOException {
        electionService.runElection();
        return node.toString();
    }
}