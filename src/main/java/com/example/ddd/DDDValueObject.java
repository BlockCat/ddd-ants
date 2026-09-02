package com.example.ddd;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a <b>value object</b>: an immutable object whose equality is based on
 * <i>what it is</i>, not <i>who it is</i>.
 *
 * <p>Value objects are self-validating and expose behaviour, never raw
 * setters. In this demo: {@code Position}, the typed ids ({@code ColonyId},
 * {@code AntId}, …), policies ({@code ColonyPolicy}, {@code AntPolicy}).
 *
 * <p>Descriptive only.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DDDValueObject {
}
