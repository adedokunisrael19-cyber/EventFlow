package com.eventflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class EventflowApiApplication {

    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }

	public static void main(String[] args) {
		SpringApplication.run(EventflowApiApplication.class, args);
	}

}
