package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableControllerAutowireTest {
	
	private Entity parent;
	private Long adminUserId;
	
	@Before
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		parent = new Project();
		parent.setName(UUID.randomUUID().toString());
		parent = ServletTestHelper.createEntity(DispatchServletSingleton.getInstance(), parent, adminUserId);
		Assert.assertNotNull(parent);
	}
	
	@After
	public void after(){
		if(parent != null){
			try {
				ServletTestHelper.deleteEntity(DispatchServletSingleton.getInstance(), Project.class, parent.getId(), adminUserId);
			} catch (Exception e) {} 
		}
	}
	
	@Test
	public void testCreateGetDeleteColumnModel() throws ServletException, Exception{
		ColumnModel cm = new ColumnModel();
		cm.setName("TableControllerAutowireTest One");
		cm.setColumnType(ColumnType.STRING);
		// Save the column
		cm = ServletTestHelper.createColumnModel(DispatchServletSingleton.getInstance(), cm, adminUserId);
		assertNotNull(cm);
		assertNotNull(cm.getId());
		// Make sure we can get it
		ColumnModel clone = ServletTestHelper.getColumnModel(DispatchServletSingleton.getInstance(), cm.getId(), adminUserId);
		assertEquals(cm, clone);
	}
	
	@Test
	public void testCreateTableEntity() throws Exception{
		// Create a table with two ColumnModels
		// one
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.STRING);
		one = ServletTestHelper.createColumnModel(DispatchServletSingleton.getInstance(), one, adminUserId);
		// two
		ColumnModel two = new ColumnModel();
		two.setName("two");
		two.setColumnType(ColumnType.STRING);
		two = ServletTestHelper.createColumnModel(DispatchServletSingleton.getInstance(), two, adminUserId);
		// Now create a TableEntity with these Columns
		TableEntity table = new TableEntity();
		table.setName("TableEntity");
		table.setParentId(parent.getId());
		List<String> idList = new LinkedList<String>();
		idList.add(one.getId());
		idList.add(two.getId());
		table.setColumnIds(idList);
		table = ServletTestHelper.createEntity(DispatchServletSingleton.getInstance(), table, adminUserId);
		assertNotNull(table);
		assertNotNull(table.getId());
		TableEntity clone = ServletTestHelper.getEntity(DispatchServletSingleton.getInstance(), TableEntity.class, table.getId(), adminUserId);
		assertNotNull(clone);
		assertEquals(table, clone);
		// Now make sure we can get the list of columns for this entity
		List<ColumnModel> cols = ServletTestHelper.getColumnModelsForTableEntity(DispatchServletSingleton.getInstance(), table.getId(), adminUserId);
		assertNotNull(cols);
		assertEquals(2, cols.size());
		List<ColumnModel> expected = new LinkedList<ColumnModel>();
		expected.add(one);
		expected.add(two);
		assertEquals(expected, cols);
		
		// Add some rows to the table.
		RowSet set = new RowSet();
		List<Row> rows = TableModelUtils.createRows(cols, 2);
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(cols));
		set.setTableId(table.getId());
		RowReferenceSet results = ServletTestHelper.appendTableRows(DispatchServletSingleton.getInstance(), set, adminUserId);
		assertNotNull(results);
		assertNotNull(results.getRows());
		assertEquals(2, results.getRows().size());
		assertEquals(table.getId(), results.getTableId());
		assertEquals(TableModelUtils.getHeaders(cols), results.getHeaders());
	}

	@Test
	public void testListColumnModels() throws ServletException, Exception{
		ColumnModel one = new ColumnModel();
		String prefix = UUID.randomUUID().toString();
		one.setName(prefix+"a");
		one.setColumnType(ColumnType.STRING);
		one = ServletTestHelper.createColumnModel(DispatchServletSingleton.getInstance(), one, adminUserId);
		// two
		ColumnModel two = new ColumnModel();
		two.setName(prefix+"b");
		two.setColumnType(ColumnType.STRING);
		two = ServletTestHelper.createColumnModel(DispatchServletSingleton.getInstance(), two, adminUserId);
		// three
		ColumnModel three = new ColumnModel();
		three.setName(prefix+"bb");
		three.setColumnType(ColumnType.STRING);
		three = ServletTestHelper.createColumnModel(DispatchServletSingleton.getInstance(), three, adminUserId);
		// Now make sure we can find our columns
		PaginatedColumnModels pcm = ServletTestHelper.listColumnModels(DispatchServletSingleton.getInstance(), adminUserId, null, null, null);
		assertNotNull(pcm);
		assertTrue(pcm.getTotalNumberOfResults() >= 3);
		// filter by our prefix
		pcm = ServletTestHelper.listColumnModels(DispatchServletSingleton.getInstance(), adminUserId, prefix, null, null);
		assertNotNull(pcm);
		List<ColumnModel> expected = new LinkedList<ColumnModel>();
		expected.add(one);
		expected.add(two);
		expected.add(three);
		assertEquals(new Long(3), pcm.getTotalNumberOfResults());
		assertEquals(expected, pcm.getResults());
		// Now try pagination.
		pcm = ServletTestHelper.listColumnModels(DispatchServletSingleton.getInstance(), adminUserId, prefix, 1l, 2l);
		assertNotNull(pcm);
		assertEquals(new Long(3), pcm.getTotalNumberOfResults());
		expected.clear();
		expected.add(three);
		assertEquals(expected, pcm.getResults());
	}
	
	@Test
	public void testTableQueryTableUnavailable() throws ServletException, Exception{
		// Create a table with two ColumnModels
		// one
		ColumnModel one = new ColumnModel();
		one.setName("foo");
		one.setColumnType(ColumnType.LONG);
		one = ServletTestHelper.createColumnModel(DispatchServletSingleton.getInstance(), one, adminUserId);
		// two
		ColumnModel two = new ColumnModel();
		two.setName("bar");
		two.setColumnType(ColumnType.STRING);
		two = ServletTestHelper.createColumnModel(DispatchServletSingleton.getInstance(), two, adminUserId);
		// Now create a TableEntity with these Columns
		TableEntity table = new TableEntity();
		table.setName("TableEntity2");
		table.setParentId(parent.getId());
		List<String> idList = new LinkedList<String>();
		idList.add(one.getId());
		idList.add(two.getId());
		table.setColumnIds(idList);
		table = ServletTestHelper.createEntity(DispatchServletSingleton.getInstance(), table, adminUserId);
		assertNotNull(table);
		assertNotNull(table.getId());
		TableEntity clone = ServletTestHelper.getEntity(DispatchServletSingleton.getInstance(), TableEntity.class, table.getId(), adminUserId);
		assertNotNull(clone);
		assertEquals(table, clone);
		// Now make sure we can get the list of columns for this entity
		List<ColumnModel> cols = ServletTestHelper.getColumnModelsForTableEntity(DispatchServletSingleton.getInstance(), table.getId(), adminUserId);
		assertNotNull(cols);
		
		// Add some rows to the table.
		RowSet set = new RowSet();
		List<Row> rows = TableModelUtils.createRows(cols, 2);
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(cols));
		set.setTableId(table.getId());
		ServletTestHelper.appendTableRows(DispatchServletSingleton.getInstance(), set, adminUserId);
		
		// Since the worker is not working on the table, this should fail
		try{
			Query query = new Query();
			query.setSql("select * from "+table.getId()+" limit 2");
			ServletTestHelper.tableQuery(DispatchServletSingleton.getInstance(), adminUserId, query);
			fail("This should have failed");
		}catch(TableUnavilableException e){
			TableStatus status = e.getStatus();
			assertNotNull(status);
			assertEquals(TableState.PROCESSING, status.getState());
			assertEquals(KeyFactory.stringToKey(table.getId()).toString(), status.getTableId());
			// expected
			System.out.println(e.getStatus());
		}
		
	}
}
