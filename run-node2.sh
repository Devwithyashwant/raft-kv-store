#!/bin/bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8082 --raft.node.id=node-2 --raft.node.dataDir=./data/node-2 --raft.node.peers=http://localhost:8081,http://localhost:8083"
