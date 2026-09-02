package com.example.ddd;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a <b>bounded context</b> (package-level, on {@code package-info.java}).
 *
 * <p>A bounded context is the boundary inside which one domain model — one
 * set of terms, one set of rules — is valid. In this demo each bounded
 * context is implemented as one Spring Modulith application module; this
 * annotation names the context and states its responsibility so the module
 * declaration reads like the context map.
 *
 * <p>Descriptive only — the actual boundary is enforced by Modulith
 * ({@code ApplicationModules.verify()}).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PACKAGE)
public @interface DDDBoundedContext {

	/** Name of the bounded context (matches the module / ubiquitous language). */
	String name();

	/** One-line responsibility, mirroring the context map. */
	String description() default "";
}
