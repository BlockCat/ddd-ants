package com.example.antfarm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

/**
 * Headless synchronous {@link ApplicationEventPublisher} for the plain (no
 * Spring context) lifecycle test.
 *
 * <p>Mirrors Spring's synchronous {@code @EventListener} semantics: events
 * published during a tick are dispatched immediately, on the same thread, to
 * every registered listener method whose single parameter matches the event
 * type. Cross-module event chains (request → grant → effect) therefore still
 * land in the same tick and stay deterministic, exactly as they do in the
 * running application.
 */
public class SynchronousEventBus implements ApplicationEventPublisher {

	public final List<Object> published = new ArrayList<>();

	private final List<Object> listeners = new ArrayList<>();
	private final Map<Class<?>, List<Method>> handlers = new HashMap<>();

	/** Registers a bean and scans its {@code @EventListener} methods. */
	public void register(Object bean) {
		listeners.add(bean);
		for (Method method : bean.getClass().getDeclaredMethods()) {
			if (method.isAnnotationPresent(EventListener.class)) {
				Class<?>[] params = method.getParameterTypes();
				if (params.length == 1) {
					handlers.computeIfAbsent(params[0], key -> new ArrayList<>()).add(method);
					method.setAccessible(true);
				}
			}
		}
	}

	@Override
	public void publishEvent(Object event) {
		published.add(event);
		for (Method method : handlers.getOrDefault(event.getClass(), List.of())) {
			for (Object listener : listeners) {
				if (method.getDeclaringClass().isInstance(listener)) {
					invoke(listener, method, event);
				}
			}
		}
	}

	private void invoke(Object listener, Method method, Object event) {
		try {
			method.invoke(listener, event);
		} catch (IllegalAccessException | InvocationTargetException ex) {
			Throwable cause = ex instanceof InvocationTargetException ite ? ite.getCause() : ex;
			throw new RuntimeException("Failed to dispatch " + event + " to " + method, cause);
		}
	}
}
