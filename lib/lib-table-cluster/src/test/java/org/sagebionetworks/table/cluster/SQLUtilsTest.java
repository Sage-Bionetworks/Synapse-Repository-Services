package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.table.cluster.SQLUtils.TableType;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import com.google.common.collect.Lists;




public class SQLUtilsTest {
	
	List<ColumnModel> simpleSchema;
	
	@Before
	public void before(){
		simpleSchema = new LinkedList<ColumnModel>();
		ColumnModel col = new ColumnModel();
		col.setColumnType(ColumnType.INTEGER);
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
		String expected = "CREATE TABLE IF NOT EXISTS `T123` ( ROW_ID bigint(20) NOT NULL, ROW_VERSION bigint(20) NOT NULL, `_C456_` bigint(20) DEFAULT NULL, PRIMARY KEY (ROW_ID) )";
		System.out.println(sql);
		assertEquals(expected, sql);
	}
	
	@Test
	public void testGetSQLTypeForColumnTypeString(){
		String expected = "varchar(13) CHARACTER SET utf8 COLLATE utf8_general_ci";
		String sql = SQLUtils.getSQLTypeForColumnType(ColumnType.STRING, 13L);
		assertEquals(expected, sql);
	}
	
	@Test
	public void testGetSQLTypeForColumnTypeLink() {
		String expected = "varchar(13) CHARACTER SET utf8 COLLATE utf8_general_ci";
		String sql = SQLUtils.getSQLTypeForColumnType(ColumnType.LINK, 13L);
		assertEquals(expected, sql);
	}

	@Test
	public void testGetSQLTypeForColumnTypeLong(){
		String expected = "bigint(20)";
		String sql = SQLUtils.getSQLTypeForColumnType(ColumnType.INTEGER, null);
		assertEquals(expected, sql);
	}
	
	@Test
	public void testGetSQLTypeForColumnTypeFileHandle(){
		String expected = "bigint(20)";
		String sql = SQLUtils.getSQLTypeForColumnType(ColumnType.FILEHANDLEID, null);
		assertEquals(expected, sql);
	}
	
	@Test
	public void testGetSQLTypeForColumnTypeDate() {
		String expected = "bigint(20)";
		String sql = SQLUtils.getSQLTypeForColumnType(ColumnType.DATE, null);
		assertEquals(expected, sql);
	}

	@Test
	public void testGetSQLTypeForColumnTypeDouble(){
		String expected = "double";
		String sql = SQLUtils.getSQLTypeForColumnType(ColumnType.DOUBLE, null);
		assertEquals(expected, sql);
	}
	
	@Test
	public void testGetSQLTypeForColumnTypeBoolean(){
		String expected = "boolean";
		String sql = SQLUtils.getSQLTypeForColumnType(ColumnType.BOOLEAN, null);
		assertEquals(expected, sql);
	}
	 
	@Test
	public void testGetSQLTypeForColumnTypeAllTypes(){
		// We should be able to get sql for each type.
		for(ColumnType type: ColumnType.values()){
			Long size = null;
			if(ColumnType.STRING == type){
				size = 100L;
			} else if (type == ColumnType.LINK) {
				size = 135L;
			}
			String sql = SQLUtils.getSQLTypeForColumnType(type, size);
			assertNotNull(sql);
		}
	}
	
	@Test
	public void testparseValueForDBLong(){
		Long expected = new Long(123);
		Object objectValue = SQLUtils.parseValueForDB(ColumnType.INTEGER, "123");
		assertEquals(expected, objectValue);
	}
	
	@Test
	public void testparseValueForDBFileHandle(){
		Long expected = new Long(123);
		Object objectValue = SQLUtils.parseValueForDB(ColumnType.FILEHANDLEID, "123");
		assertEquals(expected, objectValue);
	}
	
	@Test
	public void testparseValueForDBEntityId() {
		String expected = "syn123.3";
		Object objectValue = SQLUtils.parseValueForDB(ColumnType.ENTITYID, "syn123.3");
		assertEquals(expected, objectValue);
	}

	@Test
	public void testparseValueForDBDate() {
		Long expected = new Long(123);
		Object objectValue = SQLUtils.parseValueForDB(ColumnType.DATE, "123");
		assertEquals(expected, objectValue);
	}

	@Test
	public void testparseValueForDBDateString() {
		Long expected = new Long(123);
		Object objectValue = SQLUtils.parseValueForDB(ColumnType.DATE, "1970-1-1 00:00:00.123");
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
		Boolean expected = new Boolean(true);
		Object objectValue = SQLUtils.parseValueForDB(ColumnType.BOOLEAN, "true");
		assertEquals(expected, objectValue);
	}
	
