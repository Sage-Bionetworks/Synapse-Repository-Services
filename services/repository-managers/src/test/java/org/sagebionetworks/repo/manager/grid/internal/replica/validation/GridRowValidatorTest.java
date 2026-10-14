package org.sagebionetworks.repo.manager.grid.internal.replica.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaValidationManager;
import org.sagebionetworks.repo.manager.schema.JsonSubject;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ValidationResults;

@ExtendWith(MockitoExtension.class)
public class GridRowValidatorTest {

	@Mock
	private JsonSchemaManager mockJsonSchemaManager;
	@Mock
	private JsonSchemaValidationManager mockJsonSchemaValidationManager;
	@Mock
	private JsonSubject mockSubject;

	@InjectMocks
	private GridRowValidator validator;

	@Test
	public void testGetValidationSchema() {
		JsonSchema schema = new JsonSchema().set$id("my.org-Schema-1.0.0");
		when(mockJsonSchemaManager.getValidationSchema("my.org-Schema-1.0.0")).thenReturn(schema);

		// call under test
		JsonSchema result = validator.getValidationSchema("my.org-Schema-1.0.0");

		assertSame(schema, result);
		verify(mockJsonSchemaManager).getValidationSchema("my.org-Schema-1.0.0");
	}

	@Test
	public void testValidateBatchAppliesCleanup() {
		JsonSchema schema = new JsonSchema();
		ValidationResults dirty = new ValidationResults().setIsValid(true).setSchema$id("schema").setValidatedOn(new Date());
		when(mockJsonSchemaValidationManager.validateBatch(schema, List.of(mockSubject)))
				.thenReturn(List.of(dirty));

		// call under test
		List<ValidationResults> results = validator.validateBatch(schema, List.of(mockSubject));

		assertEquals(1, results.size());
		// transient fields are cleared by the shared cleanup
		assertNull(results.get(0).getSchema$id());
		assertNull(results.get(0).getValidatedOn());
		assertEquals(Boolean.TRUE, results.get(0).getIsValid());
	}

	@Test
	public void testCleanupValidationResults() {
		ValidationResults results = new ValidationResults().setSchema$id("schema").setValidatedOn(new Date());

		// call under test
		GridRowValidator.cleanupValidationResults(results);

		assertNull(results.getSchema$id());
		assertNull(results.getValidatedOn());
		assertNull(results.getValidationException());
	}
}
