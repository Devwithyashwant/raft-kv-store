package com.yashwant.raft_kv_store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RaftKvStoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(RaftKvStoreApplication.class, args);
	}
}