	@Test
	public void testparseValueForDBBooleanFalse(){
		Boolean expected = new Boolean(false);
		Object objectValue = SQLUtils.parseValueForDB(ColumnType.BOOLEAN, "false");
		assertEquals(expected, objectValue);
	}
	
	
	@Test
	public void testGetSQLDefaultsForLong(){
		String expected = "DEFAULT 123";
		String sql = SQLUtils.getSQLDefaultForColumnType(ColumnType.INTEGER, "123");
		assertEquals(expected, sql);
	}
	
	@Test
	public void testGetSQLDefaultsForFileHandle(){
		String expected = "DEFAULT 123";
		String sql = SQLUtils.getSQLDefaultForColumnType(ColumnType.FILEHANDLEID, "123");
		assertEquals(expected, sql);
	}
	
	@Test
	public void testGetSQLDefaultsForEntityId() {
		String expected = "DEFAULT 'syn123.3'";
		String sql = SQLUtils.getSQLDefaultForColumnType(ColumnType.ENTITYID, "syn123.3");
		assertEquals(expected, sql);
	}

	@Test
	public void testGetSQLDefaultsForDATE() {
		String expected = "DEFAULT 123";
		String sql = SQLUtils.getSQLDefaultForColumnType(ColumnType.DATE, "123");
		assertEquals(expected, sql);
	}

	@Test
	public void testGetSQLDefaultsForString(){
		String expected = "DEFAULT 'a string'";
		String sql = SQLUtils.getSQLDefaultForColumnType(ColumnType.STRING, "a string");
		assertEquals(expected, sql);
	}
	
	@Test
	public void testGetSQLDefaultsForLink() {
		String expected = "DEFAULT 'a link'";
		String sql = SQLUtils.getSQLDefaultForColumnType(ColumnType.LINK, "a link");
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
		String expected = "DEFAULT true";
		String sql = SQLUtils.getSQLDefaultForColumnType(ColumnType.BOOLEAN, "true");
		assertEquals(expected, sql);
	}
	
	@Test
	public void testGetSQLDefaultsForBooleanFalse(){
		String expected = "DEFAULT false";
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
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		String sql = SQLUtils.createTableSQL(allTypes, "syn123");
		assertNotNull(sql);
		System.out.println(sql);
	}
	
	@Test
	public void testCalculateColumnsToAddOverlap(){
		List<String> oldSchema = Arrays.asList("_C1_", "_C2_", "_C3_", "whatever");
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("0", "2", "4");
		// For this case we expect 0 and 4 to be added.
		List<ColumnModel> expected = helperCreateColumnsWithIds("0", "4");
		List<ColumnModel> toAdd = Lists.newArrayList();
		List<String> toDrop = Lists.newArrayList();
		SQLUtils.calculateColumnsToAddOrDrop(oldSchema, newSchema, toAdd, toDrop);
		assertEquals(expected.toString(), toAdd.toString());
		assertEquals(Lists.newArrayList("_C1_", "_C3_", "whatever"), toDrop);
	}
	
	@Test
	public void testCalculateColumnsToAddNoOverlap(){
		List<String> oldSchema = Arrays.asList("_C1_", "_C2_", "_C3_", "whatever");
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("4","5","6");
		// For this case we expect all new columns to be added
		List<ColumnModel> expected = helperCreateColumnsWithIds("4","5","6");
		List<ColumnModel> toAdd = Lists.newArrayList();
		List<String> toDrop = Lists.newArrayList();
		SQLUtils.calculateColumnsToAddOrDrop(oldSchema, newSchema, toAdd, toDrop);
		assertEquals(expected, toAdd);
		assertEquals(Lists.newArrayList("_C1_", "_C2_", "_C3_", "whatever"), toDrop);
	}
	
	@Test
	public void testCalculateColumnsToAddOldEmpty(){
		List<String> oldSchema = Arrays.asList();
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("4","5","6");
		// For this case we expect all new columns to be added
		List<ColumnModel> expected = helperCreateColumnsWithIds("4","5","6");
		List<ColumnModel> toAdd = Lists.newArrayList();
		List<String> toDrop = Lists.newArrayList();
		SQLUtils.calculateColumnsToAddOrDrop(oldSchema, newSchema, toAdd, toDrop);
		assertEquals(expected, toAdd);
		assertEquals(Lists.newArrayList(), toDrop);
	}
	
