package com.jared.lineserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
//@EnableCaching
public class LineServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(LineServerApplication.class, args);
	}

}
