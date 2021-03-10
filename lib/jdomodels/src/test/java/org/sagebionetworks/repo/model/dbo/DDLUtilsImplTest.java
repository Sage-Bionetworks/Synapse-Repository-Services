package org.sagebionetworks.repo.model.dbo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DDLUtilsImplTest {
	
	@Autowired
	DDLUtils ddlUtils;
	
	String tableName = "EXAMPLE_TEST";
	String ddlFile = "Example.sql";
	

	@Test
	public void testValidateTableExists() throws IOException{
		// Make sure we start without the table
		ddlUtils.dropTable(tableName);
		// the first time this is called the table should not exist.
		boolean result = ddlUtils.validateTableExists(new DBOExample().getTableMapping());
		assertFalse(result);
		// the second time the table should already exist
		result = ddlUtils.validateTableExists(new DBOExample().getTableMapping());
		assertTrue(result);
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
	
	@Test
	public void testremoveSQLComments() {
		String input = "/* this is * within a comment */ This is not in a comment";
		String expected = " This is not in a comment";
		// call under test
		String result = DDLUtilsImpl.removeSQLComments(input);
		assertEquals(expected, result);
	}
	
	@Test
	public void testRemoveSQLCommentsWithMultipleComments() {
		String input = "/* first comment */a/*second*comment*/b";
		String expected = "ab";
		// call under test
		String result = DDLUtilsImpl.removeSQLComments(input);
		assertEquals(expected, result);
	}
	
	@Test
	public void testremoveSQLCommentsWithStartSlash() {
		String input = "/";
		String expected = "/";
		// call under test
		String result = DDLUtilsImpl.removeSQLComments(input);
		assertEquals(expected, result);
	}
	
	@Test
	public void testremoveSQLCommentsWithEndStar() {
		String input = "/* start but not finish *";
		String expected = "";
		// call under test
		String result = DDLUtilsImpl.removeSQLComments(input);
		assertEquals(expected, result);
	}
	
}
