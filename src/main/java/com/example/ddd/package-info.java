/**
 * DDD marker annotations for this demo.
 *
 * These annotations exist <b>purely for reading and teaching purposes</b>:
 * they meta-document which DDD building block a type represents (aggregate
 * root, entity, value object, domain event, service, …). They carry no
 * behaviour, aspect, or runtime effect — nothing in the application reads
 * them. Think of them as executable comments that make the ubiquitous
 * language and the tactical patterns visible at a glance in the source.
 *
 * <p>Vocabulary (see each type for its meaning and typical usage):
 *
 * <ul>
 *   <li>{@link com.example.ddd.DDDBoundedContext} — on package-info of each application module</li>
 *   <li>{@link com.example.ddd.DDDAggregateRoot}, {@link com.example.ddd.DDDEntity},
 *       {@link com.example.ddd.DDDValueObject}</li>
 *   <li>{@link com.example.ddd.DDDEvent}, {@link com.example.ddd.DDDCommand}</li>
 *   <li>{@link com.example.ddd.DDDApplicationService}, {@link com.example.ddd.DDDDomainService},
 *       {@link com.example.ddd.DDDRepository}, {@link com.example.ddd.DDDFactory}</li>
 * </ul>
 *
 * <p>The package deliberately lives outside the application's module base
 * package ({@code com.example.antfarm}), so the markers never pollute the
 * Modulith module graph — they are meta-information about the code, not part
 * of it.
 */
package com.example.ddd;
