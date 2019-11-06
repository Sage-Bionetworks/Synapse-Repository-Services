package org.sagebionetworks.repo.model.dbo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test for the DDLUtilsImpl
 * @author John
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DDLUtilsImplTest {
	
	@Autowired
	DDLUtils ddlUtils;
	
	String tableName = "EXAMPLE_TEST";
	String ddlFile = "Example.sql";
	
	@After
	public void after() {
		// Do not drop the table, causes problem if test executed between DOExampleTest and DBOAnnotatedExampleTest
		// ddlUtils.dropTable(tableName);
	}

	@Test
	public void testValidateTableExists() throws IOException{
		// Make sure we start without the table
		ddlUtils.dropTable(tableName);
		// the first time this is called the table should not exist.
		boolean result = ddlUtils.validateTableExists(new DBOExample().getTableMapping());
		assertFalse("The first time we called this method it should have created the table", result);
		// the second time the table should already exist
		result = ddlUtils.validateTableExists(new DBOExample().getTableMapping());
		assertTrue("The second time we called this method, the table should have already existed", result);
	}
	
	@Test
	public void testCreateFunctionAndDoesExist() throws IOException{
		String functionName = "new_function";
		ddlUtils.dropFunction(functionName);
		assertFalse(ddlUtils.doesFunctionExist(functionName));
		String functionDef = "CREATE FUNCTION `"+functionName+"` () RETURNS INTEGER NO SQL BEGIN RETURN 1; END";
		ddlUtils.createFunction(functionDef);
		assertTrue(ddlUtils.doesFunctionExist(functionName));
		ddlUtils.dropFunction(functionName);
		assertFalse(ddlUtils.doesFunctionExist(functionName));
	}
}
