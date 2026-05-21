package com.example.EDIP;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableScheduling
@EnableAsync
@SpringBootApplication
@EnableJpaAuditing
public class EdipApplication {

	public static void main(String[] args) {
		SpringApplication.run(EdipApplication.class, args);
	}

}
