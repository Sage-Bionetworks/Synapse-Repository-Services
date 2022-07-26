package org.sagebionetworks.repo.manager.schema;

import java.util.Optional;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.dbo.schema.DerivedAnnotationDao;
import org.sagebionetworks.repo.model.dbo.schema.SchemaValidationResultDao;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.LocalStackChangeMesssage;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
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
	private final TransactionalMessenger messenger;
	
	@Autowired
	public EntitySchemaValidator(EntityManager entityManger, JsonSchemaManager jsonSchemaManager,
			JsonSchemaValidationManager jsonSchemaValidationManager,
			SchemaValidationResultDao schemaValidationResultDao, DerivedAnnotationDao derivedAnnotationDao,
			TransactionalMessenger messenger) {
		super();
		this.entityManger = entityManger;
		this.jsonSchemaManager = jsonSchemaManager;
		this.jsonSchemaValidationManager = jsonSchemaValidationManager;
		this.schemaValidationResultDao = schemaValidationResultDao;
		this.derivedAnnotationDao = derivedAnnotationDao;
		this.messenger = messenger;
	}

	@WriteTransaction
	@Override
	public void validateObject(String entityId) {
		ValidateArgument.required(entityId, "entityId");
		
		boolean sendEntityUpdate = false;
		
		try {
			JsonSchemaObjectBinding binding = entityManger.getBoundSchema(entityId);
			JsonSubject entitySubject = entityManger.getEntityJsonSubject(entityId, false);
			JsonSchema validationSchema = jsonSchemaManager
					.getValidationSchema(binding.getJsonSchemaVersionInfo().get$id());
			ValidationResults results = jsonSchemaValidationManager.validate(validationSchema, entitySubject);
			schemaValidationResultDao.createOrUpdateResults(results);
		
			if (binding.getEnableDerivedAnnotations()) {
				Optional<Annotations> annoOption = jsonSchemaValidationManager.calculateDerivedAnnotations(validationSchema, entitySubject.toJson());
				if(annoOption.isPresent()) {
					derivedAnnotationDao.saveDerivedAnnotations(entityId, annoOption.get());
					sendEntityUpdate = true;
				} else {
					sendEntityUpdate = derivedAnnotationDao.clearDerivedAnnotations(entityId);
				}
			} else {
				sendEntityUpdate = derivedAnnotationDao.clearDerivedAnnotations(entityId);
			}

		} catch (NotFoundException e) {
			schemaValidationResultDao.clearResults(entityId, ObjectType.entity);
			sendEntityUpdate = derivedAnnotationDao.clearDerivedAnnotations(entityId);
		}
		
		if (sendEntityUpdate) {
			// When the derived annotations are computed we want to trigger the replication so that the index is updated, since this is not an actual update of the entity
			// we simply publish a non-migratable change that is processed as a normal change message
			messenger.publishMessageAfterCommit(
				new LocalStackChangeMesssage()
					.setObjectId(entityId)
					.setObjectType(org.sagebionetworks.repo.model.ObjectType.ENTITY)
					.setChangeType(ChangeType.UPDATE)
					.setUserId(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId())
			);
		}
	}

}
