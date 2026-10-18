package com.pandora6ix.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Pandora6ixBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(Pandora6ixBackendApplication.class, args);
	}

}
