package org.sagebionetworks.repo.web.controller.metadata;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableEntityMetadataProviderTest {
	
	@Autowired
	MetadataProviderFactory metadataProviderFactory;
	TypeSpecificMetadataProvider<Entity> tableEntityMetadataProvider;
	
	@Before
	public void before(){
		List<TypeSpecificMetadataProvider<Entity>> types = metadataProviderFactory.getMetadataProvider(EntityType.getNodeTypeForClass(TableEntity.class));
		assertNotNull(types);
		tableEntityMetadataProvider = types.get(0);
	}
		
	@Test (expected=IllegalArgumentException.class)
	public void testCreateNull() throws Exception {
		EntityEvent event = new EntityEvent(EventType.CREATE, null, null);
		TableEntity table = new TableEntity();
		table.setColumnIds(null);
		tableEntityMetadataProvider.validateEntity(table, event);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testUpdateNull() throws Exception {
		EntityEvent event = new EntityEvent(EventType.UPDATE, null, null);
		TableEntity table = new TableEntity();
		table.setColumnIds(null);
		tableEntityMetadataProvider.validateEntity(table, event);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testNewVersionNull() throws Exception {
		EntityEvent event = new EntityEvent(EventType.NEW_VERSION, null, null);
		TableEntity table = new TableEntity();
		table.setColumnIds(null);
		tableEntityMetadataProvider.validateEntity(table, event);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateEmpty() throws Exception {
		EntityEvent event = new EntityEvent(EventType.CREATE, null, null);
		TableEntity table = new TableEntity();
		table.setColumnIds(new LinkedList<String>());
		tableEntityMetadataProvider.validateEntity(table, event);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testUpdateEmpty() throws Exception {
		EntityEvent event = new EntityEvent(EventType.UPDATE, null, null);
		TableEntity table = new TableEntity();
		table.setColumnIds(new LinkedList<String>());
		tableEntityMetadataProvider.validateEntity(table, event);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testNewVersionEmpty() throws Exception {
		EntityEvent event = new EntityEvent(EventType.NEW_VERSION, null, null);
		TableEntity table = new TableEntity();
		table.setColumnIds(new LinkedList<String>());
		tableEntityMetadataProvider.validateEntity(table, event);
	}
	
}
