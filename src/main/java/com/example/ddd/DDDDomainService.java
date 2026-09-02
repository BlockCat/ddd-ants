package com.example.ddd;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a <b>domain service</b>: a stateless operation that expresses
 * domain logic which does not naturally belong to a single aggregate
 * (usually because it coordinates several aggregates or applies an external
 * policy). Unlike an application service it is pure domain — no
 * transactions, no I/O.
 *
 * <p>Descriptive only.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DDDDomainService {
}
