package com.yashwant.raft_kv_store;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class RaftController {

    private final RaftNode node;

    @Autowired
    private ElectionService electionService;

    @Autowired
    private ReplicationService replicationService;

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

    @PostMapping("/heartbeat")
    public VoteResponse heartbeat(@RequestBody VoteRequest request) throws IOException {
        boolean accepted = node.receiveHeartbeat(request.getCandidateTerm(), request.getCandidateId());
        return new VoteResponse(accepted, node.getCurrentTerm());
    }

    @PostMapping("/triggerElection")
    public String triggerElection() throws IOException {
        electionService.runElection();
        return node.toString();
    }

    @PostMapping("/appendEntries")
    public AppendEntriesResponse appendEntries(@RequestBody AppendEntriesRequest request) throws IOException {
        boolean success = node.receiveAppendEntries(
                request.getLeaderTerm(),
                request.getLeaderId(),
                request.getEntryTerm(),
                request.getCommand()
        );

        return new AppendEntriesResponse(success, node.getCurrentTerm());
    }

    @PostMapping("/write")
    public String write(@RequestParam String command) throws IOException {
        if (node.getRole() != NodeRole.LEADER) {
            return "NOT_LEADER: this node is not the leader, retry against the current leader";
        }

        boolean committed = replicationService.replicate(command);

        if (committed) {
            return "OK: committed";
        } else {
            return "FAILED: could not reach majority";
        }
    }
}