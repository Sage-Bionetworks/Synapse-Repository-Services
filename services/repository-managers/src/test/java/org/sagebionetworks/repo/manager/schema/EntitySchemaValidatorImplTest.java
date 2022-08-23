package org.sagebionetworks.repo.manager.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.dataaccess.AccessRequirementManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.dbo.schema.DerivedAnnotationDao;
import org.sagebionetworks.repo.model.dbo.schema.SchemaValidationResultDao;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.LocalStackChangeMesssage;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.schema.ObjectType;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class EntitySchemaValidatorImplTest {

	@Mock
	private EntityManager mockEntityManger;
	@Mock
	private JsonSchemaManager mockJsonSchemaManager;
	@Mock
	private JsonSchemaValidationManager mockJsonSchemaValidationManager;
	@Mock
	private SchemaValidationResultDao mockSchemaValidationResultDao;
	@Mock
	private DerivedAnnotationDao mockDerivedAnnotationDao;
	@Mock
	private TransactionalMessenger mockMessenger;
	@Mock
	private AccessRequirementManager mockAccessRequirementManager;

	@InjectMocks
	private EntitySchemaValidator manager;

	private String entityId;
	private String schema$id;
	private JsonSchemaObjectBinding binding;
	@Mock
	private JsonSubject mockEntitySubject;
	@Mock
	private JsonSchema mockJsonSchema;
	@Mock
	private ValidationResults mockValidationResults;

	@BeforeEach
	public void before() {
		entityId = "syn123";
		schema$id = "my.org-foo.bar-1.0.0";
		binding = new JsonSchemaObjectBinding();
		JsonSchemaVersionInfo versionInfo = new JsonSchemaVersionInfo();
		versionInfo.set$id(schema$id);
		binding.setJsonSchemaVersionInfo(versionInfo);
		binding.setEnableDerivedAnnotations(true);
	}

	@Test
	public void testValidateObject() {
		when(mockEntityManger.getBoundSchema(entityId)).thenReturn(binding);
		when(mockEntityManger.getEntityJsonSubject(entityId, false)).thenReturn(mockEntitySubject);
		when(mockJsonSchemaManager.getValidationSchema(schema$id)).thenReturn(mockJsonSchema);
		when(mockJsonSchemaValidationManager.validate(mockJsonSchema, mockEntitySubject))
				.thenReturn(mockValidationResults);
		JSONObject subject = new JSONObject();
		when(mockEntitySubject.toJson()).thenReturn(subject);
		Annotations derivedAnnotations = new Annotations().setId(entityId);
		when(mockJsonSchemaValidationManager.calculateDerivedAnnotations(any(), any()))
				.thenReturn(Optional.of(derivedAnnotations));
		// call under test
		manager.validateObject(entityId);
		verify(mockSchemaValidationResultDao).createOrUpdateResults(mockValidationResults);
		verify(mockSchemaValidationResultDao, never()).clearResults(any(), any());
		verify(mockEntityManger).getBoundSchema(entityId);
		verify(mockEntityManger).getEntityJsonSubject(entityId, false);
		verify(mockJsonSchemaManager).getValidationSchema(schema$id);
		verify(mockJsonSchemaValidationManager).validate(mockJsonSchema, mockEntitySubject);
		verify(mockJsonSchemaValidationManager).calculateDerivedAnnotations(mockJsonSchema, subject);
		verify(mockDerivedAnnotationDao).saveDerivedAnnotations(entityId, derivedAnnotations);
		verify(mockDerivedAnnotationDao, never()).clearDerivedAnnotations(any());
		verify(mockAccessRequirementManager).setDynamicallyBoundAccessRequirementsForSubject(
				new RestrictableObjectDescriptor().setId(entityId).setType(RestrictableObjectType.ENTITY),
				Collections.emptySet());
		verify(mockMessenger).publishMessageAfterCommit(new LocalStackChangeMesssage().setObjectId(entityId)
				.setObjectType(org.sagebionetworks.repo.model.ObjectType.ENTITY).setChangeType(ChangeType.UPDATE)
				.setUserId(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()));
	}

	@Test
	public void testValidateObjectWithAccessRequirmentIds() {
		when(mockEntityManger.getBoundSchema(entityId)).thenReturn(binding);
		when(mockEntityManger.getEntityJsonSubject(entityId, false)).thenReturn(mockEntitySubject);
		when(mockJsonSchemaManager.getValidationSchema(schema$id)).thenReturn(mockJsonSchema);
		when(mockJsonSchemaValidationManager.validate(mockJsonSchema, mockEntitySubject))
				.thenReturn(mockValidationResults);
		JSONObject subject = new JSONObject();
		when(mockEntitySubject.toJson()).thenReturn(subject);
		Annotations derivedAnnotations = new Annotations().setId(entityId);
		// add the special derived annotation to bind access requirements to this entity.
		AnnotationsV2TestUtils.putAnnotations(derivedAnnotations, AnnotationsV2Utils.ACCESS_REQUIREMENT_IDS,
				List.of("11", "22", "33"), AnnotationsValueType.LONG);

		when(mockJsonSchemaValidationManager.calculateDerivedAnnotations(any(), any()))
				.thenReturn(Optional.of(derivedAnnotations));
		// call under test
		manager.validateObject(entityId);
		verify(mockSchemaValidationResultDao).createOrUpdateResults(mockValidationResults);
		verify(mockSchemaValidationResultDao, never()).clearResults(any(), any());
		verify(mockEntityManger).getBoundSchema(entityId);
		verify(mockEntityManger).getEntityJsonSubject(entityId, false);
		verify(mockJsonSchemaManager).getValidationSchema(schema$id);
		verify(mockJsonSchemaValidationManager).validate(mockJsonSchema, mockEntitySubject);
		verify(mockJsonSchemaValidationManager).calculateDerivedAnnotations(mockJsonSchema, subject);
		verify(mockDerivedAnnotationDao).saveDerivedAnnotations(entityId, derivedAnnotations);
		verify(mockDerivedAnnotationDao, never()).clearDerivedAnnotations(any());
		verify(mockAccessRequirementManager).setDynamicallyBoundAccessRequirementsForSubject(
				new RestrictableObjectDescriptor().setId(entityId).setType(RestrictableObjectType.ENTITY),
				Set.of(11L, 22L, 33L));
		verify(mockMessenger).publishMessageAfterCommit(new LocalStackChangeMesssage().setObjectId(entityId)
				.setObjectType(org.sagebionetworks.repo.model.ObjectType.ENTITY).setChangeType(ChangeType.UPDATE)
				.setUserId(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()));
	}

	@Test
	public void testValidateObjectWithNoAnnotations() {
		when(mockEntityManger.getBoundSchema(entityId)).thenReturn(binding);
		when(mockEntityManger.getEntityJsonSubject(entityId, false)).thenReturn(mockEntitySubject);
		when(mockJsonSchemaManager.getValidationSchema(schema$id)).thenReturn(mockJsonSchema);
		when(mockJsonSchemaValidationManager.validate(mockJsonSchema, mockEntitySubject))
				.thenReturn(mockValidationResults);
		JSONObject subject = new JSONObject();
		when(mockEntitySubject.toJson()).thenReturn(subject);
		when(mockJsonSchemaValidationManager.calculateDerivedAnnotations(any(), any())).thenReturn(Optional.empty());
		// call under test
		manager.validateObject(entityId);
		verify(mockSchemaValidationResultDao).createOrUpdateResults(mockValidationResults);
		verify(mockSchemaValidationResultDao, never()).clearResults(any(), any());
		verify(mockEntityManger).getBoundSchema(entityId);
		verify(mockEntityManger).getEntityJsonSubject(entityId, false);
		verify(mockJsonSchemaManager).getValidationSchema(schema$id);
		verify(mockJsonSchemaValidationManager).validate(mockJsonSchema, mockEntitySubject);
		verify(mockJsonSchemaValidationManager).calculateDerivedAnnotations(mockJsonSchema, subject);
		verify(mockDerivedAnnotationDao, never()).saveDerivedAnnotations(any(), any());
		verify(mockDerivedAnnotationDao).clearDerivedAnnotations(any());
		verify(mockAccessRequirementManager).setDynamicallyBoundAccessRequirementsForSubject(
				new RestrictableObjectDescriptor().setId(entityId).setType(RestrictableObjectType.ENTITY),
				Collections.emptySet());
		verifyZeroInteractions(mockMessenger);
	}

	@Test
	public void testValidateObjectWithClearedExistingAnnotations() {
		when(mockEntityManger.getBoundSchema(entityId)).thenReturn(binding);
		when(mockEntityManger.getEntityJsonSubject(entityId, false)).thenReturn(mockEntitySubject);
		when(mockJsonSchemaManager.getValidationSchema(schema$id)).thenReturn(mockJsonSchema);
		when(mockJsonSchemaValidationManager.validate(mockJsonSchema, mockEntitySubject))
				.thenReturn(mockValidationResults);
		JSONObject subject = new JSONObject();
		when(mockEntitySubject.toJson()).thenReturn(subject);
		when(mockJsonSchemaValidationManager.calculateDerivedAnnotations(any(), any())).thenReturn(Optional.empty());
		when(mockDerivedAnnotationDao.clearDerivedAnnotations(any())).thenReturn(true);
		// call under test
		manager.validateObject(entityId);
		verify(mockSchemaValidationResultDao).createOrUpdateResults(mockValidationResults);
		verify(mockSchemaValidationResultDao, never()).clearResults(any(), any());
		verify(mockEntityManger).getBoundSchema(entityId);
		verify(mockEntityManger).getEntityJsonSubject(entityId, false);
		verify(mockJsonSchemaManager).getValidationSchema(schema$id);
		verify(mockJsonSchemaValidationManager).validate(mockJsonSchema, mockEntitySubject);
		verify(mockJsonSchemaValidationManager).calculateDerivedAnnotations(mockJsonSchema, subject);
		verify(mockDerivedAnnotationDao, never()).saveDerivedAnnotations(any(), any());
		verify(mockDerivedAnnotationDao).clearDerivedAnnotations(any());
		verify(mockAccessRequirementManager).setDynamicallyBoundAccessRequirementsForSubject(
				new RestrictableObjectDescriptor().setId(entityId).setType(RestrictableObjectType.ENTITY),
				Collections.emptySet());
		verify(mockMessenger).publishMessageAfterCommit(new LocalStackChangeMesssage().setObjectId(entityId)
				.setObjectType(org.sagebionetworks.repo.model.ObjectType.ENTITY).setChangeType(ChangeType.UPDATE)
				.setUserId(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()));
	}

	@Test
	public void testValidateObjectWithNotFound() {
		NotFoundException exception = new NotFoundException("");
		when(mockEntityManger.getBoundSchema(entityId)).thenThrow(exception);
		// call under test
		manager.validateObject(entityId);
		verify(mockSchemaValidationResultDao, never()).createOrUpdateResults(any());
		verify(mockSchemaValidationResultDao).clearResults(entityId, ObjectType.entity);
		verify(mockDerivedAnnotationDao, never()).saveDerivedAnnotations(any(), any());
		verify(mockDerivedAnnotationDao).clearDerivedAnnotations(entityId);
		verify(mockAccessRequirementManager).setDynamicallyBoundAccessRequirementsForSubject(
				new RestrictableObjectDescriptor().setId(entityId).setType(RestrictableObjectType.ENTITY),
				Collections.emptySet());
		verifyZeroInteractions(mockMessenger);
	}

	@Test
	public void testValidateObjectWithNotFoundAndClearedExistingAnnotations() {
		NotFoundException exception = new NotFoundException("");
		when(mockEntityManger.getBoundSchema(entityId)).thenThrow(exception);
		when(mockDerivedAnnotationDao.clearDerivedAnnotations(any())).thenReturn(true);
		// call under test
		manager.validateObject(entityId);
		verify(mockSchemaValidationResultDao, never()).createOrUpdateResults(any());
		verify(mockSchemaValidationResultDao).clearResults(entityId, ObjectType.entity);
		verify(mockDerivedAnnotationDao, never()).saveDerivedAnnotations(any(), any());
		verify(mockDerivedAnnotationDao).clearDerivedAnnotations(entityId);
		verify(mockMessenger).publishMessageAfterCommit(new LocalStackChangeMesssage().setObjectId(entityId)
				.setObjectType(org.sagebionetworks.repo.model.ObjectType.ENTITY).setChangeType(ChangeType.UPDATE)
				.setUserId(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()));
	}

	@Test
	public void testValidateObjectWithNullEntityId() {
		entityId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.validateObject(entityId);
		});
	}

	@Test
	public void testValidateObjectWithEnableDerivedFalse() {
		binding.setEnableDerivedAnnotations(false);
		when(mockEntityManger.getBoundSchema(entityId)).thenReturn(binding);
		when(mockEntityManger.getEntityJsonSubject(entityId, false)).thenReturn(mockEntitySubject);
		when(mockJsonSchemaManager.getValidationSchema(schema$id)).thenReturn(mockJsonSchema);
		when(mockJsonSchemaValidationManager.validate(mockJsonSchema, mockEntitySubject))
				.thenReturn(mockValidationResults);
		// call under test
		manager.validateObject(entityId);
		verify(mockSchemaValidationResultDao).createOrUpdateResults(mockValidationResults);
		verify(mockSchemaValidationResultDao, never()).clearResults(any(), any());
		verify(mockEntityManger).getBoundSchema(entityId);
		verify(mockEntityManger).getEntityJsonSubject(entityId, false);
		verify(mockJsonSchemaManager).getValidationSchema(schema$id);
		verify(mockJsonSchemaValidationManager).validate(mockJsonSchema, mockEntitySubject);
		verify(mockJsonSchemaValidationManager, never()).calculateDerivedAnnotations(any(), any());
		verify(mockDerivedAnnotationDao, never()).saveDerivedAnnotations(any(), any());
		verify(mockDerivedAnnotationDao).clearDerivedAnnotations(entityId);
	}

	@Test
	public void testValidateObjectWithEnableDerivedFalseAndClearedExisting() {
		binding.setEnableDerivedAnnotations(false);
		when(mockEntityManger.getBoundSchema(entityId)).thenReturn(binding);
		when(mockEntityManger.getEntityJsonSubject(entityId, false)).thenReturn(mockEntitySubject);
		when(mockJsonSchemaManager.getValidationSchema(schema$id)).thenReturn(mockJsonSchema);
		when(mockJsonSchemaValidationManager.validate(mockJsonSchema, mockEntitySubject))
				.thenReturn(mockValidationResults);
		when(mockDerivedAnnotationDao.clearDerivedAnnotations(any())).thenReturn(true);
		// call under test
		manager.validateObject(entityId);
		verify(mockSchemaValidationResultDao).createOrUpdateResults(mockValidationResults);
		verify(mockSchemaValidationResultDao, never()).clearResults(any(), any());
		verify(mockEntityManger).getBoundSchema(entityId);
		verify(mockEntityManger).getEntityJsonSubject(entityId, false);
		verify(mockJsonSchemaManager).getValidationSchema(schema$id);
		verify(mockJsonSchemaValidationManager).validate(mockJsonSchema, mockEntitySubject);
		verify(mockJsonSchemaValidationManager, never()).calculateDerivedAnnotations(any(), any());
		verify(mockDerivedAnnotationDao, never()).saveDerivedAnnotations(any(), any());
		verify(mockDerivedAnnotationDao).clearDerivedAnnotations(entityId);
		verify(mockMessenger).publishMessageAfterCommit(new LocalStackChangeMesssage().setObjectId(entityId)
				.setObjectType(org.sagebionetworks.repo.model.ObjectType.ENTITY).setChangeType(ChangeType.UPDATE)
				.setUserId(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()));
	}

	@Test
	public void testExtractAccessRequirmentIds() {
		Annotations annos = new Annotations().setAnnotations(new HashMap<>());
		AnnotationsV2TestUtils.putAnnotations(annos, AnnotationsV2Utils.ACCESS_REQUIREMENT_IDS,
				List.of("11", "22", "33"), AnnotationsValueType.LONG);
		AnnotationsV2TestUtils.putAnnotations(annos, "someOtherKey", List.of("44", "55", "123"),
				AnnotationsValueType.LONG);
		// call under test
		Set<Long> arIds = EntitySchemaValidator.extractAccessRequirmentIds(annos);
		Set<Long> expected = Set.of(11L, 22L, 33L);
		assertEquals(expected, arIds);
	}

	@Test
	public void testExtractAccessRequirmentIdsWithNullAnnotations() {
		Annotations annos = null;
		// call under test
		Set<Long> arIds = EntitySchemaValidator.extractAccessRequirmentIds(annos);
		assertEquals(Collections.emptySet(), arIds);
	}

	@Test
	public void testExtractAccessRequirmentIdsWithNullAnnotationsValues() {
		Annotations annos = new Annotations().setAnnotations(null);
		// call under test
		Set<Long> arIds = EntitySchemaValidator.extractAccessRequirmentIds(annos);
		assertEquals(Collections.emptySet(), arIds);
	}

	@Test
	public void testExtractAccessRequirmentIdsWithEmptyAnnotationsValues() {
		Annotations annos = new Annotations().setAnnotations(Collections.emptyMap());
		// call under test
		Set<Long> arIds = EntitySchemaValidator.extractAccessRequirmentIds(annos);
		assertEquals(Collections.emptySet(), arIds);
	}

	@Test
	public void testExtractAccessRequirmentIdsWithOutMatch() {
		Annotations annos = new Annotations().setAnnotations(new HashMap<>());
		AnnotationsV2TestUtils.putAnnotations(annos, "someOtherKey", List.of("11", "22", "33"),
				AnnotationsValueType.LONG);
		// call under test
		Set<Long> arIds = EntitySchemaValidator.extractAccessRequirmentIds(annos);
		assertEquals(Collections.emptySet(), arIds);
	}

	@Test
	public void testExtractAccessRequirmentIdsWithEmptyValue() {
		Annotations annos = new Annotations().setAnnotations(new HashMap<>());
		AnnotationsV2TestUtils.putAnnotations(annos, AnnotationsV2Utils.ACCESS_REQUIREMENT_IDS, Collections.emptyList(),
				AnnotationsValueType.LONG);
		// call under test
		Set<Long> arIds = EntitySchemaValidator.extractAccessRequirmentIds(annos);
		assertEquals(Collections.emptySet(), arIds);
	}

	@Test
	public void testExtractAccessRequirmentIdsWithWrongType() {
		Annotations annos = new Annotations().setAnnotations(new HashMap<>());
		AnnotationsV2TestUtils.putAnnotations(annos, AnnotationsV2Utils.ACCESS_REQUIREMENT_IDS,
				List.of("11", "22", "33", "one"), AnnotationsValueType.STRING);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			EntitySchemaValidator.extractAccessRequirmentIds(annos);
		}).getMessage();
		assertEquals("The derived annotation with the key: '_accessRequirementIds'"
				+ " does not have an expected type of: 'LONG', actual type is: 'STRING'", message);
	}

}
