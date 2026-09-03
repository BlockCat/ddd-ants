package com.example.antfarm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

@Modulithic(systemName = "AntFarm")
@SpringBootApplication
public class AntFarmApplication {

	public static void main(String[] args) {
		SpringApplication.run(AntFarmApplication.class, args);
	}

}
