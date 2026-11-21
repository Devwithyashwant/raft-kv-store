#!/bin/bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8083 --raft.node.id=node-3 --raft.node.dataDir=./data/node-3 --raft.node.peers=http://localhost:8081,http://localhost:8082"
