package com.example.antfarm.simulation;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Fan-out of {@link SimulationSnapshot}s to subscribed browser clients via
 * Server-Sent Events. The engine broadcasts after every tick; each emitter
 * is removed when the client disconnects.
 */
@Component
public class SimulationBroadcaster {

	private static final Logger log = LoggerFactory.getLogger(SimulationBroadcaster.class);

	private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
	private final AtomicLong ids = new AtomicLong();

	public SseEmitter subscribe() {
		SseEmitter emitter = new SseEmitter(0L); // no timeout — stream lives as long as the client
		long id = ids.incrementAndGet();
		emitters.put(id, emitter);
		Runnable cleanup = () -> {
			if (emitters.remove(id) != null) {
				log.info("Simulation stream client {} disconnected ({} active)", id, emitters.size());
			}
		};
		emitter.onCompletion(cleanup);
		emitter.onTimeout(cleanup);
		emitter.onError(e -> cleanup.run());
		log.info("Simulation stream client {} subscribed ({} active)", id, emitters.size());
		return emitter;
	}

	public boolean hasSubscribers() {
		return !emitters.isEmpty();
	}

	public void broadcast(SimulationSnapshot snapshot) {
		for (Map.Entry<Long, SseEmitter> entry : emitters.entrySet()) {
			try {
				entry.getValue().send(SseEmitter.event().name("snapshot").data(snapshot));
			} catch (IOException | IllegalStateException ex) {
				log.debug("Dropping dead stream client {}: {}", entry.getKey(), ex.getMessage());
				emitters.remove(entry.getKey());
			}
		}
	}
}
