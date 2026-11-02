package com.yashwant.raft_kv_store;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Path;

@Configuration
public class RaftNodeConfig {

    @Value("${raft.node.id}")
    private String nodeId;

    @Value("${raft.node.dataDir}")
    private String dataDir;

    @Bean
    public RaftNode raftNode() throws IOException {
        Path logPath = Path.of(dataDir, nodeId + ".log");
        Path statePath = Path.of(dataDir, nodeId + ".state");
        return new RaftNode(nodeId, logPath, statePath);
    }
}