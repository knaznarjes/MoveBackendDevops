package com.move.contentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableDiscoveryClient
@EnableMongoAuditing

public class ContentserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ContentserviceApplication.class, args);
	}

}
