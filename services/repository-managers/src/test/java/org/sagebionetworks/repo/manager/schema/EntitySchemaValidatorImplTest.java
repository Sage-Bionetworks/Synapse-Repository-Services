package org.sagebionetworks.repo.manager.schema;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.dbo.schema.DerivedAnnotationDao;
import org.sagebionetworks.repo.model.dbo.schema.SchemaValidationResultDao;
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

	@InjectMocks
	private EntitySchemaValidator manager;

	String entityId;
	String schema$id;
	JsonSchemaObjectBinding binding;
	@Mock
	JsonSubject mockEntitySubject;
	@Mock
	JsonSchema mockJsonSchema;
	@Mock
	ValidationResults mockValidationResults;

	@BeforeEach
	public void before() {
		entityId = "syn123";
		schema$id = "my.org-foo.bar-1.0.0";
		binding = new JsonSchemaObjectBinding();
		JsonSchemaVersionInfo versionInfo = new JsonSchemaVersionInfo();
		versionInfo.set$id(schema$id);
		binding.setJsonSchemaVersionInfo(versionInfo);
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
		when(mockJsonSchemaValidationManager.calculateDerivedAnnotations(any(), any())).thenReturn(Optional.of(derivedAnnotations));
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
	}
	
	@Test
	public void testValidateObjectWithNullEntityId() {
		entityId = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.validateObject(entityId);
		});
	}
}
