package org.sagebionetworks.repo.manager.schema;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.dataaccess.AccessRequirementManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
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
	private final AccessRequirementManager accessRequirementManager;

	@Autowired
	public EntitySchemaValidator(EntityManager entityManger, JsonSchemaManager jsonSchemaManager,
			JsonSchemaValidationManager jsonSchemaValidationManager,
			SchemaValidationResultDao schemaValidationResultDao, DerivedAnnotationDao derivedAnnotationDao,
			TransactionalMessenger messenger, AccessRequirementManager accessRequirmentManager) {
		super();
		this.entityManger = entityManger;
		this.jsonSchemaManager = jsonSchemaManager;
		this.jsonSchemaValidationManager = jsonSchemaValidationManager;
		this.schemaValidationResultDao = schemaValidationResultDao;
		this.derivedAnnotationDao = derivedAnnotationDao;
		this.messenger = messenger;
		this.accessRequirementManager = accessRequirmentManager;
	}

	@WriteTransaction
	@Override
	public void validateObject(String entityId) {
		ValidateArgument.required(entityId, "entityId");

		boolean sendEntityUpdate = false;
		final RestrictableObjectDescriptor objectDescriptor = new RestrictableObjectDescriptor().setId(entityId)
				.setType(RestrictableObjectType.ENTITY);
		try {
			JsonSchemaObjectBinding binding = entityManger.getBoundSchema(entityId);
			JsonSubject entitySubject = entityManger.getEntityJsonSubject(entityId, false);
			JsonSchema validationSchema = jsonSchemaManager
					.getValidationSchema(binding.getJsonSchemaVersionInfo().get$id());
			ValidationResults results = jsonSchemaValidationManager.validate(validationSchema, entitySubject);
			schemaValidationResultDao.createOrUpdateResults(results);

			Optional<Annotations> annoOption = Optional.empty();
			if (binding.getEnableDerivedAnnotations()) {
				annoOption = jsonSchemaValidationManager.calculateDerivedAnnotations(validationSchema,
						entitySubject.toJson());
				if (annoOption.isPresent()) {
					derivedAnnotationDao.saveDerivedAnnotations(entityId, annoOption.get());
					sendEntityUpdate = true;
				} else {
					sendEntityUpdate = derivedAnnotationDao.clearDerivedAnnotations(entityId);
				}
			} else {
				sendEntityUpdate = derivedAnnotationDao.clearDerivedAnnotations(entityId);
			}

			Set<Long> accessRequirmentIdsToBind = extractAccessRequirmentIds(annoOption.orElse(new Annotations()));
			accessRequirementManager.setDynamicallyBoundAccessRequirementsForSubject(objectDescriptor,
					accessRequirmentIdsToBind);
		} catch (NotFoundException e) {
			schemaValidationResultDao.clearResults(entityId, ObjectType.entity);
			sendEntityUpdate = derivedAnnotationDao.clearDerivedAnnotations(entityId);
			accessRequirementManager.setDynamicallyBoundAccessRequirementsForSubject(objectDescriptor,
					Collections.emptySet());
		}

		if (sendEntityUpdate) {
			// When the derived annotations are computed we want to trigger the replication
			// so that the index is updated, since this is not an actual update of the
			// entity
			// we simply publish a non-migratable change that is processed as a normal
			// change message
			messenger.publishMessageAfterCommit(new LocalStackChangeMesssage().setObjectId(entityId)
					.setObjectType(org.sagebionetworks.repo.model.ObjectType.ENTITY).setChangeType(ChangeType.UPDATE)
					.setUserId(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()));
		}
	}

	/**
	 * Attempt to extract the set of access requirement ID from the provide derived
	 * annotations.
	 * 
	 * @param annotations
	 * @return
	 */
	static Set<Long> extractAccessRequirmentIds(Annotations derivedAnnotations) {
		if (derivedAnnotations == null || derivedAnnotations.getAnnotations() == null) {
			return Collections.emptySet();
		}
		Optional<AnnotationsValue> optionalValue = derivedAnnotations.getAnnotations().entrySet().stream()
				.filter(e -> AnnotationsV2Utils.ACCESS_REQUIREMENT_IDS.equals(e.getKey())).findFirst()
				.map(e -> e.getValue());
		if (!optionalValue.isPresent()) {
			return Collections.emptySet();
		}
		AnnotationsValue value = optionalValue.get();
		if (!AnnotationsValueType.LONG.equals(value.getType())) {
			throw new IllegalArgumentException(String.format(
					"The derived annotation with the key: '%s' does not have an expected type of: '%s', actual type is: '%s'",
					AnnotationsV2Utils.ACCESS_REQUIREMENT_IDS, AnnotationsValueType.LONG, value.getType()));
		}
		if (value.getValue() == null) {
			return Collections.emptySet();
		}
		return value.getValue().stream().map(Long::parseLong).collect(Collectors.toSet());
	}

}
