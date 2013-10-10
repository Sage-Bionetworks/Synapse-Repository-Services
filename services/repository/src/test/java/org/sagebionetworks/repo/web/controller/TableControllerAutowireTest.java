package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableControllerAutowireTest {
	
	private Entity parent;
	private final String testUser = AuthorizationConstants.ADMIN_USER_NAME;
	
	@Before
	public void before() throws Exception{
		parent = new Project();
		parent.setName(UUID.randomUUID().toString());
		parent = ServletTestHelper.createEntity(DispatchServletSingleton.getInstance(), parent, testUser);
		Assert.assertNotNull(parent);
	}
	
	@After
	public void after(){
		if(parent != null){
			try {
				ServletTestHelper.deleteEntity(DispatchServletSingleton.getInstance(), Project.class, parent.getId(), testUser);
			} catch (Exception e) {} 
		}
	}
	
	@Test
	public void testCreateGetDeleteColumnModel() throws ServletException, Exception{
		ColumnModel cm = new ColumnModel();
		cm.setName("TableControllerAutowireTest One");
		cm.setColumnType(ColumnType.STRING);
		// Save the column
		cm = ServletTestHelper.createColumnModel(DispatchServletSingleton.getInstance(), cm, testUser);
		assertNotNull(cm);
		assertNotNull(cm.getId());
		// Make sure we can get it
		ColumnModel clone = ServletTestHelper.getColumnModel(DispatchServletSingleton.getInstance(), cm.getId(), testUser);
		assertEquals(cm, clone);
	}
	
	@Test
	public void testCreateTableEntity() throws Exception{
		// Create a table with two ColumnModels
		// one
		ColumnModel one = new ColumnModel();
		one.setName("one");
		one.setColumnType(ColumnType.STRING);
		one = ServletTestHelper.createColumnModel(DispatchServletSingleton.getInstance(), one, testUser);
		// two
		ColumnModel two = new ColumnModel();
		two.setName("two");
		two.setColumnType(ColumnType.STRING);
		two = ServletTestHelper.createColumnModel(DispatchServletSingleton.getInstance(), two, testUser);
		// Now create a TableEntity with these Columns
		TableEntity table = new TableEntity();
		table.setName("TableEntity");
		table.setParentId(parent.getId());
		List<String> idList = new LinkedList<String>();
		idList.add(one.getId());
		idList.add(two.getId());
		table.setColumnIds(idList);
		table = ServletTestHelper.createEntity(DispatchServletSingleton.getInstance(), table, testUser);
		assertNotNull(table);
		assertNotNull(table.getId());
		TableEntity clone = ServletTestHelper.getEntity(DispatchServletSingleton.getInstance(), TableEntity.class, table.getId(), testUser);
		assertNotNull(clone);
		assertEquals(table, clone);
	}

}
