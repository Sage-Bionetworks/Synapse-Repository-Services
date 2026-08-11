package org.sagebionetworks.repo.manager.agent.tool;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.sagebionetworks.schema.adapter.JSONEntity;

/**
 * Used to provide a description to an agent tool {@link JSONEntity} parameter.
 */
@Target({ ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JSONEntityToolParam {
	/**
	 * Whether the tool argument is required.
	 */
	boolean required() default true;

	/**
	 * The description of the tool argument.
	 */
	String description() default "";

	/**
	 * The {@link JSONEntity} type used to generate the tool's input schema. Defaults to
	 * {@link JSONEntity} itself, which signals "use the parameter's own declared type".
	 * <p>
	 * Set this only when the parameter is not itself a {@link JSONEntity} — e.g. a raw
	 * {@code String} payload whose structure should still be advertised to the model by the
	 * given type's schema (used where round-tripping through the POJO would lose information
	 * the raw JSON must preserve).
	 */
	Class<? extends JSONEntity> schemaType() default JSONEntity.class;
}
