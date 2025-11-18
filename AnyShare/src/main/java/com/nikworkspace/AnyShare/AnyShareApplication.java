package com.nikworkspace.AnyShare;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class AnyShareApplication {

	public static void main(String[] args) {
		SpringApplication.run(AnyShareApplication.class, args);
        log.info("AnyShare Application Started Successfully.");
    }

}
