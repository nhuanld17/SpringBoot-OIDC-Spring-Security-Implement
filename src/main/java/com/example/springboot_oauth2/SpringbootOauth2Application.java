package com.example.springboot_oauth2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan // kích hoạt quét các @ConfigurationProperties (GoogleOidcProperties)
public class SpringbootOauth2Application {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootOauth2Application.class, args);
	}

}
