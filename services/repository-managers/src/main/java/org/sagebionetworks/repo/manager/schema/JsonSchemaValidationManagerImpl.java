package org.sagebionetworks.repo.manager.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.Validator;
import org.everit.json.schema.loader.SchemaLoader;
import org.everit.json.schema.loader.internal.DefaultSchemaClient;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ValidationException;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JsonSchemaValidationManagerImpl implements JsonSchemaValidationManager {

	public static final String DRAFT_07 = "http://json-schema.org/draft-07/schema";

	private final ValidationListenerProvider listenerProvider;

	@Autowired
	public JsonSchemaValidationManagerImpl(ValidationListenerProvider listenerProvider) {
		this.listenerProvider = listenerProvider;
	}

	@Override
	public ValidationResults validate(JsonSchema jsonSchema, JsonSubject subject) {
		try {
			return doValidate(jsonSchema, Collections.singletonList(subject)).get(0);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public List<ValidationResults> validateBatch(JsonSchema schema, List<JsonSubject> subjects) {
		try {
			return doValidate(schema, subjects);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

	List<ValidationResults> doValidate(JsonSchema jsonSchema, List<JsonSubject> subjects) throws JSONObjectAdapterException {
		ValidateArgument.requiredNotEmpty(subjects, "subject");
		boolean useDefaults= false;
		Schema schemaValidator = loadSchema(jsonSchema, useDefaults);
		
		List<ValidationResults> results = new ArrayList<>(subjects.size());
		
		for (JsonSubject subject : subjects) {
			ValidateArgument.required(subject, "subject");
			
			ValidationResults result = new ValidationResults();
			
			result.setObjectId(subject.getObjectId());
			result.setObjectType(subject.getObjectType());
			result.setObjectEtag(subject.getObjectEtag());
			result.setSchema$id(jsonSchema.get$id());
			result.setValidatedOn(new Date());
			
			try {
				schemaValidator.validate(subject.toJson());
				result.setIsValid(true);
			} catch (org.everit.json.schema.ValidationException e) {
				result.setIsValid(false);
				result.setValidationErrorMessage(e.getErrorMessage());
				result.setAllValidationMessages(e.getAllMessages());
				result.setValidationException(new ValidationException(new JSONObjectAdapterImpl(e.toJSON())));
			}
			
			results.add(result);
		}
		
		return results;
	}

	@Override
	public Optional<Annotations> calculateDerivedAnnotations(JsonSchema jsonSchema, JSONObject subject) {
		try {
			return doCalculateDerivedAnnotations(jsonSchema, subject);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		} catch (org.everit.json.schema.ValidationException e) {
			// If the subject is not valid against the schema, then there are no derived
			// annotations.
			return Optional.empty();
		}
	}

	Optional<Annotations> doCalculateDerivedAnnotations(JsonSchema jsonSchema, JSONObject subject)
			throws JSONObjectAdapterException {
		boolean useDefaults= true;
		Schema schemaValidator = loadSchema(jsonSchema, useDefaults);
		DerivedAnnotationVisitor listener = listenerProvider.createNewVisitor(schemaValidator, subject);
		Validator validator = Validator.builder().withListener(listener).build();
		validator.performValidation(schemaValidator, subject);
		return listener.getDerivedAnnotations();
	}

	/**
	 * Load the provide {@link JsonSchema} into the library {@link Schema}.
	 * 
	 * @param jsonSchema
	 * @param useDefaults When set to true, default values will be added to the
	 *                    provide subject and default values are made available to
	 *                    visitors.
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	Schema loadSchema(JsonSchema jsonSchema, boolean useDefaults) throws JSONObjectAdapterException {
		ValidateArgument.required(jsonSchema, "jsonSchema");
		if (StringUtils.isBlank(jsonSchema.get$schema())) {
			/**
			 * The validation library silently ignores all JSON schema features added after
			 * draft-04, when a $schema is not provided. This causes unexpected behavior for
			 * users that depend on newer features but forget to include a $schema.
			 * Therefore, we default to draft-07 for this case.
			 */
			jsonSchema.set$schema(DRAFT_07);
		}
		String validationSchemaJson = EntityFactory.createJSONStringForEntity(jsonSchema);
		SchemaLoader loader = SchemaLoader.builder().schemaJson(new JSONObject(validationSchemaJson))
				.schemaClient(new DefaultSchemaClient()).useDefaults(useDefaults).build();
		return loader.load().build();
	}

}
