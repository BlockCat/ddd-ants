package com.example.antfarm;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;

/**
 * Test double for {@link ApplicationEventPublisher} that records every
 * published event (used by module unit tests and the engine lifecycle test).
 */
public class RecordingPublisher implements ApplicationEventPublisher {

	public final List<Object> published = new ArrayList<>();

	@Override
	public void publishEvent(Object event) {
		published.add(event);
	}
}
