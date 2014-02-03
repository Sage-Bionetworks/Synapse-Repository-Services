package org.sagebionetworks.table.cluster;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:table-spb.xml" })
public class TableIndexDAOImplTest {

	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	StackConfiguration config;
	// This is not a bean
	TableIndexDAO tableIndexDAO;
	
	String tableId;
	SimpleJdbcTemplate connection;
	
	@Before
	public void before(){
		// Only run this test if the table feature is enabled.
		Assume.assumeTrue(config.getTableEnabled());
		tableIndexDAO = new TableIndexDAOImpl();
		tableId = "syn123";
		// First get a connection for this table
		connection = tableConnectionFactory.getConnection(tableId);
		assertNotNull(connection);
	}
	
	@After
	public void after(){
		// Drop the table
		if(connection != null && tableId != null && tableIndexDAO != null){
			tableIndexDAO.deleteTable(connection, tableId);
		}
	}
	
	@Test
	public void testGetCurrentTableColumnsDoesNotExist(){
		// There should not be any columns for this table as it does not exist
		List<String> names = tableIndexDAO.getCurrentTableColumns(connection, tableId);
		assertEquals(null, names);
	}
	
	@Test
	public void testCRUD(){
		// Create a Simple table with only a few columns
		List<ColumnModel> allTypes = TableModelUtils.createOneOfEachType();
		// Create the table
		boolean updated = tableIndexDAO.createOrUpdateTable(connection, allTypes, tableId);
		assertTrue("The table should not have existed so update should be true",updated);
		updated = tableIndexDAO.createOrUpdateTable(connection, allTypes, tableId);
		assertFalse("The table already existed in that state so it should not have been updated",updated);
		// Now we should be able to see the columns that were created
		List<String> names = tableIndexDAO.getCurrentTableColumns(connection, tableId);
		// There should be a column for each column in the schema plus one ROW_ID and one ROW_VERSION.
		assertEquals(allTypes.size() + 2, names.size());
		// Now remove a column and update the table
		allTypes.remove(0);
		tableIndexDAO.createOrUpdateTable(connection, allTypes, tableId);
		// Now we should be able to see the columns that were created
		names = tableIndexDAO.getCurrentTableColumns(connection, tableId);
		// There should be a column for each column in the schema plus one ROW_ID and one ROW_VERSION.
		assertEquals(allTypes.size() + 2, names.size());
		// Now add a column
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.BOOLEAN);
		cm.setId("9");
		allTypes.add(cm);
		tableIndexDAO.createOrUpdateTable(connection, allTypes, tableId);
		// Now we should be able to see the columns that were created
		names = tableIndexDAO.getCurrentTableColumns(connection, tableId);
		// There should be a column for each column in the schema plus one ROW_ID and one ROW_VERSION.
		assertEquals(allTypes.size() + 2, names.size());
		// Now delete the table
		assertTrue(tableIndexDAO.deleteTable(connection, tableId));
		names = tableIndexDAO.getCurrentTableColumns(connection, tableId);
		assertEquals(null, names);
	}
	
}
