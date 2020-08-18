package org.sagebionetworks.repo.manager.schema;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.dbo.schema.SchemaValidationResultDao;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.ObjectType;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EntitySchemaValidator implements ObjectSchemaValidator {

	private EntityManager entityManger;
	private JsonSchemaManager jsonSchemaManager;
	private JsonSchemaValidationManager jsonSchemaValidationManager;
	private SchemaValidationResultDao schemaValidationResultDao;

	@Autowired
	public EntitySchemaValidator(EntityManager entityManger, JsonSchemaManager jsonSchemaManager,
			JsonSchemaValidationManager jsonSchemaValidationManager,
			SchemaValidationResultDao schemaValidationResultDao) {
		super();
		this.entityManger = entityManger;
		this.jsonSchemaManager = jsonSchemaManager;
		this.jsonSchemaValidationManager = jsonSchemaValidationManager;
		this.schemaValidationResultDao = schemaValidationResultDao;
	}

	@WriteTransaction
	@Override
	public void validateObject(String entityId) {
		ValidateArgument.required(entityId, "entityId");
		try {
			JsonSchemaObjectBinding binding = entityManger.getBoundSchema(entityId);
			JsonSubject entitySubject = entityManger.getEntityJsonSubject(entityId);
			JsonSchema validationSchema = jsonSchemaManager
					.getValidationSchema(binding.getJsonSchemaVersionInfo().get$id());
			ValidationResults results = jsonSchemaValidationManager.validate(validationSchema, entitySubject);
			schemaValidationResultDao.createOrUpdateResults(results);
		} catch (NotFoundException e) {
			schemaValidationResultDao.clearResults(entityId, ObjectType.entity);
		}
	}

}
