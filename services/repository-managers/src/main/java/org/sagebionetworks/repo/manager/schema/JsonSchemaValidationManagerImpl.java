package org.sagebionetworks.repo.manager.schema;

import java.util.Date;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ValidationException;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class JsonSchemaValidationManagerImpl implements JsonSchemaValidationManager {


	@Override
	public ValidationResults validate(JsonSchema jsonSchema, JsonSubject subject) {
		try {
			return doValidate(jsonSchema, subject);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

	ValidationResults doValidate(JsonSchema jsonSchema, JsonSubject subject) throws JSONObjectAdapterException {
		ValidateArgument.required(jsonSchema, "jsonSchema");
		ValidateArgument.required(subject, "subject");
		ValidationResults result = new ValidationResults();
		result.setObjectId(subject.getObjectId());
		result.setObjectType(subject.getObjectType());
		result.setObjectEtag(subject.getObjectEtag());
		result.setValidatedOn(new Date());
		String validationSchemaJson = EntityFactory.createJSONStringForEntity(jsonSchema);
		Schema schemaValidator = SchemaLoader.load(new JSONObject(validationSchemaJson));
		try {
			schemaValidator.validate(subject.toJson());
			result.setIsValid(true);
		} catch (org.everit.json.schema.ValidationException e) {
			result.setIsValid(false);
			result.setValidationErrorMessage(e.getErrorMessage());
			result.setAllValidationMessages(e.getAllMessages());
			result.setValidationException(new ValidationException(new JSONObjectAdapterImpl(e.toJSON())));
		}
		return result;
	}

}
