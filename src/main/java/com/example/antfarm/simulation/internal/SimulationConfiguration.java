package com.example.antfarm.simulation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Module wiring for the simulation context: turns on the tick scheduler and
 * registers the typed simulation properties.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(SimulationProperties.class)
public class SimulationConfiguration {
}
