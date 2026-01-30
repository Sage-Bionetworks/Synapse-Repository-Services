package org.sagebionetworks.repo.manager.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.everit.json.schema.Schema;
import org.everit.json.schema.Validator;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ValidationException;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JsonSchemaValidationManagerImpl implements JsonSchemaValidationManager {

	private final ValidationListenerProvider listenerProvider;
	private final JsonSchemaValidatorFactory validatorFactory;

	@Autowired
	public JsonSchemaValidationManagerImpl(ValidationListenerProvider listenerProvider, JsonSchemaValidatorFactory validatorFactory) {
		this.listenerProvider = listenerProvider;
		this.validatorFactory = validatorFactory;
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
		Schema schemaValidator = validatorFactory.buildValidator(jsonSchema, useDefaults);
		
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
		Schema schemaValidator = validatorFactory.buildValidator(jsonSchema, useDefaults);
		DerivedAnnotationVisitor listener = listenerProvider.createNewVisitor(schemaValidator, subject);
		Validator validator = Validator.builder().withListener(listener).build();
		validator.performValidation(schemaValidator, subject);
		return listener.getDerivedAnnotations();
	}

}
