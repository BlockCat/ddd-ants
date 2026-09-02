package com.example.ddd;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a <b>factory</b>: a place that encapsulates complex creation — a
 * static factory or dedicated factory method that produces aggregates in a
 * valid starting state (named in the ubiquitous language, e.g.
 * {@code Colony.place(...)}), instead of exposing constructors that callers
 * can misuse.
 *
 * <p>Descriptive only.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface DDDFactory {
}
