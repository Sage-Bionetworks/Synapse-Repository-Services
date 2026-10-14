package org.sagebionetworks.repo.web.rest.doc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Forces inclusion of an otherwise-{@link Deprecated} controller method in the generated OpenAPI
 * specification. Without this annotation, {@link Deprecated} controller methods are excluded from
 * the OpenAPI documentation.
 * <p>
 * This annotation is intended to be paired with {@link Deprecated} on the same method. Included
 * methods are still flagged as {@code deprecated: true} in the OpenAPI output.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IncludeInOpenApiDoc {

}
