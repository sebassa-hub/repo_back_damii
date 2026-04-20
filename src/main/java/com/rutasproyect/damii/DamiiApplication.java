package com.rutasproyect.damii;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class DamiiApplication {

	public static void main(String[] args) {
		SpringApplication.run(DamiiApplication.class, args);
	}

}
