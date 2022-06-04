package org.sagebionetworks.repo.manager.schema;

import java.util.LinkedHashMap;

import org.everit.json.schema.ObjectSchema;
import org.everit.json.schema.Schema;
import org.everit.json.schema.event.CombinedSchemaMatchEvent;
import org.everit.json.schema.event.CombinedSchemaMismatchEvent;
import org.everit.json.schema.event.ConditionalSchemaMatchEvent;
import org.everit.json.schema.event.ConditionalSchemaMismatchEvent;
import org.everit.json.schema.event.SchemaReferencedEvent;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;

public class DerivedAnnotationVistorImpl implements DerivedAnnotationVistor {

	private final AnnotationsTranslator translator;
	private final JSONObject subject;
	private final Annotations annotations;

	public DerivedAnnotationVistorImpl(AnnotationsTranslator translator, Schema schema, JSONObject subjectJson) {
		this.translator = translator;
		this.subject = subjectJson;
		this.annotations = new Annotations().setAnnotations(new LinkedHashMap<>());
		addMatchingSchema(schema);
	}

	@Override
	public void combinedSchemaMismatch(CombinedSchemaMismatchEvent event) {
		System.out.println(event);
	}

	@Override
	public void schemaReferenced(SchemaReferencedEvent event) {
		System.out.println(event);
	}

	@Override
	public void ifSchemaMismatch(ConditionalSchemaMismatchEvent event) {
		System.out.println(event);
	}

	@Override
	public void thenSchemaMismatch(ConditionalSchemaMismatchEvent event) {
		System.out.println(event);
	}

	@Override
	public void elseSchemaMismatch(ConditionalSchemaMismatchEvent event) {
		System.out.println(event);
	}

	@Override
	public void combinedSchemaMatch(CombinedSchemaMatchEvent event) {
		System.out.println(event);
	}

	@Override
	public void ifSchemaMatch(ConditionalSchemaMatchEvent event) {
		System.out.println(event);
	}

	@Override
	public void thenSchemaMatch(ConditionalSchemaMatchEvent event) {
		System.out.println(event);
	}

	@Override
	public void elseSchemaMatch(ConditionalSchemaMatchEvent event) {
		System.out.println(event);
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
		}
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
		// This is only a derived candidate if the subject does not have this key.
		if(!this.subject.has(key)) {

			Object defaultValue = schema.getDefaultValue();
			if(defaultValue != null) {
				JSONObject value = new JSONObject(defaultValue);
				value.put(key, defaultValue);
				AnnotationsValue anValue = translator.getAnnotationValueFromJsonObject(key, value);
				annotations.getAnnotations().put(key, anValue);
			}
		}
	}

}
