package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.RestResourceList;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * A unit test for SchemaManagerImpl
 * @author jmhill
 *
 */
public class SchemaManagerTest {
	

	SchemaManager schemaManager = new SchemaManagerImpl();
	
	@Test
	public void testGetRESTResources(){
		RestResourceList list = schemaManager.getRESTResources();
		assertNotNull(list);
		assertNotNull(list.getList());
		assertTrue(list.getList().size() > 10);
//		System.out.println(list.getList());
	}
	
	/**
	 * Can we get the effective schema for everything in the list?
	 * @throws NotFoundException 
	 * @throws JSONObjectAdapterException 
	 * @throws DatastoreException 
	 */
	@Test
	public void testGetEffectiveSchema() throws NotFoundException, JSONObjectAdapterException, DatastoreException{
		RestResourceList list = schemaManager.getRESTResources();
		assertNotNull(list);
		assertNotNull(list.getList());
		for(String id: list.getList()){
			ObjectSchema effective = schemaManager.getEffectiveSchema(id);
			assertNotNull(effective);
		}
	}
	
	@Test
	public void testGetFullSchema() throws NotFoundException, IOException, JSONObjectAdapterException, DatastoreException{
		RestResourceList list = schemaManager.getRESTResources();
		assertNotNull(list);
		assertNotNull(list.getList());
		for(String id: list.getList()){
			ObjectSchema full = schemaManager.getFullSchema(id);
			assertNotNull(full);
		}
	}
	
	@Test
	public void testGetFullSchemaEntity() throws NotFoundException, IOException, JSONObjectAdapterException, DatastoreException{
		ObjectSchema full = schemaManager.getFullSchema(Entity.class.getName());
		assertNotNull(full);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetFullSchemaNotFound() throws NotFoundException, IOException, JSONObjectAdapterException, DatastoreException{
		ObjectSchema full = schemaManager.getFullSchema("some.fake.Name");
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetEffectiveSchemaNotFound() throws NotFoundException, IOException, JSONObjectAdapterException, DatastoreException{
		ObjectSchema full = schemaManager.getEffectiveSchema("some.fake.Name");
	}

}
