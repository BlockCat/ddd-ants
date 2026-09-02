package com.example.antfarm.simulation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.antfarm.world.WorldService;

/**
 * Browser-facing API of the simulation (read-only view model + run
 * controls): the static terrain, the current state, live SSE snapshots, and
 * pause/resume/speed.
 */
@RestController
@RequestMapping("/api/sim")
public class SimulationApiController {

	private static final Logger log = LoggerFactory.getLogger(SimulationApiController.class);

	private final WorldService world;
	private final SimulationSnapshotBuilder snapshots;
	private final SimulationBroadcaster broadcaster;
	private final SimulationEngine engine;

	public SimulationApiController(WorldService world, SimulationSnapshotBuilder snapshots,
			SimulationBroadcaster broadcaster, SimulationEngine engine) {
		this.world = world;
		this.snapshots = snapshots;
		this.broadcaster = broadcaster;
		this.engine = engine;
	}

	/** Terrain grid (changes as workers dig): {@code cells[y][x]} kind ordinal. */
	@GetMapping("/terrain")
	public TerrainView terrain() {
		return new TerrainView(world.width(), world.height(), world.terrainRows());
	}

	public record TerrainView(int width, int height, int[][] cells) {
	}

	/** Current simulation state (polling fallback / initial paint). */
	@GetMapping("/state")
	public SimulationSnapshot state() {
		return snapshots.current(engine.tick(), engine.colonyId(), !engine.isPaused(), engine.ticksPerSecond());
	}

	/** Live snapshot stream — one {@code snapshot} event per tick. */
	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream() {
		SseEmitter emitter = broadcaster.subscribe();
		try {
			emitter.send(SseEmitter.event().name("snapshot")
					.data(snapshots.current(engine.tick(), engine.colonyId(), !engine.isPaused(), engine.ticksPerSecond())));
		} catch (Exception ex) {
			log.warn("Could not send initial snapshot to new stream client: {}", ex.getMessage());
		}
		return emitter;
	}

	@PostMapping("/pause")
	public void pause() {
		engine.pause();
	}

	@PostMapping("/resume")
	public void resume() {
		engine.resume();
	}

	/** Speed multiplier: 1 = base tick rate, 2 = double, 0.5 = half. */
	@PostMapping("/speed")
	public void speed(@RequestParam("multiplier") double multiplier) {
		engine.setSpeed(multiplier);
	}

	@GetMapping("/status")
	public Status status() {
		return new Status(engine.tick(), engine.isPaused(), engine.ticksPerSecond(), engine.colonyId() != null);
	}

	public record Status(long tick, boolean paused, double ticksPerSecond, boolean worldInitialized) {
	}
}
