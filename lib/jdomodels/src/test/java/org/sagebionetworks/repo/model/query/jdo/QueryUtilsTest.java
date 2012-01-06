package org.sagebionetworks.repo.model.query.jdo;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.sagebionetworks.repo.model.query.FieldType;

public class QueryUtilsTest {


	@Test
	public void testGetTableNameForFieldType() {
		// Only test for the types that are valid
		// String
		String tableName = QueryUtils.getTableNameForFieldType(FieldType.STRING_ATTRIBUTE);
		assertNotNull(tableName);
		// Date
		tableName = QueryUtils.getTableNameForFieldType(FieldType.DATE_ATTRIBUTE);
		assertNotNull(tableName);
		// Long
		tableName = QueryUtils.getTableNameForFieldType(FieldType.LONG_ATTRIBUTE);
		assertNotNull(tableName);
		// Double
		tableName = QueryUtils.getTableNameForFieldType(FieldType.DOUBLE_ATTRIBUTE);
		assertNotNull(tableName);
	}
}
