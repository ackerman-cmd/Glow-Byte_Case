package com.work.matmode.upload_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class UploadServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UploadServiceApplication.class, args);
	}

}
