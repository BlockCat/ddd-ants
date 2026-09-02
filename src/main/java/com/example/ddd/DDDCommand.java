package com.example.ddd;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a <b>command</b>: an instruction to change state, named in the
 * imperative mood, carrying everything needed to perform one use case step.
 *
 * <p>Commands cross module boundaries in one direction only: the caller
 * expresses intent, the owning module performs the change. In this demo:
 * {@code SpawnAnt} (engine → ants context).
 *
 * <p>Descriptive only.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DDDCommand {
}
