package org.sagebionetworks.repo.manager.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.dataaccess.AccessRequirementManager;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
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

	@Spy
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
	
	private RestrictableObjectDescriptor objectDescriptor;
	private Annotations annotations;
	private Set<Long> accessRequirmentIdsToBind;

	@BeforeEach
	public void before() {
		entityId = "syn123";
		schema$id = "my.org-foo.bar-1.0.0";
		binding = new JsonSchemaObjectBinding();
		JsonSchemaVersionInfo versionInfo = new JsonSchemaVersionInfo();
		versionInfo.set$id(schema$id);
		binding.setJsonSchemaVersionInfo(versionInfo);
		binding.setEnableDerivedAnnotations(true);
		
		objectDescriptor = new RestrictableObjectDescriptor().setId(entityId).setType(RestrictableObjectType.ENTITY);
		annotations = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(annotations, "keyOne", "valueOne", AnnotationsValueType.STRING);
		accessRequirmentIdsToBind = Set.of(111L,222L);
	}
	
	@Test
	public void testValidateObjectWithBindingAndUpdate() {
		when(mockEntityManger.findBoundSchema(any())).thenReturn(Optional.of(binding));
		
		doReturn(true).when(manager).validateAgainstBoundSchema(any(), any());
		
		// call under test
		manager.validateObject(entityId);
		
		verify(mockEntityManger).findBoundSchema(entityId);
		verify(manager).validateAgainstBoundSchema(objectDescriptor, binding);
		verify(manager, never()).clearAllBoundSchemaRelatedData(any());
		LocalStackChangeMesssage expectedMessage = new LocalStackChangeMesssage().setObjectId(entityId)
				.setObjectType(org.sagebionetworks.repo.model.ObjectType.ENTITY).setChangeType(ChangeType.UPDATE)
				.setUserId(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		verify(mockMessenger).publishMessageAfterCommit(expectedMessage);
	}
	
	@Test
	public void testValidateObjectWithBindingNoUpdate() {
		when(mockEntityManger.findBoundSchema(any())).thenReturn(Optional.of(binding));
		
		doReturn(false).when(manager).validateAgainstBoundSchema(any(), any());
		
		// call under test
		manager.validateObject(entityId);
		
		verify(mockEntityManger).findBoundSchema(entityId);
		verify(manager).validateAgainstBoundSchema(objectDescriptor, binding);
		verify(manager, never()).clearAllBoundSchemaRelatedData(any());
		verify(mockMessenger, never()).publishMessageAfterCommit(any());
	}
	
	@Test
	public void testValidateObjectWithoutBindingAndUpdate() {
		when(mockEntityManger.findBoundSchema(any())).thenReturn(Optional.empty());
		
		doReturn(true).when(manager).clearAllBoundSchemaRelatedData(any());
		
		// call under test
		manager.validateObject(entityId);
		
		verify(mockEntityManger).findBoundSchema(entityId);
		verify(manager, never()).validateAgainstBoundSchema(any(), any());
		verify(manager).clearAllBoundSchemaRelatedData(objectDescriptor);
		
		LocalStackChangeMesssage expectedMessage = new LocalStackChangeMesssage().setObjectId(entityId)
				.setObjectType(org.sagebionetworks.repo.model.ObjectType.ENTITY).setChangeType(ChangeType.UPDATE)
				.setUserId(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		verify(mockMessenger).publishMessageAfterCommit(expectedMessage);
	}
	
	@Test
	public void testValidateObjectWithoutBindingNoUpdate() {
		when(mockEntityManger.findBoundSchema(any())).thenReturn(Optional.empty());
		
		doReturn(false).when(manager).clearAllBoundSchemaRelatedData(any());
		
		// call under test
		manager.validateObject(entityId);
		
		verify(mockEntityManger).findBoundSchema(entityId);
		verify(manager, never()).validateAgainstBoundSchema(any(), any());
		verify(manager).clearAllBoundSchemaRelatedData(objectDescriptor);
		verify(mockMessenger, never()).publishMessageAfterCommit(any());
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
	
	@Test
	public void testContainsAccessRequirementIdsWithMatch() {
		JsonSchema schema = new JsonSchema().setProperties(
				Map.of("one", new JsonSchema(), AnnotationsV2Utils.ACCESS_REQUIREMENT_IDS, new JsonSchema()));
		// call under test
		assertTrue(EntitySchemaValidator.containsAccessRequirementIds(schema));
	}
	
	@Test
	public void testContainsAccessRequirementIdsWithNoMatch() {
		JsonSchema schema = new JsonSchema().setProperties(
				Map.of("one", new JsonSchema()));
		// call under test
		assertFalse(EntitySchemaValidator.containsAccessRequirementIds(schema));
	}
	
	@Test
	public void testContainsAccessRequirementIdsWithNestedMatch() {
		JsonSchema schema = new JsonSchema().setProperties(Map.of("one",
				new JsonSchema().setProperties(Map.of(AnnotationsV2Utils.ACCESS_REQUIREMENT_IDS, new JsonSchema()))));
		// call under test
		assertTrue(EntitySchemaValidator.containsAccessRequirementIds(schema));
	}
	
	@Test
	public void testContainsAccessRequirementIdsWithNestedNoMatch() {
		JsonSchema schema = new JsonSchema().setProperties(Map.of("one",
				new JsonSchema().setProperties(Map.of("two", new JsonSchema()))));
		// call under test
		assertFalse(EntitySchemaValidator.containsAccessRequirementIds(schema));
	}
	
	@Test
	public void testSetDerivedAnnotationsAndBindAccessRequirements() {
		when(mockDerivedAnnotationDao.getDerivedAnnotations(any())).thenReturn(Optional.empty());

		// call under test
		boolean changed = manager.setDerivedAnnotationsAndBindAccessRequirements(objectDescriptor, annotations,
				accessRequirmentIdsToBind);
		assertTrue(changed);
		verify(mockDerivedAnnotationDao).getDerivedAnnotations(entityId);
		verify(mockAccessRequirementManager).setDynamicallyBoundAccessRequirementsForSubject(objectDescriptor,
				accessRequirmentIdsToBind);
		verify(mockDerivedAnnotationDao).saveDerivedAnnotations(entityId, annotations);
		verify(mockDerivedAnnotationDao, never()).clearDerivedAnnotations(any());
	}

	@Test
	public void testSetDerivedAnnotationsAndBindAccessRequirementsWithEqualAnnotations() {
		when(mockDerivedAnnotationDao.getDerivedAnnotations(any())).thenReturn(Optional.of(annotations));

		// call under test
		boolean changed = manager.setDerivedAnnotationsAndBindAccessRequirements(objectDescriptor, annotations,
				accessRequirmentIdsToBind);
		assertFalse(changed);
		verify(mockDerivedAnnotationDao).getDerivedAnnotations(entityId);
		verify(mockAccessRequirementManager).setDynamicallyBoundAccessRequirementsForSubject(objectDescriptor,
				accessRequirmentIdsToBind);
		verify(mockDerivedAnnotationDao, never()).clearDerivedAnnotations(any());
	}
	
	@Test
	public void testSetDerivedAnnotationsAndBindAccessRequirementsWithNullAnnotations() {
		annotations = null;
		when(mockDerivedAnnotationDao.clearDerivedAnnotations(any())).thenReturn(true);
		// call under test
		boolean changed = manager.setDerivedAnnotationsAndBindAccessRequirements(objectDescriptor, annotations,
				accessRequirmentIdsToBind);
		assertTrue(changed);
		verify(mockAccessRequirementManager).setDynamicallyBoundAccessRequirementsForSubject(objectDescriptor,
				accessRequirmentIdsToBind);
		verify(mockDerivedAnnotationDao, never()).saveDerivedAnnotations(any(), any());
		verify(mockDerivedAnnotationDao).clearDerivedAnnotations(entityId);
	}
	
	@Test
	public void testSetDerivedAnnotationsAndBindAccessRequirementsWithNullAnnotationsAndNoChange() {
		annotations = null;
		when(mockDerivedAnnotationDao.clearDerivedAnnotations(any())).thenReturn(false);
		// call under test
		boolean changed = manager.setDerivedAnnotationsAndBindAccessRequirements(objectDescriptor, annotations,
				accessRequirmentIdsToBind);
		assertFalse(changed);
		verify(mockAccessRequirementManager).setDynamicallyBoundAccessRequirementsForSubject(objectDescriptor,
				accessRequirmentIdsToBind);
		verify(mockDerivedAnnotationDao, never()).saveDerivedAnnotations(any(), any());
		verify(mockDerivedAnnotationDao).clearDerivedAnnotations(entityId);
	}
	
	@Test
	public void testValidateAgainstBoundSchema() {

		binding.setEnableDerivedAnnotations(true);
		JSONObject subject = new JSONObject();
		when(mockEntitySubject.toJson()).thenReturn(subject);
		when(mockEntityManger.getEntityJsonSubject(any(), anyBoolean())).thenReturn(mockEntitySubject);
		when(mockJsonSchemaManager.getValidationSchema(any())).thenReturn(mockJsonSchema);
		when(mockValidationResults.getIsValid()).thenReturn(true);
		when(mockJsonSchemaValidationManager.validate(any(), any())).thenReturn(mockValidationResults);
		when(mockJsonSchemaValidationManager.calculateDerivedAnnotations(any(), any())).thenReturn(Optional.of(annotations));
		
		doReturn(true).when(manager).setDerivedAnnotationsAndBindAccessRequirements(any(), any(), any());
		
		// call under test
		boolean changed = manager.validateAgainstBoundSchema(objectDescriptor, binding);
		
		assertTrue(changed);
		verify(mockSchemaValidationResultDao).createOrUpdateResults(mockValidationResults);
		verify(mockEntityManger).getEntityJsonSubject(entityId, false);
		verify(mockJsonSchemaManager).getValidationSchema(binding.getJsonSchemaVersionInfo().get$id());
		verify(mockJsonSchemaValidationManager).validate(mockJsonSchema, mockEntitySubject);
		verify(manager).setDerivedAnnotationsAndBindAccessRequirements(objectDescriptor, annotations, Collections.emptySet());
		verify(mockJsonSchemaValidationManager).calculateDerivedAnnotations(mockJsonSchema, subject);
	}
	
	@Test
	public void testValidateAgainstBoundSchemaWithNoChnage() {

		binding.setEnableDerivedAnnotations(true);
		JSONObject subject = new JSONObject();
		when(mockEntitySubject.toJson()).thenReturn(subject);
		when(mockEntityManger.getEntityJsonSubject(any(), anyBoolean())).thenReturn(mockEntitySubject);
		when(mockJsonSchemaManager.getValidationSchema(any())).thenReturn(mockJsonSchema);
		when(mockValidationResults.getIsValid()).thenReturn(true);
		when(mockJsonSchemaValidationManager.validate(any(), any())).thenReturn(mockValidationResults);
		when(mockJsonSchemaValidationManager.calculateDerivedAnnotations(any(), any())).thenReturn(Optional.of(annotations));
		
		doReturn(false).when(manager).setDerivedAnnotationsAndBindAccessRequirements(any(), any(), any());
		
		// call under test
		boolean changed = manager.validateAgainstBoundSchema(objectDescriptor, binding);
		
		assertFalse(changed);
		verify(mockSchemaValidationResultDao).createOrUpdateResults(mockValidationResults);
		verify(mockEntityManger).getEntityJsonSubject(entityId, false);
		verify(mockJsonSchemaManager).getValidationSchema(binding.getJsonSchemaVersionInfo().get$id());
		verify(mockJsonSchemaValidationManager).validate(mockJsonSchema, mockEntitySubject);
		verify(manager).setDerivedAnnotationsAndBindAccessRequirements(objectDescriptor, annotations, Collections.emptySet());
		verify(mockJsonSchemaValidationManager).calculateDerivedAnnotations(mockJsonSchema, subject);
	}
	
	@Test
	public void testValidateAgainstBoundSchemaWithEmptyAnnotations() {

		binding.setEnableDerivedAnnotations(true);
		JSONObject subject = new JSONObject();
		when(mockEntitySubject.toJson()).thenReturn(subject);
		when(mockEntityManger.getEntityJsonSubject(any(), anyBoolean())).thenReturn(mockEntitySubject);
		when(mockJsonSchemaManager.getValidationSchema(any())).thenReturn(mockJsonSchema);
		when(mockValidationResults.getIsValid()).thenReturn(true);
		when(mockJsonSchemaValidationManager.validate(any(), any())).thenReturn(mockValidationResults);
		when(mockJsonSchemaValidationManager.calculateDerivedAnnotations(any(), any())).thenReturn(Optional.empty());
		
		doReturn(true).when(manager).setDerivedAnnotationsAndBindAccessRequirements(any(), any(), any());
		
		// call under test
		boolean changed = manager.validateAgainstBoundSchema(objectDescriptor, binding);
		
		assertTrue(changed);
		verify(mockSchemaValidationResultDao).createOrUpdateResults(mockValidationResults);
		verify(mockEntityManger).getEntityJsonSubject(entityId, false);
		verify(mockJsonSchemaManager).getValidationSchema(binding.getJsonSchemaVersionInfo().get$id());
		verify(mockJsonSchemaValidationManager).validate(mockJsonSchema, mockEntitySubject);
		verify(manager).setDerivedAnnotationsAndBindAccessRequirements(objectDescriptor, null, Collections.emptySet());
		verify(mockJsonSchemaValidationManager).calculateDerivedAnnotations(mockJsonSchema, subject);
	}
	
	@Test
	public void testValidateAgainstBoundSchemaWithAccessRequirmentIds() {

		binding.setEnableDerivedAnnotations(true);
		JSONObject subject = new JSONObject();
		when(mockEntitySubject.toJson()).thenReturn(subject);
		when(mockEntityManger.getEntityJsonSubject(any(), anyBoolean())).thenReturn(mockEntitySubject);
		when(mockJsonSchemaManager.getValidationSchema(any())).thenReturn(mockJsonSchema);
		when(mockValidationResults.getIsValid()).thenReturn(true);
		when(mockJsonSchemaValidationManager.validate(any(), any())).thenReturn(mockValidationResults);

		AnnotationsV2TestUtils.putAnnotations(annotations, AnnotationsV2Utils.ACCESS_REQUIREMENT_IDS,
				List.of("11", "22"), AnnotationsValueType.LONG);
		when(mockJsonSchemaValidationManager.calculateDerivedAnnotations(any(), any()))
				.thenReturn(Optional.of(annotations));

		doReturn(true).when(manager).setDerivedAnnotationsAndBindAccessRequirements(any(), any(), any());

		// call under test
		boolean changed = manager.validateAgainstBoundSchema(objectDescriptor, binding);

		assertTrue(changed);
		verify(mockSchemaValidationResultDao).createOrUpdateResults(mockValidationResults);
		verify(mockEntityManger).getEntityJsonSubject(entityId, false);
		verify(mockJsonSchemaManager).getValidationSchema(binding.getJsonSchemaVersionInfo().get$id());
		verify(mockJsonSchemaValidationManager).validate(mockJsonSchema, mockEntitySubject);
		verify(mockJsonSchemaValidationManager).calculateDerivedAnnotations(mockJsonSchema, subject);
		Set<Long> expectedAccessRequirementIds = new LinkedHashSet<Long>(List.of(11L,22L));
		verify(manager).setDerivedAnnotationsAndBindAccessRequirements(objectDescriptor, annotations,
				expectedAccessRequirementIds);

	}
	
	@Test
	public void testValidateAgainstBoundSchemaWithDerivedDisabled() {

		binding.setEnableDerivedAnnotations(false);
		when(mockEntityManger.getEntityJsonSubject(any(), anyBoolean())).thenReturn(mockEntitySubject);
		when(mockJsonSchemaManager.getValidationSchema(any())).thenReturn(mockJsonSchema);
		when(mockValidationResults.getIsValid()).thenReturn(true);
		when(mockJsonSchemaValidationManager.validate(any(), any())).thenReturn(mockValidationResults);
		doReturn(true).when(manager).setDerivedAnnotationsAndBindAccessRequirements(any(), any(), any());

		// call under test
		boolean changed = manager.validateAgainstBoundSchema(objectDescriptor, binding);

		assertTrue(changed);
		verify(mockSchemaValidationResultDao).createOrUpdateResults(mockValidationResults);
		verify(mockEntityManger).getEntityJsonSubject(entityId, false);
		verify(mockJsonSchemaManager).getValidationSchema(binding.getJsonSchemaVersionInfo().get$id());
		verify(mockJsonSchemaValidationManager).validate(mockJsonSchema, mockEntitySubject);
		verify(mockJsonSchemaValidationManager, never()).calculateDerivedAnnotations(any(), any());
		verify(manager).setDerivedAnnotationsAndBindAccessRequirements(objectDescriptor, null,
				Collections.emptySet());
	}
	
	@Test
	public void testValidateAgainstBoundSchemaWithInvalidAndDerivedEnabledAndAccessRequirmentsInSchema() {

		binding.setEnableDerivedAnnotations(true);
		when(mockJsonSchema.getProperties()).thenReturn(Map.of(AnnotationsV2Utils.ACCESS_REQUIREMENT_IDS, new JsonSchema()));
		when(mockEntityManger.getEntityJsonSubject(any(), anyBoolean())).thenReturn(mockEntitySubject);
		when(mockJsonSchemaManager.getValidationSchema(any())).thenReturn(mockJsonSchema);
		when(mockValidationResults.getIsValid()).thenReturn(false);
		when(mockJsonSchemaValidationManager.validate(any(), any())).thenReturn(mockValidationResults);
		
		doReturn(true).when(manager).setDerivedAnnotationsAndBindAccessRequirements(any(), any(), any());
		
		// call under test
		boolean changed = manager.validateAgainstBoundSchema(objectDescriptor, binding);
		
		assertTrue(changed);
		verify(mockSchemaValidationResultDao).createOrUpdateResults(mockValidationResults);
		verify(mockEntityManger).getEntityJsonSubject(entityId, false);
		verify(mockJsonSchemaManager).getValidationSchema(binding.getJsonSchemaVersionInfo().get$id());
		verify(mockJsonSchemaValidationManager).validate(mockJsonSchema, mockEntitySubject);
		verify(mockJsonSchemaValidationManager, never()).calculateDerivedAnnotations(any(), any());
		Set<Long> expectedAccessRequirementIds = new LinkedHashSet<Long>(List.of(AccessRequirementDAO.INVALID_ANNOTATIONS_LOCK_ID));
		verify(manager).setDerivedAnnotationsAndBindAccessRequirements(objectDescriptor, null, expectedAccessRequirementIds);
	}
	
	@Test
	public void testValidateAgainstBoundSchemaWithInvalidAndDerivedDisabledAndAccessRequirmentsInSchema() {

		binding.setEnableDerivedAnnotations(false);
		when(mockJsonSchema.getProperties()).thenReturn(Map.of(AnnotationsV2Utils.ACCESS_REQUIREMENT_IDS, new JsonSchema()));
		when(mockEntityManger.getEntityJsonSubject(any(), anyBoolean())).thenReturn(mockEntitySubject);
		when(mockJsonSchemaManager.getValidationSchema(any())).thenReturn(mockJsonSchema);
		when(mockValidationResults.getIsValid()).thenReturn(false);
		when(mockJsonSchemaValidationManager.validate(any(), any())).thenReturn(mockValidationResults);
		
		doReturn(true).when(manager).setDerivedAnnotationsAndBindAccessRequirements(any(), any(), any());
		
		// call under test
		boolean changed = manager.validateAgainstBoundSchema(objectDescriptor, binding);
		
		assertTrue(changed);
		verify(mockSchemaValidationResultDao).createOrUpdateResults(mockValidationResults);
		verify(mockEntityManger).getEntityJsonSubject(entityId, false);
		verify(mockJsonSchemaManager).getValidationSchema(binding.getJsonSchemaVersionInfo().get$id());
		verify(mockJsonSchemaValidationManager).validate(mockJsonSchema, mockEntitySubject);
		verify(mockJsonSchemaValidationManager, never()).calculateDerivedAnnotations(any(), any());
		Set<Long> expectedAccessRequirementIds = Collections.emptySet();
		verify(manager).setDerivedAnnotationsAndBindAccessRequirements(objectDescriptor, null, expectedAccessRequirementIds);
	}
	
	@Test
	public void testValidateAgainstBoundSchemaWithInvalidAndDerivedEnabledAndNoAccessRequirmentsInSchema() {

		binding.setEnableDerivedAnnotations(true);
		when(mockJsonSchema.getProperties()).thenReturn(Map.of("one", new JsonSchema()));
		when(mockEntityManger.getEntityJsonSubject(any(), anyBoolean())).thenReturn(mockEntitySubject);
		when(mockJsonSchemaManager.getValidationSchema(any())).thenReturn(mockJsonSchema);
		when(mockValidationResults.getIsValid()).thenReturn(false);
		when(mockJsonSchemaValidationManager.validate(any(), any())).thenReturn(mockValidationResults);
		
		doReturn(true).when(manager).setDerivedAnnotationsAndBindAccessRequirements(any(), any(), any());
		
		// call under test
		boolean changed = manager.validateAgainstBoundSchema(objectDescriptor, binding);
		
		assertTrue(changed);
		verify(mockSchemaValidationResultDao).createOrUpdateResults(mockValidationResults);
		verify(mockEntityManger).getEntityJsonSubject(entityId, false);
		verify(mockJsonSchemaManager).getValidationSchema(binding.getJsonSchemaVersionInfo().get$id());
		verify(mockJsonSchemaValidationManager).validate(mockJsonSchema, mockEntitySubject);
		verify(mockJsonSchemaValidationManager, never()).calculateDerivedAnnotations(any(), any());
		Set<Long> expectedAccessRequirementIds = Collections.emptySet();
		verify(manager).setDerivedAnnotationsAndBindAccessRequirements(objectDescriptor, null, expectedAccessRequirementIds);
	}

	@Test
	public void testClearAllBoundSchemaRelatedData() {

		// call under test
		manager.clearAllBoundSchemaRelatedData(objectDescriptor);
		verify(mockSchemaValidationResultDao).clearResults(entityId, ObjectType.entity);
		verify(mockAccessRequirementManager).setDynamicallyBoundAccessRequirementsForSubject(objectDescriptor,
				Collections.emptySet());
		verify(mockDerivedAnnotationDao).clearDerivedAnnotations(entityId);
	}

}
