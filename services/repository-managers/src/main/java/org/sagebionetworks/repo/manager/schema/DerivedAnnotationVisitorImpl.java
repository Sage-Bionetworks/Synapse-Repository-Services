package org.sagebionetworks.repo.manager.schema;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.everit.json.schema.ArraySchema;
import org.everit.json.schema.CombinedSchema;
import org.everit.json.schema.ConstSchema;
import org.everit.json.schema.ObjectSchema;
import org.everit.json.schema.ReferenceSchema;
import org.everit.json.schema.Schema;
import org.everit.json.schema.event.ConditionalSchemaMatchEvent;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.util.ValidateArgument;

public class DerivedAnnotationVisitorImpl implements DerivedAnnotationVisitor {

	private final AnnotationsTranslator translator;
	private final Map<String, AnnotationsValue> annotations;

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
	public DerivedAnnotationVisitorImpl(AnnotationsTranslator translator, Schema schema, JSONObject subjectJson) {
		this.translator = translator;
		this.annotations = new LinkedHashMap<>();
		this.keysPresentAtStart = new HashSet<>(subjectJson.keySet());
		// The root schema unconditionally matches, so add it.
		addMatchingSchema(schema);
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
		if (annotations.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(new Annotations().setAnnotations(annotations));
		}
	}

	/**
	 * Add a schema that has been shown to match the subject.
	 * 
	 * @param schema
	 */
	void addMatchingSchema(Schema schema) {
		// We only consider ObjectSchemas that have properties with key-value pairs.
		streamOverSubschemas(schema).filter(s -> s instanceof ObjectSchema).map(s -> (ObjectSchema) s)
				.forEach(s -> addMatchingObjectSchema(s));
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
			streamOverSubschemas(schema).forEach(sub -> {
				addOrUpdateConstAndDefaultAnnotation(key, sub);
			});
		}
	}

	/**
	 * Attempt to add or update an annotation with the given key if the provided
	 * schema contains a 'const' or default value.
	 * @param key
	 * @param schema
	 */
	void addOrUpdateConstAndDefaultAnnotation(String key, Schema schema) {
		getConstOrDefaultValue(schema).ifPresent(object -> {
			JSONObject value = new JSONObject();
			value.put(key, object);
			AnnotationsValue newValue = translator.getAnnotationValueFromJsonObject(key, value);
			// the existing value might be null
			AnnotationsValue existingValue = annotations.get(key);
			annotations.put(key, mergeArrayAnnotations(existingValue, newValue));
		});
	}

	/**
	 * Given two annotation values, attempt to merge the values if they are of the
	 * same type.
	 * 
	 * @param existing
	 * @param newValue
	 * @return The merged AnnotationValue.
	 */
	static AnnotationsValue mergeArrayAnnotations(AnnotationsValue existing, AnnotationsValue newValue) {
		ValidateArgument.required(newValue, "newValue");
		ValidateArgument.required(newValue.getType(), "newValue.type");
		ValidateArgument.required(newValue.getValue(), "newValue.value");
		if (existing == null) {
			return newValue;
		}
		if (!newValue.getType().equals(existing.getType())) {
			// ignore the new since the types do not match
			return existing;
		}
		// used linked to maintain insert order.
		Set<String> mergedValues = new LinkedHashSet<>(existing.getValue());
		mergedValues.addAll(newValue.getValue());
		return new AnnotationsValue().setType(newValue.getType())
				.setValue(mergedValues.stream().collect(Collectors.toList()));
	}

	/**
	 * Get a stream over the given schema. If the schema represents a leaf, then the
	 * resulting stream will only contain that leaf. If the schema has subschemas,
	 * the resulting stream will include each subschema recursively. Note:
	 * Properties of an ObjectSchema are not considered subschemas.
	 * 
	 * @param schema
	 * @return
	 */
	static Stream<Schema> streamOverSubschemas(Schema schema) {
		List<Schema> list = new LinkedList<>();
		streamOverSubSchemasRecursive(schema, list);
		return list.stream();
	}

	/**
	 * Recursively build up the list of leaf schemas.
	 * 
	 * @param schema
	 * @param list
	 */
	private static void streamOverSubSchemasRecursive(Schema schema, List<Schema> list) {
		if (schema == null) {
			return;
		}
		if (schema instanceof CombinedSchema) {
			((CombinedSchema) schema).getSubschemas().forEach(sub -> {
				streamOverSubSchemasRecursive(sub, list);
			});
		} else if (schema instanceof ArraySchema) {
			streamOverSubSchemasRecursive(((ArraySchema) schema).getContainedItemSchema(), list);
		} else if (schema instanceof ReferenceSchema) {
			streamOverSubSchemasRecursive(((ReferenceSchema) schema).getReferredSchema(), list);
		} else {
			list.add(schema);
		}
	}

	/**
	 * Get either the const or default value from a property's schema.
	 * 
	 * @param schema The schema of a single property.
	 * @return {@link Optional#empty()} if this schema does not represent either a
	 *         'default' or 'const'.
	 */
	static Optional<Object> getConstOrDefaultValue(Schema schema) {
		if (schema == null) {
			return Optional.empty();
		}
		if (schema instanceof ConstSchema) {
			return Optional.of(((ConstSchema) schema).getPermittedValue());
		} else {
			return Optional.ofNullable(schema.getDefaultValue());
		}
	}

}
