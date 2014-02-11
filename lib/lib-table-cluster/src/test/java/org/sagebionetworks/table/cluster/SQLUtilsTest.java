package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import com.hp.hpl.jena.sparql.function.library.e;

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
		String sql = SQLUtils.createTableSQL(simpleSchema, "syn123");
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
	public void testparseValueForDBLong(){
		Long expected = new Long(123);
		Object objectValue = SQLUtils.parseValueForDB(ColumnType.LONG, "123");
		assertEquals(expected, objectValue);
	}
	
	@Test
	public void testparseValueForDBFileHandle(){
		Long expected = new Long(123);
		Object objectValue = SQLUtils.parseValueForDB(ColumnType.FILEHANDLEID, "123");
		assertEquals(expected, objectValue);
	}
	
	@Test
	public void testparseValueForDBDouble(){
		Double expected = new Double(123.456);
		Object objectValue = SQLUtils.parseValueForDB(ColumnType.DOUBLE, "123.456");
		assertEquals(expected, objectValue);
	}
	
	@Test
	public void testparseValueForDBBooleanTrue(){
		Integer expected = new Integer(1);
		Object objectValue = SQLUtils.parseValueForDB(ColumnType.BOOLEAN, "true");
		assertEquals(expected, objectValue);
	}
	
	@Test
	public void testparseValueForDBBooleanFalse(){
		Integer expected = new Integer(0);
		Object objectValue = SQLUtils.parseValueForDB(ColumnType.BOOLEAN, "false");
		assertEquals(expected, objectValue);
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
		String sql = SQLUtils.createTableSQL(allTypes, "syn123");
		assertNotNull(sql);
		System.out.println(sql);
	}
	
	@Test
	public void testCalculateColumnsToAddOverlap(){
		List<String> oldSchema = Arrays.asList("1","2","3");
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("0","2","4");
		// For this case we expect 0 and 4 to be added.
		List<ColumnModel> expected = helperCreateColumnsWithIds("0","4");
		List<ColumnModel> toAdd = SQLUtils.calculateColumnsToAdd(oldSchema, newSchema);
		assertEquals(expected, toAdd);
	}
	
	@Test
	public void testCalculateColumnsToAddNoOverlap(){
		List<String> oldSchema = Arrays.asList("1","2","3");
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("4","5","6");
		// For this case we expect all new columns to be added
		List<ColumnModel> expected = helperCreateColumnsWithIds("4","5","6");
		List<ColumnModel> toAdd = SQLUtils.calculateColumnsToAdd(oldSchema, newSchema);
		assertEquals(expected, toAdd);
	}
	
	@Test
	public void testCalculateColumnsToAddOldEmpty(){
		List<String> oldSchema = Arrays.asList();
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("4","5","6");
		// For this case we expect all new columns to be added
		List<ColumnModel> expected = helperCreateColumnsWithIds("4","5","6");
		List<ColumnModel> toAdd = SQLUtils.calculateColumnsToAdd(oldSchema, newSchema);
		assertEquals(expected, toAdd);
	}
	
	@Test
	public void testCalculateColumnsToAddNewEmpty(){
		List<String> oldSchema = Arrays.asList("1","2","3");
		List<ColumnModel> newSchema = helperCreateColumnsWithIds();
		// For this case we expect no columns to be added
		List<ColumnModel> expected = helperCreateColumnsWithIds();
		List<ColumnModel> toAdd = SQLUtils.calculateColumnsToAdd(oldSchema, newSchema);
		assertEquals(expected, toAdd);
	}
	
	@Test
	public void testCalculateColumnsToDropOverlap(){
		List<String> oldSchema = Arrays.asList("1","2","3");
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("0","2","4");
		// For this case we expect 1 and 3 to be removed.
		List<String> expected = Arrays.asList("1","3");
		List<String> toRemove = SQLUtils.calculateColumnsToDrop(oldSchema, newSchema);
		assertEquals(expected, toRemove);
	}
	
	@Test
	public void testCalculateColumnsToDropNoOverlap(){
		List<String> oldSchema = Arrays.asList("1","2","3");
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("4","5","6");
		// For this case all old columns should be dropped
		List<String> expected = Arrays.asList("1","2","3");
		List<String> toRemove = SQLUtils.calculateColumnsToDrop(oldSchema, newSchema);
		assertEquals(expected, toRemove);
	}
	
	@Test
	public void testCalculateColumnsToDropOldEmpty(){
		List<String> oldSchema = Arrays.asList();
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("4","5","6");
		// For this case nothing needs to be dropped
		List<String> expected = Arrays.asList();
		List<String> toRemove = SQLUtils.calculateColumnsToDrop(oldSchema, newSchema);
		assertEquals(expected, toRemove);
	}
	
	@Test
	public void testCalculateColumnsToDropNewEmpty(){
		List<String> oldSchema = Arrays.asList("1","2","3");
		List<ColumnModel> newSchema = helperCreateColumnsWithIds();
		// For this case everything needs to be dropped
		List<String> expected = Arrays.asList("1","2","3");
		List<String> toRemove = SQLUtils.calculateColumnsToDrop(oldSchema, newSchema);
		assertEquals(expected, toRemove);
	}
	
	@Test
	public void testAlterTable(){
		List<String> oldSchema = Arrays.asList("1","2","3");
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("0","2","4");
		// This should drop columns 1 & 3 and then add columns 0 & 4
		String sql = SQLUtils.alterTableSql(oldSchema, newSchema, "syn999");
		assertNotNull(sql);
		String expected = "ALTER TABLE `T999` DROP COLUMN `C1`, DROP COLUMN `C3`, ADD COLUMN `C0` bigint(20) DEFAULT NULL, ADD COLUMN `C4` bigint(20) DEFAULT NULL";
		assertEquals(expected, sql);
	}
	
	@Test
	public void testAlterTableNoChange(){
		List<String> oldSchema = Arrays.asList("1","2","3");
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("1","2","3");
		// This should drop columns 1 & 3 and then add columns 0 & 4
		String sql = SQLUtils.alterTableSql(oldSchema, newSchema, "syn999");
		assertEquals(null, sql);
	}
	
	@Test
	public void testGetTableNameForId(){
		assertEquals("T123", SQLUtils.getTableNameForId("syn123"));
	}
	
	@Test
	public void testGetColumnNameForId(){
		assertEquals("C456", SQLUtils.getColumnNameForId("456"));
	}
	
	@Test
	public void testConvertColumnNamesToColumnId(){
		// Start with column
		List<String> columnNames = Arrays.asList(SQLUtils.ROW_ID, SQLUtils.ROW_VERSION,"C2","C1");
		List<String> expected = Arrays.asList("2","1");
		List<String> results = SQLUtils.convertColumnNamesToColumnId(columnNames);
		assertEquals(expected, results);
	}
	
	@Test
	public void testCreatOrAlterTableSQLNoChange(){
		List<String> oldSchema = Arrays.asList("1","2","3");
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("1","2","3");
		// When both the old and new are the same there is nothing to do
		String dml = SQLUtils.creatOrAlterTableSQL(oldSchema, newSchema, "syn123");
		assertEquals("When no schema change is needed the DML should be null",null, dml);
	}
	
	@Test
	public void testBuildCreateOrUpdateRowSQL(){
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("0","2","4");
		String result = SQLUtils.buildCreateOrUpdateRowSQL(newSchema, "syn123");
		String expected = "INSERT INTO T123 (ROW_ID, ROW_VERSION, C0, C2, C4) VALUES ( :bRI, :bRV, :C0, :C2, :C4) ON DUPLICATE KEY UPDATE ROW_VERSION = :bRV, C0 = :C0, C2 = :C2, C4 = :C4";
		assertEquals(expected, result);
	}
	
	@Test
	public void testBindParametersForCreateOrUpdate(){
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("1","2","3");
		// This column will be missing in the RowSet so it should get this default value.
		newSchema.get(0).setDefaultValue("456");
		List<ColumnModel> oldSchema = helperCreateColumnsWithIds("0","2","4");
		RowSet set = new RowSet();
		List<Row> rows = new LinkedList<Row>();
		// Set the row IDs
		for(int i=0; i<2; i++){
			Row row = new Row();
			row.setRowId(new Long(i));
			row.setVersionNumber(3L);
			row.setValues(Arrays.asList("111"+i, "222"+i, "333"+i));
			rows.add(row);
		}
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getHeaders(oldSchema));
		set.setTableId("syn123");
		// bind!
		SqlParameterSource[] results = SQLUtils.bindParametersForCreateOrUpdate(set, newSchema);
		assertNotNull(results);
		assertEquals("There should be one mapping for each row in the batch",2, results.length);
		// First row
		assertEquals(new Long(0), results[0].getValue(SQLUtils.ROW_ID_BIND));
		assertEquals(new Long(3), results[0].getValue(SQLUtils.ROW_VERSION_BIND));
		assertEquals(new Long(456), results[0].getValue("C1"));
		assertEquals(new Long(2220), results[0].getValue("C2"));
		assertEquals(null, results[0].getValue("C3"));
		// second
		assertEquals(new Long(456), results[1].getValue("C1"));
		assertEquals(new Long(2221), results[1].getValue("C2"));
		assertEquals(null, results[1].getValue("C3"));
	}
	
	
	@Test
	public void testBindParametersForCreateOrUpdateAllTypes(){
		List<ColumnModel> newSchema = TableModelUtils.createOneOfEachType();
		RowSet set = new RowSet();
		set.setRows(TableModelUtils.createRows(newSchema, 3));
		set.setHeaders(TableModelUtils.getHeaders(newSchema));
		set.setTableId("syn123");
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelUtils.assignRowIdsAndVersionNumbers(set, range);
		// bind!
		SqlParameterSource[] results = SQLUtils.bindParametersForCreateOrUpdate(set, newSchema);
		assertNotNull(results);
		assertEquals("There should be one mapping for each row in the batch",3, results.length);
		// First row
		assertEquals(new Long(100), results[0].getValue(SQLUtils.ROW_ID_BIND));
		assertEquals(new Long(3), results[0].getValue(SQLUtils.ROW_VERSION_BIND));
		assertEquals(new Double(0.0), results[0].getValue("C1"));
		assertEquals(new Long(0), results[0].getValue("C2"));
		assertEquals(new Integer(0), results[0].getValue("C3"));
		assertEquals(new Long(0), results[0].getValue("C4"));
		// second
		assertEquals(new Long(101), results[1].getValue(SQLUtils.ROW_ID_BIND));
		assertEquals(new Long(3), results[1].getValue(SQLUtils.ROW_VERSION_BIND));
		assertEquals(new Double(3.41), results[1].getValue("C1"));
		assertEquals(new Long(1), results[1].getValue("C2"));
		assertEquals(new Integer(1), results[1].getValue("C3"));
		assertEquals(new Long(1), results[1].getValue("C4"));
	}
	
	@Test
	public void testGetRowCountSQL(){
		String expected = "SELECT COUNT(ROW_ID) FROM T123";
		String result = SQLUtils.getCountSQL("123");
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetMaxVersionSQL(){
		String expected = "SELECT MAX(ROW_VERSION) FROM T123";
		String result = SQLUtils.getMaxVersionSQL("123");
		assertEquals(expected, result);
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
