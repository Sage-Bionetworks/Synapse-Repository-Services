package org.sagebionetworks.repo.manager.schema;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.dataaccess.AccessRequirementManager;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
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
import org.sagebionetworks.repo.model.schema.SubSchemaIterable;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EntitySchemaValidator implements ObjectSchemaValidator {
	
	static Log log = LogFactory.getLog(EntitySchemaValidator.class);	

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
		Optional<JsonSchemaObjectBinding> binding = entityManger.findBoundSchema(entityId);
		if(binding.isPresent()) {
			sendEntityUpdate = validateAgainstBoundSchema(objectDescriptor, binding.get());
		}else {
			sendEntityUpdate = clearAllBoundSchemaRelatedData(objectDescriptor);
		}
		
		if (sendEntityUpdate) {
			/*
			 * When the derived annotations are computed we want to trigger the replication
			 * so that the index is updated, since this is not an actual update of the
			 * entity we simply publish a non-migratable change that is processed as a
			 * normal change message
			 */
			messenger.publishMessageAfterCommit(new LocalStackChangeMesssage().setObjectId(entityId)
					.setObjectType(org.sagebionetworks.repo.model.ObjectType.ENTITY).setChangeType(ChangeType.UPDATE)
					.setUserId(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()));
		}
	}

	/**
	 * Clear all data related to a bound schema for this object.
	 * @param entityId
	 * @param objectDescriptor
	 * @return
	 */
	boolean clearAllBoundSchemaRelatedData(RestrictableObjectDescriptor objectDescriptor) {
		schemaValidationResultDao.clearResults(objectDescriptor.getId(), ObjectType.entity);
		accessRequirementManager.setDynamicallyBoundAccessRequirementsForSubject(objectDescriptor,
				Collections.emptySet());
		return derivedAnnotationDao.clearDerivedAnnotations(objectDescriptor.getId());
	}

	/**
	 * Validate the object against the provide bound schema.
	 * @param entityId
	 * @param objectDescriptor
	 * @param binding
	 * @return true if the derived annotations changed.
	 */
	boolean validateAgainstBoundSchema(RestrictableObjectDescriptor objectDescriptor,
			JsonSchemaObjectBinding binding) {
		JsonSubject entitySubject = entityManger.getEntityJsonSubject(objectDescriptor.getId(), false);
		JsonSchema validationSchema = jsonSchemaManager
				.getValidationSchema(binding.getJsonSchemaVersionInfo().get$id());
		ValidationResults results = jsonSchemaValidationManager.validate(validationSchema, entitySubject);
		schemaValidationResultDao.createOrUpdateResults(results);
		Set<Long> accessRequirmentIdsToBind = Collections.emptySet();
		if(!results.getIsValid()) {
			if(containsAccessRequirementIds(validationSchema) && binding.getEnableDerivedAnnotations()) {
				/*
				 * This entity is not valid against according to its bound schema. Also, this
				 * schema controls which access requirements would be bound to the schema. Since
				 * it is invalid, it is not possible to correctly identify which ARs should be
				 * bound. Therefore, we bind this entity to a lock AR that will prevent all user
				 * download. The lock will automatically be removed when the entity is valid
				 * against the schema.
				 */
				accessRequirmentIdsToBind = Collections.singleton(AccessRequirementDAO.INVALID_ANNOTATIONS_LOCK_ID);
			}
			return setDerivedAnnotationsAndBindAccessRequirements(objectDescriptor, null, accessRequirmentIdsToBind);
		}
		
		if(!binding.getEnableDerivedAnnotations()) {
			return setDerivedAnnotationsAndBindAccessRequirements(objectDescriptor, null, Collections.emptySet());
		}

		Optional<Annotations> annoOption = jsonSchemaValidationManager.calculateDerivedAnnotations(validationSchema,
				entitySubject.toJson());
		accessRequirmentIdsToBind = extractAccessRequirmentIds(annoOption.orElse(new Annotations()));
		return setDerivedAnnotationsAndBindAccessRequirements(objectDescriptor, annoOption.orElseGet(()->null), accessRequirmentIdsToBind);
	}
	
	/**
	 * 
	 * @param objectDescriptor
	 * @param annotations
	 * @param accessRequirmentIdsToBind
	 * @return
	 */
	boolean setDerivedAnnotationsAndBindAccessRequirements(RestrictableObjectDescriptor objectDescriptor,
			Annotations annotations,
			Set<Long> accessRequirmentIdsToBind) {
		accessRequirementManager.setDynamicallyBoundAccessRequirementsForSubject(objectDescriptor, accessRequirmentIdsToBind);
		if(annotations != null) {
			if (annotations.equals(derivedAnnotationDao.getDerivedAnnotations(objectDescriptor.getId()).orElse(null))) {
				return false;
			}

			derivedAnnotationDao.saveDerivedAnnotations(objectDescriptor.getId(), annotations);

			return true;
		}else {
			return derivedAnnotationDao.clearDerivedAnnotations(objectDescriptor.getId()); 
		}
	}
	
	
	/**
	 * Does the given schema (or sub-schemas) contain the '_accessRequirementsIds' property key?
	 * @param schema
	 * @return
	 */
	static boolean containsAccessRequirementIds(JsonSchema schema) {
		return StreamSupport.stream(SubSchemaIterable.depthFirstIterable(schema).spliterator(), false)
				.filter((s) -> s.getProperties() != null).map((p) -> p.getProperties().keySet())
				.filter((s) -> s.contains(AnnotationsV2Utils.ACCESS_REQUIREMENT_IDS)).findFirst().isPresent();
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