	@Test
	public void testCalculateColumnsToAddNewEmpty(){
		List<String> oldSchema = Arrays.asList("_C1_", "_C2_", "_C3_", "whatever");
		List<ColumnModel> newSchema = helperCreateColumnsWithIds();
		// For this case we expect no columns to be added
		List<ColumnModel> expected = helperCreateColumnsWithIds();
		List<ColumnModel> toAdd = Lists.newArrayList();
		List<String> toDrop = Lists.newArrayList();
		SQLUtils.calculateColumnsToAddOrDrop(oldSchema, newSchema, toAdd, toDrop);
		assertEquals(expected, toAdd);
		assertEquals(Lists.newArrayList("_C1_", "_C2_", "_C3_", "whatever"), toDrop);
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
		List<String> oldSchema = Arrays.asList("_C1_", "_C2_", "_C3_");
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("0","2","4");
		// This should drop columns 1 & 3 and then add columns 0 & 4
		String sql = SQLUtils.alterTableSql(oldSchema, newSchema, "syn999");
		assertNotNull(sql);
		String expected = "ALTER TABLE `T999` DROP COLUMN `_C1_`, DROP COLUMN `_C3_`, ADD COLUMN `_C0_` bigint(20) DEFAULT NULL, ADD COLUMN `_C4_` bigint(20) DEFAULT NULL";
		assertEquals(expected, sql);
	}
	
	@Test
	public void testAlterTableNoChange(){
		List<String> oldSchema = Arrays.asList("_C1_", "_C2_", "_C3_");
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("1","2","3");
		// This should drop columns 1 & 3 and then add columns 0 & 4
		String sql = SQLUtils.alterTableSql(oldSchema, newSchema, "syn999");
		assertEquals(null, sql);
	}
	
	@Test
	public void testGetTableNameForId(){
		assertEquals("T123", SQLUtils.getTableNameForId("syn123", TableType.INDEX));
		assertEquals("T123S", SQLUtils.getTableNameForId("syn123", TableType.STATUS));
		assertEquals("T123CR", SQLUtils.getTableNameForId("syn123", TableType.CURRENT_ROW));
	}
	
	@Test
	public void testGetColumnNameForId(){
		assertEquals("_C456_", SQLUtils.getColumnNameForId("456"));
	}
	
	@Test
	public void testGetColumnNames() {
		assertEquals("", createColNames());
		assertEquals("_C0_", createColNames(ColumnType.STRING));
		assertEquals("_C0_,_DBL_C0_", createColNames(ColumnType.DOUBLE));
		assertEquals("_C0_,_DBL_C0_,_C1_", createColNames(ColumnType.DOUBLE, ColumnType.STRING));
		assertEquals("_C0_,_C1_,_DBL_C1_", createColNames(ColumnType.STRING, ColumnType.DOUBLE));
		assertEquals("_C0_,_C1_,_DBL_C1_,_C2_,_DBL_C2_", createColNames(ColumnType.STRING, ColumnType.DOUBLE, ColumnType.DOUBLE));
		assertEquals("_C0_,_C1_,_DBL_C1_,_C2_", createColNames(ColumnType.STRING, ColumnType.DOUBLE, ColumnType.STRING));
		assertEquals("_C0_,_DBL_C0_,_C1_,_C2_,_DBL_C2_", createColNames(ColumnType.DOUBLE, ColumnType.STRING, ColumnType.DOUBLE));
	}

	private String createColNames(ColumnType... types) {
		List<ColumnModel> models = Lists.newArrayList();
		for (int i = 0; i < types.length; i++) {
			ColumnType columnType = types[i];
			models.add(TableModelTestUtils.createColumn((long) i, "x", columnType));
		}
		return StringUtils.join(Lists.newArrayList(SQLUtils.getColumnNames(models)), ",");
	}

	@Test
	public void testAppendDoubleCase() {
		StringBuilder sb = new StringBuilder();
		SQLUtils.appendDoubleCase(TableModelTestUtils.createColumn(3), "", null, true, sb);
		assertEquals("CASE WHEN _DBL_C3_ IS NULL THEN _C3_ ELSE _DBL_C3_ END AS _C3_", sb.toString());
	}

