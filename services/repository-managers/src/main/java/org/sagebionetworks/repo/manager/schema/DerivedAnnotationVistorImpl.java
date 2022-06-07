package org.sagebionetworks.repo.manager.schema;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Set;

import org.everit.json.schema.CombinedSchema;
import org.everit.json.schema.ConstSchema;
import org.everit.json.schema.ObjectSchema;
import org.everit.json.schema.ReferenceSchema;
import org.everit.json.schema.Schema;
import org.everit.json.schema.event.ConditionalSchemaMatchEvent;
import org.everit.json.schema.event.SchemaReferencedEvent;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;

public class DerivedAnnotationVistorImpl implements DerivedAnnotationVistor {

	private final AnnotationsTranslator translator;
	private final Annotations annotations;

	/*
	 * Note: The validation library only provides 'default' values when configured
	 * with 'useDefaults=true'. However, when configured with 'useDefaults=true',
	 * the library will add each default value to the original input subject during
	 * validation. Therefore, we capture the original property key of the subject
	 * before we start validation.
	 */
	private final Set<String> keysPresentAtStart;

	/**
	 * Create a new stateful visitor to capture the derived annotations of an
	 * {@link Entity}.
	 * 
	 * @param translator  Used to translate from {@link JSONObject} to
	 *                    {@link Annotations} in a standard manner.
	 * @param schema      The validation schema that defines both conditional and
	 *                    unconditional properties of a subject.
	 * @param subjectJson The subject is a {@link JSONObject} representation of the
	 *                    existing existing annotations on an {@link Entity}
	 */
	public DerivedAnnotationVistorImpl(AnnotationsTranslator translator, Schema schema, JSONObject subjectJson) {
		this.translator = translator;
		this.annotations = new Annotations().setAnnotations(new LinkedHashMap<>());
		this.keysPresentAtStart = new HashSet<>(subjectJson.keySet());
		// The root schema unconditionally matches, so add it.
		addMatchingSchema(schema);
	}
	
	

	@Override
	public void schemaReferenced(SchemaReferencedEvent event) {
		System.out.println(event);
	}



	/**
	 * Triggered when the schema validation library detects an 'if' block that
	 * validates to 'valid'. The provided {@link ConditionalSchemaMatchEvent}
	 * provides the corresponding 'then' block sub-schema. When this event occurs
	 * any property of the 'then' sub-schema with 'default' or 'const' will be
	 * considered as a possible derived annotation.
	 */
	@Override
	public void thenSchemaMatch(ConditionalSchemaMatchEvent event) {
		Optional<Schema> op = event.getSchema().getThenSchema();
		if (op.isPresent()) {
			addMatchingSchema(op.get());
		}
	}

	/**
	 * Triggered when the schema validation library detects an 'if' block that
	 * validates to 'invalid'. The provided {@link ConditionalSchemaMatchEvent}
	 * provides the corresponding 'else' block sub-schema. When this event occurs
	 * any property of the 'else' sub-schema with 'default' or 'const' will be
	 * considered as a possible derived annotation.
	 */
	@Override
	public void elseSchemaMatch(ConditionalSchemaMatchEvent event) {
		Optional<Schema> op = event.getSchema().getElseSchema();
		if (op.isPresent()) {
			addMatchingSchema(op.get());
		}
	}

	/**
	 * Fetch the captured state of this visitor.
	 */
	public Optional<Annotations> getDerivedAnnotations() {
		if(annotations.getAnnotations().isEmpty()) {
			return Optional.empty();
		}else {
			return Optional.of(annotations);
		}
	}

	/**
	 * Add a schema that has been shown to match the subject.
	 * 
	 * @param schema
	 */
	void addMatchingSchema(Schema schema) {
		if (schema instanceof ObjectSchema) {
			addMatchingObjectSchema((ObjectSchema) schema);
		} else if (schema instanceof CombinedSchema) {
			addMatchingCombinedSchema((CombinedSchema) schema);
		}
	}

	/**
	 * Add a matching combined schema that contains one or more sub-schemas.
	 * 
	 * @param schema
	 */
	void addMatchingCombinedSchema(CombinedSchema schema) {
		schema.getSubschemas().forEach((subSchema) -> {
			addMatchingSchema(subSchema);
		});
	}

	/**
	 * Add a matching object schema.
	 * 
	 * @param objectSchema
	 */
	void addMatchingObjectSchema(ObjectSchema objectSchema) {
		objectSchema.getPropertySchemas().forEach((key, subSchema) -> {
			considerMatchingProperty(key, subSchema);
		});
	}

	/**
	 * Called when a schema property matches during the validation of a subject.
	 * Each match is considered a potential derived annotation, if the subject did
	 * not already declare a value for the given property key, and the property is
	 * either a 'const' or 'default'.
	 * </p>
	 * 
	 * 
	 * @param key
	 * @param schema
	 */
	void considerMatchingProperty(String key, Schema schema) {
		if (!this.keysPresentAtStart.contains(key)) {
			Optional<Object> optionalObjectValue = getConstOrDefaultValue(schema);
			if (optionalObjectValue.isPresent()) {
				JSONObject value = new JSONObject();
				value.put(key, optionalObjectValue.get());
				AnnotationsValue anValue = translator.getAnnotationValueFromJsonObject(key, value);
				annotations.getAnnotations().put(key, anValue);
			}
		}
	}

	/**
	 * Get either the const or default value from a property's schema.
	 * 
	 * @param schema The schema of a single property.
	 * @return {@link Optional#empty()} if this schema does not represent either a
	 *         'default' or 'const'.
	 */
	Optional<Object> getConstOrDefaultValue(Schema schema) {
		if (schema instanceof ConstSchema) {
			return Optional.of(((ConstSchema) schema).getPermittedValue());
		} else if (schema instanceof ReferenceSchema) {
			// recursive follow $ref to the referenced schema.
			return getConstOrDefaultValue(((ReferenceSchema)schema).getReferredSchema());
		}else {
			return Optional.ofNullable(schema.getDefaultValue());
		}
	}

}
