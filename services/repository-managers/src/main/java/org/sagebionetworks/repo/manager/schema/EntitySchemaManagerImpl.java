package org.sagebionetworks.repo.manager.schema;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;

public class EntitySchemaManagerImpl implements EntitySchemaManager {
	

	EntityManager entityManger;
	JsonSchemaManager jsonSchemaManager;
	JsonSchemaValidationManager jsonSchemaValidationManager;

	@WriteTransaction
	@Override
	public void validateEntityAgainstBoundSchema(String entityId) {
		ValidateArgument.required(entityId, "entityId");
		try {
			JsonSchemaObjectBinding binding = entityManger.getBoundSchema(entityId);
			JsonSubject entitySubject = entityManger.getEntityJsonSubject(entityId);
			JsonSchema validationSchema = jsonSchemaManager.getValidationSchema(binding.getJsonSchemaVersionInfo().get$id());
			ValidationResults results = jsonSchemaValidationManager.validate(validationSchema, entitySubject);
		} catch (NotFoundException e) {
			
		}

	}

}
