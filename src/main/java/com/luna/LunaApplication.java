package com.luna;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LunaApplication {

	public static void main(String[] args) {
		// Load .env file for development
		try {
			java.nio.file.Files.lines(java.nio.file.Paths.get(".env"))
				.filter(line -> !line.startsWith("#") && line.contains("="))
				.forEach(line -> {
					String[] parts = line.split("=", 2);
					if (parts.length == 2) {
						System.setProperty(parts[0].trim(), parts[1].trim());
					}
				});
		} catch (Exception e) {
			// .env file not found or error reading, use defaults
		}
		
		SpringApplication app = new SpringApplication(LunaApplication.class);
		app.run(args);
	}

}
