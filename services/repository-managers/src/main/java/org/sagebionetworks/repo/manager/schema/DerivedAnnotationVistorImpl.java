package org.sagebionetworks.repo.manager.schema;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Set;

import org.everit.json.schema.CombinedSchema;
import org.everit.json.schema.ConstSchema;
import org.everit.json.schema.ObjectSchema;
import org.everit.json.schema.Schema;
import org.everit.json.schema.event.ConditionalSchemaMatchEvent;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;

public class DerivedAnnotationVistorImpl implements DerivedAnnotationVistor {

	private final AnnotationsTranslator translator;
	private final Annotations annotations;
	private final Set<String> keysPresentAtStart;

	public DerivedAnnotationVistorImpl(AnnotationsTranslator translator, Schema schema, JSONObject subjectJson) {
		this.translator = translator;
		this.annotations = new Annotations().setAnnotations(new LinkedHashMap<>());
		this.keysPresentAtStart = new HashSet<>(subjectJson.keySet());
		addMatchingSchema(schema);
	}

	@Override
	public void thenSchemaMatch(ConditionalSchemaMatchEvent event) {
		Optional<Schema> op = event.getSchema().getThenSchema();
		if(op.isPresent()) {
			addMatchingSchema(op.get());
		}
	}

	@Override
	public void elseSchemaMatch(ConditionalSchemaMatchEvent event) {
		Optional<Schema> op = event.getSchema().getElseSchema();
		if(op.isPresent()) {
			addMatchingSchema(op.get());
		}
	}

	public Annotations getDerivedAnnotations() {
		return annotations;
	}

	/**
	 * Add a schema that has been shown to match the subject.
	 * 
	 * @param schema
	 */
	void addMatchingSchema(Schema schema) {
		if (schema instanceof ObjectSchema) {
			addMatchingObjectSchema((ObjectSchema) schema);
		}else if(schema instanceof CombinedSchema) {
			addMatchingCombinedSchema((CombinedSchema) schema);
		}
	}
	
	/**
	 * Add a matching combined schema that contains one or more sub-schemas.
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
			addMatchingProperty(key, subSchema);
		});
	}
	
	void addMatchingProperty(String key, Schema schema) {
		// A derived annotation must not override an annotation that was already present.
		if(!this.keysPresentAtStart.contains(key)) {
			Object objectValue = getConstOrDefaultValue(schema);
			if(objectValue != null) {
				JSONObject value = new JSONObject(objectValue);
				value.put(key, objectValue);
				AnnotationsValue anValue = translator.getAnnotationValueFromJsonObject(key, value);
				annotations.getAnnotations().put(key, anValue);
			}
		}
	}
	
	/**
	 * Get either the const or default value from a schema.
	 * @param schema
	 * @return
	 */
	Object getConstOrDefaultValue(Schema schema) {
		if(schema instanceof ConstSchema) {
			return ((ConstSchema)schema).getPermittedValue();
		}else {
			return schema.getDefaultValue();
		}
	}

}
