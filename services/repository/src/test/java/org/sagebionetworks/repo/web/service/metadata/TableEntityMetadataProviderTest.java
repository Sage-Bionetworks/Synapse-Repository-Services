package org.sagebionetworks.repo.web.service.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.AbstractAutowiredControllerTestBase;
import org.springframework.beans.factory.annotation.Autowired;

public class TableEntityMetadataProviderTest extends AbstractAutowiredControllerTestBase {
	
	@Autowired
	private MetadataProviderFactory metadataProviderFactory;
	private EntityValidator<Entity> tableEntityMetadataProvider;
	
	@Autowired
	private ColumnModelManager columnModelManager;
	
	@Autowired
	private UserManager userManager;
	
	private UserInfo adminUserInfo;
	private ColumnModel one;
	
	@Before
	public void before() throws Exception {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		List<EntityProvider<Entity>> types = metadataProviderFactory.getMetadataProvider(EntityType.getEntityTypeForClass(TableEntity.class));
		assertNotNull(types);
		tableEntityMetadataProvider = (EntityValidator) types.get(0);
		// Create some columns
		one = new ColumnModel();
		one.setName("TableEntityMetadataProviderTest");
		one.setColumnType(ColumnType.STRING);
		one = columnModelManager.createColumnModel(adminUserInfo, one);
	}
	
	@After
	public void after(){
		columnModelManager.truncateAllColumnData(new UserInfo(true));
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
	
	@Test
	public void testCreateHappy() throws InvalidModelException, DatastoreException, UnauthorizedException, NotFoundException{
		// Before we start nothing should be bound to the column one
		Set<String> columnIds = new HashSet<String>();
		columnIds.add(one.getId());
		PaginatedIds results = columnModelManager.listObjectsBoundToColumn(adminUserInfo, columnIds, false, 100, 0);
		assertNotNull(results);
		assertEquals(new Long(0), results.getTotalNumberOfResults());
		// Create a table entity
		EntityEvent event = new EntityEvent(EventType.CREATE, null, adminUserInfo);
		TableEntity table = new TableEntity();
		table.setId("syn123");
		table.setColumnIds(new LinkedList<String>());
		table.getColumnIds().add(one.getId());
		// Validate that we could create this table
		tableEntityMetadataProvider.validateEntity(table, event);
		// Now this table should be listed as bound to the column\
		List<String> expectedIds = new LinkedList<String>();
		expectedIds.add("syn123");
		results = columnModelManager.listObjectsBoundToColumn(adminUserInfo, columnIds, false, 100, 0);
		assertNotNull(results);
		assertEquals(new Long(1), results.getTotalNumberOfResults());
		assertEquals(expectedIds, results.getResults());
	}
	
	@Test
	public void testUpdateHappy() throws InvalidModelException, DatastoreException, UnauthorizedException, NotFoundException{
		// Before we start nothing should be bound to the column one
		Set<String> columnIds = new HashSet<String>();
		columnIds.add(one.getId());
		PaginatedIds results = columnModelManager.listObjectsBoundToColumn(adminUserInfo, columnIds, false, 100, 0);
		assertNotNull(results);
		assertEquals(new Long(0), results.getTotalNumberOfResults());
		// Create a table entity
		EntityEvent event = new EntityEvent(EventType.UPDATE, null, adminUserInfo);
		TableEntity table = new TableEntity();
		table.setId("syn123");
		table.setColumnIds(new LinkedList<String>());
		table.getColumnIds().add(one.getId());
		// Validate that we could create this table
		tableEntityMetadataProvider.validateEntity(table, event);
		// Now this table should be listed as bound to the column\
		List<String> expectedIds = new LinkedList<String>();
		expectedIds.add("syn123");
		results = columnModelManager.listObjectsBoundToColumn(adminUserInfo, columnIds, false, 100, 0);
		assertNotNull(results);
		assertEquals(new Long(1), results.getTotalNumberOfResults());
		assertEquals(expectedIds, results.getResults());
	}
	
}
