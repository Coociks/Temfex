package com.coociks.temfex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TemfexApplication {

	public static void main(String[] args) {
		SpringApplication.run(TemfexApplication.class, args);
	}

}
