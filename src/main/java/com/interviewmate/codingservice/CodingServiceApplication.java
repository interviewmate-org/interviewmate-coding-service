package com.interviewmate.codingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.ReactiveUserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {
	ReactiveUserDetailsServiceAutoConfiguration.class,
})
public class CodingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CodingServiceApplication.class, args);
	}

}
