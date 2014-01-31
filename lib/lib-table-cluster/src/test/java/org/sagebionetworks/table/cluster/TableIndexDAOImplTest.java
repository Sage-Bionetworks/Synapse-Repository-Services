package org.sagebionetworks.table.cluster;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
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
	
	@Test
	public void testGetCurrentTableColumnsDoesNotExist(){
		// There should not be any columns for this table as it does not exist
		List<String> names = tableIndexDAO.getCurrentTableColumns(connection, tableId);
		assertEquals(null, names);
	}
	
	@Test
	public void testCreateOrUpdateTable(){
		// Create a Simple table with only a few columns
		List<ColumnModel> allTypes = TableModelUtils.createOneOfEachType();
		assertTrue(tableIndexDAO.createOrUpdateTable(connection, allTypes, tableId));
		// Now we should be able to see the columns that were created
		List<String> names = tableIndexDAO.getCurrentTableColumns(connection, tableId);
		// There should be a column for each column in the schema plus one ROW_ID and one ROW_VERSION.
		assertEquals(allTypes.size() + 2, names.size());
	}
	
}