	@Test
	public void testAppendDoubleCaseWithSubname() {
		StringBuilder sb = new StringBuilder();
		SQLUtils.appendDoubleCase(TableModelTestUtils.createColumn(3), "sub_", null, true, sb);
		assertEquals("CASE WHEN _DBLsub__C3_ IS NULL THEN sub__C3_ ELSE _DBLsub__C3_ END AS sub__C3_", sb.toString());
	}

	@Test
	public void testAppendDoubleCaseInOrderBy() {
		StringBuilder sb = new StringBuilder();
		SQLUtils.appendDoubleCase(TableModelTestUtils.createColumn(3), "", "table", false, sb);
		assertEquals("table._C3_", sb.toString());
	}

	@Test
	public void testAppendDoubleWithSubnameCaseInOrderBy() {
		StringBuilder sb = new StringBuilder();
		SQLUtils.appendDoubleCase(TableModelTestUtils.createColumn(3), "sub_", "table", false, sb);
		assertEquals("table.sub__C3_", sb.toString());
	}

	@Test
	public void testCreatOrAlterTableSQLNoChange(){
		List<String> oldSchema = Arrays.asList("_C1_", "_C2_", "_C3_");
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("1","2","3");
		// When both the old and new are the same there is nothing to do
		String dml = SQLUtils.creatOrAlterTableSQL(oldSchema, newSchema, "syn123");
		assertEquals("When no schema change is needed the DML should be null",null, dml);
	}
	
	@Test
	public void testBuildCreateOrUpdateRowSQL(){
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("0","2","4");
		String result = SQLUtils.buildCreateOrUpdateRowSQL(newSchema, "syn123");
		String expected = "INSERT INTO T123 (ROW_ID, ROW_VERSION, _C0_, _C2_, _C4_) VALUES ( :bRI, :bRV, :_C0_, :_C2_, :_C4_) ON DUPLICATE KEY UPDATE ROW_VERSION = VALUES(ROW_VERSION), _C0_ = VALUES(_C0_), _C2_ = VALUES(_C2_), _C4_ = VALUES(_C4_)";
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
		assertEquals(new Long(456), results[0].getValue("_C1_"));
		assertEquals(new Long(2220), results[0].getValue("_C2_"));
		assertEquals(null, results[0].getValue("_C3_"));
		// second
		assertEquals(new Long(456), results[1].getValue("_C1_"));
		assertEquals(new Long(2221), results[1].getValue("_C2_"));
		assertEquals(null, results[1].getValue("_C3_"));
	}
	
	
	@Test
	public void testBindParametersForCreateOrUpdateAllTypes(){
		List<ColumnModel> newSchema = TableModelTestUtils.createOneOfEachType();
		RowSet set = new RowSet();
		set.setRows(TableModelTestUtils.createRows(newSchema, 3));
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
		assertEquals("string0", results[0].getValue("_C0_"));
		assertEquals(new Double(341003.12), results[0].getValue("_C1_"));
		assertEquals(null, results[0].getValue("_DBL_C1_"));
		assertEquals(new Long(203000), results[0].getValue("_C2_"));
		assertEquals(new Boolean(false), results[0].getValue("_C3_"));
		assertEquals(new Long(404000), results[0].getValue("_C4_"));
		assertEquals(new Long(505000), results[0].getValue("_C5_"));
		assertEquals("syn606000.607000", results[0].getValue("_C6_"));
		// second
		assertEquals(new Long(101), results[1].getValue(SQLUtils.ROW_ID_BIND));
		assertEquals(new Long(3), results[1].getValue(SQLUtils.ROW_VERSION_BIND));
		assertEquals("string1", results[1].getValue("_C0_"));
		assertEquals(new Double(341006.53), results[1].getValue("_C1_"));
		assertEquals(null, results[0].getValue("_DBL_C1_"));
		assertEquals(new Long(203001), results[1].getValue("_C2_"));
		assertEquals(new Boolean(true), results[1].getValue("_C3_"));
		assertEquals(new Long(404001), results[1].getValue("_C4_"));
		assertEquals(new Long(505001), results[1].getValue("_C5_"));
		assertEquals("syn606001.607001", results[1].getValue("_C6_"));
	}
	
	@Test
	public void testGetRowCountSQL(){
		String expected = "SELECT COUNT(ROW_ID) FROM T123";
		String result = SQLUtils.getCountSQL("123");
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetMaxVersionSQL(){
		String expected = "SELECT ROW_VERSION FROM T123S";
		String result = SQLUtils.getStatusMaxVersionSQL("123");
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
				cm.setColumnType(ColumnType.INTEGER);
				list.add(cm);
			}
		}
		return list;
	}
	
}
