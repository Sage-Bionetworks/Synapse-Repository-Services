package org.sagebionetworks.repo.manager.schema;

import java.util.Optional;

import org.everit.json.schema.event.ValidationListener;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.schema.JsonSchema;

/**
 * Derived annotations are annotations that are implied either conditionally, or
 * unconditionally according to either 'const' or 'default' properties of a
 * {@link JsonSchema}. This interface defines an abstraction of
 * {@link ValidationListener} that is used to calculate the derived annotations
 * of a {@link JSONObject} (subject) according to a {@link JsonSchema}.
 * </p>
 * 
 * Note: A visitor is stateful, so a new visitor must be created for each case.
 *
 */
public interface DerivedAnnotationVisitor extends ValidationListener {

	/**
	 * The derived {@link Annotations} that were captured during the validation of
	 * {@link JSONObject} against its defining {@link JsonSchema}.
	 * 
	 * @return {@link Optional#empty()} when there are no derived annotations.
	 */
	Optional<Annotations> getDerivedAnnotations();
}
