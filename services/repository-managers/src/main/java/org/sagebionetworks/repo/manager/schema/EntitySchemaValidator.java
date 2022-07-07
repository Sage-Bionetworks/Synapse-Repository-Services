package org.sagebionetworks.repo.manager.schema;

import java.util.Optional;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.dbo.schema.DerivedAnnotationDao;
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

	private final EntityManager entityManger;
	private final JsonSchemaManager jsonSchemaManager;
	private final JsonSchemaValidationManager jsonSchemaValidationManager;
	private final SchemaValidationResultDao schemaValidationResultDao;
	private final DerivedAnnotationDao derivedAnnotationDao;

	@Autowired
	public EntitySchemaValidator(EntityManager entityManger, JsonSchemaManager jsonSchemaManager,
			JsonSchemaValidationManager jsonSchemaValidationManager,
			SchemaValidationResultDao schemaValidationResultDao, DerivedAnnotationDao derivedAnnotationDao) {
		super();
		this.entityManger = entityManger;
		this.jsonSchemaManager = jsonSchemaManager;
		this.jsonSchemaValidationManager = jsonSchemaValidationManager;
		this.schemaValidationResultDao = schemaValidationResultDao;
		this.derivedAnnotationDao = derivedAnnotationDao;
	}

	@WriteTransaction
	@Override
	public void validateObject(String entityId) {
		ValidateArgument.required(entityId, "entityId");
		try {
			JsonSchemaObjectBinding binding = entityManger.getBoundSchema(entityId);
			JsonSubject entitySubject = entityManger.getEntityJsonSubject(entityId, false);
			JsonSchema validationSchema = jsonSchemaManager
					.getValidationSchema(binding.getJsonSchemaVersionInfo().get$id());
			ValidationResults results = jsonSchemaValidationManager.validate(validationSchema, entitySubject);
			schemaValidationResultDao.createOrUpdateResults(results);
			
			if(binding.getEnableDerivedAnnotations()) {
				Optional<Annotations> annoOption = jsonSchemaValidationManager.calculateDerivedAnnotations(validationSchema, entitySubject.toJson());
				if(annoOption.isPresent()) {
					derivedAnnotationDao.saveDerivedAnnotations(entityId, annoOption.get());
				}else {
					derivedAnnotationDao.clearDerivedAnnotations(entityId);
				}
			}else {
				derivedAnnotationDao.clearDerivedAnnotations(entityId);
			}

		} catch (NotFoundException e) {
			schemaValidationResultDao.clearResults(entityId, ObjectType.entity);
			derivedAnnotationDao.clearDerivedAnnotations(entityId);
		}
	}

}
