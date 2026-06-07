package com.symphonia;

import org.springframework.boot.SpringApplication;

public class TestSymphoniaApplication {

	public static void main(String[] args) {
		SpringApplication.from(SymphoniaApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
