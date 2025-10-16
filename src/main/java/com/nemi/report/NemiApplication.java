package com.nemi.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@ComponentScan("com.nemi")
public class NemiApplication {

	static {
		// Disable AWS SDK initialization before Spring Boot starts
		// This runs before any Spring configuration
		disableAwsForLocal();
	}

	private static void disableAwsForLocal() {
		// Only disable AWS if running with local profile
		String[] profiles = System.getProperty("spring.profiles.active", "").split(",");
		boolean isLocal = false;
		for (String profile : profiles) {
			if ("local".equals(profile.trim())) {
				isLocal = true;
				break;
			}
		}

		if (isLocal) {
			// Set AWS region and credentials to prevent region detection
			System.setProperty("aws.region", "ap-southeast-1");
			System.setProperty("AWS_REGION", "ap-southeast-1");
			System.setProperty("AWS_DEFAULT_REGION", "ap-southeast-1");
			System.setProperty("aws.accessKeyId", "dummy");
			System.setProperty("aws.secretAccessKey", "dummy");
			System.setProperty("AWS_ACCESS_KEY_ID", "dummy");
			System.setProperty("AWS_SECRET_ACCESS_KEY", "dummy");

			// Disable EC2 metadata service
			System.setProperty("com.amazonaws.sdk.disableEc2Metadata", "true");
			System.setProperty("aws.disableEc2Metadata", "true");
			System.setProperty("aws.ec2MetadataDisabled", "true");

			// Disable region provider chain
			System.setProperty("software.amazon.awssdk.core.region.provider.disable", "true");
			System.setProperty("aws.region.provider.disable", "true");

			System.out.println("AWS SDK disabled for local profile");
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(NemiApplication.class, args);
	}
}
