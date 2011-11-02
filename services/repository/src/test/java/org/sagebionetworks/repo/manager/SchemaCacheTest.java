package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class SchemaCacheTest {
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetSchemaNull() throws JSONObjectAdapterException{
		SchemaCache.getSchema(null);
	}
	
	@Test
	public void testGetSchema() throws JSONObjectAdapterException{
		Dataset ds = new Dataset();
		ObjectSchema schema = SchemaCache.getSchema(ds);
		assertNotNull(schema);
		assertNotNull(schema.getProperties());
		assertNotNull(schema.getProperties().get("name"));
		// If we get it again it should be the same object
		ObjectSchema schema2 = SchemaCache.getSchema(ds);
		assertTrue(schema == schema2);
	}

}
