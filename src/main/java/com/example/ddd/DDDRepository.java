package com.example.ddd;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a <b>repository</b>: a collection-like access port for aggregates —
 * retrieval by id/query and persistence of new or modified roots. One
 * repository per aggregate root; the interface belongs to the domain, the
 * implementation to infrastructure.
 *
 * <p>Descriptive only. (In this in-memory increment, each context's
 * application service keeps its aggregates in a map — a repository
 * stand-in.)
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DDDRepository {
}
