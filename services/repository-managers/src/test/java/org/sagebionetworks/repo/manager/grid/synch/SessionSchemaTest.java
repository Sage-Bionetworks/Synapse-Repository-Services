package org.sagebionetworks.repo.manager.grid.synch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;

public class SessionSchemaTest {

	private static final String SCHEMA_ID = "my.org-schema";

	@Test
	public void testOfWithBinding() {
		JsonSchemaObjectBinding binding = new JsonSchemaObjectBinding()
				.setJsonSchemaVersionInfo(new JsonSchemaVersionInfo().set$id(SCHEMA_ID).setVersionId("4567"));

		// call under test
		SessionSchema result = SessionSchema.of(Optional.of(binding));

		assertEquals(new SessionSchema(SCHEMA_ID), result);
	}

	@Test
	public void testOfWithNoBinding() {
		// call under test
		SessionSchema result = SessionSchema.of(Optional.empty());

		assertEquals(new SessionSchema(null), result);
	}
}
