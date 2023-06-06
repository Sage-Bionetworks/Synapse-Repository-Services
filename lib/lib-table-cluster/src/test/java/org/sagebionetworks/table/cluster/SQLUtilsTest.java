package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.ObjectAnnotationDTO;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.table.cluster.SQLUtils.TableIndexType;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.model.Grouping;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.table.model.SparseRow;
import org.sagebionetworks.util.doubles.AbstractDouble;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class SQLUtilsTest {

	@Mock
	PreparedStatement mockPreparedStatement;

	List<ColumnModel> simpleSchema;

	Map<Long, ColumnModel> schemaIdToModelMap;

	ObjectAnnotationDTO annotationDto;

	boolean useDepricatedUtf8ThreeBytes;
	
	String tableName;

	IdAndVersion tableId;
	Long viewId;

	private String LINE_SEPERATOR =  System.getProperty("line.separator");
	
	@BeforeEach
	public void before(){
		simpleSchema = new LinkedList<ColumnModel>();
		ColumnModel col = new ColumnModel();
		col.setColumnType(ColumnType.INTEGER);
		col.setDefaultValue(null);
		col.setId("456");
		col.setName("colOne");
		simpleSchema.add(col);
		col = new ColumnModel();
		col.setColumnType(ColumnType.STRING);
		col.setDefaultValue(null);
		col.setId("789");
		col.setMaximumSize(300L);
		col.setName("coTwo");
		simpleSchema.add(col);
		col = new ColumnModel();
		col.setColumnType(ColumnType.STRING);
		col.setDefaultValue(null);
		col.setId("123");
		col.setMaximumSize(150L);
		col.setName("colThree");
		simpleSchema.add(col);

		schemaIdToModelMap = TableModelUtils.createIDtoColumnModelMap(simpleSchema);
		ObjectDataDTO data = new ObjectDataDTO().setId(1L).setVersion(2L);
		annotationDto = new ObjectAnnotationDTO(data);
		annotationDto.setObjectId(123L);
		annotationDto.setType(AnnotationType.STRING);
		annotationDto.setKey("someKey");
		annotationDto.setValue("someString");

		useDepricatedUtf8ThreeBytes = false;
		tableName = "T999";

		tableId = IdAndVersion.parse("syn999");
		viewId = 123L;
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
	public void testparseValueForDBUserId(){
		Long expected = new Long(123);
		Object objectValue = SQLUtils.parseValueForDB(ColumnType.USERID, "123");
		assertEquals(expected, objectValue);
	}

	@Test
	public void testparseValueForDBEntityId() {
		Long expected = new Long(123);
		Object objectValue = SQLUtils.parseValueForDB(ColumnType.ENTITYID, "syn123");
		assertEquals(expected, objectValue);
	}
	
	@Test
	public void testparseValueForDBEvaluationId() {
		Long expected = new Long(123);
		Object objectValue = SQLUtils.parseValueForDB(ColumnType.EVALUATIONID, "123");
		assertEquals(expected, objectValue);
	}
	
	@Test
	public void testparseValueForDBSubmissionId() {
		Long expected = new Long(123);
		Object objectValue = SQLUtils.parseValueForDB(ColumnType.SUBMISSIONID, "123");
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
	public void testparseValueForDBLargeText(){
		String expected = "this is some text";
		Object objectValue = SQLUtils.parseValueForDB(ColumnType.LARGETEXT, "this is some text");
		assertEquals(expected, objectValue);
	}


	@Test
	public void testGetTableNameForId(){
		assertEquals("T999", SQLUtils.getTableNameForId(tableId, TableIndexType.INDEX));
		assertEquals("T999S", SQLUtils.getTableNameForId(tableId, TableIndexType.STATUS));
	}

	@Test
	public void testGetTableNameForIdWithVersion(){
		tableId = IdAndVersion.parse("syn123.456");
		assertEquals("T123_456", SQLUtils.getTableNameForId(tableId, TableIndexType.INDEX));
		assertEquals("T123_456S", SQLUtils.getTableNameForId(tableId, TableIndexType.STATUS));
	}
	
	@Test
	public void testGetTableNameForIdWithNegativeId(){
		tableId = IdAndVersion.newBuilder().setId(-999L).build();
		assertEquals("T__999", SQLUtils.getTableNameForId(tableId, TableIndexType.INDEX));
		assertEquals("T__999S", SQLUtils.getTableNameForId(tableId, TableIndexType.STATUS));
	}

	@Test
	public void testGetColumnNameForId(){
		assertEquals("_C456_", SQLUtils.getColumnNameForId("456"));
	}


	@Test
	public void testGetUnnestedColumnNameForId_nullColumnId(){
		assertThrows(IllegalArgumentException.class, () -> {
			SQLUtils.getUnnestedColumnNameForId(null);
		});
	}

	@Test
	public void testGetUnnestedColumnNameForId(){
		assertEquals("_C12345__UNNEST",
			SQLUtils.getUnnestedColumnNameForId("12345"));
	}

	@Test
	public void testGetRowIdRefColumnNameForId_nullColumnId(){
		assertThrows(IllegalArgumentException.class, () -> {
			SQLUtils.getRowIdRefColumnNameForId(null);
		});
	}

	@Test
	public void testGetRowIdRefColumnNameForId(){
		assertEquals("ROW_ID_REF_C12345_",
				SQLUtils.getRowIdRefColumnNameForId("12345"));
	}

	@Test
	public void testGetTableNameForMultiValueColumnMaterlization_nullId(){
		assertThrows(IllegalArgumentException.class, ()-> {
			SQLUtils.getTableNameForMultiValueColumnIndex(null, null);
		});
	}

	@Test
	public void testGetTableNameForMultiValueColumnMaterlization_nullColumnModelId(){
		assertThrows(IllegalArgumentException.class, ()-> {
			SQLUtils.getTableNameForMultiValueColumnIndex(tableId, null );
		});
	}

	@Test
	public void testGetTableNameForMultiValueColumnIndex(){
		String temp = SQLUtils.getTableNameForMultiValueColumnIndex(tableId, "123", true);
		assertEquals("TEMPT999_INDEX_C123_", temp);
	}
	
	@Test
	public void testGetTableNamePrefixForMultiValueColumns_Id_NoVersion(){
		String tableName = SQLUtils.getTableNamePrefixForMultiValueColumns(IdAndVersion.parse("syn123"), false);
		assertEquals("T123_INDEX", tableName);
	}

	@Test
	public void testGetTableNamePrefixForMultiValueColumns_Id_WithVersion(){
		String tableName = SQLUtils.getTableNamePrefixForMultiValueColumns(IdAndVersion.parse("syn123.456"), false);
		assertEquals("T123_456_INDEX", tableName);
	}
	
	@Test
	public void testGetTableNamePrefixForMultiValueColumnsWithNegativeId(){
		tableId = IdAndVersion.newBuilder().setId(-123L).build();
		String tableName = SQLUtils.getTableNamePrefixForMultiValueColumns(tableId, false);
		assertEquals("T__123_INDEX", tableName);
	}

	@Test
	public void testGetTableNamePrefixForMultiValueColumns_alterTempTrue(){
		String tableName = SQLUtils.getTableNamePrefixForMultiValueColumns(IdAndVersion.parse("syn123"), true);
		assertEquals("TEMPT123_INDEX", tableName);
	}

	@Test
	public void testGetTableNameForMultiValueColumnMaterlization_Id_NoVersion(){
		String tableName = SQLUtils.getTableNameForMultiValueColumnIndex(IdAndVersion.parse("syn123"), "456");
		assertEquals("T123_INDEX_C456_", tableName);
	}

	@Test
	public void testGetTableNameForMultiValueColumnMaterlization_Id_WithVersion(){
		String tableName = SQLUtils.getTableNameForMultiValueColumnIndex(IdAndVersion.parse("syn123.456"), "456");
		assertEquals("T123_456_INDEX_C456_", tableName);
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
	public void testBuildCreateOrUpdateRowSQL(){
		List<ColumnModel> newSchema = helperCreateColumnsWithIds("0","2","4");
		String result = SQLUtils.buildCreateOrUpdateRowSQL(newSchema, tableId);
		String expected = "INSERT INTO T999 (ROW_ID, ROW_VERSION, _C0_, _C2_, _C4_) VALUES ( :bRI, :bRV, :_C0_, :_C2_, :_C4_) ON DUPLICATE KEY UPDATE ROW_VERSION = VALUES(ROW_VERSION), _C0_ = VALUES(_C0_), _C2_ = VALUES(_C2_), _C4_ = VALUES(_C4_)";
		assertEquals(expected, result);
	}

	@Test
	public void testBindParametersForCreateOrUpdate(){
		List<ColumnModel> oldSchema = helperCreateColumnsWithIds("0","2","4");
		SparseChangeSet set = new SparseChangeSet("syn123", oldSchema);
		for(int i=0; i<2; i++){
			SparseRow row = set.addEmptyRow();
			row.setRowId(new Long(i));
			row.setVersionNumber(3L);
			row.setCellValue("0", "111"+i);
			row.setCellValue("2", "222"+i);
			row.setCellValue("4", "333"+i);
		}
		Grouping grouping = set.groupByValidValues().iterator().next();

		// bind!
		SqlParameterSource[] results = SQLUtils.bindParametersForCreateOrUpdate(grouping);
		assertNotNull(results);
		assertEquals(2, results.length,"There should be one mapping for each row in the batch");
		// First row
		assertEquals(new Long(0), results[0].getValue(SQLUtils.ROW_ID_BIND));
		assertEquals(new Long(3), results[0].getValue(SQLUtils.ROW_VERSION_BIND));
		assertEquals(new Long(1110), results[0].getValue("_C0_"));
		assertEquals(new Long(2220), results[0].getValue("_C2_"));
		assertEquals(new Long(3330), results[0].getValue("_C4_"));
		// second
		assertEquals(new Long(1111), results[1].getValue("_C0_"));
		assertEquals(new Long(2221), results[1].getValue("_C2_"));
		assertEquals(new Long(3331), results[1].getValue("_C4_"));
	}


	@Test
	public void testBindParametersForCreateOrUpdateAllTypes(){
		List<ColumnModel> newSchema = TableModelTestUtils.createOneOfEachType();
		RowSet set = new RowSet();
		set.setRows(TableModelTestUtils.createRows(newSchema, 3));
		set.setHeaders(TableModelUtils.getSelectColumns(newSchema));
		set.setTableId("syn123");
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		SparseChangeSet sparseSet = TableModelUtils.createSparseChangeSet(set, newSchema);
		Grouping grouping = sparseSet.groupByValidValues().iterator().next();
		// bind!
		SqlParameterSource[] results = SQLUtils.bindParametersForCreateOrUpdate(grouping);
		assertNotNull(results);
		assertEquals(3, results.length,"There should be one mapping for each row in the batch");
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
		assertEquals(new Long(606000), results[0].getValue("_C6_"));
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
		assertEquals(new Long(606001), results[1].getValue("_C6_"));
	}

	@Test
	public void testGetRowCountSQL(){
		String expected = "SELECT COUNT(ROW_ID) FROM T999";
		String result = SQLUtils.getCountSQL(tableId);
		assertEquals(expected, result);
	}

	@Test
	public void testGetMaxVersionSQL(){
		String expected = "SELECT ROW_VERSION FROM T999S";
		String result = SQLUtils.getStatusMaxVersionSQL(tableId);
		assertEquals(expected, result);
	}

	@Test
	public void testCreateSQLInsertIgnoreFileHandleId(){
		String expected = "INSERT IGNORE INTO T999F (FILE_ID) VALUES(?)";
		String result = SQLUtils.createSQLInsertIgnoreFileHandleId(tableId);
		assertEquals(expected, result);
	}

	@Test
	public void testCreateSQLGetBoundFileHandleId(){
		String expected = "SELECT FILE_ID FROM T999F WHERE FILE_ID IN( :bFIds)";
		String result = SQLUtils.createSQLGetBoundFileHandleId(tableId);
		assertEquals(expected, result);
	}

	@Test
	public void testCreateSQLGetDistinctValues(){
		String expected = "SELECT DISTINCT ROW_BENEFACTORS FROM T999";
		String result = SQLUtils.createSQLGetDistinctValues(tableId, "ROW_BENEFACTORS");
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

	@Test
	public void testAppendAppendColumnDefinitionWithDefult(){
		StringBuilder builder = new StringBuilder();
		ColumnModel cm = new ColumnModel();
		cm.setId("123");
		cm.setColumnType(ColumnType.INTEGER);
		cm.setDefaultValue("456");
		// call under test
		SQLUtils.appendColumnDefinition(builder, cm, useDepricatedUtf8ThreeBytes);
		assertEquals("_C123_ BIGINT DEFAULT 456 COMMENT 'INTEGER'", builder.toString());
	}

	@Test
	public void testAppendAppendColumnDefinitionDefaultNull(){
		StringBuilder builder = new StringBuilder();
		ColumnModel cm = new ColumnModel();
		cm.setId("123");
		cm.setColumnType(ColumnType.INTEGER);
		cm.setDefaultValue(null);
		// call under test
		SQLUtils.appendColumnDefinition(builder, cm, useDepricatedUtf8ThreeBytes);
		assertEquals("_C123_ BIGINT DEFAULT NULL COMMENT 'INTEGER'", builder.toString());
	}


	@Test
	public void testAppendAddColumn(){
		StringBuilder builder = new StringBuilder();
		ColumnModel cm = new ColumnModel();
		cm.setId("123");
		cm.setColumnType(ColumnType.INTEGER);
		// call under test
		SQLUtils.appendAddColumn(builder, cm, useDepricatedUtf8ThreeBytes);
		assertEquals("ADD COLUMN _C123_ BIGINT DEFAULT NULL COMMENT 'INTEGER'", builder.toString());
	}

	@Test
	public void testAppendAddColumnDouble(){
		StringBuilder builder = new StringBuilder();
		ColumnModel cm = new ColumnModel();
		cm.setId("123");
		cm.setColumnType(ColumnType.DOUBLE);
		// call under test
		SQLUtils.appendAddColumn(builder, cm, useDepricatedUtf8ThreeBytes);
		assertEquals("ADD COLUMN _C123_ DOUBLE DEFAULT NULL COMMENT 'DOUBLE'"
				+ ", ADD COLUMN _DBL_C123_ ENUM ('NaN', 'Infinity', '-Infinity') DEFAULT null", builder.toString());
	}

	@Test
	public void testAppendDropColumn(){
		StringBuilder builder = new StringBuilder();
		ColumnModel cm = new ColumnModel();
		cm.setId("123");
		cm.setColumnType(ColumnType.INTEGER);
		// call under test
		SQLUtils.appendDeleteColumn(builder, cm);
		assertEquals("DROP COLUMN _C123_", builder.toString());
	}

	@Test
	public void testAppendDropColumnDouble(){
		StringBuilder builder = new StringBuilder();
		ColumnModel cm = new ColumnModel();
		cm.setId("123");
		cm.setColumnType(ColumnType.DOUBLE);
		// call under test
		SQLUtils.appendDeleteColumn(builder, cm);
		assertEquals("DROP COLUMN _C123_, DROP COLUMN _DBL_C123_", builder.toString());
	}

	@Test
	public void testAppendAddDoubleEnum(){
		StringBuilder builder = new StringBuilder();
		// call under test
		SQLUtils.appendAddDoubleEnum(builder, "123");
		assertEquals(", ADD COLUMN _DBL_C123_ ENUM ('NaN', 'Infinity', '-Infinity') DEFAULT null", builder.toString());
	}

	@Test
	public void testAppendDropDoubleEnum(){
		StringBuilder builder = new StringBuilder();
		// call under test
		SQLUtils.appendDropDoubleEnum(builder, "123");
		assertEquals(", DROP COLUMN _DBL_C123_", builder.toString());
	}

	@Test
	public void testAppendRenameDoubleEnum(){
		StringBuilder builder = new StringBuilder();
		// call under test
		SQLUtils.appendRenameDoubleEnum(builder, "123", "456");
		assertEquals(", CHANGE COLUMN _DBL_C123_ _DBL_C456_ ENUM ('NaN', 'Infinity', '-Infinity') DEFAULT null", builder.toString());
	}

	@Test
	public void testAppendUpdateColumnNullInfo(){
		StringBuilder builder = new StringBuilder();
		// old column.
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("123");
		oldColumn.setColumnType(ColumnType.BOOLEAN);
		DatabaseColumnInfo oldColumnInfo = null;
		// new column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("456");
		newColumn.setColumnType(ColumnType.BOOLEAN);

		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, oldColumnInfo, newColumn);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			SQLUtils.appendUpdateColumn(builder, change, useDepricatedUtf8ThreeBytes);
		});
	}

	@Test
	public void testAppendUpdateColumnWithNullIndexName(){
		StringBuilder builder = new StringBuilder();
		// old column.
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("123");
		oldColumn.setColumnType(ColumnType.BOOLEAN);
		DatabaseColumnInfo oldColumnInfo = new DatabaseColumnInfo();
		oldColumnInfo.setHasIndex(true);
		// Index name is required when an index exists.
		oldColumnInfo.setIndexName(null);
		// new column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("456");
		newColumn.setColumnType(ColumnType.BOOLEAN);

		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, oldColumnInfo, newColumn);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			SQLUtils.appendUpdateColumn(builder, change, useDepricatedUtf8ThreeBytes);
		});
	}

	@Test
	public void testAppendUpdateColumnNoIndex(){
		StringBuilder builder = new StringBuilder();
		// old column.
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("123");
		oldColumn.setColumnType(ColumnType.BOOLEAN);
		DatabaseColumnInfo oldColumnInfo = new DatabaseColumnInfo();
		oldColumnInfo.setHasIndex(false);
		// new column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("456");
		newColumn.setColumnType(ColumnType.BOOLEAN);

		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, oldColumnInfo, newColumn);
		// call under test
		SQLUtils.appendUpdateColumn(builder, change, useDepricatedUtf8ThreeBytes);
		assertEquals("CHANGE COLUMN _C123_ _C456_ BOOLEAN DEFAULT NULL COMMENT 'BOOLEAN'", builder.toString());
	}

	@Test
	public void testAppendUpdateColumnWithIndex(){
		StringBuilder builder = new StringBuilder();
		// old column.
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("123");
		oldColumn.setColumnType(ColumnType.BOOLEAN);
		DatabaseColumnInfo oldColumnInfo = new DatabaseColumnInfo();
		oldColumnInfo.setHasIndex(true);
		oldColumnInfo.setIndexName("indexName");
		// new column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("456");
		newColumn.setColumnType(ColumnType.INTEGER);

		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, oldColumnInfo, newColumn);
		// call under test
		SQLUtils.appendUpdateColumn(builder, change, useDepricatedUtf8ThreeBytes);
		assertEquals("DROP INDEX indexName, CHANGE COLUMN _C123_ _C456_ BIGINT DEFAULT NULL COMMENT 'INTEGER'", builder.toString());
	}

	@Test
	public void testAppendUpdateColumn(){
		StringBuilder builder = new StringBuilder();
		// old column.
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("123");
		oldColumn.setColumnType(ColumnType.STRING);
		DatabaseColumnInfo oldColumnInfo = new DatabaseColumnInfo();
		oldColumnInfo.setHasIndex(false);
		// new column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("456");
		newColumn.setColumnType(ColumnType.INTEGER);

		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, oldColumnInfo, newColumn);
		// call under test
		SQLUtils.appendUpdateColumn(builder, change, useDepricatedUtf8ThreeBytes);
		assertEquals("CHANGE COLUMN _C123_ _C456_ BIGINT DEFAULT NULL COMMENT 'INTEGER'", builder.toString());
	}

	@Test
	public void testAppendUpdateColumnOldDouble(){
		StringBuilder builder = new StringBuilder();
		// old column.
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("123");
		oldColumn.setColumnType(ColumnType.DOUBLE);
		DatabaseColumnInfo oldColumnInfo = new DatabaseColumnInfo();
		oldColumnInfo.setHasIndex(false);
		// new column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("456");
		newColumn.setColumnType(ColumnType.INTEGER);

		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, oldColumnInfo, newColumn);
		// call under test
		SQLUtils.appendUpdateColumn(builder, change, useDepricatedUtf8ThreeBytes);
		assertEquals("CHANGE COLUMN _C123_ _C456_ BIGINT DEFAULT NULL COMMENT 'INTEGER'"
				+ ", DROP COLUMN _DBL_C123_", builder.toString());
	}

	@Test
	public void testAppendUpdateColumnNewDouble(){
		StringBuilder builder = new StringBuilder();
		// old column.
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("123");
		oldColumn.setColumnType(ColumnType.INTEGER);
		DatabaseColumnInfo oldColumnInfo = new DatabaseColumnInfo();
		oldColumnInfo.setHasIndex(false);
		// new column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("456");
		newColumn.setColumnType(ColumnType.DOUBLE);

		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, oldColumnInfo, newColumn);
		// call under test
		SQLUtils.appendUpdateColumn(builder, change, useDepricatedUtf8ThreeBytes);
		assertEquals("CHANGE COLUMN _C123_ _C456_ DOUBLE DEFAULT NULL COMMENT 'DOUBLE'"
				+ ", ADD COLUMN _DBL_C456_ ENUM ('NaN', 'Infinity', '-Infinity') DEFAULT null", builder.toString());
	}

	@Test
	public void testAppendUpdateColumnOldAndNewDouble(){
		StringBuilder builder = new StringBuilder();
		// old column.
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("123");
		oldColumn.setColumnType(ColumnType.DOUBLE);
		DatabaseColumnInfo oldColumnInfo = new DatabaseColumnInfo();
		oldColumnInfo.setHasIndex(false);
		// new column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("456");
		newColumn.setColumnType(ColumnType.DOUBLE);

		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, oldColumnInfo, newColumn);
		// call under test
		SQLUtils.appendUpdateColumn(builder, change, useDepricatedUtf8ThreeBytes);
		assertEquals("CHANGE COLUMN _C123_ _C456_ DOUBLE DEFAULT NULL COMMENT 'DOUBLE'"
				+ ", CHANGE COLUMN _DBL_C123_ _DBL_C456_ ENUM ('NaN', 'Infinity', '-Infinity') DEFAULT null", builder.toString());
	}

	@Test
	public void testAppendAlterTableSql(){
		// old column.
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("123");
		oldColumn.setColumnType(ColumnType.BOOLEAN);
		DatabaseColumnInfo oldColumnInfo = new DatabaseColumnInfo();
		oldColumnInfo.setHasIndex(false);
		// new column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("456");
		newColumn.setColumnType(ColumnType.BOOLEAN);

		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, oldColumnInfo, newColumn);
		// call under test
		String sql = SQLUtils.appendAlterTableSql(change, useDepricatedUtf8ThreeBytes, tableName);
		assertEquals("ALTER TABLE T999 CHANGE COLUMN _C123_ _C456_ BOOLEAN DEFAULT NULL COMMENT 'BOOLEAN'", sql);
	}

	@Test
	public void testAppendAlterNoChange(){
		// old column.
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("123");
		oldColumn.setColumnType(ColumnType.BOOLEAN);
		// new column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("123");
		newColumn.setColumnType(ColumnType.BOOLEAN);

		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, newColumn);
		// call under test
		String sql = SQLUtils.appendAlterTableSql(change, useDepricatedUtf8ThreeBytes, tableName);
		assertEquals("", sql);
	}

	@Test
	public void testAppendAlterAdd(){
		// old column.
		ColumnModel oldColumn = null;
		// new column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("123");
		newColumn.setColumnType(ColumnType.BOOLEAN);

		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, newColumn);
		// call under test
		String sql = SQLUtils.appendAlterTableSql(change, useDepricatedUtf8ThreeBytes, tableName);
		assertEquals("ALTER TABLE T999 ADD COLUMN _C123_ BOOLEAN DEFAULT NULL COMMENT 'BOOLEAN'", sql);
	}

	@Test
	public void testAppendAlterDrop(){
		// old column.
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("123");
		oldColumn.setColumnType(ColumnType.BOOLEAN);
		// new column
		ColumnModel newColumn = null;
		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, newColumn);
		// call under test
		String sql = SQLUtils.appendAlterTableSql(change, useDepricatedUtf8ThreeBytes, tableName);
		assertEquals("ALTER TABLE T999 DROP COLUMN _C123_", sql);
	}

	@Test
	public void testAppendAlterOldAndNewNull(){
		// old column.
		ColumnModel oldColumn = null;
		// new column
		ColumnModel newColumn = null;
		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, newColumn);
		// call under test
		String sql = SQLUtils.appendAlterTableSql(change, useDepricatedUtf8ThreeBytes, tableName);
		assertEquals("", sql);
	}

	@Test
	public void testCreateAlterTableSqlMultiple(){
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("123");
		oldColumn.setColumnType(ColumnType.BOOLEAN);
		DatabaseColumnInfo oldColumnInfo = new DatabaseColumnInfo();
		oldColumnInfo.setHasIndex(false);
		// new column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("456");
		newColumn.setColumnType(ColumnType.INTEGER);
		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, oldColumnInfo, newColumn);

		oldColumn = new ColumnModel();
		oldColumn.setId("111");
		oldColumn.setColumnType(ColumnType.DOUBLE);
		oldColumnInfo = new DatabaseColumnInfo();
		oldColumnInfo.setHasIndex(false);
		// new column
		newColumn = new ColumnModel();
		newColumn.setId("222");
		newColumn.setColumnType(ColumnType.STRING);
		newColumn.setMaximumSize(15L);
		newColumn.setDefaultValue("foo");
		ColumnChangeDetails change2 = new ColumnChangeDetails(oldColumn, oldColumnInfo, newColumn);
		boolean alterTemp = false;
		String[] expected = {"ALTER TABLE T999 CHANGE COLUMN _C123_ _C456_ BIGINT DEFAULT NULL COMMENT 'INTEGER'",
				"ALTER TABLE T999 CHANGE COLUMN _C111_ _C222_ VARCHAR(15) "
				+ "CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'foo' COMMENT 'STRING', "
				+ "DROP COLUMN _DBL_C111_"
		};
		// call under test
		String[] results = SQLUtils.createAlterTableSql(Lists.newArrayList(change, change2), tableId, alterTemp);
		assertArrayEquals(results, expected);
	}

	@Test
	public void testCreateAlterTableSqlWithListChangeAndAlterTempTrue(){
		// change 1
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("111");
		oldColumn.setColumnType(ColumnType.DOUBLE);
		DatabaseColumnInfo oldColumnInfo = new DatabaseColumnInfo();
		oldColumnInfo.setHasIndex(false);
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("222");
		newColumn.setColumnType(ColumnType.STRING);
		newColumn.setMaximumSize(15L);
		newColumn.setDefaultValue("foo");
		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, oldColumnInfo, newColumn);
		// change 2
		oldColumn = new ColumnModel();
		oldColumn.setId("123");
		oldColumn.setColumnType(ColumnType.BOOLEAN);
		oldColumnInfo = new DatabaseColumnInfo();
		oldColumnInfo.setHasIndex(false);
		newColumn = new ColumnModel();
		newColumn.setId("456");
		newColumn.setColumnType(ColumnType.BOOLEAN_LIST);
		ColumnChangeDetails change2 = new ColumnChangeDetails(oldColumn, oldColumnInfo, newColumn);
		boolean alterTemp = true;
		String[] expected = {"ALTER TABLE TEMPT999 CHANGE COLUMN _C111_ _C222_ VARCHAR(15) " 
				+ "CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'foo' COMMENT 'STRING', "
				+ "DROP COLUMN _DBL_C111_",
				"ALTER TABLE TEMPT999 ADD COLUMN _C456_ JSON DEFAULT NULL COMMENT 'BOOLEAN_LIST'",
				"UPDATE TEMPT999 SET _C456_ = JSON_ARRAY(_C123_)",
				"ALTER TABLE TEMPT999 DROP COLUMN _C123_"
		};
		// call under test
		String[] results = SQLUtils.createAlterTableSql(Lists.newArrayList(change, change2), tableId, alterTemp);
		assertArrayEquals(results, expected);
	}

	@Test
	public void testCreateAlterTableSqlWithListChangeAndAlterTempFalse(){
		// change 1
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("111");
		oldColumn.setColumnType(ColumnType.DOUBLE);
		DatabaseColumnInfo oldColumnInfo = new DatabaseColumnInfo();
		oldColumnInfo.setHasIndex(false);
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("222");
		newColumn.setColumnType(ColumnType.STRING);
		newColumn.setMaximumSize(15L);
		newColumn.setDefaultValue("foo");
		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, oldColumnInfo, newColumn);
		// change 2
		oldColumn = new ColumnModel();
		oldColumn.setId("123");
		oldColumn.setColumnType(ColumnType.BOOLEAN);
		oldColumnInfo = new DatabaseColumnInfo();
		oldColumnInfo.setHasIndex(false);
		newColumn = new ColumnModel();
		newColumn.setId("456");
		newColumn.setColumnType(ColumnType.BOOLEAN_LIST);
		ColumnChangeDetails change2 = new ColumnChangeDetails(oldColumn, oldColumnInfo, newColumn);
		boolean alterTemp = false;
		String[] expected = {"ALTER TABLE T999 CHANGE COLUMN _C111_ _C222_ VARCHAR(15) " 
				+ "CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'foo' COMMENT 'STRING', "
				+ "DROP COLUMN _DBL_C111_",
				"ALTER TABLE T999 ADD COLUMN _C456_ JSON DEFAULT NULL COMMENT 'BOOLEAN_LIST'",
				"UPDATE T999 SET _C456_ = JSON_ARRAY(_C123_)",
				"ALTER TABLE T999 DROP COLUMN _C123_"
		};
		// call under test
		String[] results = SQLUtils.createAlterTableSql(Lists.newArrayList(change, change2), tableId, alterTemp);
		assertArrayEquals(results, expected);
	}

	@Test
	public void testCreateAlterListColumnIndexTable(){
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("42");
		newColumn.setName("testerino");
		newColumn.setColumnType(ColumnType.STRING_LIST);
		newColumn.setMaximumSize(58L);

		Long oldColumn = 21L;


		String sql = SQLUtils.createAlterListColumnIndexTable(tableId, oldColumn, newColumn, false);
		String expected = "ALTER TABLE T999_INDEX_C21_" +
				" DROP INDEX _C21__UNNEST_IDX," +
				" DROP FOREIGN KEY T999_INDEX_C21__ibfk_FK," +
				" RENAME COLUMN ROW_ID_REF_C21_ TO ROW_ID_REF_C42_," +
				" ADD CONSTRAINT T999_INDEX_C42__ibfk_FK FOREIGN KEY (ROW_ID_REF_C42_) REFERENCES T999(ROW_ID) ON DELETE CASCADE," +
				" CHANGE COLUMN _C21__UNNEST _C42__UNNEST VARCHAR(58) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'STRING'," +
				" ADD INDEX _C42__UNNEST_IDX (_C42__UNNEST ASC)," +
				" RENAME T999_INDEX_C42_";
		assertEquals(expected, sql);
	}

	@Test
	public void testCreateAlterListColumnIndexTable_alterTempTrue(){
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("42");
		newColumn.setName("testerino");
		newColumn.setColumnType(ColumnType.STRING_LIST);
		newColumn.setMaximumSize(58L);

		Long oldColumn = 21L;


		String sql = SQLUtils.createAlterListColumnIndexTable(tableId, oldColumn, newColumn, true);
		String expected = "ALTER TABLE TEMPT999_INDEX_C21_" +
				" DROP INDEX _C21__UNNEST_IDX," +
				" DROP FOREIGN KEY TEMPT999_INDEX_C21__ibfk_FK," +
				" RENAME COLUMN ROW_ID_REF_C21_ TO ROW_ID_REF_C42_," +
				" ADD CONSTRAINT TEMPT999_INDEX_C42__ibfk_FK FOREIGN KEY (ROW_ID_REF_C42_) REFERENCES TEMPT999(ROW_ID) ON DELETE CASCADE," +
				" CHANGE COLUMN _C21__UNNEST _C42__UNNEST VARCHAR(58) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'STRING'," +
				" ADD INDEX _C42__UNNEST_IDX (_C42__UNNEST ASC)," +
				" RENAME TEMPT999_INDEX_C42_";
		assertEquals(expected, sql);
	}

	@Test
	public void testCreateAlterTableSqlMultiple_alterTempTrue(){
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("123");
		oldColumn.setColumnType(ColumnType.BOOLEAN);
		DatabaseColumnInfo oldColumnInfo = new DatabaseColumnInfo();
		oldColumnInfo.setHasIndex(false);
		// new column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("456");
		newColumn.setColumnType(ColumnType.INTEGER);
		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, oldColumnInfo, newColumn);

		oldColumn = new ColumnModel();
		oldColumn.setId("111");
		oldColumn.setColumnType(ColumnType.DOUBLE);
		oldColumnInfo = new DatabaseColumnInfo();
		oldColumnInfo.setHasIndex(false);
		// new column
		newColumn = new ColumnModel();
		newColumn.setId("222");
		newColumn.setColumnType(ColumnType.STRING);
		newColumn.setMaximumSize(15L);
		newColumn.setDefaultValue("foo");
		ColumnChangeDetails change2 = new ColumnChangeDetails(oldColumn, oldColumnInfo, newColumn);
		boolean alterTemp = true;
		// call under test
		String[] results = SQLUtils.createAlterTableSql(Lists.newArrayList(change, change2), tableId, alterTemp);
		assertEquals("ALTER TABLE TEMPT999 CHANGE COLUMN _C123_ _C456_ BIGINT DEFAULT NULL COMMENT 'INTEGER'", results[0]);
		assertEquals("ALTER TABLE TEMPT999 CHANGE COLUMN _C111_ _C222_ VARCHAR(15) " 
				+ "CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'foo' COMMENT 'STRING', "
				+ "DROP COLUMN _DBL_C111_", results[1]);
	}

	@Test
	public void testCreateAlterTableSqlNoChange(){
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("123");
		oldColumn.setColumnType(ColumnType.BOOLEAN);
		// new column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("123");
		newColumn.setColumnType(ColumnType.BOOLEAN);
		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, newColumn);
		boolean alterTemp = false;

		// call under test
		String[] results = SQLUtils.createAlterTableSql(Lists.newArrayList(change), tableId, alterTemp);
		assertTrue(results.length == 0);
	}

	/**
	 * The error from PLFM-4560 was the result of a trailing comma in the SQL
	 * when the second change did not require anything to actually be changed.
	 */
	@Test
	public void testPLFM_4560(){
		// The first is a real change.
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("123");
		oldColumn.setColumnType(ColumnType.INTEGER);
		DatabaseColumnInfo oldColumnInfo = new DatabaseColumnInfo();
		oldColumnInfo.setHasIndex(false);
		// new column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("123");
		newColumn.setColumnType(ColumnType.BOOLEAN);
		ColumnChangeDetails changeOne = new ColumnChangeDetails(oldColumn, oldColumnInfo, newColumn);

		// the second should not be a change.
		oldColumn = new ColumnModel();
		oldColumn.setId("444");
		oldColumn.setColumnType(ColumnType.INTEGER);
		// new column
		newColumn = new ColumnModel();
		newColumn.setId("444");
		newColumn.setColumnType(ColumnType.INTEGER);
		ColumnChangeDetails changeTwo = new ColumnChangeDetails(oldColumn, newColumn);

		boolean alterTemp = false;
		// call under test
		String[] results = SQLUtils.createAlterTableSql(Lists.newArrayList(changeOne, changeTwo), tableId, alterTemp);
		assertEquals("ALTER TABLE T999 CHANGE COLUMN _C123_ _C123_ BOOLEAN DEFAULT NULL COMMENT 'BOOLEAN'", results[0]);
	}

	/**
	 * The error from PLFM-4560 was the result of a trailing comma in the SQL
	 * when the second change did not require anything to actually be changed.
	 */
	@Test
	public void testPLFM_4560NotFirst(){
		// The first is a real change.
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("123");
		oldColumn.setColumnType(ColumnType.INTEGER);
		// new column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("123");
		newColumn.setColumnType(ColumnType.INTEGER);
		ColumnChangeDetails changeOne = new ColumnChangeDetails(oldColumn, newColumn);

		// the second should not be a change.
		oldColumn = new ColumnModel();
		oldColumn.setId("444");
		oldColumn.setColumnType(ColumnType.INTEGER);
		DatabaseColumnInfo oldColumnInfo = new DatabaseColumnInfo();
		oldColumnInfo.setHasIndex(false);
		// new column
		newColumn = new ColumnModel();
		newColumn.setId("444");
		newColumn.setColumnType(ColumnType.BOOLEAN);
		ColumnChangeDetails changeTwo = new ColumnChangeDetails(oldColumn, oldColumnInfo, newColumn);

		boolean alterTemp = false;
		// call under test
		String[] results = SQLUtils.createAlterTableSql(Lists.newArrayList(changeOne, changeTwo), tableId, alterTemp);
		assertEquals("ALTER TABLE T999 CHANGE COLUMN _C444_ _C444_ BOOLEAN DEFAULT NULL COMMENT 'BOOLEAN'", results[0]);
	}

	/**
	 * This is a test case for altering a table that is not part of the set of
	 * tables that are too large for 4 byte UTF-8.
	 */
	@Test
	public void testPLFM_5458TableNotTooLarge() {
		assertFalse(ColumnConstants.isTableTooLargeForFourByteUtf8(tableId.getId()));
		// simple add column
		ColumnModel oldColumn = null;
		// new column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("123");
		newColumn.setColumnType(ColumnType.STRING);
		newColumn.setMaximumSize(100L);
		ColumnChangeDetails changeOne = new ColumnChangeDetails(oldColumn, newColumn);

		boolean alterTemp = false;
		// call under test
		String[] results = SQLUtils.createAlterTableSql(Lists.newArrayList(changeOne), tableId, alterTemp);
		assertEquals("ALTER TABLE T999 ADD COLUMN _C123_ VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'STRING'", results[0]);
	}

	/**
	 * This is a test case for altering a table that is
	 * too large for 4 byte UTF-8.
	 */
	@Test
	public void testPLFM_5458TableTooLarge() {
		tableId = IdAndVersion.parse("syn10227900");
		assertTrue(ColumnConstants.isTableTooLargeForFourByteUtf8(tableId.getId()));
		// simple add column.
		ColumnModel oldColumn = null;
		// new column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("123");
		newColumn.setColumnType(ColumnType.STRING);
		newColumn.setMaximumSize(100L);
		ColumnChangeDetails changeOne = new ColumnChangeDetails(oldColumn, newColumn);

		boolean alterTemp = false;
		// call under test
		String[] results = SQLUtils.createAlterTableSql(Lists.newArrayList(changeOne), tableId, alterTemp);
		assertEquals("ALTER TABLE T10227900 ADD COLUMN _C123_ VARCHAR(100) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT 'STRING'", results[0]);
	}

	@Test
	public void testCreateTruncateSql(){
		String sql = SQLUtils.createTruncateSql(tableId);
		assertEquals("DELETE FROM T999", sql);
	}

	@Test
	public void testCreateCardinalitySqlEmpty(){
		List<DatabaseColumnInfo> list = new LinkedList<DatabaseColumnInfo>();
		String results = SQLUtils.createCardinalitySql(list, tableId);
		assertEquals(null, results);
	}

	@Test
	public void testCreateCardinalitySqlMultiple(){
		DatabaseColumnInfo one = new DatabaseColumnInfo();
		one.setColumnName("_C111_");
		one.setType(MySqlColumnType.VARCHAR);
		DatabaseColumnInfo two = new DatabaseColumnInfo();
		two.setColumnName("_C222_");
		two.setType(MySqlColumnType.BIGINT);

		List<DatabaseColumnInfo> list = Lists.newArrayList(one, two);
		String results = SQLUtils.createCardinalitySql(list, tableId);
		assertEquals("SELECT COUNT(DISTINCT _C111_) AS _C111_, COUNT(DISTINCT _C222_) AS _C222_ FROM T999", results);
	}
	
	@Test
	public void testCreateCardinalitySqlWithNoIndexColumns() {
		List<DatabaseColumnInfo> list = new ArrayList<>();
		for (int i=0; i< MySqlColumnType.values().length; i++) {
			MySqlColumnType type = MySqlColumnType.values()[i];
			DatabaseColumnInfo column = new DatabaseColumnInfo();
			column.setColumnName(type.name());
			column.setType(type);
			list.add(column);
		}
		String results = SQLUtils.createCardinalitySql(list, tableId);
		assertEquals(
			"SELECT "
			+ "COUNT(DISTINCT BIGINT) AS BIGINT, "
			+ "COUNT(DISTINCT VARCHAR) AS VARCHAR, "
			+ "COUNT(DISTINCT DOUBLE) AS DOUBLE, "
			+ "COUNT(DISTINCT BOOLEAN) AS BOOLEAN, "
			+ "MAX(0) AS MEDIUMTEXT, "
			+ "COUNT(DISTINCT TEXT) AS TEXT, "
			+ "COUNT(DISTINCT TINYINT) AS TINYINT, "
			+ "COUNT(DISTINCT ENUM) AS ENUM, "
			+ "MAX(0) AS JSON FROM T999", results
		);
	}
	
	@Test
	public void testCreateCardinalitySqlWithMetadataColumns() {
		List<DatabaseColumnInfo> list = new ArrayList<>();
		for (String name : TableConstants.RESERVED_COLUMNS_NAMES) {
			DatabaseColumnInfo column = new DatabaseColumnInfo();
			column.setColumnName(name);
			list.add(column);
		}
		list.sort(Comparator.comparing(DatabaseColumnInfo::getColumnName));
		
		String results = SQLUtils.createCardinalitySql(list, tableId);
		
		assertEquals(
			"SELECT MAX(0) AS ROW_BENEFACTOR, MAX(0) AS ROW_ETAG, MAX(0) AS ROW_ID, MAX(0) AS ROW_SEARCH_CONTENT, MAX(0) AS ROW_VERSION FROM T999", results
		);
	}
	
	public List<DatabaseColumnInfo> createDatabaseColumnInfo(long rowCount){
		List<DatabaseColumnInfo> list = new LinkedList<DatabaseColumnInfo>();
		//row id
		DatabaseColumnInfo rowId = new DatabaseColumnInfo();
		rowId.setColumnName(TableConstants.ROW_ID);
		rowId.setCardinality(rowCount);
		rowId.setHasIndex(true);
		rowId.setIndexName("PRIMARY");
		rowId.setType(MySqlColumnType.BIGINT);
		list.add(rowId);
		//row version
		DatabaseColumnInfo rowVersion = new DatabaseColumnInfo();
		rowVersion.setColumnName(TableConstants.ROW_VERSION);
		rowVersion.setCardinality(1L);
		rowVersion.setHasIndex(true);
		rowVersion.setIndexName("");
		rowVersion.setType(MySqlColumnType.BIGINT);
		list.add(rowVersion);

		// Create rows with descending cardinality.
		for(int i=0; i<rowCount; i++){
			DatabaseColumnInfo info = new DatabaseColumnInfo();
			info.setColumnName("_C"+i+"_");
			info.setCardinality(rowCount-i);
			info.setHasIndex(false);
			info.setIndexName(null);
			info.setType(MySqlColumnType.VARCHAR);
			list.add(info);
		}
		return list;
	}

	@Test
	public void testCalculateIndexOptimizationWithNoIndexColumns() {
		int maxNumberOfIndex = 10000;
		
		List<DatabaseColumnInfo> list = new ArrayList<>();
		
		for (MySqlColumnType type : MySqlColumnType.values()) {
			if (type.isCreateIndex()) {
				continue;
			}
			DatabaseColumnInfo column = new DatabaseColumnInfo();
			column.setColumnName(type.name());
			column.setType(type);
			column.setHasIndex(false);
			column.setCardinality(-1L);
			list.add(column);
		}
		
		IndexChange changes = SQLUtils.calculateIndexOptimization(list, tableId, maxNumberOfIndex);
		
		assertEquals(0, changes.getToAdd().size());
		assertEquals(0, changes.getToRemove().size());
		assertEquals(0, changes.getToRename().size());
	}
	
	@Test
	public void testCalculateIndexOptimizationWithNoIndexColumnsAndExistingIndex() {
		int maxNumberOfIndex = 10000;
		
		List<DatabaseColumnInfo> list = new ArrayList<>();
		
		for (MySqlColumnType type : MySqlColumnType.values()) {
			if (type.isCreateIndex()) {
				continue;
			}
			DatabaseColumnInfo column = new DatabaseColumnInfo();
			column.setColumnName(type.name());
			column.setType(type);
			column.setHasIndex(true);
			column.setCardinality(-1L);
			list.add(column);
		}
		
		IndexChange changes = SQLUtils.calculateIndexOptimization(list, tableId, maxNumberOfIndex);
		
		assertEquals(0, changes.getToAdd().size());
		assertEquals(2, changes.getToRemove().size());
		assertEquals(0, changes.getToRename().size());
	}
	
	@Test
	public void testCalculateIndexOptimizationWithMetadataColumns() {
		int maxNumberOfIndex = 10000;
		
		List<DatabaseColumnInfo> list = new ArrayList<>();
		
		for (String name : TableConstants.RESERVED_COLUMNS_NAMES) {
			DatabaseColumnInfo column = new DatabaseColumnInfo();
			column.setColumnName(name);
			column.setCardinality(-1L);
			column.setHasIndex(true);
			list.add(column);
		}
		
		IndexChange changes = SQLUtils.calculateIndexOptimization(list, tableId, maxNumberOfIndex);
		
		assertEquals(0, changes.getToAdd().size());
		assertEquals(0, changes.getToRemove().size());
		assertEquals(0, changes.getToRename().size());
	}

	@Test
	public void testCalculateIndexChangesUnderMax(){
		int maxNumberOfIndex = 10000;
		int columnCount = 2;
		List<DatabaseColumnInfo> currentInfo = createDatabaseColumnInfo(columnCount);
		// call under test
		IndexChange changes = SQLUtils.calculateIndexOptimization(currentInfo, tableId, maxNumberOfIndex);
		// both columns should have an index added.
		assertEquals(columnCount, changes.getToAdd().size());
		assertEquals("_C0_", changes.getToAdd().get(0).getColumnName());
		assertEquals("_C1_", changes.getToAdd().get(1).getColumnName());

		assertEquals(0, changes.getToRemove().size());
		assertEquals(0, changes.getToRename().size());
	}

	@Test
	public void testCalculateIndexChangesUnderMaxNeedsRename(){
		int maxNumberOfIndex = 10000;
		int columnCount = 1;
		List<DatabaseColumnInfo> currentInfo = createDatabaseColumnInfo(columnCount);
		// set an index on the last column.
		DatabaseColumnInfo lastInfo = currentInfo.get(2);
		lastInfo.setHasIndex(true);
		// set the wrong name.
		lastInfo.setIndexName("wrongName");
		// call under test.
		IndexChange changes = SQLUtils.calculateIndexOptimization(currentInfo, tableId, maxNumberOfIndex);

		assertEquals(0, changes.getToAdd().size());
		assertEquals(0, changes.getToRemove().size());
		// the last column needs to be renamed.
		assertEquals(1, changes.getToRename().size());
		assertEquals("_C0_", changes.getToRename().get(0).getColumnName());
	}

	@Test
	public void testCalculateIndexChangesUnderMaxNameCorrect(){
		int maxNumberOfIndex = 10000;
		int columnCount = 1;
		List<DatabaseColumnInfo> currentInfo = createDatabaseColumnInfo(columnCount);
		// set an index on the last column.
		DatabaseColumnInfo lastInfo = currentInfo.get(2);
		lastInfo.setHasIndex(true);
		// set the correct name
		lastInfo.setIndexName(SQLUtils.getIndexName(lastInfo.getColumnName()));
		// call under test.
		IndexChange changes = SQLUtils.calculateIndexOptimization(currentInfo, tableId, maxNumberOfIndex);

		assertEquals(0, changes.getToAdd().size());
		assertEquals(0, changes.getToRemove().size());
		// rename not needed.
		assertEquals(0, changes.getToRename().size());
	}

	@Test
	public void testCalculateIndexChangesOverMaxWithTooManyIndices(){
		int maxNumberOfIndex = 1;
		int columnCount = 1;
		List<DatabaseColumnInfo> currentInfo = createDatabaseColumnInfo(columnCount);
		// set an index on the last column.
		DatabaseColumnInfo lastInfo = currentInfo.get(2);
		lastInfo.setHasIndex(true);
		// set the correct name
		lastInfo.setIndexName(SQLUtils.getIndexName(lastInfo.getColumnName()));
		// call under test.
		IndexChange changes = SQLUtils.calculateIndexOptimization(currentInfo, tableId, maxNumberOfIndex);

		assertEquals(0, changes.getToAdd().size());
		assertEquals(1, changes.getToRemove().size());
		assertEquals("_C0_", changes.getToRemove().get(0).getColumnName());
		assertEquals(0, changes.getToRename().size());
	}

	@Test
	public void testCalculateIndexChangesLowCardinalityReplacedWithHigh(){
		int maxNumberOfIndex = 2;
		int columnCount = 2;
		List<DatabaseColumnInfo> currentInfo = createDatabaseColumnInfo(columnCount);
		//  index with a lower cardinality
		DatabaseColumnInfo firstInfo = currentInfo.get(2);
		firstInfo.setHasIndex(true);
		firstInfo.setIndexName(SQLUtils.getIndexName(firstInfo.getColumnName()));
		firstInfo.setCardinality(1L);

		// no index with a higher cardinality.
		DatabaseColumnInfo lastInfo = currentInfo.get(3);
		lastInfo.setHasIndex(false);
		lastInfo.setCardinality(2L);

		// call under test.
		IndexChange changes = SQLUtils.calculateIndexOptimization(currentInfo, tableId, maxNumberOfIndex);

		assertEquals(1, changes.getToAdd().size());
		assertEquals("_C1_", changes.getToAdd().get(0).getColumnName(), "Higher cardinality should be added");
		assertEquals(1, changes.getToRemove().size());
		assertEquals("_C0_", changes.getToRemove().get(0).getColumnName(),"Lower cardinality should be dropped.");
		assertEquals(0, changes.getToRename().size());
	}

	@Test
	public void testCalculateIndexChangesWithAddRemoveRename(){
		int maxNumberOfIndex = 3;
		int columnCount = 3;
		List<DatabaseColumnInfo> currentInfo = createDatabaseColumnInfo(columnCount);
		//  index with a lower cardinality
		DatabaseColumnInfo firstInfo = currentInfo.get(2);
		firstInfo.setHasIndex(true);
		firstInfo.setIndexName(SQLUtils.getIndexName(firstInfo.getColumnName()));
		firstInfo.setCardinality(1L);
		firstInfo.setType(MySqlColumnType.BIGINT);
		// index with a high cardinality but wrong name.
		DatabaseColumnInfo midInfo = currentInfo.get(3);
		midInfo.setHasIndex(true);
		midInfo.setIndexName("wrongName");
		midInfo.setCardinality(3L);
		midInfo.setType(MySqlColumnType.BIGINT);

		// no index with a higher cardinality.
		DatabaseColumnInfo lastInfo = currentInfo.get(4);
		lastInfo.setHasIndex(false);
		lastInfo.setCardinality(2L);
		lastInfo.setType(MySqlColumnType.BIGINT);

		// call under test.
		IndexChange changes = SQLUtils.calculateIndexOptimization(currentInfo, tableId, maxNumberOfIndex);

		assertEquals(1, changes.getToAdd().size());
		assertEquals("_C2_", changes.getToAdd().get(0).getColumnName(), "Higher cardinality should be added");
		assertEquals(1, changes.getToRemove().size());
		assertEquals("_C0_", changes.getToRemove().get(0).getColumnName(), "Lower cardinality should be dropped.");
		assertEquals(1, changes.getToRename().size());
		assertEquals("_C1_", changes.getToRename().get(0).getColumnName(), "High cardinality should be renamed.");
	}

	@Test
	public void testCreateAlterSqlEmpty(){
		// test with nothing to do.
		List<DatabaseColumnInfo> toAdd = new LinkedList<DatabaseColumnInfo>();
		List<DatabaseColumnInfo> toRemove = new LinkedList<DatabaseColumnInfo>();
		List<DatabaseColumnInfo> toRename = new LinkedList<DatabaseColumnInfo>();
		IndexChange changes = new IndexChange(toAdd, toRemove, toRename);
		// call under test
		String results = SQLUtils.createAlterIndices(changes, tableId);
		assertEquals(null, results);
	}


	@Test
	public void testCreateAlterSqlAdd(){
		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName("_C1_");
		info.setCardinality(1L);
		info.setType(MySqlColumnType.MEDIUMTEXT);

		List<DatabaseColumnInfo> toAdd = Lists.newArrayList(info);
		List<DatabaseColumnInfo> toRemove = new LinkedList<DatabaseColumnInfo>();
		List<DatabaseColumnInfo> toRename = new LinkedList<DatabaseColumnInfo>();
		IndexChange changes = new IndexChange(toAdd, toRemove, toRename);
		// call under test
		String results = SQLUtils.createAlterIndices(changes, tableId);
		assertEquals("ALTER TABLE T999 ADD INDEX _C1_idx_ (_C1_(255))", results);
	}

	@Test
	public void testCreateAlterSqlDrop(){
		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName("_C1_");
		info.setIndexName("_C1_IDX");
		info.setCardinality(1L);

		List<DatabaseColumnInfo> toAdd = new LinkedList<DatabaseColumnInfo>();
		List<DatabaseColumnInfo> toRemove = Lists.newArrayList(info);
		List<DatabaseColumnInfo> toRename = new LinkedList<DatabaseColumnInfo>();
		IndexChange changes = new IndexChange(toAdd, toRemove, toRename);
		// call under test
		String results = SQLUtils.createAlterIndices(changes, tableId);
		assertEquals("ALTER TABLE T999 DROP INDEX _C1_IDX", results);
	}

	@Test
	public void testCreateAlterSqlRename(){
		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName("_C1_");
		info.setIndexName("_C2_idx_");
		info.setType(MySqlColumnType.BIGINT);

		List<DatabaseColumnInfo> toAdd = new LinkedList<DatabaseColumnInfo>();
		List<DatabaseColumnInfo> toRemove =  new LinkedList<DatabaseColumnInfo>();
		List<DatabaseColumnInfo> toRename = Lists.newArrayList(info);
		IndexChange changes = new IndexChange(toAdd, toRemove, toRename);
		// call under test
		String results = SQLUtils.createAlterIndices(changes, tableId);
		assertEquals("ALTER TABLE T999 DROP INDEX _C2_idx_, ADD INDEX _C1_idx_ (_C1_)", results);
	}

	@Test
	public void testAlterSqlAddRemoveRename(){
		int maxNumberOfIndex = 3;
		int columnCount = 3;
		List<DatabaseColumnInfo> currentInfo = createDatabaseColumnInfo(columnCount);
		//  index with a lower cardinality
		DatabaseColumnInfo firstInfo = currentInfo.get(2);
		firstInfo.setHasIndex(true);
		firstInfo.setCardinality(1L);
		firstInfo.setIndexName("_C0_idx_");
		firstInfo.setType(MySqlColumnType.BIGINT);
		// index with a high cardinality but wrong name.
		DatabaseColumnInfo midInfo = currentInfo.get(3);
		midInfo.setHasIndex(true);
		midInfo.setIndexName("wrongName");
		midInfo.setCardinality(3L);
		midInfo.setType(MySqlColumnType.BIGINT);

		// no index with a higher cardinality.
		DatabaseColumnInfo lastInfo = currentInfo.get(4);
		lastInfo.setHasIndex(false);
		lastInfo.setCardinality(2L);
		lastInfo.setType(MySqlColumnType.BIGINT);

		String results = SQLUtils.createOptimizedAlterIndices(currentInfo, tableId, maxNumberOfIndex);
		assertEquals("ALTER TABLE T999 "
				+ "DROP INDEX _C0_idx_, "
				+ "DROP INDEX wrongName, ADD INDEX _C1_idx_ (_C1_), "
				+ "ADD INDEX _C2_idx_ (_C2_)", results);
	}

	@Test
	public void testCreateReplaceSchemaChange(){
		ColumnModel one = TableModelTestUtils.createColumn(1L);
		ColumnModel two = TableModelTestUtils.createColumn(2L);
		ColumnModel three = TableModelTestUtils.createColumn(3L);
		List<ColumnModel> oldSchema = Lists.newArrayList(one, two);
		List<ColumnModel> newSchema = Lists.newArrayList(two, three);
		// call under test
		List<ColumnChangeDetails> results = SQLUtils.createReplaceSchemaChangeIds(oldSchema, newSchema);
		assertNotNull(results);
		assertEquals(2, results.size());
		// one should be removed
		ColumnChangeDetails toRemove = results.get(0);
		assertEquals("1", toRemove.getOldColumn().getId());
		assertEquals(null, toRemove.getNewColumn());
		// three should be added.
		ColumnChangeDetails toAdd = results.get(1);
		assertEquals(null, toAdd.getOldColumn());
		assertEquals(three, toAdd.getNewColumn());
	}

	@Test
	public void testCreateReplaceSchemaChangeNoChange(){
		ColumnModel one = TableModelTestUtils.createColumn(1L);
		ColumnModel two = TableModelTestUtils.createColumn(2L);
		List<ColumnModel> oldSchema = Lists.newArrayList(one, two);
		List<ColumnModel> newSchema = Lists.newArrayList(two, one);
		// call under test
		List<ColumnChangeDetails> results = SQLUtils.createReplaceSchemaChangeIds(oldSchema, newSchema);
		assertNotNull(results);
		assertEquals(0, results.size());
	}

	@Test
	public void testGetColumnId(){
		String columnName = SQLUtils.getColumnNameForId("123");
		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName(columnName);
		long columnId = SQLUtils.getColumnId(info);
		assertEquals(123L, columnId);
	}

	@Test
	public void testGetColumnIdNotAnId(){
		String columnName = SQLUtils.getColumnNameForId("foo");
		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName(columnName);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test.
			SQLUtils.getColumnId(info);
		});
	}

	@Test
	public void testGetColumnIds(){
		String columnName = SQLUtils.getColumnNameForId("123");
		DatabaseColumnInfo rowId = new DatabaseColumnInfo();
		rowId.setColumnName(TableConstants.ROW_ID);
		DatabaseColumnInfo rowVersion = new DatabaseColumnInfo();
		rowVersion.setColumnName(TableConstants.ROW_VERSION);

		DatabaseColumnInfo info = new DatabaseColumnInfo();
		info.setColumnName(columnName);
		info.setColumnType(ColumnType.STRING);
		info.setMaxSize(22);

		DatabaseColumnInfo noType = new DatabaseColumnInfo();
		noType.setColumnName("noType");
		noType.setColumnType(null);

		List<DatabaseColumnInfo> infoList = Lists.newArrayList(rowId, rowVersion, info, noType);
		List<ColumnModel> results = SQLUtils.extractSchemaFromInfo(infoList);

		ColumnModel cm = new ColumnModel();
		cm.setId("123");
		cm.setColumnType(info.getColumnType());
		cm.setMaximumSize(22L);

		List<ColumnModel> expected = Lists.newArrayList(cm);
		assertEquals(expected, results);
	}

	@Test
	public void testGetColumnIdFromMultivalueColumnIndexTableName_nullTableId(){
		assertThrows(IllegalArgumentException.class, () -> {
			SQLUtils.getColumnIdFromMultivalueColumnIndexTableName(null, "_T123_123__INDEX_C555_");
		});
	}
	@Test
	public void testGetColumnIdFromMultivalueColumnIndexTableName_nullIndexTableName(){
		IdAndVersion tableId = IdAndVersion.parse("syn123");
		assertThrows(IllegalArgumentException.class, () -> {
			SQLUtils.getColumnIdFromMultivalueColumnIndexTableName(tableId, null);
		});

	}

	@Test
	public void testGetColumnIdFromMultivalueColumnIndexTableName(){
		IdAndVersion tableId = IdAndVersion.parse("syn123.456");
		String indexTableName = "T123_456_INDEX_C555_";

		assertEquals(555, SQLUtils.getColumnIdFromMultivalueColumnIndexTableName(tableId, indexTableName));
	}

	@Test
	public void testTemporaryTableName(){
		String temp = SQLUtils.getTemporaryTableName(tableId);
		assertEquals("TEMPT999", temp);
	}

	@Test
	public void testCreateTempTableSql(){
		String sql = SQLUtils.createTempTableSql(tableId);
		assertEquals("CREATE TABLE TEMPT999 LIKE T999", sql);
	}


	@Test
	public void testTempMultiValueColumnIndexTableSql(){
		String[] sql = SQLUtils.createTempMultiValueColumnIndexTableSql(tableId, "123");
		String[] expected = new String[]{"CREATE TABLE TEMPT999_INDEX_C123_ LIKE T999_INDEX_C123_",
				"ALTER TABLE TEMPT999_INDEX_C123_ " +
				"ADD CONSTRAINT TEMPT999_INDEX_C123__ibfk_FK FOREIGN KEY (ROW_ID_REF_C123_) REFERENCES TEMPT999(ROW_ID) " +
				"ON DELETE CASCADE"};
		assertArrayEquals(expected, sql);
	}

	@Test
	public void testCopyTableToTempSql(){
		String sql = SQLUtils.copyTableToTempSql(tableId);
		assertEquals("INSERT INTO TEMPT999 SELECT * FROM T999", sql);
	}

	@Test
	public void testMultiValueColumnIndexTableToTempSql(){
		String sql = SQLUtils.copyMultiValueColumnIndexTableToTempSql(tableId, "123");
		assertEquals("INSERT INTO TEMPT999_INDEX_C123_ SELECT * FROM T999_INDEX_C123_", sql);
	}

	@Test
	public void testDeleteTempTableSql(){
		String sql = SQLUtils.deleteTempTableSql(tableId);
		assertEquals("DROP TABLE IF EXISTS TEMPT999", sql);
	}

	@Test
	public void testTranslateColumnTypeToAnnotationType(){
		assertEquals(AnnotationType.STRING, SQLUtils.translateColumnTypeToAnnotationType(ColumnType.STRING));
		assertEquals(AnnotationType.BOOLEAN, SQLUtils.translateColumnTypeToAnnotationType(ColumnType.BOOLEAN));
		assertEquals(AnnotationType.DATE, SQLUtils.translateColumnTypeToAnnotationType(ColumnType.DATE));
		assertEquals(AnnotationType.DOUBLE, SQLUtils.translateColumnTypeToAnnotationType(ColumnType.DOUBLE));
		assertEquals(AnnotationType.STRING, SQLUtils.translateColumnTypeToAnnotationType(ColumnType.ENTITYID));
		assertEquals(AnnotationType.STRING, SQLUtils.translateColumnTypeToAnnotationType(ColumnType.SUBMISSIONID));
		assertEquals(AnnotationType.STRING, SQLUtils.translateColumnTypeToAnnotationType(ColumnType.EVALUATIONID));
		assertEquals(AnnotationType.STRING, SQLUtils.translateColumnTypeToAnnotationType(ColumnType.FILEHANDLEID));
		assertEquals(AnnotationType.STRING, SQLUtils.translateColumnTypeToAnnotationType(ColumnType.USERID));
		assertEquals(AnnotationType.LONG, SQLUtils.translateColumnTypeToAnnotationType(ColumnType.INTEGER));
		assertEquals(AnnotationType.STRING, SQLUtils.translateColumnTypeToAnnotationType(ColumnType.LARGETEXT));
		assertEquals(AnnotationType.STRING, SQLUtils.translateColumnTypeToAnnotationType(ColumnType.LINK));
		assertEquals(AnnotationType.STRING, SQLUtils.translateColumnTypeToAnnotationType(ColumnType.STRING_LIST));
		assertEquals(AnnotationType.STRING, SQLUtils.translateColumnTypeToAnnotationType(ColumnType.BOOLEAN_LIST));
		assertEquals(AnnotationType.DATE , SQLUtils.translateColumnTypeToAnnotationType(ColumnType.DATE_LIST));
		assertEquals(AnnotationType.LONG, SQLUtils.translateColumnTypeToAnnotationType(ColumnType.INTEGER_LIST));
	}

	@Test
	public void testTranslateColumnTypeToAnnotationValueName(){
		assertEquals(TableConstants.ANNOTATION_REPLICATION_COL_STRING_VALUE, SQLUtils.translateColumnTypeToAnnotationValueName(ColumnType.STRING));
		assertEquals(TableConstants.ANNOTATION_REPLICATION_COL_BOOLEAN_VALUE, SQLUtils.translateColumnTypeToAnnotationValueName(ColumnType.BOOLEAN));
		assertEquals(TableConstants.ANNOTATION_REPLICATION_COL_LONG_VALUE, SQLUtils.translateColumnTypeToAnnotationValueName(ColumnType.DATE));
		assertEquals(TableConstants.ANNOTATION_REPLICATION_COL_DOUBLE_VALUE, SQLUtils.translateColumnTypeToAnnotationValueName(ColumnType.DOUBLE));
		assertEquals(TableConstants.ANNOTATION_REPLICATION_COL_LONG_VALUE, SQLUtils.translateColumnTypeToAnnotationValueName(ColumnType.ENTITYID));
		assertEquals(TableConstants.ANNOTATION_REPLICATION_COL_LONG_VALUE, SQLUtils.translateColumnTypeToAnnotationValueName(ColumnType.SUBMISSIONID));
		assertEquals(TableConstants.ANNOTATION_REPLICATION_COL_LONG_VALUE, SQLUtils.translateColumnTypeToAnnotationValueName(ColumnType.EVALUATIONID));
		assertEquals(TableConstants.ANNOTATION_REPLICATION_COL_LONG_VALUE, SQLUtils.translateColumnTypeToAnnotationValueName(ColumnType.FILEHANDLEID));
		assertEquals(TableConstants.ANNOTATION_REPLICATION_COL_LONG_VALUE, SQLUtils.translateColumnTypeToAnnotationValueName(ColumnType.USERID));
		assertEquals(TableConstants.ANNOTATION_REPLICATION_COL_LONG_VALUE, SQLUtils.translateColumnTypeToAnnotationValueName(ColumnType.INTEGER));
		assertEquals(TableConstants.ANNOTATION_REPLICATION_COL_STRING_VALUE, SQLUtils.translateColumnTypeToAnnotationValueName(ColumnType.LARGETEXT));
		assertEquals(TableConstants.ANNOTATION_REPLICATION_COL_STRING_VALUE, SQLUtils.translateColumnTypeToAnnotationValueName(ColumnType.LINK));
		assertEquals(TableConstants.ANNOTATION_REPLICATION_COL_STRING_LIST_VALUE, SQLUtils.translateColumnTypeToAnnotationValueName(ColumnType.STRING_LIST));
		assertEquals(TableConstants.ANNOTATION_REPLICATION_COL_LONG_LIST_VALUE, SQLUtils.translateColumnTypeToAnnotationValueName(ColumnType.ENTITYID_LIST));
		assertEquals(TableConstants.ANNOTATION_REPLICATION_COL_LONG_LIST_VALUE, SQLUtils.translateColumnTypeToAnnotationValueName(ColumnType.USERID_LIST));
		assertEquals(TableConstants.ANNOTATION_REPLICATION_COL_LONG_LIST_VALUE, SQLUtils.translateColumnTypeToAnnotationValueName(ColumnType.INTEGER_LIST));
		assertEquals(TableConstants.ANNOTATION_REPLICATION_COL_LONG_LIST_VALUE, SQLUtils.translateColumnTypeToAnnotationValueName(ColumnType.DATE_LIST));
		assertEquals(TableConstants.ANNOTATION_REPLICATION_COL_BOOLEAN_LIST_VALUE, SQLUtils.translateColumnTypeToAnnotationValueName(ColumnType.BOOLEAN_LIST));

	}

	@Test
	public void testbuildInsertValues(){
		ColumnMetadata one = createMetadataForAnnotation(ColumnType.ENTITYID, 1);
		ColumnMetadata two = createMetadataForAnnotation(ColumnType.STRING, 2);
		ColumnMetadata three = createMetadataForAnnotation(ColumnType.DOUBLE, 3);
		List<ColumnMetadata> metaList = ImmutableList.of(one, two, three);
		StringBuilder builder = new StringBuilder();
		// call under test
		SQLUtils.buildInsertValues(builder, metaList);
		assertEquals("ROW_ID, ROW_VERSION, ROW_ETAG, ROW_BENEFACTOR, _C1_, _C2_, _DBL_C3_, _C3_", builder.toString());
	}

	@Test
	public void testBuildObjectReplicationSelectEachColumnType(){
		// Build a select for each type.
		List<ColumnMetadata> metaList = new LinkedList<>();
		int i = 0;
		for(ColumnType type: ColumnType.values()){
			ColumnMetadata cm = createMetadataForAnnotation(type, i);
			metaList.add(cm);
			i++;
		}

		StringBuilder builder = new StringBuilder();
		// call under test
		SQLUtils.buildObjectReplicationSelect(builder, metaList);
		assertEquals(
				"R.OBJECT_ID,"
				+ " R.OBJECT_VERSION,"
				+ " MAX(R.ETAG) AS ETAG,"
				+ " MAX(R.BENEFACTOR_ID) AS BENEFACTOR_ID,"
				+ " MAX(IF(A.ANNO_KEY ='string', A.STRING_VALUE, NULL)) AS _C0_,"
				+ " MAX(IF(A.ANNO_KEY ='double', A.DOUBLE_ABSTRACT, NULL)) AS _DBL_C1_,"
				+ " MAX(IF(A.ANNO_KEY ='double', A.DOUBLE_VALUE, NULL)) AS _C1_,"
				+ " MAX(IF(A.ANNO_KEY ='integer', A.LONG_VALUE, NULL)) AS _C2_,"
				+ " MAX(IF(A.ANNO_KEY ='boolean', A.BOOLEAN_VALUE, NULL)) AS _C3_,"
				+ " MAX(IF(A.ANNO_KEY ='date', A.LONG_VALUE, NULL)) AS _C4_,"
				+ " MAX(IF(A.ANNO_KEY ='filehandleid', A.LONG_VALUE, NULL)) AS _C5_,"
				+ " MAX(IF(A.ANNO_KEY ='entityid', A.LONG_VALUE, NULL)) AS _C6_,"
				+ " MAX(IF(A.ANNO_KEY ='submissionid', A.LONG_VALUE, NULL)) AS _C7_,"
				+ " MAX(IF(A.ANNO_KEY ='evaluationid', A.LONG_VALUE, NULL)) AS _C8_,"
				+ " MAX(IF(A.ANNO_KEY ='link', A.STRING_VALUE, NULL)) AS _C9_,"
				+ " MAX(IF(A.ANNO_KEY ='mediumtext', A.STRING_VALUE, NULL)) AS _C10_,"
				+ " MAX(IF(A.ANNO_KEY ='largetext', A.STRING_VALUE, NULL)) AS _C11_,"
				+ " MAX(IF(A.ANNO_KEY ='userid', A.LONG_VALUE, NULL)) AS _C12_,"
				+ " MAX(IF(A.ANNO_KEY ='string_list', A.STRING_LIST_VALUE, NULL)) AS _C13_,"
				+ " MAX(IF(A.ANNO_KEY ='integer_list', A.LONG_LIST_VALUE, NULL)) AS _C14_,"
				+ " MAX(IF(A.ANNO_KEY ='boolean_list', A.BOOLEAN_LIST_VALUE, NULL)) AS _C15_,"
				+ " MAX(IF(A.ANNO_KEY ='date_list', A.LONG_LIST_VALUE, NULL)) AS _C16_,"
				+ " MAX(IF(A.ANNO_KEY ='entityid_list', A.LONG_LIST_VALUE, NULL)) AS _C17_,"
				+ " MAX(IF(A.ANNO_KEY ='userid_list', A.LONG_LIST_VALUE, NULL)) AS _C18_"
				, builder.toString());
	}

	@Test
	public void testBuildObjectReplicationSelectEachEntityType(){
		// Build a select for each type.
		List<ColumnMetadata> metaList = new LinkedList<>();
		int i = 0;
		for(ObjectField field: ObjectField.values()){
			ColumnMetadata cm = createMetadataForEntityField(field, i);
			metaList.add(cm);
			i++;
		}
		StringBuilder builder = new StringBuilder();
		// call under test
		SQLUtils.buildObjectReplicationSelect(builder, metaList);
		assertEquals(
				"R.OBJECT_ID,"
				+ " R.OBJECT_VERSION,"
				+ " MAX(R.ETAG) AS ETAG,"
				+ " MAX(R.BENEFACTOR_ID) AS BENEFACTOR_ID,"
				+ " MAX(R.OBJECT_ID) AS OBJECT_ID,"
				+ " MAX(R.NAME) AS NAME,"
				+ " MAX(R.DESCRIPTION) AS DESCRIPTION,"
				+ " MAX(R.CREATED_ON) AS CREATED_ON,"
				+ " MAX(R.CREATED_BY) AS CREATED_BY,"
				+ " MAX(R.ETAG) AS ETAG,"
				+ " MAX(R.SUBTYPE) AS SUBTYPE,"
				+ " MAX(R.CURRENT_VERSION) AS CURRENT_VERSION,"
				+ " MAX(R.PARENT_ID) AS PARENT_ID,"
				+ " MAX(R.BENEFACTOR_ID) AS BENEFACTOR_ID,"
				+ " MAX(R.PROJECT_ID) AS PROJECT_ID,"
				+ " MAX(R.MODIFIED_ON) AS MODIFIED_ON,"
				+ " MAX(R.MODIFIED_BY) AS MODIFIED_BY,"
				+ " MAX(R.FILE_ID) AS FILE_ID,"
				+ " MAX(R.FILE_SIZE_BYTES) AS FILE_SIZE_BYTES,"
				+ " MAX(R.FILE_MD5) AS FILE_MD5,"
				+ " MAX(R.FILE_SIZE_BYTES) AS FILE_SIZE_BYTES,"
				+ " MAX(R.FILE_MD5) AS FILE_MD5,"
				+ " MAX(R.ITEM_COUNT) AS ITEM_COUNT,"
				+ " MAX(R.FILE_CONCRETE_TYPE) AS FILE_CONCRETE_TYPE,"
				+ " MAX(R.FILE_BUCKET) AS FILE_BUCKET,"
				+ " MAX(R.FILE_KEY) AS FILE_KEY"
				, builder.toString());
	}
	
	
	private ColumnMetadata createMetadataForEntityField(ObjectField field, int id) {
		return new ColumnMetadata(getColumnModel(field), field.getDatabaseColumnName(), SQLUtils.getColumnNameForId("" + id), true);
	}
	
	private ColumnModel getColumnModel(ObjectField field) {
		ColumnModel model = new ColumnModel();
		model.setName(field.name());
		model.setMaximumSize(field.getSize());
		model.setFacetType(field.getFacetType());
		model.setColumnType(field.getColumnType() == null ? ColumnType.ENTITYID : field.getColumnType());
		return model;
	}
	
	private ColumnMetadata createMetadataForAnnotation(ColumnType type, int id) {
		ColumnModel model = new ColumnModel();
		model.setId(String.valueOf(id));
		model.setName(type.name().toLowerCase());
		model.setColumnType(type);
		return new ColumnMetadata(model, SQLUtils.translateColumnTypeToAnnotationValueName(model.getColumnType()), SQLUtils.getColumnNameForId("" + id), false);
	}

	@Test
	public void testBuildAnnotationSelectString() {
		StringBuilder builder = new StringBuilder();
		ColumnMetadata meta = createMetadataForAnnotation(ColumnType.STRING, 123);
		boolean isDoubleAbstract = false;
		// call under test
		SQLUtils.buildAnnotationSelect(builder, meta, isDoubleAbstract);
		assertEquals(", MAX(IF(A.ANNO_KEY ='string', A.STRING_VALUE, NULL)) AS _C123_", builder.toString());
	}

	@Test
	public void testBuildAnnotationSelectDoubleAbstract() {
		StringBuilder builder = new StringBuilder();
		ColumnMetadata meta = createMetadataForAnnotation(ColumnType.DOUBLE, 123);
		boolean isDoubleAbstract = true;
		// call under test
		SQLUtils.buildAnnotationSelect(builder, meta, isDoubleAbstract);
		assertEquals(", MAX(IF(A.ANNO_KEY ='double', A.DOUBLE_ABSTRACT, NULL)) AS _DBL_C123_", builder.toString());
	}

	@Test
	public void testBuildAnnotationSelectDoubleNotAbstract() {
		StringBuilder builder = new StringBuilder();
		ColumnMetadata meta = createMetadataForAnnotation(ColumnType.DOUBLE, 123);
		boolean isDoubleAbstract = false;
		// call under test
		SQLUtils.buildAnnotationSelect(builder, meta, isDoubleAbstract);
		assertEquals(", MAX(IF(A.ANNO_KEY ='double', A.DOUBLE_VALUE, NULL)) AS _C123_", builder.toString());
	}

	@Test
	public void testBuildObjectReplicationSelectMetadataEntityField() {
		StringBuilder builder = new StringBuilder();
		ColumnMetadata meta = createMetadataForEntityField(ObjectField.createdOn, 1);
		// call under test
		SQLUtils.buildObjectReplicationSelectMetadata(builder, meta);
		assertEquals(", MAX(R.CREATED_ON) AS CREATED_ON", builder.toString());
	}

	@Test
	public void testBuildObjectReplicationSelectMetadataStringAnnotation() {
		StringBuilder builder = new StringBuilder();
		ColumnMetadata meta = createMetadataForAnnotation(ColumnType.STRING, 123);
		// call under test
		SQLUtils.buildObjectReplicationSelectMetadata(builder, meta);
		assertEquals(", MAX(IF(A.ANNO_KEY ='string', A.STRING_VALUE, NULL)) AS _C123_", builder.toString());
	}

	@Test
	public void testBuildObjectReplicationSelectMetadataDoubleAnnotation() {
		StringBuilder builder = new StringBuilder();
		ColumnMetadata meta = createMetadataForAnnotation(ColumnType.DOUBLE, 456);
		// call under test
		SQLUtils.buildObjectReplicationSelectMetadata(builder, meta);
		// Should include two selects, one for the abstract double and the other for the double value.
		assertEquals(
				", MAX(IF(A.ANNO_KEY ='double', A.DOUBLE_ABSTRACT, NULL)) AS _DBL_C456_"
				+ ", MAX(IF(A.ANNO_KEY ='double', A.DOUBLE_VALUE, NULL)) AS _C456_", builder.toString());
	}

	@Test
	public void testBuildObjectReplicationSelectMetadataListAnnotation() {
		//list columns do not have an abstract column
		StringBuilder builder = new StringBuilder();
		ColumnMetadata meta = createMetadataForAnnotation(ColumnType.STRING_LIST, 456);
		// call under test
		SQLUtils.buildObjectReplicationSelectMetadata(builder, meta);
		// Should include two selects, one for the abstract double and the other for the double value.
		assertEquals(", MAX(IF(A.ANNO_KEY ='string_list', A.STRING_LIST_VALUE, NULL)) AS _C456_", builder.toString());
	}

	@Test
	public void testBuildObjectReplicationSelect() {
		StringBuilder builder = new StringBuilder();
		String columnName = TableConstants.OBJECT_REPLICATION_COL_BENEFACTOR_ID;
		// Call under test
		SQLUtils.buildObjectReplicationSelect(builder, columnName);
		assertEquals(", MAX(R.BENEFACTOR_ID) AS BENEFACTOR_ID", builder.toString());
	}

	@Test
	public void testBuildObjectReplicationSelectStandardColumns() {
		StringBuilder builder = new StringBuilder();
		// call under test
		SQLUtils.buildObjectReplicationSelectStandardColumns(builder);
		assertEquals("R.OBJECT_ID"
				+ ", R.OBJECT_VERSION"
				+ ", MAX(R.ETAG) AS ETAG"
				+ ", MAX(R.BENEFACTOR_ID) AS BENEFACTOR_ID", builder.toString());
	}

	@Test
	public void testCreateSelectFromObjectReplication(){
		ColumnMetadata one = createMetadataForAnnotation(ColumnType.STRING, 1);
		ColumnMetadata id = createMetadataForEntityField(ObjectField.id, 2);
		
		String filter = " the-filter";	
		StringBuilder builder = new StringBuilder();
		List<ColumnMetadata> metadata = ImmutableList.of(one, id);
		SQLUtils.createSelectFromObjectReplication(builder, metadata, filter);
		String sql = builder.toString();
		assertEquals("SELECT"
				+ " R.OBJECT_ID,"
				+ " R.OBJECT_VERSION,"
				+ " MAX(R.ETAG) AS ETAG,"
				+ " MAX(R.BENEFACTOR_ID) AS BENEFACTOR_ID,"
				+ " MAX(IF(A.ANNO_KEY ='string', A.STRING_VALUE, NULL)) AS _C1_,"
				+ " MAX(R.OBJECT_ID) AS OBJECT_ID"
				+ " FROM"
				+ " OBJECT_REPLICATION R"
				+ " LEFT JOIN ANNOTATION_REPLICATION A"
				+ " ON(R.OBJECT_TYPE = A.OBJECT_TYPE AND R.OBJECT_ID = A.OBJECT_ID AND R.OBJECT_VERSION = A.OBJECT_VERSION)"
				+ " WHERE"
				+ " the-filter"
				+ " GROUP BY R.OBJECT_ID, R.OBJECT_VERSION ORDER BY R.OBJECT_ID, R.OBJECT_VERSION", sql);
	}

	@Test
	public void createMaxListLengthValidationSQL(){
		Set<String> annotationNames = Sets.newHashSet("foo");
		String filter = " the-filter";	
		String sql = SQLUtils.createAnnotationMaxListLengthSQL(annotationNames, filter);

		assertEquals("SELECT"
				+ " A.ANNO_KEY, MAX(A.LIST_LENGTH)"
				+ " FROM"
				+ " OBJECT_REPLICATION R"
				+ " LEFT JOIN ANNOTATION_REPLICATION A"
				+ " ON(R.OBJECT_TYPE = A.OBJECT_TYPE AND R.OBJECT_ID = A.OBJECT_ID AND R.OBJECT_VERSION = A.OBJECT_VERSION)"
				+ " WHERE"
				+ " the-filter"
				+ " AND A.ANNO_KEY IN (:annotationKeys)"
				+ " GROUP BY A.ANNO_KEY", sql);
	}

	@Test
	public void createMaxListLengthValidationSQL_nullAnnotationNames(){
		Set<String> annotationNames = null;
		String filter = " the-filter";	
		assertThrows(IllegalArgumentException.class, () ->
			SQLUtils.createAnnotationMaxListLengthSQL(annotationNames, filter)
		);

	}

	@Test
	public void createMaxListLengthValidationSQL_emptyAnnotationNames(){
		Set<String> annotationNames = Collections.emptySet();
		String filter = " the-filter";
		assertThrows(IllegalArgumentException.class, () ->
				SQLUtils.createAnnotationMaxListLengthSQL(annotationNames, filter)
		);
	}
	
	@Test
	public void testCreateSelectFromObjectReplicationFilterByRows(){
		ColumnMetadata one = createMetadataForAnnotation(ColumnType.STRING, 1);
		ColumnMetadata id = createMetadataForEntityField(ObjectField.id, 2);
		String filter = " the-filter";
		StringBuilder builder = new StringBuilder();
		List<ColumnMetadata> metadata = ImmutableList.of(one, id);
		// call under test
		SQLUtils.createSelectFromObjectReplication(builder, metadata, filter);
		String sql = builder.toString();
		assertEquals("SELECT"
				+ " R.OBJECT_ID,"
				+ " R.OBJECT_VERSION,"
				+ " MAX(R.ETAG) AS ETAG,"
				+ " MAX(R.BENEFACTOR_ID) AS BENEFACTOR_ID,"
				+ " MAX(IF(A.ANNO_KEY ='string', A.STRING_VALUE, NULL)) AS _C1_,"
				+ " MAX(R.OBJECT_ID) AS OBJECT_ID"
				+ " FROM"
				+ " OBJECT_REPLICATION R"
				+ " LEFT JOIN ANNOTATION_REPLICATION A"
				+ " ON(R.OBJECT_TYPE = A.OBJECT_TYPE AND R.OBJECT_ID = A.OBJECT_ID AND R.OBJECT_VERSION = A.OBJECT_VERSION)"
				+ " WHERE"
				+ " the-filter"
				+ " GROUP BY R.OBJECT_ID, R.OBJECT_VERSION ORDER BY R.OBJECT_ID, R.OBJECT_VERSION", sql);
	}


	@Test
	public void testCreateSelectInsertFromObjectReplication(){
		ColumnMetadata one = createMetadataForAnnotation(ColumnType.STRING, 1);
		ColumnMetadata id = createMetadataForEntityField(ObjectField.id, 2);
		String filter = " the-filter";
		List<ColumnMetadata> metadata = ImmutableList.of(one, id);
		String sql = SQLUtils.createSelectInsertFromObjectReplication(viewId, metadata, filter);
		assertEquals("INSERT INTO T123(ROW_ID, ROW_VERSION, ROW_ETAG, ROW_BENEFACTOR, _C1_, _C2_)"
				+ " SELECT"
				+ " R.OBJECT_ID,"
				+ " R.OBJECT_VERSION,"
				+ " MAX(R.ETAG) AS ETAG,"
				+ " MAX(R.BENEFACTOR_ID) AS BENEFACTOR_ID,"
				+ " MAX(IF(A.ANNO_KEY ='string', A.STRING_VALUE, NULL)) AS _C1_,"
				+ " MAX(R.OBJECT_ID) AS OBJECT_ID"
				+ " FROM"
				+ " OBJECT_REPLICATION R"
				+ " LEFT JOIN ANNOTATION_REPLICATION A"
				+ " ON(R.OBJECT_TYPE = A.OBJECT_TYPE AND R.OBJECT_ID = A.OBJECT_ID AND R.OBJECT_VERSION = A.OBJECT_VERSION)"
				+ " WHERE"
				+ " the-filter"
				+ " GROUP BY R.OBJECT_ID, R.OBJECT_VERSION ORDER BY R.OBJECT_ID, R.OBJECT_VERSION", sql);
	}

	@Test
	public void testCreateSelectInsertFromObjectReplicationWithDouble(){
		ColumnMetadata one = createMetadataForAnnotation(ColumnType.DOUBLE, 3);
		String filter = " the-filter";
		List<ColumnMetadata> metadata = ImmutableList.of(one);
		String sql = SQLUtils.createSelectInsertFromObjectReplication(viewId, metadata, filter);
		assertEquals("INSERT INTO T123(ROW_ID, ROW_VERSION, ROW_ETAG, ROW_BENEFACTOR, _DBL_C3_, _C3_)"
				+ " SELECT"
				+ " R.OBJECT_ID, R.OBJECT_VERSION,"
				+ " MAX(R.ETAG) AS ETAG,"
				+ " MAX(R.BENEFACTOR_ID) AS BENEFACTOR_ID,"
				+ " MAX(IF(A.ANNO_KEY ='double', A.DOUBLE_ABSTRACT, NULL)) AS _DBL_C3_,"
				+ " MAX(IF(A.ANNO_KEY ='double', A.DOUBLE_VALUE, NULL)) AS _C3_"
				+ " FROM OBJECT_REPLICATION R"
				+ " LEFT JOIN ANNOTATION_REPLICATION A"
				+ " ON(R.OBJECT_TYPE = A.OBJECT_TYPE AND R.OBJECT_ID = A.OBJECT_ID AND R.OBJECT_VERSION = A.OBJECT_VERSION)"
				+ " WHERE"
				+ " the-filter"
				+ " GROUP BY R.OBJECT_ID, R.OBJECT_VERSION ORDER BY R.OBJECT_ID, R.OBJECT_VERSION", sql);
	}

	@Test
	public void testBuildSelectRowIds(){
		RowReference ref1 = new RowReference();
		ref1.setRowId(222L);
		RowReference ref2 = new RowReference();
		ref2.setRowId(333L);

		ColumnModel c1 = TableModelTestUtils.createColumn(1L);
		ColumnModel c2 = TableModelTestUtils.createColumn(2L);

		String sql = SQLUtils.buildSelectRowIds("syn123", Lists.newArrayList(ref1, ref2), Lists.newArrayList(c1,  c2));
		String expected = "SELECT \"col_1\", \"col_2\" FROM syn123 WHERE ROW_ID IN (222, 333)";
		assertEquals(expected, sql);
	}

	@Test
	public void testBuildSelectRowIdsColumnsWithDoubleQuotes(){
		RowReference ref1 = new RowReference();
		ref1.setRowId(222L);
		RowReference ref2 = new RowReference();
		ref2.setRowId(333L);

		ColumnModel c1 = TableModelTestUtils.createColumn(1L);
		ColumnModel c2 = TableModelTestUtils.createColumn(2L, "\"quoted\"Name", ColumnType.STRING);

		String sql = SQLUtils.buildSelectRowIds("syn123", Lists.newArrayList(ref1, ref2), Lists.newArrayList(c1,  c2));
		String expected = "SELECT \"col_1\", \"\"\"quoted\"\"Name\" FROM syn123 WHERE ROW_ID IN (222, 333)";
		assertEquals(expected, sql);
	}

	@Test
	public void testBuildSelectRowIdsNullRefts(){
		RowReference ref1 = new RowReference();
		ref1.setRowId(222L);
		RowReference ref2 = new RowReference();
		ref2.setRowId(333L);

		ColumnModel c1 = TableModelTestUtils.createColumn(1L);
		ColumnModel c2 = TableModelTestUtils.createColumn(2L);

		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			SQLUtils.buildSelectRowIds("syn123", null, Lists.newArrayList(c1,  c2));
		});
	}

	@Test
	public void testBuildSelectRowIdsNullColumns(){
		RowReference ref1 = new RowReference();
		ref1.setRowId(222L);
		RowReference ref2 = new RowReference();
		ref2.setRowId(333L);

		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			SQLUtils.buildSelectRowIds("syn123", Lists.newArrayList(ref1, ref2), null);
		});
	}

	@Test
	public void testBuildSelectRowIdsEmptyRefs(){
		ColumnModel c1 = TableModelTestUtils.createColumn(1L);
		ColumnModel c2 = TableModelTestUtils.createColumn(2L);

		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			SQLUtils.buildSelectRowIds("syn123", new LinkedList<RowReference>(), Lists.newArrayList(c1,  c2));
		});
	}

	@Test
	public void testBuildSelectRowIdsEmptyColumns(){
		RowReference ref1 = new RowReference();
		ref1.setRowId(222L);
		RowReference ref2 = new RowReference();
		ref2.setRowId(333L);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			SQLUtils.buildSelectRowIds("syn123", Lists.newArrayList(ref1, ref2), new LinkedList<ColumnModel>());
		});
	}

	@Test
	public void testBuildSelectRowIdsNullRefRowId(){
		RowReference ref1 = new RowReference();
		ref1.setRowId(null);
		RowReference ref2 = new RowReference();
		ref2.setRowId(333L);

		ColumnModel c1 = TableModelTestUtils.createColumn(1L);
		ColumnModel c2 = TableModelTestUtils.createColumn(2L);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			SQLUtils.buildSelectRowIds("syn123", Lists.newArrayList(ref1, ref2), Lists.newArrayList(c1,  c2));
		});
	}

	@Test
	public void testMatchChangesToCurrentInfoOldExists(){
		DatabaseColumnInfo rowId = new DatabaseColumnInfo();
		rowId.setColumnName(TableConstants.ROW_ID);
		DatabaseColumnInfo one = new DatabaseColumnInfo();
		one.setColumnName("_C111_");
		one.setColumnType(ColumnType.STRING);
		DatabaseColumnInfo two = new DatabaseColumnInfo();
		two.setColumnName("_C222_");
		two.setColumnType(ColumnType.STRING);
		List<DatabaseColumnInfo> curretIndexSchema = Lists.newArrayList(rowId, one, two);
		// the old exists in the current.
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("222");
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("333");
		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, newColumn);

		List<ColumnChangeDetails> changes = Lists.newArrayList(change);

		// call under test
		List<ColumnChangeDetails> results = SQLUtils.matchChangesToCurrentInfo(curretIndexSchema, changes);
		// the results should be unchanged.
		assertEquals(changes, results);
		ColumnChangeDetails updated = results.get(0);
		// should not be the same instance.
		assertNotSame(change, updated);
		assertEquals(oldColumn, updated.getOldColumn());
		assertEquals(two, updated.getOldColumnInfo());
		assertEquals(newColumn, updated.getNewColumn());
	}

	@Test
	public void testMatchChangesToCurrentInfoOldNotExistNewExists(){
		DatabaseColumnInfo rowId = new DatabaseColumnInfo();
		rowId.setColumnName(TableConstants.ROW_ID);
		DatabaseColumnInfo one = new DatabaseColumnInfo();
		one.setColumnName("_C111_");
		one.setColumnType(ColumnType.STRING);
		DatabaseColumnInfo three = new DatabaseColumnInfo();
		three.setColumnName("_C333_");
		three.setColumnType(ColumnType.STRING);
		List<DatabaseColumnInfo> curretIndexSchema = Lists.newArrayList(rowId, one, three);
		// the old column was already deleted
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("222");
		// the new column was already added
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("333");
		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, newColumn);

		List<ColumnChangeDetails> changes = Lists.newArrayList(change);

		// call under test
		List<ColumnChangeDetails> results = SQLUtils.matchChangesToCurrentInfo(curretIndexSchema, changes);
		// the results should be changed.
		assertNotNull(results);
		ColumnChangeDetails updated = results.get(0);
		// should not be the same instance.
		assertNotSame(change, updated);
		assertNull(updated.getOldColumnInfo());
		assertNull(updated.getOldColumn());
		assertNull(updated.getNewColumn());
	}

	@Test
	public void testMatchChangesToCurrentInfoNewAndOldExists(){
		DatabaseColumnInfo rowId = new DatabaseColumnInfo();
		rowId.setColumnName(TableConstants.ROW_ID);
		DatabaseColumnInfo one = new DatabaseColumnInfo();
		one.setColumnName("_C111_");
		one.setColumnType(ColumnType.STRING);
		DatabaseColumnInfo two = new DatabaseColumnInfo();
		two.setColumnName("_C222_");
		two.setColumnType(ColumnType.STRING);
		DatabaseColumnInfo three = new DatabaseColumnInfo();
		three.setColumnName("_C333_");
		three.setColumnType(ColumnType.STRING);
		List<DatabaseColumnInfo> curretIndexSchema = Lists.newArrayList(rowId, one, two, three);
		// the old column was not yet deleted
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("222");
		// the new column was already added
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("333");
		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, newColumn);

		List<ColumnChangeDetails> changes = Lists.newArrayList(change);

		// call under test
		List<ColumnChangeDetails> results = SQLUtils.matchChangesToCurrentInfo(curretIndexSchema, changes);
		// the results should be changed.
		assertNotNull(results);
		ColumnChangeDetails updated = results.get(0);
		// should not be the same instance.
		assertNotSame(change, updated);
		assertEquals(oldColumn, updated.getOldColumn());
		assertEquals(two, updated.getOldColumnInfo());
		assertNull(updated.getNewColumn());
	}

	@Test
	public void testMatchChangesToCurrentInfoSameNewAndOldNotExistInDatabase(){
		DatabaseColumnInfo rowId = new DatabaseColumnInfo();
		rowId.setColumnName(TableConstants.ROW_ID);
		DatabaseColumnInfo one = new DatabaseColumnInfo();
		one.setColumnName("_C111_");
		one.setColumnType(ColumnType.STRING);
		List<DatabaseColumnInfo> curretIndexSchema = Lists.newArrayList(rowId, one);

		//old and new column are the same
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("222");
		ColumnModel newColumn = oldColumn;
		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, newColumn);

		List<ColumnChangeDetails> changes = Lists.newArrayList(change);

		// call under test
		List<ColumnChangeDetails> results = SQLUtils.matchChangesToCurrentInfo(curretIndexSchema, changes);
		// the results should be changed.
		assertNotNull(results);
		ColumnChangeDetails updated = results.get(0);
		// should not be the same instance.
		assertNotSame(change, updated);
		assertNull(updated.getOldColumn());
		assertNull(updated.getOldColumnInfo());
		assertEquals(newColumn, updated.getNewColumn());
	}


	@Test
	public void testMatchChangesToCurrentInfoSameNewAndOldExistInDatabase(){
		DatabaseColumnInfo rowId = new DatabaseColumnInfo();
		rowId.setColumnName(TableConstants.ROW_ID);
		DatabaseColumnInfo one = new DatabaseColumnInfo();
		one.setColumnName("_C111_");
		one.setColumnType(ColumnType.STRING);
		DatabaseColumnInfo two = new DatabaseColumnInfo();
		two.setColumnName("_C222_");
		two.setColumnType(ColumnType.STRING);
		List<DatabaseColumnInfo> curretIndexSchema = Lists.newArrayList(rowId, one, two);
		//old and new column are the same
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("222");
		ColumnModel newColumn = oldColumn;
		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, newColumn);

		List<ColumnChangeDetails> changes = Lists.newArrayList(change);

		// call under test
		List<ColumnChangeDetails> results = SQLUtils.matchChangesToCurrentInfo(curretIndexSchema, changes);
		// the results should be changed.
		assertNotNull(results);
		ColumnChangeDetails updated = results.get(0);
		// should not be the same instance.
		assertNotSame(change, updated);
		assertNotNull(updated.getNewColumn());
		assertNotNull(updated.getOldColumn());
		assertEquals(two, updated.getOldColumnInfo());
		assertEquals(updated.getNewColumn(), updated.getOldColumn());
	}

	@Test
	public void testMatchChangesToCurrentInfoOldDoesNotExist(){
		DatabaseColumnInfo rowId = new DatabaseColumnInfo();
		rowId.setColumnName(TableConstants.ROW_ID);
		DatabaseColumnInfo one = new DatabaseColumnInfo();
		one.setColumnName("_C111_");
		one.setColumnType(ColumnType.STRING);
		DatabaseColumnInfo two = new DatabaseColumnInfo();
		two.setColumnName("_C222_");
		two.setColumnType(ColumnType.STRING);
		List<DatabaseColumnInfo> curretIndexSchema = Lists.newArrayList(rowId, one, two);

		// the old does not exist in the current
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("333");
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("444");
		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, newColumn);

		List<ColumnChangeDetails> changes = Lists.newArrayList(change);

		// call under test
		List<ColumnChangeDetails> results = SQLUtils.matchChangesToCurrentInfo(curretIndexSchema, changes);
		// the results should be changed
		assertNotNull(results);
		assertEquals(1, results.size());
		ColumnChangeDetails updated = results.get(0);
		// should not be the same instance.
		assertNotSame(change, updated);
		assertNull(updated.getOldColumn());
		assertNull(updated.getOldColumnInfo());
		assertEquals(newColumn, updated.getNewColumn());
	}

	@Test
	public void testMatchChangesToCurrentInfoCurrentEmpty(){
		List<DatabaseColumnInfo> curretIndexSchema = new LinkedList<>();
		// the old does not exist in the current
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("333");
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("444");
		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, newColumn);

		List<ColumnChangeDetails> changes = Lists.newArrayList(change);

		// call under test
		List<ColumnChangeDetails> results = SQLUtils.matchChangesToCurrentInfo(curretIndexSchema, changes);
		// the results should be changed
		assertNotNull(results);
		assertEquals(1, results.size());
		ColumnChangeDetails updated = results.get(0);
		// should not be the same instance.
		assertNotSame(change, updated);
		assertNull(updated.getOldColumn());
		assertNull(updated.getOldColumnInfo());
		assertEquals(newColumn, updated.getNewColumn());
	}

	/**
	 * This is a test case for PLFM-4235 where a view's string columns were
	 * set to be too small for the annotation values.
	 */
	@Test
	public void testDetermineCauseOfExceptionTooSmall() {
		Exception oringal = new Exception("Some exception");
		ColumnModel columnModel = new ColumnModel();
		columnModel.setName("foo");
		columnModel.setColumnType(ColumnType.STRING);
		columnModel.setMaximumSize(10L);

		ColumnModel annotationModel = new ColumnModel();
		annotationModel.setName("foo");
		annotationModel.setColumnType(ColumnType.STRING);
		annotationModel.setMaximumSize(11L);
		try {
			// call under test
			SQLUtils.determineCauseOfException(oringal, columnModel,
					annotationModel);
			fail("Should have failed.");
		} catch (IllegalArgumentException expected) {
			assertEquals(
					"The size of the column 'foo' is too small.  The column size needs to be at least 11 characters.",
					expected.getMessage());
			// the cause should be kept
			assertEquals(oringal, expected.getCause());
		}
	}

	@Test
	public void testDetermineCauseOfExceptionSameSize() {
		Exception oringal = new Exception("Some exception");
		ColumnModel columnModel = new ColumnModel();
		columnModel.setName("foo");
		columnModel.setColumnType(ColumnType.STRING);
		columnModel.setMaximumSize(10L);

		ColumnModel annotationModel = new ColumnModel();
		annotationModel.setName("foo");
		annotationModel.setColumnType(ColumnType.STRING);
		annotationModel.setMaximumSize(10L);
		// call under test
		SQLUtils.determineCauseOfException(oringal, columnModel, annotationModel);
	}

	@Test
	public void testDetermineCauseOfExceptionNameDoesNotMatch() {
		Exception oringal = new Exception("Some exception");
		ColumnModel columnModel = new ColumnModel();
		columnModel.setName("foo");
		columnModel.setColumnType(ColumnType.STRING);
		columnModel.setMaximumSize(10L);

		ColumnModel annotationModel = new ColumnModel();
		annotationModel.setName("bar");
		annotationModel.setColumnType(ColumnType.STRING);
		annotationModel.setMaximumSize(11L);
		// call under test
		SQLUtils.determineCauseOfException(oringal, columnModel, annotationModel);
	}

	@Test
	public void testDetermineCauseOfExceptionTypeDoesNotMatch() {
		Exception oringal = new Exception("Some exception");
		ColumnModel columnModel = new ColumnModel();
		columnModel.setName("foo");
		columnModel.setColumnType(ColumnType.STRING);
		columnModel.setMaximumSize(10L);

		ColumnModel annotationModel = new ColumnModel();
		annotationModel.setName("foo");
		annotationModel.setColumnType(ColumnType.INTEGER);
		annotationModel.setMaximumSize(11L);
		// call under test
		SQLUtils.determineCauseOfException(oringal, columnModel, annotationModel);
	}


	/**
	 * <p>
	 * The original fix to PLFM-4260 involved throwing an exception when trying to
	 * map a string annotation to a boolean column. However, the fix for PLFM-4864
	 * involved changing how we copy data from the ANNOTATION_REPLICATION table to
	 * views. We now only copy data from ANNOTATION_REPLICATION from columns that
	 * match the view column type. As a result, the only type of exception that can
	 * occur is when a string annotation is too large of for a string column.
	 * </p>
	 * This means the original condition in PLFM-4260 will no longer result in an
	 * error, so we should not throw an exception for this case.
	 * <p>
	 * This is part of the fix for PLFM-5348.
	 */
	@Test
	public void testDetermineCauseOfExceptionWrongType() {
		Exception oringal = new Exception("Some exception");
		ColumnModel columnModel = new ColumnModel();
		columnModel.setName("foo");
		columnModel.setColumnType(ColumnType.BOOLEAN);

		ColumnModel annotationModel = new ColumnModel();
		annotationModel.setName("foo");
		annotationModel.setColumnType(ColumnType.STRING);
		annotationModel.setMaximumSize(11L);
		// call under test - this should not throw an exception
		SQLUtils.determineCauseOfException(oringal, columnModel, annotationModel);
	}
	
	/**
	 * This test was added for PLFM-6745.
	 * 
	 */
	@Test
	public void testDetermineCauseOfExceptionWithBooleanAnnotation() {
		Exception oringal = new Exception("Some exception");
		ColumnModel columnModel = new ColumnModel();
		columnModel.setName("foo");
		columnModel.setColumnType(ColumnType.STRING);
		columnModel.setMaximumSize(12L);

		ColumnModel annotationModel = new ColumnModel();
		annotationModel.setName("foo");
		annotationModel.setColumnType(ColumnType.BOOLEAN);
		// call under test
		SQLUtils.determineCauseOfException(oringal, columnModel, annotationModel);
	}
	
	/**
	 * This test was added for PLFM-6745.
	 * 
	 */
	@Test
	public void testDetermineCauseOfExceptionWithNullMaxSizeAnnotation() {
		Exception oringal = new Exception("Some exception");
		ColumnModel columnModel = new ColumnModel();
		columnModel.setName("foo");
		columnModel.setColumnType(ColumnType.STRING);
		columnModel.setMaximumSize(12L);

		ColumnModel annotationModel = new ColumnModel();
		annotationModel.setName("foo");
		annotationModel.setColumnType(ColumnType.STRING);
		annotationModel.setMaximumSize(null);
		
		// call under test
		SQLUtils.determineCauseOfException(oringal, columnModel, annotationModel);
	}
	
	/**
	 * This test was added for PLFM-6745.
	 * 
	 */
	@Test
	public void testDetermineCauseOfExceptionWithNullMaxSizeColumn() {
		Exception oringal = new Exception("Some exception");
		ColumnModel columnModel = new ColumnModel();
		columnModel.setName("foo");
		columnModel.setColumnType(ColumnType.STRING);
		columnModel.setMaximumSize(null);

		ColumnModel annotationModel = new ColumnModel();
		annotationModel.setName("foo");
		annotationModel.setColumnType(ColumnType.STRING);
		annotationModel.setMaximumSize(12L);
		
		// call under test
		SQLUtils.determineCauseOfException(oringal, columnModel, annotationModel);
	}

	@Test
	public void testGetDistinctAnnotationColumnsSql(){
		String filter = " the-filter";
		String sql = SQLUtils.getDistinctAnnotationColumnsSql(filter);
		sql = sql.replaceAll(LINE_SEPERATOR, "");
		String expected = 
				"SELECT "
				+ "	A.ANNO_KEY,"
				+ "    GROUP_CONCAT(DISTINCT A.ANNO_TYPE),"
				+ "    MAX(MAX_STRING_LENGTH),"
				+ "    MAX(LIST_LENGTH)"
				+ "		FROM OBJECT_REPLICATION AS R"
				+ "			INNER JOIN ANNOTATION_REPLICATION AS A ON (R.OBJECT_TYPE = A.OBJECT_TYPE AND R.OBJECT_ID = A.OBJECT_ID AND R.OBJECT_VERSION = A.OBJECT_VERSION)"
				+ "				WHERE  the-filter"
				+ "        GROUP BY A.ANNO_KEY LIMIT :pLimit OFFSET :pOffset";
		assertEquals(expected, sql);
	}
	
	@Test
	public void testWriteAnnotationDtoToPreparedStatementWithIsDerivedFalse() throws SQLException{
		// string value
		annotationDto.setDerived(false);
		
		// Call under test
		SQLUtils.writeAnnotationDtoToPreparedStatement(ReplicationType.ENTITY, mockPreparedStatement, annotationDto);
		
		verify(mockPreparedStatement).setBoolean(16, false);
		verify(mockPreparedStatement).setBoolean(27, false);
	}


	@Test
	public void testWriteAnnotationDtoToPreparedStatementString() throws SQLException{
		// string value
		annotationDto.setValue("someString");
		// Call under test
		SQLUtils.writeAnnotationDtoToPreparedStatement(ReplicationType.ENTITY, mockPreparedStatement, annotationDto);
		
		verify(mockPreparedStatement).setString(1, ViewObjectType.ENTITY.name());
		verify(mockPreparedStatement).setLong(2, annotationDto.getObjectId());
		verify(mockPreparedStatement).setLong(3, annotationDto.getObjectVersion());
		verify(mockPreparedStatement).setString(4, annotationDto.getKey());
		verify(mockPreparedStatement).setString(5, annotationDto.getType().name());
		verify(mockPreparedStatement).setString(6, annotationDto.getValue().get(0));
		// all others should be set to null since the string cannot be converted to any other type.
		verify(mockPreparedStatement).setNull(7, Types.BIGINT);
		verify(mockPreparedStatement).setNull(8, Types.DOUBLE);
		verify(mockPreparedStatement).setNull(9, Types.VARCHAR);
		verify(mockPreparedStatement).setNull(10, Types.BOOLEAN);
	}

	@Test
	public void testWriteAnnotationDtoToPreparedStatementStringList() throws SQLException{
		// string value
		annotationDto.setValue(Arrays.asList("abc", "defg"));
		// Call under test
		SQLUtils.writeAnnotationDtoToPreparedStatement(ReplicationType.ENTITY, mockPreparedStatement, annotationDto);
		
		verify(mockPreparedStatement).setString(1, ViewObjectType.ENTITY.name());
		verify(mockPreparedStatement).setLong(2, annotationDto.getObjectId());
		verify(mockPreparedStatement).setLong(3, annotationDto.getObjectVersion());
		verify(mockPreparedStatement).setString(4, annotationDto.getKey());
		verify(mockPreparedStatement).setString(5, annotationDto.getType().name());
		verify(mockPreparedStatement).setString(6, annotationDto.getValue().get(0));
		// all others should be set to null since the string cannot be converted to any other type.
		verify(mockPreparedStatement).setNull(7, Types.BIGINT);
		verify(mockPreparedStatement).setNull(8, Types.DOUBLE);
		verify(mockPreparedStatement).setNull(9, Types.VARCHAR);
		verify(mockPreparedStatement).setNull(10, Types.BOOLEAN);

		verify(mockPreparedStatement).setString(11, "[\"abc\",\"defg\"]");
		verify(mockPreparedStatement).setString(12, null);
		verify(mockPreparedStatement).setString(13, null);
		verify(mockPreparedStatement).setLong(14, 4);
		verify(mockPreparedStatement).setLong(15, 2);

	}

	@Test
	public void testWriteAnnotationDtoToPreparedStatementBooleanTrue() throws SQLException{
		// string value
		annotationDto.setValue("True");
		// Call under test
		SQLUtils.writeAnnotationDtoToPreparedStatement(ReplicationType.ENTITY, mockPreparedStatement, annotationDto);
		// type can be set as a boolean.
		verify(mockPreparedStatement).setNull(7, Types.BIGINT);
		verify(mockPreparedStatement).setNull(8, Types.DOUBLE);
		verify(mockPreparedStatement).setNull(9, Types.VARCHAR);
		verify(mockPreparedStatement).setBoolean(10, Boolean.TRUE);
	}

	@Test
	public void testWriteAnnotationDtoToPreparedStatementBooleanFalse() throws SQLException{
		// string value
		annotationDto.setValue("false");
		// Call under test
		SQLUtils.writeAnnotationDtoToPreparedStatement(ReplicationType.ENTITY, mockPreparedStatement, annotationDto);
		// type can be set as a boolean.
		verify(mockPreparedStatement).setNull(7, Types.BIGINT);
		verify(mockPreparedStatement).setNull(8, Types.DOUBLE);
		verify(mockPreparedStatement).setNull(9, Types.VARCHAR);
		verify(mockPreparedStatement).setBoolean(10, Boolean.FALSE);
	}

	@Test
	public void testWriteAnnotationDtoToPreparedStatementBooleanList() throws SQLException{
		// string value
		annotationDto.setValue(Arrays.asList("false", "true", "false"));
		// Call under test
		SQLUtils.writeAnnotationDtoToPreparedStatement(ReplicationType.ENTITY, mockPreparedStatement, annotationDto);

		verify(mockPreparedStatement).setString(1, ViewObjectType.ENTITY.name());
		verify(mockPreparedStatement).setLong(2, annotationDto.getObjectId());
		verify(mockPreparedStatement).setLong(3, annotationDto.getObjectVersion());
		verify(mockPreparedStatement).setString(4, annotationDto.getKey());
		verify(mockPreparedStatement).setString(5, annotationDto.getType().name());
		verify(mockPreparedStatement).setString(6, annotationDto.getValue().get(0));

		// type can be set as a boolean.
		verify(mockPreparedStatement).setNull(7, Types.BIGINT);
		verify(mockPreparedStatement).setNull(8, Types.DOUBLE);
		verify(mockPreparedStatement).setNull(9, Types.VARCHAR);
		verify(mockPreparedStatement).setBoolean(10, Boolean.FALSE);

		verify(mockPreparedStatement).setString(11, "[\"false\",\"true\",\"false\"]");
		verify(mockPreparedStatement).setString(12, null);
		verify(mockPreparedStatement).setString(13, "[false,true,false]");
		verify(mockPreparedStatement).setLong(14, 5);
		verify(mockPreparedStatement).setLong(15, 3);
	}

	@Test
	public void testWriteAnnotationDtoToPreparedStatementSynapseId() throws SQLException{
		// string value
		annotationDto.setValue("syn123456");
		// Call under test
		SQLUtils.writeAnnotationDtoToPreparedStatement(ReplicationType.ENTITY, mockPreparedStatement, annotationDto);
		// the synapse ID can be set as a long.
		verify(mockPreparedStatement).setLong(7, 123456L);
		verify(mockPreparedStatement).setNull(8, Types.DOUBLE);
		verify(mockPreparedStatement).setNull(9, Types.VARCHAR);
		verify(mockPreparedStatement).setNull(10, Types.BOOLEAN);
	}

	@Test
	public void testWriteAnnotationDtoToPreparedStatementDateString() throws SQLException{
		// string value
		annotationDto.setValue("1970-1-1 00:00:00.123");
		// Call under test
		SQLUtils.writeAnnotationDtoToPreparedStatement(ReplicationType.ENTITY, mockPreparedStatement, annotationDto);
		// the date string can be treated as a long.
		verify(mockPreparedStatement).setLong(7, 123L);
		verify(mockPreparedStatement).setNull(8, Types.DOUBLE);
		verify(mockPreparedStatement).setNull(9, Types.VARCHAR);
		verify(mockPreparedStatement).setNull(10, Types.BOOLEAN);
	}

	@Test
	public void testWriteAnnotationDtoToPreparedStatementLong() throws SQLException{
		// string value
		annotationDto.setValue("123");
		// Call under test
		SQLUtils.writeAnnotationDtoToPreparedStatement(ReplicationType.ENTITY, mockPreparedStatement, annotationDto);
		// can be a long or a double
		verify(mockPreparedStatement).setLong(7, 123L);
		verify(mockPreparedStatement).setDouble(8, 123);
		verify(mockPreparedStatement).setNull(9, Types.VARCHAR);
		verify(mockPreparedStatement).setNull(10, Types.BOOLEAN);
	}


	@Test
	public void testWriteAnnotationDtoToPreparedStatementLongList() throws SQLException{
		// string value
		annotationDto.setValue(Arrays.asList("123", "4560", "789"));
		// Call under test
		SQLUtils.writeAnnotationDtoToPreparedStatement(ReplicationType.ENTITY, mockPreparedStatement, annotationDto);
		
		verify(mockPreparedStatement).setString(1, ViewObjectType.ENTITY.name());
		verify(mockPreparedStatement).setLong(2, annotationDto.getObjectId());
		verify(mockPreparedStatement).setLong(3, annotationDto.getObjectVersion());
		verify(mockPreparedStatement).setString(4, annotationDto.getKey());
		verify(mockPreparedStatement).setString(5, annotationDto.getType().name());
		verify(mockPreparedStatement).setString(6, annotationDto.getValue().get(0));

		// can be a long or a double
		verify(mockPreparedStatement).setLong(7, 123L);
		verify(mockPreparedStatement).setDouble(8, 123);
		verify(mockPreparedStatement).setNull(9, Types.VARCHAR);
		verify(mockPreparedStatement).setNull(10, Types.BOOLEAN);

		verify(mockPreparedStatement).setString(11, "[\"123\",\"4560\",\"789\"]");
		verify(mockPreparedStatement).setString(12, "[123,4560,789]");
		verify(mockPreparedStatement).setString(13, null);
		verify(mockPreparedStatement).setLong(14, 4);
		verify(mockPreparedStatement).setLong(15, 3);

	}

	@Test
	public void testWriteAnnotationDtoToPreparedStatementFiniteDouble() throws SQLException{
		// string value
		annotationDto.setValue("123.456");
		// Call under test
		SQLUtils.writeAnnotationDtoToPreparedStatement(ReplicationType.ENTITY, mockPreparedStatement, annotationDto);
		// value can be a double
		verify(mockPreparedStatement).setNull(7, Types.BIGINT);
		verify(mockPreparedStatement).setDouble(8, 123.456);
		// 7 is the abstract enum for doubles.  Null since this is a finite value
		verify(mockPreparedStatement).setNull(9, Types.VARCHAR);
		verify(mockPreparedStatement).setNull(10, Types.BOOLEAN);
	}

	@Test
	public void testWriteAnnotationDtoToPreparedStatementDoubleNaN() throws SQLException{
		// string value
		annotationDto.setValue("NAN");
		// Call under test
		SQLUtils.writeAnnotationDtoToPreparedStatement(ReplicationType.ENTITY, mockPreparedStatement, annotationDto);
		verify(mockPreparedStatement).setNull(7, Types.BIGINT);
		// the approximation of NaN is null.
		verify(mockPreparedStatement).setNull(8, Types.DOUBLE);
		// 7 is the abstract enum for doubles.  Null since this is a finite value
		verify(mockPreparedStatement).setString(9, AbstractDouble.NAN.getEnumerationValue());
		verify(mockPreparedStatement).setNull(10, Types.BOOLEAN);
	}

	@Test
	public void testWriteAnnotationDtoToPreparedStatementInfinity() throws SQLException{
		// string value
		annotationDto.setValue("+Infinity");
		// Call under test
		SQLUtils.writeAnnotationDtoToPreparedStatement(ReplicationType.ENTITY, mockPreparedStatement, annotationDto);
		verify(mockPreparedStatement).setNull(7, Types.BIGINT);
		verify(mockPreparedStatement).setDouble(8, AbstractDouble.POSITIVE_INFINITY.getApproximateValue());
		verify(mockPreparedStatement).setString(9, AbstractDouble.POSITIVE_INFINITY.getEnumerationValue());
		verify(mockPreparedStatement).setNull(10, Types.BOOLEAN);
	}

	@Test
	public void testWriteAnnotationDtoToPreparedStatementNegativeInfinity() throws SQLException{
		// string value
		annotationDto.setValue("-Infinity");
		// Call under test
		SQLUtils.writeAnnotationDtoToPreparedStatement(ReplicationType.ENTITY, mockPreparedStatement, annotationDto);
		verify(mockPreparedStatement).setNull(7, Types.BIGINT);
		verify(mockPreparedStatement).setDouble(8, AbstractDouble.NEGATIVE_INFINITY.getApproximateValue());
		verify(mockPreparedStatement).setString(9, AbstractDouble.NEGATIVE_INFINITY.getEnumerationValue());
		verify(mockPreparedStatement).setNull(10, Types.BOOLEAN);
	}
		
	@Test
	public void testCreateInsertIntoTableIndex() {
		String[] headers = new String[] {"foo","bar"};
		tableId = IdAndVersion.parse("syn999.23");
		// call under test
		String sql = SQLUtils.createInsertIntoTableIndex(tableId, headers);
		assertEquals("INSERT INTO T999_23 (foo,bar) VALUES  (?,?)", sql);
	}
	
	@Test
	public void testCalcualteBytes() {
		String[] row = new String[] {"foo","barbar"};
		long bytes = SQLUtils.calculateBytes(row);
		assertEquals(3*4+6*4, bytes);
	}

	@Test
	public void testCreateListColumnIndexTable__nullTableId(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.STRING_LIST);
		columnModel.setId("0");
		columnModel.setMaximumSize(42L);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			SQLUtils.createListColumnIndexTable(null, columnModel, false);
		}).getMessage();
		assertEquals("tableIdAndVersion is required.", errorMessage);
	}

	@Test
	public void testCreateListColumnIndexTable__nullColumnModel(){
		ColumnModel nullColumnModel = null;
		String errorMessage = assertThrows(IllegalArgumentException.class, () ->{
			SQLUtils.createListColumnIndexTable(tableId, nullColumnModel, false);
		}).getMessage();
		assertEquals("columnModel is required.", errorMessage);
	}

	@Test
	public void testCreateListColumnIndexTable__columnModelNotListType(){
		ColumnModel columnModel = new ColumnModel();
		columnModel.setColumnType(ColumnType.STRING);
		columnModel.setId("0");
		columnModel.setMaximumSize(42L);

		String errorMessage = assertThrows(IllegalArgumentException.class, () ->{
			SQLUtils.createListColumnIndexTable(tableId, columnModel, false);
		}).getMessage();

		assertEquals("columnModel's type must be a LIST type", errorMessage);
	}

	@Test
	public void testCreateListColumnIndexTable_alterTempTrue(){
		ColumnModel columnInfo = new ColumnModel();
		columnInfo.setColumnType(ColumnType.STRING_LIST);
		columnInfo.setId("0");
		columnInfo.setMaximumSize(42L);
		String sql = SQLUtils.createListColumnIndexTable(tableId, columnInfo, false);
		String expected = "CREATE TABLE IF NOT EXISTS T999_INDEX_C0_ (" +
				"ROW_ID_REF_C0_ BIGINT NOT NULL, " +
				"INDEX_NUM BIGINT NOT NULL, " +
				"_C0__UNNEST VARCHAR(42) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'STRING', " +
				"PRIMARY KEY (ROW_ID_REF_C0_, INDEX_NUM)," +
				" INDEX _C0__UNNEST_IDX (_C0__UNNEST ASC)," +
				" CONSTRAINT T999_INDEX_C0__ibfk_FK FOREIGN KEY (ROW_ID_REF_C0_) REFERENCES T999(ROW_ID) ON DELETE CASCADE);";
		assertEquals(expected, sql);
	}

	@Test
	public void testCreateListColumnIndexTable_alterTempFalse(){
		ColumnModel columnInfo = new ColumnModel();
		columnInfo.setColumnType(ColumnType.STRING_LIST);
		columnInfo.setId("0");
		columnInfo.setMaximumSize(42L);
		String sql = SQLUtils.createListColumnIndexTable(tableId, columnInfo, true);
		String expected = "CREATE TABLE IF NOT EXISTS TEMPT999_INDEX_C0_ (" +
				"ROW_ID_REF_C0_ BIGINT NOT NULL, " +
				"INDEX_NUM BIGINT NOT NULL, " +
				"_C0__UNNEST VARCHAR(42) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'STRING', " +
				"PRIMARY KEY (ROW_ID_REF_C0_, INDEX_NUM)," +
				" INDEX _C0__UNNEST_IDX (_C0__UNNEST ASC)," +
				" CONSTRAINT TEMPT999_INDEX_C0__ibfk_FK FOREIGN KEY (ROW_ID_REF_C0_) REFERENCES TEMPT999(ROW_ID) ON DELETE CASCADE);";
		assertEquals(expected, sql);
	}
	
	@Test
	public void testCreateListColumnIndexTableWithNegativeID(){
		ColumnModel columnInfo = new ColumnModel();
		columnInfo.setColumnType(ColumnType.STRING_LIST);
		columnInfo.setId("0");
		columnInfo.setMaximumSize(42L);
		tableId = IdAndVersion.newBuilder().setId(-999L).build();
		String sql = SQLUtils.createListColumnIndexTable(tableId, columnInfo, true);
		String expected = "CREATE TABLE IF NOT EXISTS TEMPT__999_INDEX_C0_ (" +
				"ROW_ID_REF_C0_ BIGINT NOT NULL, " +
				"INDEX_NUM BIGINT NOT NULL, " +
				"_C0__UNNEST VARCHAR(42) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'STRING', " +
				"PRIMARY KEY (ROW_ID_REF_C0_, INDEX_NUM)," +
				" INDEX _C0__UNNEST_IDX (_C0__UNNEST ASC)," +
				" CONSTRAINT TEMPT__999_INDEX_C0__ibfk_FK FOREIGN KEY (ROW_ID_REF_C0_) REFERENCES TEMPT__999(ROW_ID) ON DELETE CASCADE);";
		assertEquals(expected, sql);
	}

	@Test
	public void testInsertIntoListColumnIndexTable(){
		ColumnModel columnInfo = new ColumnModel();
		columnInfo.setColumnType(ColumnType.STRING_LIST);
		columnInfo.setId("0");
		columnInfo.setMaximumSize(42L);
		boolean filterRows = false;
		String sql = SQLUtils.insertIntoListColumnIndexTable(tableId, columnInfo, filterRows, false);
		String expected = "INSERT INTO T999_INDEX_C0_ (ROW_ID_REF_C0_,INDEX_NUM,_C0__UNNEST) " +
				"SELECT ROW_ID ,  TEMP_JSON_TABLE.ORDINAL - 1 , TEMP_JSON_TABLE.COLUMN_EXPAND" +
				" FROM T999, JSON_TABLE(" +
				"_C0_," +
				" '$[*]' COLUMNS (" +
				" ORDINAL FOR ORDINALITY," +
				"  COLUMN_EXPAND VARCHAR(42) PATH '$' ERROR ON ERROR " +
				")" +
				") TEMP_JSON_TABLE";
		assertEquals(expected, sql);
	}
	
	@Test
	public void testInsertIntoListColumnIndexTableFilterRows_alterTempFalse(){
		ColumnModel columnInfo = new ColumnModel();
		columnInfo.setColumnType(ColumnType.STRING_LIST);
		columnInfo.setId("0");
		columnInfo.setMaximumSize(42L);
		boolean filterRows = true;
		String sql = SQLUtils.insertIntoListColumnIndexTable(tableId, columnInfo, filterRows, false);
		String expected = "INSERT INTO T999_INDEX_C0_ (ROW_ID_REF_C0_,INDEX_NUM,_C0__UNNEST) " +
				"SELECT ROW_ID ,  TEMP_JSON_TABLE.ORDINAL - 1 , TEMP_JSON_TABLE.COLUMN_EXPAND" +
				" FROM T999, JSON_TABLE(" +
				"_C0_," +
				" '$[*]' COLUMNS (" +
				" ORDINAL FOR ORDINALITY," +
				"  COLUMN_EXPAND VARCHAR(42) PATH '$' ERROR ON ERROR " +
				")" +
				") TEMP_JSON_TABLE WHERE T999.ROW_ID IN (:ids)";
		assertEquals(expected, sql);
	}

	@Test
	public void testInsertIntoListColumnIndexTableFilterRows_alterTempTrue(){
		ColumnModel columnInfo = new ColumnModel();
		columnInfo.setColumnType(ColumnType.STRING_LIST);
		columnInfo.setId("0");
		columnInfo.setMaximumSize(42L);
		boolean filterRows = true;
		String sql = SQLUtils.insertIntoListColumnIndexTable(tableId, columnInfo, filterRows, true);
		String expected = "INSERT INTO TEMPT999_INDEX_C0_ (ROW_ID_REF_C0_,INDEX_NUM,_C0__UNNEST) " +
				"SELECT ROW_ID ,  TEMP_JSON_TABLE.ORDINAL - 1 , TEMP_JSON_TABLE.COLUMN_EXPAND" +
				" FROM TEMPT999, JSON_TABLE(" +
				"_C0_," +
				" '$[*]' COLUMNS (" +
				" ORDINAL FOR ORDINALITY," +
				"  COLUMN_EXPAND VARCHAR(42) PATH '$' ERROR ON ERROR " +
				")" +
				") TEMP_JSON_TABLE WHERE TEMPT999.ROW_ID IN (:ids)";
		assertEquals(expected, sql);
	}
	
	@Test
	public void testGetOutOfDateRowsForViewSql() {
		String filter = " the-filter";
		// call under test
		String sql = SQLUtils.getOutOfDateRowsForViewSql(tableId, filter);
		sql = sql.replace(LINE_SEPERATOR, "");
		String expected = 
				"WITH DELTAS (ID, MISSING) AS ( "
				+ "	SELECT R.OBJECT_ID, V.ROW_ID FROM OBJECT_REPLICATION R "
				+ "		LEFT JOIN T999 V ON ( R.OBJECT_ID = V.ROW_ID AND R.ETAG = V.ROW_ETAG AND R.BENEFACTOR_ID = V.ROW_BENEFACTOR)"
				+ "        	WHERE  the-filter"
				+ "UNION ALL"
				+ "	SELECT V.ROW_ID, R.OBJECT_ID FROM OBJECT_REPLICATION R "
				+ "		RIGHT JOIN T999 V ON ( R.OBJECT_ID = V.ROW_ID AND R.ETAG = V.ROW_ETAG AND R.BENEFACTOR_ID = V.ROW_BENEFACTOR "
				+ "			AND  the-filter )"
				+ ")"
				+ "SELECT ID FROM DELTAS WHERE MISSING IS NULL ORDER BY ID DESC LIMIT :pLimit";
		assertEquals(expected, sql);
	}
	
	@Test
	public void testGetDeleteRowsFromViewSql() {
		// call under test
		String sql = SQLUtils.getDeleteRowsFromViewSql(tableId);
		assertEquals("DELETE FROM T999 WHERE ROW_ID = ?", sql);
	}
	
	@Test
	public void testGenerateSqlToRefreshViewBenefactors() {
		// call under test
		String sql = SQLUtils.generateSqlToRefreshViewBenefactors(tableId);
		assertEquals(
				"UPDATE T999 T JOIN OBJECT_REPLICATION O ON (T.ROW_ID = O.OBJECT_ID AND O.OBJECT_TYPE = ?)"
				+ " SET T.ROW_BENEFACTOR = O.BENEFACTOR_ID WHERE T.ROW_BENEFACTOR <> O.BENEFACTOR_ID",
				sql);
	}
	
	@Test
	public void testGenerateSqlToRefreshViewBenefactorsWithNull() {
		assertThrows(IllegalArgumentException.class, ()->{
			SQLUtils.generateSqlToRefreshViewBenefactors(null);
		});
	}
	
	@Test
	public void testCreateAlterToListColumnTypeSql() {
		// PLFM-6247
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("123");
		oldColumn.setColumnType(ColumnType.BOOLEAN);
		DatabaseColumnInfo oldColumnInfo = new DatabaseColumnInfo();
		oldColumnInfo.setHasIndex(false);
		// new column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("456");
		newColumn.setColumnType(ColumnType.BOOLEAN_LIST);
		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, oldColumnInfo, newColumn);
		List<String> results = SQLUtils.createAlterToListColumnTypeSqlBatch(change, tableName, useDepricatedUtf8ThreeBytes);
		assertEquals("ALTER TABLE T999 ADD COLUMN _C456_ JSON DEFAULT NULL COMMENT 'BOOLEAN_LIST'", results.get(0));
		assertEquals("UPDATE T999 SET _C456_ = JSON_ARRAY(_C123_)", results.get(1));
		assertEquals("ALTER TABLE T999 DROP COLUMN _C123_", results.get(2));
	}
	
	@Test
	public void testBuildSelectTableDataByRowIdSQL() {
		String expected = "SELECT ROW_ID,_C456_,_C789_,_C123_ FROM T999 WHERE ROW_ID IN(:ROW_ID)";
		
		// Call under test
		String sql = SQLUtils.buildSelectTableDataByRowIdSQL(tableId, simpleSchema);
		
		assertEquals(expected, sql);
	}
	
	@Test
	public void testBuildSelectTableDataByRowIdSQLWithNullId() {
		tableId = null;
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			SQLUtils.buildSelectTableDataByRowIdSQL(tableId, simpleSchema);
		});
		
		assertEquals("The id is required.", ex.getMessage());
	}
	
	@Test
	public void testBuildSelectTableDataByRowIdSQLWithNullColumns() {
		simpleSchema = null;
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			SQLUtils.buildSelectTableDataByRowIdSQL(tableId, simpleSchema);
		});
		
		assertEquals("The columns is required and must not be empty.", ex.getMessage());
	}
	
	@Test
	public void testBuildSelectTableDataByRowIdSQLWithEmptyColumns() {
		simpleSchema = Collections.emptyList();
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			SQLUtils.buildSelectTableDataByRowIdSQL(tableId, simpleSchema);
		});
		
		assertEquals("The columns is required and must not be empty.", ex.getMessage());
	}
	
	@Test
	public void testBuildSelectTableDataPageSQL() {
		String expected = "SELECT ROW_ID,_C456_,_C789_,_C123_ FROM T999 ORDER BY ROW_ID LIMIT :pLimit OFFSET :pOffset";
		
		// Call under test
		String sql = SQLUtils.buildSelectTableDataPage(tableId, simpleSchema);
		
		assertEquals(expected, sql);
	}
	
	@Test
	public void testBuildSelectTableDataSQL() {
		String expected = "SELECT _C456_,_C789_,_C123_ FROM T999";
		
		// Call under test
		String sql = SQLUtils.buildSelectTableData(tableId, simpleSchema).toString();
		
		assertEquals(expected, sql);
	}
	
	@Test
	public void testBuildSelectTableDataSQLWithAdditionalColumns() {
		String[] additionalColumns = new String[] {TableConstants.ROW_ID, TableConstants.ROW_VERSION, TableConstants.ROW_BENEFACTOR, TableConstants.ROW_ETAG};

		String expected = "SELECT ROW_ID,ROW_VERSION,ROW_BENEFACTOR,ROW_ETAG,_C456_,_C789_,_C123_ FROM T999";
		
		// Call under test
		String sql = SQLUtils.buildSelectTableData(tableId, simpleSchema, additionalColumns).toString();
		
		assertEquals(expected, sql);
	}
	
	@Test
	public void testBuildSelectTableDataPageSQLWithNullId() {
		tableId = null;
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			SQLUtils.buildSelectTableDataPage(tableId, simpleSchema);
		});
		
		assertEquals("The id is required.", ex.getMessage());
	}
	
	@Test
	public void testBuildSelectTableDataPageSQLWithNullColumns() {
		simpleSchema = null;
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			SQLUtils.buildSelectTableDataPage(tableId, simpleSchema);
		});
		
		assertEquals("The columns is required and must not be empty.", ex.getMessage());
	}
	
	@Test
	public void testBuildSelectTableDataPageSQLWithEmptyColumns() {
		simpleSchema = Collections.emptyList();
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			SQLUtils.buildSelectTableDataPage(tableId, simpleSchema);
		});
		
		assertEquals("The columns is required and must not be empty.", ex.getMessage());
	}
	
	@Test
	public void testGetSelectTableDataHeaders() {
		
		List<String> expected = List.of("_C456_", "_C789_", "_C123_");
		// Call under test
		List<String> result = SQLUtils.getSelectTableDataHeaders(simpleSchema);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetSelectTableDataHeadersWithNullSchema() {
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			SQLUtils.getSelectTableDataHeaders(null);
		}).getMessage();
		
		assertEquals("The columns is required.", result);
		
	}
	
	@Test
	public void testGetSelectTableDataHeadersWithEmptySchema() {
		
		List<String> expected = Collections.emptyList();
			// Call under test
		List<String> result = SQLUtils.getSelectTableDataHeaders(Collections.emptyList());

		assertEquals(expected, result);
	}
	
	@Test
	public void testGetSelectTableDataHeadersWithAdditionalColumns() {
		
		List<String> expected = List.of("ROW_ID", "ROW_VERSION", "_C456_", "_C789_", "_C123_");
		// Call under test
		List<String> result = SQLUtils.getSelectTableDataHeaders(simpleSchema, TableConstants.ROW_ID, TableConstants.ROW_VERSION);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetSelectTableDataHeadersWithDoubleColumn() {
		simpleSchema = List.of(
			new ColumnModel()
				.setColumnType(ColumnType.DOUBLE)
				.setId("456")
				.setName("colOne"),
			new ColumnModel()
				.setColumnType(ColumnType.INTEGER)
				.setId("789")
				.setName("colTwo")
		);
		List<String> expected = List.of("_C456_", "_DBL_C456_", "_C789_");
		// Call under test
		List<String> result = SQLUtils.getSelectTableDataHeaders(simpleSchema);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildBatchUpdateSearchContentSql() {
		String expected = "UPDATE T999 SET `ROW_SEARCH_CONTENT` = ? WHERE ROW_ID = ?";
		
		// Call under test
		String sql = SQLUtils.buildBatchUpdateSearchContentSql(tableId);
		
		assertEquals(expected, sql);
	}
	
	@Test
	public void testBuildBatchUpdateSearchContentSqlWithNullId() {
		tableId = null;
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			SQLUtils.buildBatchUpdateSearchContentSql(tableId);
		});
		
		assertEquals("The id is required.", ex.getMessage());
	}
	
	@Test
	public void testBuildClearSearchContentSql() {
		String expected = "UPDATE T999 SET `ROW_SEARCH_CONTENT` = NULL";
		
		// Call under test
		String sql = SQLUtils.buildClearSearchContentSql(tableId);
		
		assertEquals(expected, sql);
	}
	
	@Test
	public void testBuildClearSearchContentSqlWithNullId() {
		tableId = null;
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			SQLUtils.buildClearSearchContentSql(tableId);
		});
		
		assertEquals("The id is required.", ex.getMessage());
	}
	
	@Test
	public void testBuildCreateOrUpdateStatusSQL() {
		
		String result = SQLUtils.buildCreateOrUpdateStatusSQL(tableId);
		
		assertEquals("INSERT INTO T" + tableId.getId() + "S"
				+ " ( SINGLE_KEY,ROW_VERSION,SCHEMA_HASH,SEARCH_ENABLED )"
				+ " VALUES ('1', ?, '" + TableModelUtils.EMPTY_SCHEMA_MD5 + "', FALSE)"
				+ " ON DUPLICATE KEY UPDATE ROW_VERSION = ?", result);
	}
	
	@Test
	public void testBuildCreateOrUpdateSearchStatusSQL() {
		
		String result = SQLUtils.buildCreateOrUpdateSearchStatusSQL(tableId);
		
		assertEquals("INSERT INTO T" + tableId.getId() + "S"
				+ " ( SINGLE_KEY,ROW_VERSION,SCHEMA_HASH,SEARCH_ENABLED )"
				+ " VALUES ('1', -1, '" + TableModelUtils.EMPTY_SCHEMA_MD5 + "', ?)"
				+ " ON DUPLICATE KEY UPDATE SEARCH_ENABLED = ?", result);
	}
	
	@Test
	public void testBuildCreateOrUpdateStatusHashSQL() {
		
		String result = SQLUtils.buildCreateOrUpdateStatusHashSQL(tableId);
		
		assertEquals("INSERT INTO T" + tableId.getId() + "S"
				+ " ( SINGLE_KEY,ROW_VERSION,SCHEMA_HASH,SEARCH_ENABLED )"
				+ " VALUES ('1', -1, ?, FALSE)"
				+ " ON DUPLICATE KEY UPDATE SCHEMA_HASH = ?", result);
	}
}
