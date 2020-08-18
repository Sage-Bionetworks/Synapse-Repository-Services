package org.sagebionetworks.repo.manager.schema;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.EntityManager;
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
		when(mockEntityManger.getEntityJsonSubject(entityId)).thenReturn(mockEntitySubject);
		when(mockJsonSchemaManager.getValidationSchema(schema$id)).thenReturn(mockJsonSchema);
		when(mockJsonSchemaValidationManager.validate(mockJsonSchema, mockEntitySubject))
				.thenReturn(mockValidationResults);
		// call under test
		manager.validateObject(entityId);
		verify(mockSchemaValidationResultDao).createOrUpdateResults(mockValidationResults);
		verify(mockSchemaValidationResultDao, never()).clearResults(any(), any());
		verify(mockEntityManger).getBoundSchema(entityId);
		verify(mockEntityManger).getEntityJsonSubject(entityId);
		verify(mockJsonSchemaManager).getValidationSchema(schema$id);
		verify(mockJsonSchemaValidationManager).validate(mockJsonSchema, mockEntitySubject);
	}
	
	@Test
	public void testValidateObjectWithNotFound() {
		NotFoundException exception = new NotFoundException();
		when(mockEntityManger.getBoundSchema(entityId)).thenThrow(exception);
		// call under test
		manager.validateObject(entityId);
		verify(mockSchemaValidationResultDao, never()).createOrUpdateResults(any());
		verify(mockSchemaValidationResultDao).clearResults(entityId, ObjectType.entity);
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
