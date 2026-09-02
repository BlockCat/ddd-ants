package com.example.antfarm;

import org.springframework.boot.SpringApplication;

public class TestAntFarmApplication {

	public static void main(String[] args) {
		SpringApplication.from(AntFarmApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
