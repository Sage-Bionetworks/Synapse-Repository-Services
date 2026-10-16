package org.sagebionetworks.repo.manager.grid.synch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.GridSession;
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

	@Test
	public void testDiffersFromWithSameSchemaId() {
		GridSession session = new GridSession().setGridJsonSchema$Id(SCHEMA_ID);

		// call under test
		assertFalse(new SessionSchema(SCHEMA_ID).differsFrom(session));
	}

	@Test
	public void testDiffersFromWithNoSchemaOnEither() {
		// call under test
		assertFalse(new SessionSchema(null).differsFrom(new GridSession()));
	}

	@Test
	public void testDiffersFromWithDifferentSchemaId() {
		GridSession session = new GridSession().setGridJsonSchema$Id("other.org-schema");

		// call under test
		assertTrue(new SessionSchema(SCHEMA_ID).differsFrom(session));
	}

	@Test
	public void testDiffersFromWithSchemaBoundAfterSessionCreated() {
		// call under test
		assertTrue(new SessionSchema(SCHEMA_ID).differsFrom(new GridSession()));
	}

	@Test
	public void testDiffersFromWithSchemaUnbound() {
		GridSession session = new GridSession().setGridJsonSchema$Id(SCHEMA_ID);

		// call under test
		assertTrue(new SessionSchema(null).differsFrom(session));
	}
}
