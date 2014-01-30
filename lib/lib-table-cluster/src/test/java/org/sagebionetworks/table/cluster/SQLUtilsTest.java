package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;

public class SQLUtilsTest {
	
	List<ColumnModel> simpleSchema;
	
	@Before
	public void before(){
		simpleSchema = new LinkedList<ColumnModel>();
		ColumnModel col = new ColumnModel();
		col.setColumnType(ColumnType.LONG);
		col.setDefaultValue(null);
		col.setId("456");
		simpleSchema.add(col);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateTableSQLNullSchema(){
		// cannot be null
		SQLUtils.createTableSQL(null, "123");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateTableSQLNullTable(){
		// cannot be null
		SQLUtils.createTableSQL(simpleSchema, null);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testCreateTableSQLEmptySchema(){
		// cannot be null
		simpleSchema.clear();
		SQLUtils.createTableSQL(simpleSchema, "123");
	}
	
	@Test
	public void testCreateTableSQL(){
		// Build the create DDL for this table
		String sql = SQLUtils.createTableSQL(simpleSchema, "123");
		assertNotNull(sql);
		// Validate it contains the expected elements
		String expected = "CREATE TABLE IF NOT EXISTS `T123` ( ROW_ID bigint(20) NOT NULL, ROW_VERSION bigint(20) NOT NULL, `C456` bigint(20) DEFAULT NULL, PRIMARY KEY (ROW_ID) )";
		System.out.println(sql);
		assertEquals(expected, sql);
	}
	
	@Test
	public void testGetSQLTypeForColumnTypeString(){
		String expected = "varchar(2000) CHARACTER SET utf8 COLLATE utf8_general_ci";
		String sql = SQLUtils.getSQLTypeForColumnType(ColumnType.STRING);
		assertEquals(expected, sql);
	}
	
	@Test
	public void testGetSQLTypeForColumnTypeLong(){
		String expected = "bigint(20)";
		String sql = SQLUtils.getSQLTypeForColumnType(ColumnType.LONG);
		assertEquals(expected, sql);
	}
	
	@Test
	public void testGetSQLTypeForColumnTypeFileHandle(){
		String expected = "bigint(20)";
		String sql = SQLUtils.getSQLTypeForColumnType(ColumnType.FILEHANDLEID);
		assertEquals(expected, sql);
	}
	
	@Test
	public void testGetSQLTypeForColumnTypeDouble(){
		String expected = "double";
		String sql = SQLUtils.getSQLTypeForColumnType(ColumnType.DOUBLE);
		assertEquals(expected, sql);
	}
	
	@Test
	public void testGetSQLTypeForColumnTypeBoolean(){
		String expected = "boolean";
		String sql = SQLUtils.getSQLTypeForColumnType(ColumnType.BOOLEAN);
		assertEquals(expected, sql);
	}
	 
	@Test
	public void testGetSQLTypeForColumnTypeAllTypes(){
		// We should be able to get sql for each type.
		for(ColumnType type: ColumnType.values()){
			String sql = SQLUtils.getSQLTypeForColumnType(type);
			assertNotNull(sql);
		}
	}
	
	@Test
	public void testGetSQLDefaultsForLong(){
		String expected = "DEFAULT 123";
		String sql = SQLUtils.getSQLDefaultForColumnType(ColumnType.LONG, "123");
		assertEquals(expected, sql);
	}
	
	@Test
	public void testGetSQLDefaultsForFileHandle(){
		String expected = "DEFAULT 123";
		String sql = SQLUtils.getSQLDefaultForColumnType(ColumnType.FILEHANDLEID, "123");
		assertEquals(expected, sql);
	}
	
	@Test
	public void testGetSQLDefaultsForString(){
		String expected = "DEFAULT 'a string'";
		String sql = SQLUtils.getSQLDefaultForColumnType(ColumnType.STRING, "a string");
		assertEquals(expected, sql);
	}
	
	@Test
	public void testGetSQLDefaultsForDouble(){
		String expected = "DEFAULT 1.3888998E-13";
		String sql = SQLUtils.getSQLDefaultForColumnType(ColumnType.DOUBLE, "1.3888998e-13");
		assertEquals(expected, sql);
	}
	
	@Test
	public void testGetSQLDefaultsForAllTypesNull(){
		// We should be able to get sql for each type.
		for(ColumnType type: ColumnType.values()){
			String sql = SQLUtils.getSQLDefaultForColumnType(type, null);
			assertEquals("DEFAULT NULL", sql);
		}
	}
	
	@Test
	public void testGetSQLDefaultsForBooleanTrue(){
		String expected = "DEFAULT 1";
		String sql = SQLUtils.getSQLDefaultForColumnType(ColumnType.BOOLEAN, "true");
		assertEquals(expected, sql);
	}
	
	@Test
	public void testGetSQLDefaultsForBooleanFalse(){
		String expected = "DEFAULT 0";
		String sql = SQLUtils.getSQLDefaultForColumnType(ColumnType.BOOLEAN, "false");
		assertEquals(expected, sql);
	}
	
	@Test
	public void testSQLInjection(){
		String expected = "DEFAULT ''' DROP TABLE FOO '''";
		String sql = SQLUtils.getSQLDefaultForColumnType(ColumnType.STRING, "' DROP TABLE FOO '");
		assertEquals(expected, sql);
	}
	
	@Test
	public void testCreateAllTypes(){
		List<ColumnModel> allTypes = TableModelUtils.createOneOfEachType();
		String sql = SQLUtils.createTableSQL(allTypes, "123");
		assertNotNull(sql);
		System.out.println(sql);
	}
	
	@Test
	public void testCalculateColumnsToAddOverlap(){
		List<ColumnModel> oldSchema = helperCreateColumnsWithIds("1","2","3");
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("0","2","4");
		// For this case we expect 0 and 4 to be added.
		List<ColumnModel> expected = helperCreateColumnsWithIds("0","4");
		List<ColumnModel> toAdd = SQLUtils.calculateColumnsToAdd(oldSchema, newSchema);
		assertEquals(expected, toAdd);
	}
	
	@Test
	public void testCalculateColumnsToAddNoOverlap(){
		List<ColumnModel> oldSchema = helperCreateColumnsWithIds("1","2","3");
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("4","5","6");
		// For this case we expect all new columns to be added
		List<ColumnModel> expected = helperCreateColumnsWithIds("4","5","6");
		List<ColumnModel> toAdd = SQLUtils.calculateColumnsToAdd(oldSchema, newSchema);
		assertEquals(expected, toAdd);
	}
	
	@Test
	public void testCalculateColumnsToAddOldEmpty(){
		List<ColumnModel> oldSchema = helperCreateColumnsWithIds();
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("4","5","6");
		// For this case we expect all new columns to be added
		List<ColumnModel> expected = helperCreateColumnsWithIds("4","5","6");
		List<ColumnModel> toAdd = SQLUtils.calculateColumnsToAdd(oldSchema, newSchema);
		assertEquals(expected, toAdd);
	}
	
	@Test
	public void testCalculateColumnsToAddNewEmpty(){
		List<ColumnModel> oldSchema = helperCreateColumnsWithIds("1","2","3");
		List<ColumnModel> newSchema = helperCreateColumnsWithIds();
		// For this case we expect no columns to be added
		List<ColumnModel> expected = helperCreateColumnsWithIds();
		List<ColumnModel> toAdd = SQLUtils.calculateColumnsToAdd(oldSchema, newSchema);
		assertEquals(expected, toAdd);
	}
	
	@Test
	public void testCalculateColumnsToDropOverlap(){
		List<ColumnModel> oldSchema = helperCreateColumnsWithIds("1","2","3");
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("0","2","4");
		// For this case we expect 1 and 3 to be removed.
		List<ColumnModel> expected = helperCreateColumnsWithIds("1","3");
		List<ColumnModel> toRemove = SQLUtils.calculateColumnsToDrop(oldSchema, newSchema);
		assertEquals(expected, toRemove);
	}
	
	@Test
	public void testCalculateColumnsToDropNoOverlap(){
		List<ColumnModel> oldSchema = helperCreateColumnsWithIds("1","2","3");
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("4","5","6");
		// For this case all old columns should be dropped
		List<ColumnModel> expected = helperCreateColumnsWithIds("1","2","3");
		List<ColumnModel> toRemove = SQLUtils.calculateColumnsToDrop(oldSchema, newSchema);
		assertEquals(expected, toRemove);
	}
	
	@Test
	public void testCalculateColumnsToDropOldEmpty(){
		List<ColumnModel> oldSchema = helperCreateColumnsWithIds();
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("4","5","6");
		// For this case nothing needs to be dropped
		List<ColumnModel> expected = helperCreateColumnsWithIds();
		List<ColumnModel> toRemove = SQLUtils.calculateColumnsToDrop(oldSchema, newSchema);
		assertEquals(expected, toRemove);
	}
	
	@Test
	public void testCalculateColumnsToDropNewEmpty(){
		List<ColumnModel> oldSchema = helperCreateColumnsWithIds("1","2","3");
		List<ColumnModel> newSchema = helperCreateColumnsWithIds();
		// For this case everything needs to be dropped
		List<ColumnModel> expected = helperCreateColumnsWithIds("1","2","3");
		List<ColumnModel> toRemove = SQLUtils.calculateColumnsToDrop(oldSchema, newSchema);
		assertEquals(expected, toRemove);
	}
	
	@Test
	public void testAlterTable(){
		List<ColumnModel> oldSchema = helperCreateColumnsWithIds("1","2","3");
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("0","2","4");
		// This should drop columns 1 & 3 and then add columns 0 & 4
		String sql = SQLUtils.alterTableSql(oldSchema, newSchema, "999");
		assertNotNull(sql);
		String expected = "ALTER TABLE `T999` DROP COLUMN `C1`, DROP COLUMN `C3`, ADD COLUMN `C0` bigint(20) DEFAULT NULL, ADD COLUMN `C4` bigint(20) DEFAULT NULL";
		assertEquals(expected, sql);
	}
	
	
	/**
	 * A helper to create a list of ColumnModels from column model ids.
	 * 
	 * @param values
	 * @return
	 */
	List<ColumnModel> helperCreateColumnsWithIds(String...values){
		List<ColumnModel> list = new LinkedList<ColumnModel>();
		if(values != null){
			for(String value: values){
				ColumnModel cm = new ColumnModel();
				cm.setId(value);
				cm.setColumnType(ColumnType.LONG);
				list.add(cm);
			}
		}
		return list;
	}
}
