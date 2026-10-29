package com.yashwant.raft_kv_store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RaftKvStoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(RaftKvStoreApplication.class, args);
		RaftNode node = new RaftNode("node-1");
		System.out.println(node);
	}

}
