#!/bin/bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081 --raft.node.id=node-1 --raft.node.dataDir=./data/node-1 --raft.node.peers=http://localhost:8082,http://localhost:8083"
