package org.sagebionetworks.table.cluster;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_MAX_STRING_LENGTH;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_OBJECT_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_COL_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.table.TableConstants.ANNOTATION_REPLICATION_TABLE;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_SEARCH_CONTENT;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections4.ListUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.IdAndChecksum;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.report.SynapseStorageProjectStats;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.ObjectAnnotationDTO;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.repo.model.table.Table;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.table.cluster.SQLUtils.TableIndexType;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.TableIndexDescription;
import org.sagebionetworks.table.cluster.description.ViewIndexDescription;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolverFactory;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldTypeMapper;
import org.sagebionetworks.table.cluster.search.RowSearchContent;
import org.sagebionetworks.table.cluster.search.TableRowData;
import org.sagebionetworks.table.cluster.search.TypedCellValue;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.cluster.view.filter.HierarchicaFilter;
import org.sagebionetworks.table.cluster.view.filter.IdAndVersionFilter;
import org.sagebionetworks.table.cluster.view.filter.ViewFilter;
import org.sagebionetworks.table.model.Grouping;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.table.model.SparseRow;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;
import org.sagebionetworks.table.query.util.SimpleAggregateQueryException;
import org.sagebionetworks.table.query.util.SqlElementUtils;
import org.sagebionetworks.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:table-cluster-spb.xml" })
public class TableIndexDAOImplTest {

	@Autowired
	ObjectFieldModelResolverFactory objectFieldModelResolverFactory;
	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	StackConfiguration config;
	
	// not a bean
	TableIndexDAO tableIndexDAO;
	
	ProgressCallback mockProgressCallback;

	IdAndVersion tableId;
	IndexDescription indexDescription;
	
	ObjectDataDTO entityOne;
	ObjectDataDTO entityTwo;
	
	private ViewObjectType objectType;
	private ViewObjectType otherObjectType;
	
	private ReplicationType mainType;
	private Set<SubType> subTypes;
	
	ObjectFieldTypeMapper fieldTypeMapper;
	
	@SuppressWarnings("rawtypes")
	Class<? extends Enum> objectSubType = EntityType.class;
	
	Long userId;
	
	@BeforeEach
	public void before() {
		objectType = ViewObjectType.ENTITY;
		otherObjectType = ViewObjectType.SUBMISSION;
		mainType = ReplicationType.ENTITY;
		mockProgressCallback = Mockito.mock(ProgressCallback.class);
		tableId = IdAndVersion.parse("syn123");
		// First get a connection for this table
		tableIndexDAO = tableConnectionFactory.getConnection(tableId);
		tableIndexDAO.deleteTable(tableId);
		tableIndexDAO.truncateIndex();
		indexDescription = new TableIndexDescription(tableId);
		userId = 1L;
		fieldTypeMapper = new ObjectFieldTypeMapper() {
			
			@Override
			public ViewObjectType getObjectType() {
				return objectType;
			}
			
			@Override
			public ColumnType getParentIdColumnType() {
				return ColumnType.ENTITYID;
			}
			
			@Override
			public ColumnType getIdColumnType() {
				return ColumnType.ENTITYID;
			}
			
			@Override
			public ColumnType getBenefactorIdColumnType() {
				return ColumnType.ENTITYID;
			}
		};
		subTypes = Set.of(SubType.file);
		
	}

	@AfterEach
	public void after() {
		// Drop the table
		if (tableId != null && tableIndexDAO != null) {
			tableIndexDAO.deleteTable(tableId);
		}
		tableIndexDAO.truncateIndex();
	}
	
	/**
	 * Helper to setup a table with a new schema.
	 * 
	 * @param newSchema
	 * @param tableId
	 */
	public boolean createOrUpdateTable(List<ColumnModel> newSchema, IndexDescription indexDescription){
		List<DatabaseColumnInfo> currentSchema = tableIndexDAO.getDatabaseInfo(indexDescription.getIdAndVersion());
		List<ColumnChangeDetails> changes = SQLUtils.createReplaceSchemaChange(currentSchema, newSchema);
		tableIndexDAO.createTableIfDoesNotExist(indexDescription);
		boolean alterTemp = false;
		// Ensure all all updated columns actually exist.
		changes = SQLUtils.matchChangesToCurrentInfo(currentSchema, changes);
		boolean altered = tableIndexDAO.alterTableAsNeeded(indexDescription.getIdAndVersion(), changes, alterTemp);

		for(ColumnModel columnModel : newSchema){
			if(ColumnTypeListMappings.isList(columnModel.getColumnType())){
				tableIndexDAO.createMultivalueColumnIndexTable(indexDescription.getIdAndVersion(), columnModel, false);
			}
		}

		return altered;
	}

	/**
	 * Helper to alter the table as needed.
	 * @param tableId
	 * @param changes
	 * @param alterTemp
	 * @return
	 */
	boolean alterTableAsNeeded(IdAndVersion tableId, List<ColumnChangeDetails> changes, boolean alterTemp){
		// Lookup the current schema of the index.
		List<DatabaseColumnInfo> currentIndedSchema = tableIndexDAO.getDatabaseInfo(tableId);
		tableIndexDAO.provideIndexName(currentIndedSchema, tableId);
		// Ensure all all updated columns actually exist.
		changes = SQLUtils.matchChangesToCurrentInfo(currentIndedSchema, changes);
		return tableIndexDAO.alterTableAsNeeded(tableId, changes, alterTemp);
	}
	
	/**
	 * Helper to generate a given number of rows and append them to the given table
	 * 
	 * @param tableId
	 * @param columns
	 * @param rowsCount
	 * @return
	 */
	private List<Row> generateAndAppendRows(IdAndVersion tableId, List<ColumnModel> columns, int rowsCount) {
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(columns, rowsCount);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(columns);
		set.setHeaders(headers);
		set.setTableId(tableId.toString());
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(range.getMinimumId() + rowsCount);
		range.setVersionNumber(1L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		
		// Now fill the table with data
		createOrUpdateOrDeleteRows(tableId, set, columns);
		
		return set.getRows();
	}
	
	/**
	 * Helper to apply a change set to the index.s
	 * @param rowSet
	 * @param schema
	 */
	public void createOrUpdateOrDeleteRows(IdAndVersion tableId, RowSet rowSet, List<ColumnModel> schema){
		ListUtils.partition(rowSet.getRows(), 10_000).forEach( rows -> {
			RowSet batch = new RowSet().setEtag(rowSet.getEtag())
					.setHeaders(rowSet.getHeaders())
					.setTableId(rowSet.getTableId())
					.setRows(rows);
		
			SparseChangeSet sparse = TableModelUtils.createSparseChangeSet(batch, schema);
		
			for(Grouping grouping: sparse.groupByValidValues()){
				tableIndexDAO.createOrUpdateOrDeleteRows(tableId, grouping);
			}
		
		});
		
	}
	
	@Test
	public void testTableEnityTypes(){
		TableEntity tableEntity = new TableEntity();
		assertTrue(tableEntity instanceof Table);
	}
	
	@Test
	public void testFileViewTypes(){
		EntityView entityView = new EntityView();
		assertTrue(entityView instanceof Table);
	}

	@Test
	public void testCRUD() {
		// Create a Simple table with only a few columns
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		// Create the table
		boolean updated = createOrUpdateTable(allTypes, indexDescription);
		assertTrue(updated,
				"The table should not have existed so update should be true");
		updated = createOrUpdateTable(allTypes, indexDescription);
		assertFalse(updated,
				"The table already existed in that state so it should not have been updated");
		// Now we should be able to see the columns that were created
		List<DatabaseColumnInfo> columns = getAllColumnInfo(tableId);
		// There should be a column for each column in the schema plus one
		// ROW_ID, one ROW_VERSION and on ROW_SEARCH_CONTENT plus one extra for doubles.
		assertEquals(allTypes.size() + 3 + 1, columns.size());
		for (int i = 0; i < allTypes.size(); i++) {
			// Now remove a column and update the table
			ColumnModel removed = allTypes.remove(0);
			createOrUpdateTable(allTypes, indexDescription);
			// Now we should be able to see the columns that were created
			columns = getAllColumnInfo(tableId);
			// There should be a column for each column in the schema plus one
			// ROW_ID, one ROW_VERSION and on ROW_SEARCH_CONTENT.
			int extraColumns = 1;
			if (removed.getColumnType() == ColumnType.DOUBLE) {
				extraColumns = 0;
			}
			// removed
			assertEquals(allTypes.size() + 3
					+ extraColumns, columns.size(),"removed " + removed);
			// Now add a column
			allTypes.add(removed);
			createOrUpdateTable(allTypes, indexDescription);
			// Now we should be able to see the columns that were created
			columns = getAllColumnInfo(tableId);
			// There should be a column for each column in the schema plus one
			// ROW_ID, one ROW_VERSION and on ROW_SEARCH_CONTENT .
			assertEquals(allTypes.size() + 3 + 1,
					columns.size(), "read " + removed);
		}
	}

	@Test
	public void testCreateOrUpdateRows() {
		// Create a Simple table with only a few columns
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		List<Row> rows = TableModelTestUtils.createRows(allTypes, 5);
		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getSelectColumns(allTypes));
		set.setTableId(tableId.toString());
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Create the table
		createOrUpdateTable(allTypes, indexDescription);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(tableId, set, allTypes);
		List<Map<String, Object>> result = tableIndexDAO.getConnection()
				.queryForList(
						"SELECT * FROM "
								+ SQLUtils.getTableNameForId(tableId,
										TableIndexType.INDEX));
		assertNotNull(result);
		assertEquals(5, result.size());
		// Row zero
		Map<String, Object> row = result.get(0);
		assertEquals(100l, row.get(ROW_ID));
		assertEquals(404000l, row.get("_C4_"));
		// row four
		row = result.get(4);
		assertEquals(104l, row.get(ROW_ID));
		assertEquals(341016.76, row.get("_C1_"));
		assertEquals(404004l, row.get("_C4_"));

		// We should be able to update all of the rows
		rows.get(4).setValues(
				Arrays.asList("update", "99.99", "3", "false", "123", "123",
						"syn123", "456", "789", "link2", "mediumText", "largeText", "42"));
		rows.get(4).setVersionNumber(5L);
		rows.get(0).setVersionNumber(5L);
		// This should not fail
		createOrUpdateOrDeleteRows(tableId, set, allTypes);
		// Check the update
		result = tableIndexDAO.getConnection().queryForList(
				"SELECT * FROM "
						+ SQLUtils.getTableNameForId(tableId, TableIndexType.INDEX));
		assertNotNull(result);
		assertEquals(5, result.size());
		// row four
		row = result.get(4);
		// Check all values on the updated row.
		assertEquals(104l, row.get(ROW_ID));
		assertEquals(5L, row.get(ROW_VERSION));
		assertEquals("update", row.get("_C0_"));
		assertEquals(99.99, row.get("_C1_"));
		assertEquals(3L, row.get("_C2_"));
		assertEquals(Boolean.FALSE, row.get("_C3_"));
		assertEquals(123L, row.get("_C4_"));
		assertEquals(123L, row.get("_C5_"));
		assertEquals(new Long(123), row.get("_C6_"));
		assertEquals(456L, row.get("_C7_"));
		assertEquals(789L, row.get("_C8_"));
		assertEquals("link2", row.get("_C9_"));
		assertEquals("mediumText", row.get("_C10_"));
		assertEquals("largeText", row.get("_C11_"));
		assertEquals(42L, row.get("_C12_"));
	}

	@Test
	public void testGetRowCountForTable() {
		// Before the table exists the max version should be null
		Long count = tableIndexDAO.getRowCountForTable(tableId);
		assertEquals(null, count,
				"The row count should be null when the table does not exist");
		// Create the table
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		createOrUpdateTable(allTypes, indexDescription);
		// the table now exists
		count = tableIndexDAO.getRowCountForTable(tableId);
		assertEquals(new Long(0), count,
				"The row count should be 0 when the table is empty");
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(allTypes, 4);
		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getSelectColumns(allTypes));
		set.setTableId(tableId.toString());
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(tableId, set, allTypes);
		// Check again
		count = tableIndexDAO.getRowCountForTable(tableId);
		assertEquals(new Long(rows.size()), count);
		
		// truncate and get the count again
		tableIndexDAO.truncateTable(tableId);
		count = tableIndexDAO.getRowCountForTable(tableId);
		assertEquals(new Long(0), count);
	}

	@Test
	public void testGetMaxVersionForTable() {
		tableIndexDAO.createSecondaryTables(tableId);
		// Before the table exists the max version should be -1L
		Long maxVersion = tableIndexDAO
				.getMaxCurrentCompleteVersionForTable(tableId);
		assertEquals(-1L, maxVersion.longValue());

		// Create the table
		tableIndexDAO.setMaxCurrentCompleteVersionForTable(tableId, 2L);

		maxVersion = tableIndexDAO
				.getMaxCurrentCompleteVersionForTable(tableId);
		assertEquals(2L, maxVersion.longValue());

		tableIndexDAO.setMaxCurrentCompleteVersionForTable(tableId, 4L);

		maxVersion = tableIndexDAO
				.getMaxCurrentCompleteVersionForTable(tableId);
		assertEquals(4L, maxVersion.longValue());
	}
	
	@Test
	public void testGetSchemaHashForTableWithTableDoesNotExist(){
		tableIndexDAO.deleteTable(tableId);
		// call under test
		Optional<String> hashOptional = tableIndexDAO.getCurrentSchemaMD5Hex(tableId);
		assertTrue(hashOptional.isEmpty());
	}
	
	@Test
	public void testGetSchemaHashForTable(){
		tableIndexDAO.createSecondaryTables(tableId);
		// Before the table exists the max version should be -1L
		Optional<String> hashOptional = tableIndexDAO.getCurrentSchemaMD5Hex(tableId);
		assertTrue(hashOptional.isEmpty());
		
		String hash = "some hash";
		tableIndexDAO.setCurrentSchemaMD5Hex(tableId, hash);
		hashOptional = tableIndexDAO.getCurrentSchemaMD5Hex(tableId);
		assertEquals(Optional.of(hash), hashOptional);
		// setting the version should not change the hash
		tableIndexDAO.setMaxCurrentCompleteVersionForTable(tableId, 4L);
		// did it change?
		hashOptional = tableIndexDAO.getCurrentSchemaMD5Hex(tableId);
		assertEquals(Optional.of(hash), hashOptional);
	}

	@Test
	public void testSimpleQuery() throws ParseException, SimpleAggregateQueryException {
		// Create the table
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		createOrUpdateTable(allTypes, indexDescription);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(allTypes, 2);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(allTypes);
		set.setHeaders(headers);
		set.setTableId(tableId.toString());
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(tableId, set, allTypes);
				
		// This is our query
		QueryTranslator query = QueryTranslator.builder("select * from " + tableId, schemaProvider(allTypes), userId).indexDescription(new TableIndexDescription(tableId)).build();
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		System.out.println(results);
		assertEquals(headers, results.getHeaders());
		assertNotNull(results.getRows());
		assertEquals(tableId.toString(), results.getTableId());
		assertEquals(2, results.getRows().size());
		// test the count
		String countSql = SqlElementUtils.createCountSql(query.getTranslatedModel().getFirstElementOfType(QuerySpecification.class)).get();
		Long count = tableIndexDAO.countQuery(countSql, query.getParameters());
		assertEquals(new Long(2), count);
		// test the rowIds
		long limit = 2;
		String rowIdSql = SqlElementUtils.buildSqlSelectRowIdAndVersions(query.getTranslatedModel().getFirstElementOfType(QuerySpecification.class), limit).get();
		
		List<IdAndVersion> expectedRowIdAdVersions = Arrays.asList(
				IdAndVersion.parse("100.3"),
				IdAndVersion.parse("101.3")
		);
		
		List<IdAndVersion> rowIdAndVersions = tableIndexDAO.getRowIdAndVersions(rowIdSql, query.getParameters());
		
		assertEquals(expectedRowIdAdVersions, rowIdAndVersions);
		
		// the first row
		Row row = results.getRows().get(0);
		assertNotNull(row);
		assertEquals(new Long(100), row.getRowId());
		assertEquals(new Long(3), row.getVersionNumber());
		List<String> expectedValues = Arrays.asList("string0", "341003.12",
				"203000", "false", "404000", "505000", "syn606000", "703000", "803000",
				"link908000", "mediumText1004000", "largeText1104000", "1203000", "[\"string1300000\", \"otherstring1300000\"]", "[1403000]", "[false]", "[1604000]", "[\"syn1706000\"]", "[1803000]");
		assertEquals(expectedValues, row.getValues());
		// Second row
		row = results.getRows().get(1);
		assertNotNull(row);
		assertEquals(new Long(101), row.getRowId());
		assertEquals(new Long(3), row.getVersionNumber());
		expectedValues = Arrays.asList("string1", "341006.53", "203001",
				"true", "404001", "505001", "syn606001", "703001", "803001",
				"link908001", "mediumText1004001", "largeText1104001", "1203001", "[\"string1300001\", \"otherstring1300001\"]", "[1403001]", "[true]", "[1604001]", "[\"syn1706001\"]", "[1803001]");
		assertEquals(expectedValues, row.getValues());
		// must also be able to run the query with a null callback
		mockProgressCallback = null;
	}

	@Test
	public void testDoubleQuery() throws ParseException {
		// Create the table
		List<ColumnModel> doubleColumn = Lists.newArrayList(TableModelTestUtils
				.createColumn(1L, "col1", ColumnType.DOUBLE));
		createOrUpdateTable(doubleColumn, indexDescription);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(doubleColumn, 5);
		// insert special values
		rows.get(1).getValues().set(0, "0");
		rows.get(2).getValues().set(0, "NaN");
		rows.get(3).getValues().set(0, "Infinity");
		rows.get(4).getValues().set(0, "-Infinity");
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(doubleColumn);
		set.setHeaders(headers);
		set.setTableId(tableId.toString());
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(tableId, set, doubleColumn);
		
		// This is our query
		QueryTranslator query = QueryTranslator.builder("select * from " + tableId, schemaProvider(doubleColumn), userId).indexDescription(new TableIndexDescription(tableId)).build();
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		System.out.println(results);
		assertEquals(headers, results.getHeaders());
		assertNotNull(results.getRows());
		assertEquals(tableId.toString(), results.getTableId());
		assertEquals(5, results.getRows().size());
		assertEquals("3.12", results.getRows().get(0).getValues().get(0));
		assertEquals("0", results.getRows().get(1).getValues().get(0));
		assertEquals("NaN", results.getRows().get(2).getValues().get(0));
		assertEquals("Infinity", results.getRows().get(3).getValues().get(0));
		assertEquals("-Infinity", results.getRows().get(4).getValues().get(0));
	}

	@Test
	public void testSimpleQueryWithDeletedRows() throws ParseException {
		// Create the table
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		createOrUpdateTable(allTypes, indexDescription);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(allTypes, 4);
		Row deletion = new Row();
		rows.add(deletion);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(allTypes);
		set.setHeaders(headers);
		set.setTableId(tableId.toString());
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setMaximumUpdateId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(tableId, set, allTypes);

		// now delete the second and fourth row
		set.getRows().remove(0);
		set.getRows().remove(1);
		set.getRows().get(0).getValues().clear();
		set.getRows().get(1).getValues().clear();
		range.setVersionNumber(4L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		createOrUpdateOrDeleteRows(tableId, set, allTypes);
		SchemaProvider schemaProvider = schemaProvider(allTypes);
		
		// This is our query
		QueryTranslator query = QueryTranslator.builder("select * from " + tableId, schemaProvider, userId).indexDescription(new TableIndexDescription(tableId)).build();
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		System.out.println(results);
		assertEquals(2, results.getRows().size());
		assertEquals(100L, results.getRows().get(0).getRowId().longValue());
		assertEquals(102L, results.getRows().get(1).getRowId().longValue());
	}

	@Test
	public void testNullQuery() throws ParseException {
		// Create the table
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		createOrUpdateTable(allTypes, indexDescription);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createNullRows(allTypes, 2);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(allTypes);
		set.setHeaders(headers);
		set.setTableId(tableId.toString());
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(tableId, set, allTypes);
		
		// Now query for the results
		QueryTranslator query = QueryTranslator.builder("select * from " + tableId, schemaProvider(allTypes), userId).indexDescription(new TableIndexDescription(tableId)).build();
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		System.out.println(results);
		assertEquals(headers, results.getHeaders());
		assertNotNull(results.getRows());
		assertEquals(tableId.toString(), results.getTableId());
		assertEquals(2, results.getRows().size());
		// the first row
		Row row = results.getRows().get(0);
		assertNotNull(row);
		assertEquals(new Long(100), row.getRowId());
		assertEquals(new Long(3), row.getVersionNumber());
		List<String> expectedValues = Arrays.asList(null, null, null, null, null, null,
				null, null, null, null,  null, null, null, null, null, null, null, null, null);
		assertEquals(expectedValues, row.getValues());
		// Second row
		row = results.getRows().get(1);
		assertNotNull(row);
		assertEquals(new Long(101), row.getRowId());
		assertEquals(new Long(3), row.getVersionNumber());
		expectedValues = Arrays.asList(null, null, null, null, null, null, null, null,
				null, null, null, null, null, null, null, null, null, null, null);
		assertEquals(expectedValues, row.getValues());
	}

	@Test
	public void testQueryAggregate() throws ParseException {
		// Create the table
		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		SelectColumn selectColumn = new SelectColumn();
		selectColumn.setColumnType(null);
		selectColumn.setId(null);
		selectColumn.setName("count(*)");
		createOrUpdateTable(allTypes, indexDescription);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(allTypes, 2);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(allTypes);
		set.setHeaders(headers);
		set.setTableId(tableId.toString());
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(tableId, set, allTypes);
		// Now a count query
		QueryTranslator query = QueryTranslator.builder("select count(*) from " + tableId,
				schemaProvider(allTypes), userId).indexDescription(new TableIndexDescription(tableId)).build();
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		List<SelectColumn> expectedHeaders = Lists.newArrayList(TableModelUtils
				.createSelectColumn("COUNT(*)", ColumnType.INTEGER, null));
		assertEquals(expectedHeaders, results.getHeaders());
		assertNotNull(results.getRows());
		assertEquals(tableId.toString(), results.getTableId());
		assertEquals(1, results.getRows().size());
		// first and only row.
		Row row = results.getRows().get(0);
		assertNotNull(row);
		assertEquals(null,	row.getRowId(),
				"RowId should be null for an aggregate function.");
		assertEquals(null, row.getVersionNumber(),
				"RowVersion should be null for an aggregate function");
		List<String> expectedValues = Arrays.asList("2");
		assertEquals(expectedValues, row.getValues());
	}

	@Test
	public void testQueryAllParts() throws ParseException {
		ColumnModel foo = new ColumnModel();
		foo.setColumnType(ColumnType.STRING);
		foo.setName("foo");
		foo.setId("111");
		foo.setMaximumSize(10L);
		ColumnModel bar = new ColumnModel();
		bar.setColumnType(ColumnType.INTEGER);
		bar.setId("222");
		bar.setName("bar");
		List<ColumnModel> schema = new LinkedList<ColumnModel>();
		schema.add(foo);
		schema.add(bar);
		// Create the table.
		createOrUpdateTable(schema, indexDescription);
		// Create some data
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 100);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(schema);
		set.setHeaders(headers);
		set.setTableId(tableId.toString());
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(4L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(tableId, set, schema);
		// Now create the query
		QueryTranslator query = QueryTranslator.builder(
				"select foo, sum(bar) from "
						+ tableId
						+ " where foo is not null group by foo order by sum(bar) desc limit 1 offset 0",
				schemaProvider(schema), userId).indexDescription(new TableIndexDescription(tableId)).build();
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		assertEquals(schema.size(),
				results.getHeaders().size());
		assertNotNull(results.getRows());
		assertEquals(tableId.toString(), results.getTableId());
		assertEquals(1, results.getRows().size());
		// first and only row.
		Row row = results.getRows().get(0);
		assertNotNull(row);
		// is aggregate, so no row id and version
		assertEquals(null, row.getRowId());
		assertEquals(null, row.getVersionNumber());
		List<String> expectedValues = Arrays.asList("string99", "103099");
		assertEquals(expectedValues, row.getValues());
	}

	@Test
	public void testQueryRowIdAndRowVersion() throws ParseException {
		ColumnModel foo = new ColumnModel();
		foo.setColumnType(ColumnType.STRING);
		foo.setName("foo");
		foo.setId("111");
		foo.setMaximumSize(10L);
		ColumnModel bar = new ColumnModel();
		bar.setColumnType(ColumnType.INTEGER);
		bar.setId("222");
		bar.setName("bar");
		List<ColumnModel> schema = new LinkedList<ColumnModel>();
		schema.add(foo);
		schema.add(bar);
		// Create the table.
		createOrUpdateTable(schema, indexDescription);
		// Create some data
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 100);
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(schema);
		set.setHeaders(headers);
		set.setTableId(tableId.toString());
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(4L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(tableId, set, schema);
		// Now create the query
		QueryTranslator query = QueryTranslator.builder("select * from " + tableId
				+ " where ROW_ID = 104 AND Row_Version > 1 limit 1 offset 0",
				schemaProvider(schema), userId).indexDescription(new TableIndexDescription(tableId)).build();
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		assertNotNull(results.getRows());
		assertEquals(tableId.toString(), results.getTableId());
		assertEquals(1, results.getRows().size());
		// first and only row.
		Row row = results.getRows().get(0);
		assertNotNull(row);
		assertEquals(new Long(104), row.getRowId());
		assertEquals(new Long(4), row.getVersionNumber());
		List<String> expectedValues = Arrays.asList("string4", "103004");
		assertEquals(expectedValues, row.getValues());
	}

	@Test
	public void testTooManyColumns() throws Exception {
		List<ColumnModel> schema = Lists.newArrayList();
		List<String> indexes = Lists.newArrayList();
		indexes.add("ROW_ID");
		for (int i = 0; i < 100; i++) {
			ColumnModel cm = new ColumnModel();
			cm.setColumnType(ColumnType.STRING);
			cm.setName("foo" + i);
			cm.setId("111" + i);
			cm.setMaximumSize(30L);
			schema.add(cm);
			if (indexes.size() < 64) {
				indexes.add(SQLUtils.getColumnNameForId(cm.getId()));
			}
		}
		// Create the table.
		createOrUpdateTable(schema, indexDescription);
	}

	@Test
	public void testTooManyColumnsAppended() throws Exception {
		List<ColumnModel> schema = new LinkedList<ColumnModel>();
		List<String> indexes = Lists.newArrayList();
		indexes.add("ROW_ID");
		for (int i = 0; i < 63; i++) {
			ColumnModel cm = new ColumnModel();
			cm.setColumnType(ColumnType.STRING);
			cm.setName("foo" + i);
			cm.setId("111" + i);
			cm.setMaximumSize(30L);
			schema.add(cm);
			if (indexes.size() < 64) {
				indexes.add(SQLUtils.getColumnNameForId(cm.getId()));
			}
		}

		// Create the table.
		createOrUpdateTable(schema, indexDescription);

		// replace 10 columns
		for (int i = 30; i < 40; i++) {
			ColumnModel cm = schema.get(i);
			cm.setId("333" + i);
			indexes.set(i + 1, SQLUtils.getColumnNameForId(cm.getId()));
		}

		createOrUpdateTable(schema, indexDescription);

		// replace 10 and add 10 columns
		for (int i = 20; i < 30; i++) {
			ColumnModel cm = schema.get(i);
			cm.setId("444" + i);
			indexes.set(i + 1, SQLUtils.getColumnNameForId(cm.getId()));
		}
		for (int i = 0; i < 10; i++) {
			ColumnModel cm = new ColumnModel();
			cm.setColumnType(ColumnType.STRING);
			cm.setName("foo" + i);
			cm.setId("222" + i);
			cm.setMaximumSize(30L);
			schema.add(cm);
			if (indexes.size() < 64) {
				indexes.add(SQLUtils.getColumnNameForId(cm.getId()));
			}
		}
		createOrUpdateTable(schema, indexDescription);
	}

	/**
	 * Test for the secondary table used to bind file handle IDs to a table.
	 * Once a file handle is bound to a table, any user with download permission
	 * will haver permission to download that file from the table.
	 */
	@Test
	public void testBindFileHandles() {
		// ensure the secondary tables for this index exist
		this.tableIndexDAO.createSecondaryTables(tableId);
		Set<Long> toBind = Set.of(1L, 2L, 5L);
		// find the files
		this.tableIndexDAO.applyFileHandleIdsToTable(tableId, toBind);
		Set<Long> toTest = Set.of(0L, 2L, 3L, 5L, 8L);
		// Expect to find the intersection of the toBound and toTest.
		Set<Long> expected = Set.of(2L, 5L);

		Set<Long> results = this.tableIndexDAO.getFileHandleIdsAssociatedWithTable(toTest, tableId);
		assertEquals(expected, results);
	}
	
	@Test
	public void testBindFileHandlesWithOverlap() {
		// ensure the secondary tables for this index exist
		this.tableIndexDAO.createSecondaryTables(tableId);
		Set<Long> toBind1 = Set.of(1L, 2L, 3L);
		this.tableIndexDAO.applyFileHandleIdsToTable(tableId, toBind1);
		Set<Long> toBind2 = Set.of(2L, 3L, 4L);
		// must add 4 and ignore 2 & 3.
		this.tableIndexDAO.applyFileHandleIdsToTable(tableId, toBind2);
		Set<Long> toTest = Set.of(5L,4L,3L,2L,1L,0L);
		// Expect to find the intersection of the toBound and toTest.
		Set<Long> expected = Set.of(4L,3L,2L,1L);
		Set<Long> results = this.tableIndexDAO.getFileHandleIdsAssociatedWithTable(toTest, tableId);
		assertEquals(expected, results);
	}
	
	/**
	 * When the secondary table does not exist, an empty set should be returned.
	 */
	@Test
	public void testBindFileHandlesTableDoesNotExist() {
		this.tableIndexDAO.deleteTable(tableId);
		Set<Long> toTest = Set.of(0L, 2L, 3L, 5L, 8L);
		Set<Long> results = this.tableIndexDAO.getFileHandleIdsAssociatedWithTable(toTest, tableId);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
	
	@Test
	public void testDoesIndexStateMatch(){
		// ensure the secondary tables for this index exist
		this.tableIndexDAO.createSecondaryTables(tableId);
		
		String md5 = "md5hex";
		this.tableIndexDAO.setCurrentSchemaMD5Hex(tableId, md5);
		long version = 123;
		this.tableIndexDAO.setMaxCurrentCompleteVersionForTable(tableId, version);
		// call under test
		boolean match = this.tableIndexDAO.doesIndexStateMatch(tableId, version, md5, false);
		assertTrue(match);
		
		// call under test
		match = this.tableIndexDAO.doesIndexStateMatch(tableId, version+1, md5, false);
		assertFalse(match);
		
		// call under test
		match = this.tableIndexDAO.doesIndexStateMatch(tableId, version, md5+1, false);
		assertFalse(match);
		
		// call under test
		match = this.tableIndexDAO.doesIndexStateMatch(tableId, version, md5, true);
		assertFalse(match);
	}
		
	@Test
	public void testIsSearchEnabledWithNoTable() {
		// Call under test
		boolean result = tableIndexDAO.isSearchEnabled(tableId); 
		assertFalse(result);
	}
	
	@Test
	public void testIsSearchEnabledWithEmptyTable() {
		// ensure the secondary tables for this index exist
		tableIndexDAO.createSecondaryTables(tableId);
		// Call under test
		boolean result = tableIndexDAO.isSearchEnabled(tableId); 
		assertFalse(result);
	}
	
	@Test
	public void testSetSearchEnabledWithFalse() {
		// ensure the secondary tables for this index exist
		tableIndexDAO.createSecondaryTables(tableId);
		
		tableIndexDAO.setSearchEnabled(tableId, false);
		
		// Call under test
		boolean result = tableIndexDAO.isSearchEnabled(tableId);
		
		assertFalse(result);
	}
	
	@Test
	public void testSetSearchEnabledWithTrue() {
		// ensure the secondary tables for this index exist
		tableIndexDAO.createSecondaryTables(tableId);
		
		tableIndexDAO.setSearchEnabled(tableId, true);
		
		// Call under test
		boolean result = tableIndexDAO.isSearchEnabled(tableId);
		
		assertTrue(result);
	}
	
	@Test
	public void testSetMaxCurrentCompleteVersionForTable() {
		// ensure the secondary tables for this index exist
		tableIndexDAO.createSecondaryTables(tableId);
		
		Long version = 123L;
		
		// Call under test
		tableIndexDAO.setMaxCurrentCompleteVersionForTable(tableId, version);
		
		assertEquals(version, tableIndexDAO.getMaxCurrentCompleteVersionForTable(tableId));
		
	}
	
	@Test
	public void testGetTableDataForRowIds() {
		List<ColumnModel> columns = Arrays.asList(
			TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING),
			TableModelTestUtils.createColumn(2L, "two", ColumnType.STRING_LIST)
		);
		
		createOrUpdateTable(columns, indexDescription);
		
		List<Row> rows = generateAndAppendRows(tableId, columns, 100);
		
		Set<Long> rowIds = rows.stream().map(Row::getRowId).collect(Collectors.toSet());
		
		List<TableRowData> expected = rows.stream().map(row -> {
			List<TypedCellValue> rowData = new ArrayList<>();
			for (int i=0; i<columns.size(); i++) {
				rowData.add(new TypedCellValue(columns.get(i).getColumnType(), row.getValues().get(i)));
			}
			return new TableRowData(row.getRowId(), rowData);
		}).collect(Collectors.toList());
		
		// Call under test
		List<TableRowData> result = tableIndexDAO.getTableDataForRowIds(tableId, columns, rowIds);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetTableDataForRowIdsWithSubSchema() {
		List<ColumnModel> columns = Arrays.asList(
			TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING),
			TableModelTestUtils.createColumn(2L, "two", ColumnType.STRING_LIST),
			TableModelTestUtils.createColumn(3L, "three", ColumnType.INTEGER),
			TableModelTestUtils.createColumn(4L, "four", ColumnType.DOUBLE)
		);
		
		createOrUpdateTable(columns, indexDescription);
		
		List<Row> rows = generateAndAppendRows(tableId, columns, 100);
		
		Set<Long> rowIds = rows.stream().map(Row::getRowId).collect(Collectors.toSet());
		
		// We want to fetch only the first and third column data
		List<ColumnModel> subSchema = Arrays.asList(columns.get(0), columns.get(2));
		
		List<TableRowData> expected = rows.stream().map(row -> {
			List<TypedCellValue> rowData = new ArrayList<>();
			
			rowData.add(new TypedCellValue(columns.get(0).getColumnType(), row.getValues().get(0)));
			rowData.add(new TypedCellValue(columns.get(2).getColumnType(), row.getValues().get(2)));
			
			return new TableRowData(row.getRowId(), rowData);
		}).collect(Collectors.toList());
		
		// Call under test
		List<TableRowData> result = tableIndexDAO.getTableDataForRowIds(tableId, subSchema, rowIds);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetTableDataPage() {
		List<ColumnModel> columns = Arrays.asList(
			TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING),
			TableModelTestUtils.createColumn(2L, "two", ColumnType.STRING_LIST)
		);
		
		createOrUpdateTable(columns, indexDescription);
		
		List<Row> rows = generateAndAppendRows(tableId, columns, 100);
		
		long offset = 0;
		long limit = 10;
		
		List<TableRowData> expected = rows.stream().limit(limit).map(row -> {
			List<TypedCellValue> rowData = new ArrayList<>();
			for (int i=0; i<columns.size(); i++) {
				rowData.add(new TypedCellValue(columns.get(i).getColumnType(), row.getValues().get(i)));
			}
			return new TableRowData(row.getRowId(), rowData);
		}).collect(Collectors.toList());
		
		// Call under test
		List<TableRowData> result = tableIndexDAO.getTableDataPage(tableId, columns, limit, offset);
		
		assertEquals(expected, result);
	}
		
	@Test
	public void testGetTableDataPageWithOffset() {
		List<ColumnModel> columns = Arrays.asList(
			TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING),
			TableModelTestUtils.createColumn(2L, "two", ColumnType.STRING_LIST)
		);
		
		createOrUpdateTable(columns, indexDescription);
		
		List<Row> rows = generateAndAppendRows(tableId, columns, 100);
		
		long offset = 10;
		long limit = 10;
		
		List<TableRowData> expected = rows.stream().skip(offset).limit(limit).map(row -> {
			List<TypedCellValue> rowData = new ArrayList<>();
			for (int i=0; i<columns.size(); i++) {
				rowData.add(new TypedCellValue(columns.get(i).getColumnType(), row.getValues().get(i)));
			}
			return new TableRowData(row.getRowId(), rowData);
		}).collect(Collectors.toList());
		
		// Call under test
		List<TableRowData> result = tableIndexDAO.getTableDataPage(tableId, columns, limit, offset);
		
		assertEquals(expected, result);
	}

	@Test
	public void testGetTableDataPageWithSubSchema() {
		List<ColumnModel> columns = Arrays.asList(
			TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING),
			TableModelTestUtils.createColumn(2L, "two", ColumnType.STRING_LIST),
			TableModelTestUtils.createColumn(3L, "three", ColumnType.INTEGER),
			TableModelTestUtils.createColumn(4L, "four", ColumnType.DOUBLE)
		);
		
		createOrUpdateTable(columns, indexDescription);
		
		List<Row> rows = generateAndAppendRows(tableId, columns, 100);		
			
		// We want to fetch only the first and third column data
		List<ColumnModel> subSchema = Arrays.asList(columns.get(0), columns.get(2));
		
		long offset = 0;
		long limit = 10;
		
		List<TableRowData> expected = rows.stream().limit(limit).map(row -> {
			List<TypedCellValue> rowData = new ArrayList<>();
			
			rowData.add(new TypedCellValue(columns.get(0).getColumnType(), row.getValues().get(0)));
			rowData.add(new TypedCellValue(columns.get(2).getColumnType(), row.getValues().get(2)));
			
			return new TableRowData(row.getRowId(), rowData);
		}).collect(Collectors.toList());
		
		// Call under test
		List<TableRowData> result = tableIndexDAO.getTableDataPage(tableId, subSchema, limit, offset);
		
		assertEquals(expected, result);
	}
		
	@Test
	public void testUpdateSearchIndex() {
		List<ColumnModel> columns = Arrays.asList(
			TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING),
			TableModelTestUtils.createColumn(2L, "two", ColumnType.STRING_LIST)
		);
		
		createOrUpdateTable(columns, indexDescription);
		
		List<Row> rows = generateAndAppendRows(tableId, columns, 100);
		
		List<RowSearchContent> expectedBatch = rows.stream().map(row -> 
			new RowSearchContent(row.getRowId(), String.join(" - ", row.getValues()))
		).collect(Collectors.toList());
			
		// Call under test
		tableIndexDAO.updateSearchIndex(tableId, expectedBatch);
		
		Set<Long> rowIds = rows.stream().map(Row::getRowId).collect(Collectors.toSet());

		assertEquals(expectedBatch, tableIndexDAO.fetchSearchContent(tableId, rowIds));
	}
	
	@Test
	public void testUpdateSearchIndexWithLargeBatch() {
		List<ColumnModel> columns = Arrays.asList(
			TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING),
			TableModelTestUtils.createColumn(2L, "two", ColumnType.STRING_LIST)
		);
		
		createOrUpdateTable(columns, indexDescription);
		
		List<Row> rows = generateAndAppendRows(tableId, columns, 10_000);
		
		List<RowSearchContent> batch = rows.stream().map(row -> 
			new RowSearchContent(row.getRowId(), String.join(" - ", row.getValues()))
		).collect(Collectors.toList());
			
		assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {	
			// Call under test
			tableIndexDAO.updateSearchIndex(tableId, batch);
		});
	}
		
	@Test
	public void testUpdateSearchIndexWithEmptyTable() {
		List<ColumnModel> columns = Arrays.asList(
			TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING),
			TableModelTestUtils.createColumn(2L, "two", ColumnType.STRING_LIST)
		);
		
		createOrUpdateTable(columns, indexDescription);
				
		Set<Long> rowIds = ImmutableSet.of(1L, 2L);
		
		List<RowSearchContent> batch = Arrays.asList(
			new RowSearchContent(1L, "something"),
			new RowSearchContent(2L, "something else")
		);	
					
		// Call under test
		tableIndexDAO.updateSearchIndex(tableId, batch);
		
		assertTrue(tableIndexDAO.fetchSearchContent(tableId, rowIds).isEmpty());
	}
	
	@Test
	public void testClearSearchIndex() {
		List<ColumnModel> columns = Arrays.asList(
			TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING),
			TableModelTestUtils.createColumn(2L, "two", ColumnType.STRING_LIST)
		);
		
		createOrUpdateTable(columns, indexDescription);
		
		// Now add some data
		List<Row> rows = generateAndAppendRows(tableId, columns, 100);
		Set<Long> rowIds = rows.stream().map(Row::getRowId).collect(Collectors.toSet());
		
		// Now sets the search content for each row to "search content"
		List<RowSearchContent> rowsSearchContent = rows.stream().map(row -> new RowSearchContent(row.getRowId(), "search content")).collect(Collectors.toList());
		
		tableIndexDAO.updateSearchIndex(tableId, rowsSearchContent);
		
		// Make sure we have the expected search content
		assertEquals(rowsSearchContent, tableIndexDAO.fetchSearchContent(tableId, rowIds));
		
		// Call under test
		tableIndexDAO.clearSearchIndex(tableId);

		// Makes sure all the rows were cleared and set to null
		List<RowSearchContent> expected = rows.stream().map(row -> new RowSearchContent(row.getRowId(), null)).collect(Collectors.toList());
			
		assertEquals(expected, tableIndexDAO.fetchSearchContent(tableId, rowIds));
	}
	
	@Test
	public void testDoesIndexStateMatchTableDoesNotExist(){
		// ensure the secondary tables for this index exist
		this.tableIndexDAO.createSecondaryTables(tableId);
		// the status table does not exist for this case.
		String md5 = "md5hex";
		long version = 123;
		// call under test
		boolean match = this.tableIndexDAO.doesIndexStateMatch(tableId, version, md5, false);
		assertFalse(match);
	}
	
	@Test
	public void testGetDistinctLongValues(){
		// create a table with a long column.
		ColumnModel column = new ColumnModel();
		column.setId("12");
		column.setName("foo");
		column.setColumnType(ColumnType.INTEGER);
		List<ColumnModel> schema = Lists.newArrayList(column);
		
		createOrUpdateTable(schema, indexDescription);
		// create three rows.
		List<Row> rows = TableModelTestUtils.createRows(schema, 2);
		// add duplicate values
		rows.addAll(TableModelTestUtils.createRows(schema, 2));
		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getSelectColumns(schema));
		set.setTableId(tableId.toString());
		
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		
		createOrUpdateOrDeleteRows(tableId, set, schema);
		
		Set<Long> results = tableIndexDAO.getDistinctLongValues(tableId, SQLUtils.getColumnNameForId(column.getId()));
		Set<Long> expected = Set.of(3000L, 3001L);
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetCurrentTableColumns(){
		// Create the table
		tableIndexDAO.createTableIfDoesNotExist(indexDescription);
		// Call under test.
		List<DatabaseColumnInfo> schema = getAllColumnInfo(tableId);
		assertNotNull(schema);
		assertEquals(3, schema.size());
		DatabaseColumnInfo cd = schema.get(0);
		assertEquals(ROW_ID, cd.getColumnName());
		assertTrue(cd.hasIndex(), "ROW_ID is the primary key so it should have an index.");
		
		cd = schema.get(1);
		assertEquals(ROW_VERSION, cd.getColumnName());
		assertFalse(cd.hasIndex());
		
		cd = schema.get(2);
		assertEquals(ROW_SEARCH_CONTENT, cd.getColumnName());
		assertTrue(cd.hasIndex());
	}
	
	@Test
	public void testAlterTableAsNeeded(){
		// This will be an add, so the old is null.
		ColumnModel oldColumn = null;
		ColumnModel newColumn = new ColumnModel();
		newColumn.setColumnType(ColumnType.BOOLEAN);
		newColumn.setId("123");
		newColumn.setName("aBoolean");
		ColumnChangeDetails change = new ColumnChangeDetails(oldColumn, newColumn);
		// Create the table
		tableIndexDAO.createTableIfDoesNotExist(indexDescription);
		boolean alterTemp = false;
		// call under test.
		boolean wasAltered = alterTableAsNeeded(tableId, Lists.newArrayList(change), alterTemp);
		assertTrue(wasAltered);
		// Check the results
		List<DatabaseColumnInfo> schema =  getAllColumnInfo(tableId);
		assertNotNull(schema);
		assertEquals(4, schema.size());
		DatabaseColumnInfo cd = schema.get(3);
		assertEquals("_C123_", cd.getColumnName());
		assertFalse(cd.hasIndex());
		
		// Another update of the same column with no change should not alter the table
		oldColumn = newColumn;
		change = new ColumnChangeDetails(oldColumn, newColumn);
		wasAltered = alterTableAsNeeded(tableId, Lists.newArrayList(change), alterTemp);
		assertFalse(wasAltered);
	}
	
	@Test
	public void testAlterTableAsNeededWithConvertToBooleanListColumnType() {
		// PLFM-6247
		
		ColumnModel oldColumn = TableModelTestUtils.createColumn(1L, "foo", ColumnType.BOOLEAN);
		List<ColumnModel> schema = Lists.newArrayList(oldColumn);
		createOrUpdateTable(schema, indexDescription);
		// add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 2);
		// apply the rows
		createOrUpdateOrDeleteRows(tableId, rows, schema);
		ColumnModel newColumn = TableModelTestUtils.createColumn(2L, "bar", ColumnType.BOOLEAN_LIST);
		List<ColumnChangeDetails> changes = Lists.newArrayList(new ColumnChangeDetails(oldColumn, newColumn));
		boolean alterTemp = false;
		boolean wasAltered = alterTableAsNeeded(tableId, changes, alterTemp);
		assertTrue(wasAltered);
		
		// another update should return false
		ColumnModel anotherColumn = newColumn;
		ColumnChangeDetails change = new ColumnChangeDetails(newColumn, anotherColumn);
		wasAltered = alterTableAsNeeded(tableId, Lists.newArrayList(change), alterTemp);
		assertFalse(wasAltered);
		// should be 2 rows
		assert(tableIndexDAO.getRowCountForTable(tableId) == 2);
	}
	
	@Test
	public void testAlterTableAsNeededWithConvertToIntegerListColumnType() {
		// PLFM-6247
		ColumnModel oldColumn = TableModelTestUtils.createColumn(1L, "foo", ColumnType.INTEGER);
		List<ColumnModel> schema = Lists.newArrayList(oldColumn);
		createOrUpdateTable(schema, indexDescription);
		// add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 2);
		// apply the rows
		createOrUpdateOrDeleteRows(tableId, rows, schema);
		ColumnModel newColumn = TableModelTestUtils.createColumn(2L, "bar", ColumnType.INTEGER_LIST);
		List<ColumnChangeDetails> changes = Lists.newArrayList(new ColumnChangeDetails(oldColumn, newColumn));
		boolean alterTemp = false;
		boolean wasAltered = alterTableAsNeeded(tableId, changes, alterTemp);
		assertTrue(wasAltered);
		
		// another update should return false
		ColumnModel anotherColumn = newColumn;
		ColumnChangeDetails change = new ColumnChangeDetails(newColumn, anotherColumn);
		wasAltered = alterTableAsNeeded(tableId, Lists.newArrayList(change), alterTemp);
		assertFalse(wasAltered);
		// should be 2 rows
		assert(tableIndexDAO.getRowCountForTable(tableId) == 2);
	}
	
	@Test
	public void testAlterTableAsNeededWithConvertToStringListColumnType() {
		// PLFM-6247
		ColumnModel oldColumn = TableModelTestUtils.createColumn(1L, "foo", ColumnType.STRING);
		List<ColumnModel> schema = Lists.newArrayList(oldColumn);
		createOrUpdateTable(schema, indexDescription);
		// add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 2);
		// apply the rows
		createOrUpdateOrDeleteRows(tableId, rows, schema);
		ColumnModel newColumn = TableModelTestUtils.createColumn(2L, "bar", ColumnType.STRING_LIST);
		List<ColumnChangeDetails> changes = Lists.newArrayList(new ColumnChangeDetails(oldColumn, newColumn));
		boolean alterTemp = false;
		boolean wasAltered = alterTableAsNeeded(tableId, changes, alterTemp);
		assertTrue(wasAltered);
		
		// another update should return false
		ColumnModel anotherColumn = newColumn;
		ColumnChangeDetails change = new ColumnChangeDetails(newColumn, anotherColumn);
		wasAltered = alterTableAsNeeded(tableId, Lists.newArrayList(change), alterTemp);
		assertFalse(wasAltered);
		// should be 2 rows
		assert(tableIndexDAO.getRowCountForTable(tableId) == 2);
	}

	@Test
	public void testVarCharMaxSize(){
		ColumnModel stringColumn = new ColumnModel();
		stringColumn.setId("15");
		stringColumn.setName("syn");
		stringColumn.setColumnType(ColumnType.STRING);
		stringColumn.setMaximumSize(255L);

		List<ColumnModel> schema = Lists.newArrayList(stringColumn);

		createOrUpdateTable(schema, indexDescription);

		List<Row> rows = TableModelTestUtils.createRows(schema, 5);

		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getSelectColumns(schema));
		set.setTableId(tableId.toString());

		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);

		createOrUpdateOrDeleteRows(tableId, set, schema);

		List<DatabaseColumnInfo> infoList = getAllColumnInfo(tableId);
		assertNotNull(infoList);

		DatabaseColumnInfo info = infoList.get(3);
		assertEquals("_C15_", info.getColumnName());
		assertEquals(new Long(5), info.getCardinality());

		assertEquals(MySqlColumnType.VARCHAR, info.getType());
		assertEquals(255, info.getMaxSize());

	}
	
	// This test would fail with MySQL version 8.0.28 (See https://sagebionetworks.jira.com/browse/PLFM-7471)
	@Test
	public void testColumnInfoAndCardinalityWithLargeTable() {
		List<ColumnModel> schema = List.of(
			new ColumnModel()
				.setId("0")
				.setName("aLargeTextColumn")
				.setMaximumSize(20L)
				.setColumnType(ColumnType.LARGETEXT), // This is translated into a MEDIUMTEXT
			new ColumnModel()
				.setId("1")
				.setName("aStringListColumn")
				.setMaximumSize(20L)
				.setMaximumListLength(2L)
				.setColumnType(ColumnType.STRING_LIST), // This is translated into a JSON
			new ColumnModel()
				.setId("2")
				.setName("aStringColumn")
				.setMaximumSize(20L)
				.setColumnType(ColumnType.STRING)
				
		);
		
		createOrUpdateTable(schema, indexDescription);
		
		int rowNumber = 100_000;
		List<Row> rows = TableModelTestUtils.createRows(schema, rowNumber);
		
		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getSelectColumns(schema));
		set.setTableId(tableId.toString());
		
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(range.getMinimumId() + rowNumber);
		range.setVersionNumber(1L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		
		createOrUpdateOrDeleteRows(tableId, set, schema);

		// This should work and return, on 8.0.28 this would wait for the server wait_timeout
		assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
			getAllColumnInfo(tableId);
		});
	}
	
	@Test
	public void testColumnInfoAndCardinality(){
		// create a table with a long column.
		ColumnModel intColumn = new ColumnModel();
		intColumn.setId("12");
		intColumn.setName("foo");
		intColumn.setColumnType(ColumnType.INTEGER);
		
		ColumnModel booleanColumn = new ColumnModel();
		booleanColumn.setId("13");
		booleanColumn.setName("bar");
		booleanColumn.setColumnType(ColumnType.BOOLEAN);
		
		List<ColumnModel> schema = Lists.newArrayList(intColumn, booleanColumn);
		
		createOrUpdateTable(schema, indexDescription);
		// create three rows.
		List<Row> rows = TableModelTestUtils.createRows(schema, 5);
		// add duplicate values
		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getSelectColumns(schema));
		set.setTableId(tableId.toString());
		
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		
		createOrUpdateOrDeleteRows(tableId, set, schema);
		
		List<DatabaseColumnInfo> infoList = getAllColumnInfo(tableId);
		assertNotNull(infoList);
		tableIndexDAO.optimizeTableIndices(infoList, tableId, 4);
		infoList = getAllColumnInfo(tableId);
		
		assertEquals(5, infoList.size());
		
		DatabaseColumnInfo info = infoList.get(0);
		// ROW_ID
		assertEquals("ROW_ID", info.getColumnName());
		assertEquals(TableConstants.COLUMN_NO_CARDINALITY, info.getCardinality());
		assertEquals("PRIMARY", info.getIndexName());
		assertTrue(info.hasIndex());

		assertEquals(MySqlColumnType.BIGINT, info.getType());
		assertNull(info.getMaxSize());
		assertNull(info.getColumnType());

		// one
		info = infoList.get(3);
		assertEquals("_C12_", info.getColumnName());
		assertEquals(new Long(5), info.getCardinality());
		assertTrue(info.hasIndex());
		assertEquals("_C12_idx_", info.getIndexName());

		assertEquals(MySqlColumnType.BIGINT, info.getType());
		assertNull(info.getMaxSize());
		assertEquals(ColumnType.INTEGER, info.getColumnType());
		
		// two
		info = infoList.get(4);
		assertEquals("_C13_", info.getColumnName());
		assertEquals(new Long(2), info.getCardinality());
		assertTrue(info.hasIndex());
		assertEquals("_C13_idx_", info.getIndexName());
		assertEquals(MySqlColumnType.TINYINT, info.getType());
		assertNull(info.getMaxSize());
		assertEquals(ColumnType.BOOLEAN, info.getColumnType());
	}
	
	@Test
	public void testColumnInfoAndCardinalityWithEmptyTable(){
		// create a table with a long column.
		ColumnModel intColumn = new ColumnModel();
		intColumn.setId("12");
		intColumn.setName("foo");
		intColumn.setColumnType(ColumnType.INTEGER);
		
		ColumnModel booleanColumn = new ColumnModel();
		booleanColumn.setId("13");
		booleanColumn.setName("bar");
		booleanColumn.setColumnType(ColumnType.BOOLEAN);
		
		List<ColumnModel> schema = Lists.newArrayList(intColumn, booleanColumn);
		
		createOrUpdateTable(schema, indexDescription);
				
		List<DatabaseColumnInfo> infoList = getAllColumnInfo(tableId);
		tableIndexDAO.optimizeTableIndices(infoList, tableId, 4);
		infoList = getAllColumnInfo(tableId);
		assertEquals(5, infoList.size());
		
		DatabaseColumnInfo info = infoList.get(0);
		
		// ROW_ID
		assertEquals("ROW_ID", info.getColumnName());
		assertEquals(TableConstants.COLUMN_NO_CARDINALITY, info.getCardinality());
		assertEquals("PRIMARY", info.getIndexName());
		assertTrue(info.hasIndex());
		assertEquals(MySqlColumnType.BIGINT, info.getType());
		assertNull(info.getMaxSize());
		assertNull(info.getColumnType());
		
		info = infoList.get(1);
		assertEquals("ROW_VERSION", info.getColumnName());
		assertEquals(TableConstants.COLUMN_NO_CARDINALITY, info.getCardinality());
		assertNull(info.getIndexName());
		assertFalse(info.hasIndex());
		assertEquals(MySqlColumnType.BIGINT, info.getType());
		assertNull(info.getMaxSize());
		assertNull(info.getColumnType());
		
		info = infoList.get(2);
		assertEquals("ROW_SEARCH_CONTENT", info.getColumnName());
		assertEquals(TableConstants.COLUMN_NO_CARDINALITY, info.getCardinality());
		assertEquals("ROW_SEARCH_CONTENT_INDEX", info.getIndexName());
		assertTrue(info.hasIndex());
		assertEquals(MySqlColumnType.MEDIUMTEXT, info.getType());
		assertNull(info.getMaxSize());
		assertNull(info.getColumnType());

		// one
		info = infoList.get(3);
		assertEquals("_C12_", info.getColumnName());
		assertEquals(0, info.getCardinality());
		assertTrue(info.hasIndex());
		assertEquals("_C12_idx_", info.getIndexName());
		assertEquals(MySqlColumnType.BIGINT, info.getType());
		assertNull(info.getMaxSize());
		assertEquals(ColumnType.INTEGER, info.getColumnType());
		
		// two
		info = infoList.get(4);
		assertEquals("_C13_", info.getColumnName());
		assertEquals(0, info.getCardinality());
		assertTrue(info.hasIndex());
		assertEquals("_C13_idx_", info.getIndexName());
		assertEquals(MySqlColumnType.TINYINT, info.getType());
		assertNull(info.getMaxSize());
		assertEquals(ColumnType.BOOLEAN, info.getColumnType());
	}
	
	@Test
	public void testColumnInfoAndCardinalityWithEmptyTableAndEmptySchema(){
		List<ColumnModel> schema = Collections.emptyList();
		
		createOrUpdateTable(schema, indexDescription);
				
		List<DatabaseColumnInfo> infoList = getAllColumnInfo(tableId);
		tableIndexDAO.optimizeTableIndices(infoList, tableId, 4);
		infoList = getAllColumnInfo(tableId);
		assertEquals(3, infoList.size());
		
		DatabaseColumnInfo info = infoList.get(0);
		
		// ROW_ID
		assertEquals("ROW_ID", info.getColumnName());
		assertEquals(TableConstants.COLUMN_NO_CARDINALITY, info.getCardinality());
		assertEquals("PRIMARY", info.getIndexName());
		assertTrue(info.hasIndex());
		assertEquals(MySqlColumnType.BIGINT, info.getType());
		assertNull(info.getMaxSize());
		assertNull(info.getColumnType());
		
		info = infoList.get(1);
		assertEquals("ROW_VERSION", info.getColumnName());
		assertEquals(TableConstants.COLUMN_NO_CARDINALITY, info.getCardinality());
		assertNull(info.getIndexName());
		assertFalse(info.hasIndex());
		assertEquals(MySqlColumnType.BIGINT, info.getType());
		assertNull(info.getMaxSize());
		assertNull(info.getColumnType());
		
		info = infoList.get(2);
		assertEquals("ROW_SEARCH_CONTENT", info.getColumnName());
		assertEquals(TableConstants.COLUMN_NO_CARDINALITY, info.getCardinality());
		assertEquals("ROW_SEARCH_CONTENT_INDEX", info.getIndexName());
		assertTrue(info.hasIndex());
		assertEquals(MySqlColumnType.MEDIUMTEXT, info.getType());
		assertNull(info.getMaxSize());
		assertNull(info.getColumnType());
	}
	
	@Test
	public void testColumnInfoAndCardinalityWithEmptyTableAndNoIndexeableColumn(){
		// create a table with a long column.
		ColumnModel stringListColumn = new ColumnModel();
		stringListColumn.setId("12");
		stringListColumn.setName("foo");
		stringListColumn.setColumnType(ColumnType.STRING_LIST);
		stringListColumn.setMaximumSize(10L);
		stringListColumn.setMaximumListLength(2L);
		
		ColumnModel largeTextColumn = new ColumnModel();
		largeTextColumn.setId("13");
		largeTextColumn.setName("bar");
		largeTextColumn.setColumnType(ColumnType.LARGETEXT);
		
		List<ColumnModel> schema = Lists.newArrayList(stringListColumn, largeTextColumn);
		
		createOrUpdateTable(schema, indexDescription);
				
		List<DatabaseColumnInfo> infoList = getAllColumnInfo(tableId);
		tableIndexDAO.optimizeTableIndices(infoList, tableId, 4);
		infoList = getAllColumnInfo(tableId);
		assertEquals(5, infoList.size());
		
		DatabaseColumnInfo info = infoList.get(0);
		
		// ROW_ID
		assertEquals("ROW_ID", info.getColumnName());
		assertEquals(TableConstants.COLUMN_NO_CARDINALITY, info.getCardinality());
		assertEquals("PRIMARY", info.getIndexName());
		assertTrue(info.hasIndex());
		assertEquals(MySqlColumnType.BIGINT, info.getType());
		assertNull(info.getMaxSize());
		assertNull(info.getColumnType());
		
		info = infoList.get(1);
		assertEquals("ROW_VERSION", info.getColumnName());
		assertEquals(TableConstants.COLUMN_NO_CARDINALITY, info.getCardinality());
		assertNull(info.getIndexName());
		assertFalse(info.hasIndex());
		assertEquals(MySqlColumnType.BIGINT, info.getType());
		assertNull(info.getMaxSize());
		assertNull(info.getColumnType());
		
		info = infoList.get(2);
		assertEquals("ROW_SEARCH_CONTENT", info.getColumnName());
		assertEquals(TableConstants.COLUMN_NO_CARDINALITY, info.getCardinality());
		assertEquals("ROW_SEARCH_CONTENT_INDEX", info.getIndexName());
		assertTrue(info.hasIndex());
		assertEquals(MySqlColumnType.MEDIUMTEXT, info.getType());
		assertNull(info.getMaxSize());
		assertNull(info.getColumnType());

		// one
		info = infoList.get(3);
		assertEquals("_C12_", info.getColumnName());
		assertEquals(TableConstants.COLUMN_NO_CARDINALITY, info.getCardinality());
		assertFalse(info.hasIndex());
		assertNull(info.getIndexName());
		assertEquals(MySqlColumnType.JSON, info.getType());
		assertNull(info.getMaxSize());
		assertEquals(ColumnType.STRING_LIST, info.getColumnType());
		
		// two
		info = infoList.get(4);
		assertEquals("_C13_", info.getColumnName());
		assertEquals(TableConstants.COLUMN_NO_CARDINALITY, info.getCardinality());
		assertFalse(info.hasIndex());
		assertNull(info.getIndexName());
		assertEquals(MySqlColumnType.MEDIUMTEXT, info.getType());
		assertNull(info.getMaxSize());
		assertEquals(ColumnType.LARGETEXT, info.getColumnType());
	}
	
	@Test
	public void testIndexAdd(){
		// create the table
		tableIndexDAO.createTableIfDoesNotExist(indexDescription);
		ColumnModel oldColumn = null;
		
		// Create a column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("12");
		newColumn.setName("foo");
		newColumn.setColumnType(ColumnType.INTEGER);
		boolean alterTemp = false;
		// add the column
		alterTableAsNeeded(tableId, Lists.newArrayList(new ColumnChangeDetails(oldColumn, newColumn)), alterTemp);
		int maxNumberOfIndices = 5;
		optimizeTableIndices(tableId, maxNumberOfIndices);
		// Get the latest table information
		List<DatabaseColumnInfo> infoList = getAllColumnInfo(tableId);
		assertNotNull(infoList);
		assertEquals(4, infoList.size());
		DatabaseColumnInfo info = infoList.get(3);
		assertEquals("_C12_idx_",info.getIndexName());
	}
	
	@Test
	public void testIndexRename(){
		// create the table
		tableIndexDAO.createTableIfDoesNotExist(indexDescription);
		ColumnModel oldColumn = null;
		
		// Create a column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("12");
		newColumn.setName("foo");
		newColumn.setColumnType(ColumnType.INTEGER);
		boolean alterTemp = false;
		// add the column
		alterTableAsNeeded(tableId, Lists.newArrayList(new ColumnChangeDetails(oldColumn, newColumn)), alterTemp);
		int maxNumberOfIndices = 5;
		optimizeTableIndices(tableId, maxNumberOfIndices);
		// Get the latest table information
		List<DatabaseColumnInfo> infoList = getAllColumnInfo(tableId);
		assertNotNull(infoList);
		assertEquals(4, infoList.size());
		DatabaseColumnInfo info = infoList.get(3);
		assertEquals("_C12_idx_",info.getIndexName());
		
		// Now change the column type
		oldColumn = newColumn;
		newColumn = new ColumnModel();
		newColumn.setId("13");
		newColumn.setName("bar");
		newColumn.setColumnType(ColumnType.DATE);
		
		alterTableAsNeeded(tableId, Lists.newArrayList(new ColumnChangeDetails(oldColumn, newColumn)), alterTemp);
		// the index should get renamed
		optimizeTableIndices(tableId, maxNumberOfIndices);
		infoList = getAllColumnInfo(tableId);
		assertNotNull(infoList);
		assertEquals(4, infoList.size());
		info = infoList.get(3);
		assertEquals("_C13_idx_",info.getIndexName());
	}
	
	@Test
	public void testIndexDrop(){
		// create the table
		tableIndexDAO.createTableIfDoesNotExist(indexDescription);
		ColumnModel oldColumn = null;
		
		// Create a column
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("12");
		newColumn.setName("foo");
		newColumn.setColumnType(ColumnType.INTEGER);
		boolean alterTemp = false;
		// add the column
		alterTableAsNeeded(tableId, Lists.newArrayList(new ColumnChangeDetails(oldColumn, newColumn)), alterTemp);
		int maxNumberOfIndices = 5;
		optimizeTableIndices(tableId, maxNumberOfIndices);
		// Get the latest table information
		List<DatabaseColumnInfo> infoList = getAllColumnInfo(tableId);
		assertNotNull(infoList);
		assertEquals(4, infoList.size());
		DatabaseColumnInfo info = infoList.get(3);
		assertEquals("_C12_idx_",info.getIndexName());
		
		// reduce the number of allowed indices
		maxNumberOfIndices = 1;
		// the index should get renamed
		optimizeTableIndices(tableId, maxNumberOfIndices);
		// the column should no longer have an index.
		infoList = getAllColumnInfo(tableId);
		assertNotNull(infoList);
		assertEquals(4, infoList.size());
		info = infoList.get(3);
		assertFalse(info.hasIndex());
		assertEquals(null,info.getIndexName());
	}
	
	/**
	 * Helper to get all of the DatabaseColumnInfo about a table. 
	 * @param tableId
	 * @return
	 */
	public List<DatabaseColumnInfo> getAllColumnInfo(IdAndVersion tableId){
		List<DatabaseColumnInfo> info = tableIndexDAO.getDatabaseInfo(tableId);
		tableIndexDAO.provideCardinality(info, tableId);
		tableIndexDAO.provideIndexName(info, tableId);
		return info;
	}
	
	/**
	 * Helper to optimize the indices for a table.
	 * 
	 * @param tableId
	 * @param maxNumberOfIndices
	 */
	public void optimizeTableIndices(IdAndVersion tableId, int maxNumberOfIndices){
		List<DatabaseColumnInfo> info = getAllColumnInfo(tableId);
		tableIndexDAO.optimizeTableIndices(info, tableId, maxNumberOfIndices);
	}
	
	@Test
	public void testGetDatabaseInfoEmpty(){
		// table does not exist
		List<DatabaseColumnInfo> info = tableIndexDAO.getDatabaseInfo(tableId);
		assertNotNull(info);
		assertTrue(info.isEmpty());
	}
		
	@Test
	public void testCreateTempTable(){
		ColumnModel intColumn = new ColumnModel();
		intColumn.setId("12");
		intColumn.setName("foo");
		intColumn.setColumnType(ColumnType.INTEGER);
		
		ColumnModel booleanColumn = new ColumnModel();
		booleanColumn.setId("13");
		booleanColumn.setName("bar");
		booleanColumn.setColumnType(ColumnType.BOOLEAN);
		
		List<ColumnModel> schema = Lists.newArrayList(intColumn, booleanColumn);
		
		createOrUpdateTable(schema, indexDescription);
		// create five rows.
		List<Row> rows = TableModelTestUtils.createRows(schema, 5);
		// add duplicate values
		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getSelectColumns(schema));
		set.setTableId(tableId.toString());
		
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		
		createOrUpdateOrDeleteRows(tableId, set, schema);
		
		tableIndexDAO.deleteTemporaryTable(tableId);
		// Create a copy of the table
		tableIndexDAO.createTemporaryTable(tableId);
		// populate table with data
		tableIndexDAO.copyAllDataToTemporaryTable(tableId);
		
		long count = tableIndexDAO.getTempTableCount(tableId);
		assertEquals(5L, count);
		// delete the temp and get the count again
		tableIndexDAO.deleteTemporaryTable(tableId);
		count = tableIndexDAO.getTempTableCount(tableId);
		assertEquals(0L, count);
	}


	@Test
	public void testCreateMultiValueColumnIndexTable(){
		ColumnModel strListCol = new ColumnModel();
		strListCol.setId("12");
		strListCol.setName("foo");
		strListCol.setMaximumSize(50L);
		strListCol.setColumnType(ColumnType.STRING_LIST);
		strListCol.setMaximumListLength(25L);

		List<ColumnModel> schema = Lists.newArrayList(strListCol);

		createOrUpdateTable(schema, indexDescription);
		// create five rows.
		List<Row> rows = TableModelTestUtils.createRows(schema, 5);
		// add duplicate values
		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getSelectColumns(schema));
		set.setTableId(tableId.toString());

		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);

		createOrUpdateOrDeleteRows(tableId, set, schema);
		//populate the column index table
		tableIndexDAO.populateListColumnIndexTable(tableId, strListCol, null, false);

		String columnId = strListCol.getId();

		tableIndexDAO.deleteAllTemporaryMultiValueColumnIndexTable(tableId);
		tableIndexDAO.deleteTemporaryTable(tableId);

		// Create a copy of the table
		tableIndexDAO.createTemporaryTable(tableId);
		tableIndexDAO.copyAllDataToTemporaryTable(tableId);
		tableIndexDAO.createTemporaryMultiValueColumnIndexTable(tableId, columnId);
		// populate table with data
		tableIndexDAO.copyAllDataToTemporaryMultiValueColumnIndexTable(tableId, columnId);

		long count = tableIndexDAO.getTempTableMultiValueColumnIndexCount(tableId, columnId);

		// we created 5 rows using the test utility, but there are 2 values per row which are expanded into the index table
		assertEquals(10L, count);
		// delete the temp and get the count again
		tableIndexDAO.deleteAllTemporaryMultiValueColumnIndexTable(tableId);
		tableIndexDAO.deleteTemporaryTable(tableId);
		count = tableIndexDAO.getTempTableMultiValueColumnIndexCount(tableId, columnId);
		assertEquals(0L, count);
	}
	
	@Test
	public void testEntityReplication(){
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(1L,2L,3L,4L));

		ObjectDataDTO project = createObjectDataDTO(1L, EntityType.project, 0);
		ObjectDataDTO folder = createObjectDataDTO(2L, EntityType.folder, 5);
		ObjectDataDTO file = createObjectDataDTO(3L, EntityType.file, 10);
		ObjectDataDTO dataset = createObjectDataDTO(4L, EntityType.dataset, 0);
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file, dataset, folder, project));

		// lookup each
		ObjectDataDTO fetched = tableIndexDAO.getObjectData(mainType, project.getId(), project.getVersion());
		assertEquals(project, fetched);
		fetched = tableIndexDAO.getObjectData(mainType, folder.getId(), folder.getVersion());
		assertEquals(folder, fetched);
		fetched = tableIndexDAO.getObjectData(mainType, file.getId(), file.getVersion());
		assertEquals(file, fetched);
		fetched = tableIndexDAO.getObjectData(mainType, dataset.getId(), dataset.getVersion());
		assertEquals(dataset, fetched);
	}
	
	/**
	 * Test for PLFM-6306: The index failed to replicate because the insert was not handling duplicates. When replicating
	 * we first delete and then insert but with concurrent inserts and with no data it could lead to a race condition where
	 * a second thread that wasn't blocked by the first delete (since nothing needed to be deleted) tries to insert the same 
	 * record. The solution is to allow updating on duplicate.
	 */
	@Test
	public void testEntityReplicationWithUpdate(){
		
		ObjectDataDTO objectData = createObjectDataDTO(2L, EntityType.file, 2);
		
		// Add the data to the index once
		tableIndexDAO.addObjectData(mainType, Collections.singletonList(objectData));
		
		ObjectDataDTO result = tableIndexDAO.getObjectData(mainType, objectData.getId(), objectData.getVersion());
	
		assertEquals(objectData, result);
		
		// Update the data
		objectData.setEtag(objectData.getEtag() + "_updated");
		objectData.getAnnotations().get(0).setValue("updated_annotation");
		
		// Re-adding to the index should simply update the object without throwing
		tableIndexDAO.addObjectData(mainType, Collections.singletonList(objectData));
		
		// Makes sure the data was updated
		result = tableIndexDAO.getObjectData(mainType, objectData.getId(), objectData.getVersion());
		
		assertEquals(objectData, result);
	}
	
	@Test
	public void testEntityReplicationWithDuplicates() {
		
		Long objectId = 2L;
		
		// Creates two objects with the same id, but with different data
		List<ObjectDataDTO> objectData = Arrays.asList(
				createObjectDataDTO(objectId, EntityType.file, 2),
				createObjectDataDTO(objectId, EntityType.file, 3)				
		);
		
		// This should work, but only the latter should have been added
		tableIndexDAO.addObjectData(mainType, objectData);
		
		ObjectDataDTO result = tableIndexDAO.getObjectData(mainType, objectId, 2L);
		
		assertEquals(objectData.get(1), result);
		
	}

	@Test
	public void testEntityReplication_maxStringLength(){
		// delete all data
		long id = 1L;
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(id));

		ObjectDataDTO project = createObjectDataDTO(id, EntityType.project, 0);
		ObjectAnnotationDTO stringAnno = new ObjectAnnotationDTO(project);
		stringAnno.setType(AnnotationType.STRING);
		stringAnno.setKey("myStringAnno");
		stringAnno.setValue(Arrays.asList("a", "ab", "aaa", "abc", "c"));
		project.setAnnotations(Collections.singletonList(stringAnno));

		tableIndexDAO.addObjectData(mainType, Collections.singletonList(project));

		// lookup the column manually
		String query = "SELECT " + ANNOTATION_REPLICATION_COL_MAX_STRING_LENGTH +
				" FROM " + ANNOTATION_REPLICATION_TABLE +
				" WHERE " + ANNOTATION_REPLICATION_COL_OBJECT_TYPE + "='" + objectType.name() 
				+ "' AND " + ANNOTATION_REPLICATION_COL_OBJECT_ID + "=" + id;
		long queriedMaxSize = tableIndexDAO.getConnection().queryForObject(query, Long.class);
		assertEquals(3, queriedMaxSize);
	}

	@Test
	public void testEntityReplication_AbstractDoubles(){
		// delete all data
		long id = 1L;
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(id));

		ObjectDataDTO project = createObjectDataDTO(id, EntityType.project, 0);
		ObjectAnnotationDTO abstractDoubleAnnos = new ObjectAnnotationDTO(project);
		abstractDoubleAnnos.setType(AnnotationType.DOUBLE);
		abstractDoubleAnnos.setKey("abstractDoubles");
		abstractDoubleAnnos.setValue(Arrays.asList("1.2", "infinity", "+infinity", "5.6", "nan", "7.8"));
		project.setAnnotations(Collections.singletonList(abstractDoubleAnnos));

		tableIndexDAO.addObjectData(mainType, Collections.singletonList(project));

		// lookup each
		ObjectDataDTO fetched = tableIndexDAO.getObjectData(mainType, id, project.getVersion());
		assertEquals(project, fetched);
	}
	
	@Test
	public void testEntityReplicationWithNulls(){
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(1L));
		
		ObjectDataDTO project = createObjectDataDTO(1L, EntityType.project, 0);
		project.setParentId(null);
		project.setProjectId(null);
		project.setFileHandleId(null);
		project.setFileMD5(null);
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(project));
		
		// lookup each
		ObjectDataDTO fetched = tableIndexDAO.getObjectData(mainType, 1L, project.getVersion());
		assertEquals(project, fetched);
	}
	
	@Test
	public void testEntityReplicationWithNullBenefactor(){
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(1L));
		
		ObjectDataDTO project = createObjectDataDTO(1L, EntityType.project, 0);
		project.setBenefactorId(null);
		try {
			tableIndexDAO.addObjectData(mainType, Lists.newArrayList(project));
			fail();
		} catch (Exception e) {
			// expected
		}
	}
	
	@Test
	public void testEntityReplicationUpdate(){
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(1L));
		
		ObjectDataDTO file = createObjectDataDTO(1L, EntityType.file, 5);
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file));
		// delete before an update
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(file.getId()));
		file = createObjectDataDTO(1L, EntityType.file, 3);
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file));
		
		// lookup each
		ObjectDataDTO fetched = tableIndexDAO.getObjectData(mainType, 1L, file.getVersion());
		assertEquals(file, fetched);
	}

	@Test
	public void testGetMaxListSizeForAnnotations_nullFilter() throws ParseException {

		Set<String> annotationNames = Set.of("foo", "bar");
		
		ViewFilter filter = null;
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () ->
				((TableIndexDAOImpl) tableIndexDAO).getMaxListSizeForAnnotations(filter, annotationNames)
		).getMessage();

		assertEquals("filter is required.", errorMessage);

	}
	

	@Test
	public void testGetMaxListSizeForAnnotations_emptyScope() throws ParseException {

		Set<Long> emptyScope = Collections.emptySet();

		Set<String> annotationNames = Set.of("foo", "bar");
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, emptyScope);
	
		assertEquals(Collections.emptyMap(),
				((TableIndexDAOImpl) tableIndexDAO).getMaxListSizeForAnnotations(filter, annotationNames));

	}

	@Test
	public void testGetMaxListSizeForAnnotations_nullAnnotationNames() throws ParseException {

		Set<Long> scope = Set.of(222L,333L);

		Set<String> nullAnnotationNames = null;
			
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);

		String errorMessage = assertThrows(IllegalArgumentException.class, () ->
				((TableIndexDAOImpl) tableIndexDAO).getMaxListSizeForAnnotations(filter, nullAnnotationNames)
		).getMessage();

		assertEquals("annotationNames is required.", errorMessage);

	}


	@Test
	public void testGetMaxListSizeForAnnotations_emptyAnnotationNames() throws ParseException {

		Set<Long> scope = Set.of(222L,333L);

		Set<String> emptyAnnotationNames = Collections.emptySet();
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);

		assertEquals(Collections.emptyMap(),
				((TableIndexDAOImpl) tableIndexDAO).getMaxListSizeForAnnotations(filter, emptyAnnotationNames));

	}

	@Test
	public void testGetMaxListSizeForAnnotations() throws ParseException {
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(2L,3L));

		// setup some hierarchy.
		ObjectDataDTO file1 = createObjectDataDTO(2L, EntityType.file, 2);
		file1.setParentId(333L);
		ObjectAnnotationDTO foo1 = new ObjectAnnotationDTO(file1);
		foo1.setKey("foo");
		foo1.setValue(Arrays.asList("one", "two", "three"));
		foo1.setType(AnnotationType.STRING);
		ObjectAnnotationDTO bar1 = new ObjectAnnotationDTO(file1);
		bar1.setKey("bar");
		bar1.setValue(Arrays.asList("1", "2", "3"));
		bar1.setType(AnnotationType.LONG);
		file1.setAnnotations(Arrays.asList(foo1,bar1));


		ObjectDataDTO file2 = createObjectDataDTO(3L, EntityType.file, 3);
		file2.setParentId(222L);
		ObjectAnnotationDTO foo2 = new ObjectAnnotationDTO(file2);
		foo2.setKey("foo");
		foo2.setValue(Arrays.asList("one", "two"));
		foo2.setType(AnnotationType.STRING);
		ObjectAnnotationDTO bar2 = new ObjectAnnotationDTO(file2);
		bar2.setKey("bar");
		bar2.setValue(Arrays.asList("1", "2", "3", "4","5"));
		bar2.setType(AnnotationType.LONG);
		file2.setAnnotations(Arrays.asList(foo2,bar2));


		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file1, file2));

		// both parents
		Set<Long> scope = Set.of(file1.getParentId(), file2.getParentId());
		// Create the schema for this table
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(file2);
		// Create the view index
		createOrUpdateTable(schema, indexDescription);
		Set<String> annotationNames = Set.of("foo", "bar");
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);
		
		// method under test
		Map<String, Long> listSizes = ((TableIndexDAOImpl) tableIndexDAO).getMaxListSizeForAnnotations(filter, annotationNames);
		HashMap<String,Long> expected = new HashMap<>();
		expected.put("foo",3L);
		expected.put("bar",5L);
		assertEquals(expected, listSizes);
	}


	@Test
	public void testGetMaxListSizeForAnnotations_noAnnotationsInReplication() throws ParseException {
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(2L,3L));

		// setup some hierarchy.
		ObjectDataDTO file1 = createObjectDataDTO(2L, EntityType.file, 2);
		file1.setParentId(333L);

		ObjectDataDTO file2 = createObjectDataDTO(3L, EntityType.file, 3);
		file2.setParentId(222L);


		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file1, file2));

		// both parents
		Set<Long> scope = Set.of(file1.getParentId(), file2.getParentId());
		// Create the schema for this table
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(file2);
		// Create the view index
		createOrUpdateTable(schema, indexDescription);
		// Copy the entity data to the table
		// method under test
		Set<String> annotationNames = Set.of("foo", "bar");
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);
	
		Map<String, Long> listSizes = ((TableIndexDAOImpl) tableIndexDAO).getMaxListSizeForAnnotations(filter, annotationNames);
		assertEquals(Collections.emptyMap(), listSizes);
	}

	@Test
	public void testGetMaxListSizeForAnnotations_WithObjectIdFilter() throws ParseException {
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(2L,3L));

		// setup some hierarchy.
		ObjectDataDTO file1 = createObjectDataDTO(2L, EntityType.file, 2);
		file1.setParentId(333L);
		ObjectAnnotationDTO foo1 = new ObjectAnnotationDTO(file1);
		foo1.setKey("foo");
		foo1.setValue(Arrays.asList("one", "two", "three"));
		foo1.setType(AnnotationType.STRING);
		foo1.setObjectId(2L);
		ObjectAnnotationDTO bar1 = new ObjectAnnotationDTO(file1);
		bar1.setKey("bar");
		bar1.setValue(Arrays.asList("1", "2", "3"));
		bar1.setType(AnnotationType.LONG);

		file1.setAnnotations(Arrays.asList(foo1,bar1));


		ObjectDataDTO file2 = createObjectDataDTO(3L, EntityType.file, 3);
		file2.setParentId(222L);
		ObjectAnnotationDTO foo2 = new ObjectAnnotationDTO(file2);
		foo2.setKey("foo");
		foo2.setValue(Arrays.asList("one", "two"));
		foo2.setType(AnnotationType.STRING);
		ObjectAnnotationDTO bar2 = new ObjectAnnotationDTO(file2);
		bar2.setKey("bar");
		bar2.setValue(Arrays.asList("1", "2", "3", "4"));
		bar2.setType(AnnotationType.LONG);
		file2.setAnnotations(Arrays.asList(foo2,bar2));


		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file1, file2));

		// both parents
		Set<Long> scope = Set.of(file1.getParentId(), file2.getParentId());
		// Create the schema for this table
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(file2);
		// Create the view index
		createOrUpdateTable(schema, indexDescription);
		Set<String> annotationNames = Set.of("foo", "bar");

		Set<Long> objectIdFilter = Set.of(2L);
		boolean excludeDerivedKeys = false;
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, objectIdFilter, null, scope, excludeDerivedKeys);
		
		// method under test
		Map<String, Long> listSizes = ((TableIndexDAOImpl) tableIndexDAO).getMaxListSizeForAnnotations(filter, annotationNames);
		HashMap<String,Long> expected = new HashMap<>();
		expected.put("foo",3L);
		expected.put("bar",3L);
		assertEquals(expected, listSizes);
	}


	@Test
	public void testCopyEntityReplicationToTable(){
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(2L,3L));
		
		// setup some hierarchy.
		ObjectDataDTO file1 = createObjectDataDTO(2L, EntityType.file, 2);
		file1.setParentId(333L);
		ObjectDataDTO file2 = createObjectDataDTO(3L, EntityType.file, 3);
		file2.setParentId(222L);
		
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file1, file2));
		
		// both parents
		Set<Long> scope = Set.of(file1.getParentId(), file2.getParentId());
		// Create the schema for this table
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(file2);
		// Create the view index
		createOrUpdateTable(schema, indexDescription);
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);
		
		// Copy the entity data to the table
		tableIndexDAO.copyObjectReplicationToView(tableId.getId(), filter, schema, fieldTypeMapper);
		
		// Query the results
		long count = tableIndexDAO.getRowCountForTable(tableId);
		assertEquals(2, count);
	}

	@Test
	public void testCopyEntityReplicationToTable_WithListAnnotations() throws ParseException {
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(2L,3L));

		// setup some hierarchy.
		ObjectDataDTO file1 = createObjectDataDTO(2L, EntityType.file, 2);
		file1.setParentId(333L);
		ObjectAnnotationDTO double1 = new ObjectAnnotationDTO(file1);
		double1.setKey("foo");
		double1.setValue(Arrays.asList("NaN", "1.2", "Infinity"));
		double1.setType(AnnotationType.STRING);
		file1.setAnnotations(Arrays.asList(double1));
		ObjectDataDTO file2 = createObjectDataDTO(3L, EntityType.file, 3);
		file2.setParentId(222L);
		ObjectAnnotationDTO double2 = new ObjectAnnotationDTO(file2);
		double2.setKey("foo");
		double2.setValue(Arrays.asList("Infinity", "222.222"));
		double2.setType(AnnotationType.STRING);
		file2.setAnnotations(Arrays.asList(double2));

		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file1, file2));

		// both parents
		Set<Long> scope = Set.of(file1.getParentId(), file2.getParentId());
		// Create the schema for this table
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(file2);
		// Create the view index
		createOrUpdateTable(schema, indexDescription);
	
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);
		
		// Copy the entity data to the table
		// method under test
		tableIndexDAO.copyObjectReplicationToView(tableId.getId(), filter, schema, fieldTypeMapper);

		// This is our query
		QueryTranslator query = QueryTranslator.builder("select foo from " + tableId, schemaProvider(schema), userId).indexDescription(new TableIndexDescription(tableId)).build();
		// Query the results
		RowSet result = tableIndexDAO.query(mockProgressCallback, query);
		assertEquals(2, result.getRows().size());
		assertEquals(Collections.singletonList("[\"NaN\", \"1.2\", \"Infinity\"]"), result.getRows().get(0).getValues());
		assertEquals(Collections.singletonList("[\"Infinity\", \"222.222\"]"), result.getRows().get(1).getValues());
	}
	
	/*
	 * PLFM-4336
	 */
	@Test
	public void testCopyEntityReplicationToTableScopeWithDoubleAnnotation(){
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(2L,3L));
		
		// setup some hierarchy.
		ObjectDataDTO file1 = createObjectDataDTO(2L, EntityType.file, 2);
		file1.setParentId(333L);
		ObjectAnnotationDTO double1 = new ObjectAnnotationDTO(file1);
		double1.setKey("foo");
		double1.setValue("NaN");
		double1.setType(AnnotationType.DOUBLE);
		file1.setAnnotations(Arrays.asList(double1));
		ObjectDataDTO file2 = createObjectDataDTO(3L, EntityType.file, 3);
		file2.setParentId(222L);
		ObjectAnnotationDTO double2 = new ObjectAnnotationDTO(file2);
		double2.setKey("foo");
		double2.setValue("Infinity");
		double2.setType(AnnotationType.DOUBLE);
		file2.setAnnotations(Arrays.asList(double2));
		
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file1, file2));
		
		// both parents
		Set<Long> scope = Set.of(file1.getParentId(), file2.getParentId());
		List<ColumnModel> schema = Lists.newArrayList(TableModelTestUtils
				.createColumn(1L, "foo", ColumnType.DOUBLE));
		// Create the view index
		createOrUpdateTable(schema, indexDescription);
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);
		
		// Copy the entity data to the table
		tableIndexDAO.copyObjectReplicationToView(tableId.getId(), filter, schema, fieldTypeMapper);
		// Query the results
		long count = tableIndexDAO.getRowCountForTable(tableId);
		assertEquals(2, count);
	}
	
	@Test
	public void testRestoreTableIndexData(){
		tableId = IdAndVersion.parse("syn123.45");
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(2L,3L));
		tableIndexDAO.deleteTable(tableId);
		
		List<ColumnModel> schema = Arrays.asList(
			TableModelTestUtils.createColumn(1L, "foo", ColumnType.INTEGER_LIST), 
			TableModelTestUtils.createColumn(2L, "bar", ColumnType.DOUBLE)
		);
		
		createOrUpdateTable(schema, indexDescription);
		
		List<String[]> rows = Arrays.asList(
			new String[] {"ROW_ID", "ROW_VERSION", "ROW_ETAG", "ROW_BENEFACTOR" , "_C1_", "_C2_", "_DBL_C2_"},
			new String[] {"2", "2", "etag2", "2", "[123, 456, 789]", null, "NaN"}, 
			new String[] {"3", "2", "etag3", "2", "[321, 654]", "1.7976931348623157E308", "Infinity"}
		);
		
		// small batch size to force multiple batches.
		long maxBytesPerBatch = 10;
		// call under test
		tableIndexDAO.restoreTableIndexData(tableId, rows.iterator(), maxBytesPerBatch);
		
		long count = tableIndexDAO.getRowCountForTable(tableId);
		assertEquals(rows.size()-1, count);
	}

	@Test
	public void testCopyEntityReplicationToTableScopeEmpty(){
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(2L,3L));
		
		// setup some hierarchy.
		ObjectDataDTO file1 = createObjectDataDTO(2L, EntityType.file, 2);
		file1.setParentId(333L);
		ObjectDataDTO file2 = createObjectDataDTO(3L, EntityType.file, 3);
		file2.setParentId(222L);
		
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file1, file2));
		
		// both parents
		Set<Long> scope = new HashSet<Long>();
		// Create the schema for this table
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(file2);
		// Create the view index
		createOrUpdateTable(schema, indexDescription);
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);
		
		// Copy the entity data to the table
		tableIndexDAO.copyObjectReplicationToView(tableId.getId(), filter, schema, fieldTypeMapper);
		// Query the results
		long count = tableIndexDAO.getRowCountForTable(tableId);
		assertEquals(0, count);
	}
	
	@Test
	public void testGetPossibleAnnotationsForContainers(){
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(2L,3L));
		
		// setup some hierarchy.
		ObjectDataDTO file1 = createObjectDataDTO(2L, EntityType.file, 15);
		file1.setParentId(333L);
		ObjectDataDTO file2 = createObjectDataDTO(3L, EntityType.file, 12);
		file2.setParentId(222L);
		
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file1, file2));
		
		Set<Long> containerIds = Set.of(222L, 333L);
		long limit = 5;
		long offset = 0;
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, containerIds);
		
		List<ColumnModel> columns = tableIndexDAO.getPossibleColumnModelsForContainers(filter, limit, offset);
		assertNotNull(columns);
		assertEquals(limit, columns.size());
		// one
		ColumnModel cm = columns.get(0);
		assertEquals("key0", cm.getName());
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(new Long(1L), cm.getMaximumSize());
		// two
		cm = columns.get(1);
		assertEquals("key1", cm.getName());
		assertEquals(ColumnType.INTEGER, cm.getColumnType());
		assertEquals(null, cm.getMaximumSize());
		// three
		cm = columns.get(2);
		assertEquals("key10", cm.getName());
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(2, cm.getMaximumSize());
	}
	
	@Test
	public void testGetPossibleAnnotationsForContainersExcludingKeys(){
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(2L,3L));
		
		// setup some hierarchy.
		ObjectDataDTO file1 = createObjectDataDTO(2L, EntityType.file, 15);
		file1.setParentId(333L);
		ObjectDataDTO file2 = createObjectDataDTO(3L, EntityType.file, 12);
		file2.setParentId(222L);
		
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file1, file2));
		
		Set<Long> containerIds = Set.of(222L, 333L);
		long limit = 5;
		long offset = 0;
		boolean excludeDerivedKeys = false;
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, null, Set.of("key0"), containerIds, excludeDerivedKeys);
		
		List<ColumnModel> columns = tableIndexDAO.getPossibleColumnModelsForContainers(filter, limit, offset);
		
		assertNotNull(columns);
		assertEquals(limit, columns.size());
		// one
		ColumnModel cm = columns.get(0);
		assertEquals("key1", cm.getName());
		assertEquals(ColumnType.INTEGER, cm.getColumnType());
		assertEquals(null, cm.getMaximumSize());
		// two
		cm = columns.get(1);
		assertEquals("key10", cm.getName());
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(2, cm.getMaximumSize());
	}
	
	/**
	 * Test added for PLFM-5034
	 */
	@Test
	public void testGetPossibleAnnotationsForContainersPLFM_5034(){
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(2L,3L));
		
		String duplicateName = "duplicate";
		
		// one
		ObjectDataDTO file1 = createObjectDataDTO(2L, EntityType.file, 15);
		file1.getAnnotations().clear();
		file1.setParentId(333L);
		// add a string annotation with a size of 3
		ObjectAnnotationDTO annoDto = new ObjectAnnotationDTO(file1);
		annoDto.setObjectId(file1.getId());
		annoDto.setKey(duplicateName);
		annoDto.setType(AnnotationType.STRING);
		annoDto.setValue("123");
		file1.getAnnotations().add(annoDto);
	
		// two
		ObjectDataDTO file2 = createObjectDataDTO(3L, EntityType.file, 12);
		file2.getAnnotations().clear();
		file2.setParentId(222L);
		// add a long annotation with a size of 6
		annoDto = new ObjectAnnotationDTO(file2);
		annoDto.setKey(duplicateName);
		annoDto.setType(AnnotationType.LONG);
		annoDto.setValue("123456");
		file2.getAnnotations().add(annoDto);

		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file1, file2));
		
		Set<Long> containerIds = Set.of(222L, 333L);
		long limit = 5;
		long offset = 0;
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, containerIds);
		
		List<ColumnModel> columns = tableIndexDAO.getPossibleColumnModelsForContainers(filter, limit, offset);
		assertNotNull(columns);
		assertEquals(2, columns.size());
		// expected
		ColumnModel one = new ColumnModel();
		one.setName(duplicateName);
		one.setColumnType(ColumnType.STRING);
		one.setMaximumSize(6L);
		ColumnModel two = new ColumnModel();
		two.setName(duplicateName);
		two.setColumnType(ColumnType.INTEGER);
		two.setMaximumSize(null);
		Set<ColumnModel> expected = new HashSet<>(2);
		expected.add(one);
		expected.add(two);
		
		assertEquals(expected, new HashSet<>(columns));
	}
	
	/**
	 * Test added for PLFM-5449.
	 * Add two annotations keys to an entity that only differ by case.
	 * 
	 */
	@Test
	public void testCaseSensitiveAnnotationNamesPLFM_5449() {
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(2L,3L));
		// one
		ObjectDataDTO file1 = createObjectDataDTO(2L, EntityType.file, 1);
		file1.getAnnotations().clear();
		file1.setParentId(333L);

		String key = "someKey";
		// lower
		ObjectAnnotationDTO lower = new ObjectAnnotationDTO(file1);
		lower.setKey(key.toLowerCase());
		lower.setType(AnnotationType.STRING);
		lower.setValue("123");
		file1.getAnnotations().add(lower);
		//upper
		ObjectAnnotationDTO upper = new ObjectAnnotationDTO(file1);
		upper.setKey(key.toUpperCase());
		upper.setType(AnnotationType.STRING);
		upper.setValue("123");
		file1.getAnnotations().add(upper);
		// call under test
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file1));
	}

	//PLFM-6013
	@Test
	public void testGetPossibleAnnotationsForContainers_ListColumns(){
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(2L,3L));


		String annoKey = "myAnnotation";

		// setup some hierarchy.
		ObjectDataDTO file1 = createObjectDataDTO(2L, EntityType.file, 0);
		file1.setParentId(333L);

		ObjectAnnotationDTO annotationDTO1 = new ObjectAnnotationDTO(file1);
		annotationDTO1.setKey(annoKey);
		annotationDTO1.setType(AnnotationType.STRING);
		annotationDTO1.setValue(Arrays.asList("123"));
		file1.setAnnotations(Collections.singletonList(annotationDTO1));

		ObjectDataDTO file2 = createObjectDataDTO(3L, EntityType.file, 0);
		file2.setParentId(222L);
		ObjectAnnotationDTO annotationDTO2 = new ObjectAnnotationDTO(file2);
		annotationDTO2.setKey(annoKey);
		annotationDTO2.setType(AnnotationType.STRING);
		annotationDTO2.setValue(Arrays.asList("12",  "123456", "1234"));
		file2.setAnnotations(Collections.singletonList(annotationDTO2));

		ObjectDataDTO file3 = createObjectDataDTO(4L, EntityType.file, 0);
		file3.setParentId(222L);
		ObjectAnnotationDTO annotationDTO3 = new ObjectAnnotationDTO(file3);
		annotationDTO3.setKey(annoKey);
		annotationDTO3.setType(AnnotationType.STRING);
		annotationDTO3.setValue(Arrays.asList("12345"));
		file3.setAnnotations(Collections.singletonList(annotationDTO3));

		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file1, file2));

		Set<Long> containerIds = Set.of(222L, 333L);
		long limit = 5;
		long offset = 0;
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, containerIds);
		
		List<ColumnModel> columns = tableIndexDAO.getPossibleColumnModelsForContainers(filter, limit, offset);
		assertNotNull(columns);
		assertEquals(1, columns.size());

		ColumnModel cm = columns.get(0);
		assertEquals(annoKey, cm.getName());
		assertEquals(ColumnType.STRING_LIST, cm.getColumnType());
		assertEquals(6L, cm.getMaximumSize());

	}
	
	@Test
	public void testExpandFromAggregation() {
		
		ColumnAggregation one = new ColumnAggregation();
		one.setColumnName("foo");
		one.setColumnTypeConcat(concatTypes(AnnotationType.STRING, AnnotationType.DOUBLE));
		one.setMaxStringElementSize(101L);
		one.setMaxListSize(1L);


		ColumnAggregation two = new ColumnAggregation();
		two.setColumnName("bar");
		two.setColumnTypeConcat(concatTypes(AnnotationType.DOUBLE, AnnotationType.LONG));
		two.setMaxStringElementSize(0L);
		two.setMaxListSize(1L);


		ColumnAggregation three = new ColumnAggregation();
		three.setColumnName("foobar");
		three.setColumnTypeConcat(concatTypes(AnnotationType.STRING));
		three.setMaxStringElementSize(202L);
		three.setMaxListSize(1L);

		ColumnAggregation four = new ColumnAggregation();
		four.setColumnName("barbaz");
		four.setColumnTypeConcat(concatTypes(AnnotationType.STRING));
		four.setMaxStringElementSize(111L);
		four.setMaxListSize(3L);

		// call under test
		List<ColumnModel> results = TableIndexDAOImpl.expandFromAggregation(Lists.newArrayList(one,two,three,four));
		assertEquals(6, results.size());
		// zero
		ColumnModel cm = results.get(0);
		assertEquals("foo", cm.getName());
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertNull(cm.getMaximumListLength());
		assertEquals(new Long(101), cm.getMaximumSize());
		// one
		cm = results.get(1);
		assertEquals("foo", cm.getName());
		assertEquals(ColumnType.DOUBLE, cm.getColumnType());
		assertNull(cm.getMaximumListLength());
		assertEquals(null, cm.getMaximumSize());
		// two
		cm = results.get(2);
		assertEquals("bar", cm.getName());
		assertEquals(ColumnType.DOUBLE, cm.getColumnType());
		assertNull(cm.getMaximumListLength());
		assertEquals(null, cm.getMaximumSize());
		// three
		cm = results.get(3);
		assertEquals("bar", cm.getName());
		assertEquals(ColumnType.INTEGER, cm.getColumnType());
		assertNull(cm.getMaximumListLength());
		assertEquals(null, cm.getMaximumSize());
		// four
		cm = results.get(4);
		assertEquals("foobar", cm.getName());
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertNull(cm.getMaximumListLength());
		assertEquals(new Long(202), cm.getMaximumSize());
		// five
		cm = results.get(5);
		assertEquals("barbaz", cm.getName());
		assertEquals(ColumnType.STRING_LIST, cm.getColumnType());
		assertEquals(3L, cm.getMaximumListLength());
		assertEquals(new Long(111), cm.getMaximumSize());
	}
	
	/**
	 * Helper to create a concatenated list of column types delimited with dot ('.')
	 * @param types
	 * @return
	 */
	public static String concatTypes(AnnotationType...types) {
		StringJoiner joiner = new StringJoiner(",");
		for(AnnotationType type: types) {
			joiner.add(type.name());
		}
		return joiner.toString();
	}
	
	@Test
	public void testGetPossibleAnnotationsForContainersProject(){
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(2L,3L));
		
		// setup some hierarchy.
		ObjectDataDTO project1 = createObjectDataDTO(2L, EntityType.project, 15);
		project1.setParentId(111L);
		ObjectDataDTO project2 = createObjectDataDTO(3L, EntityType.project, 12);
		project2.setParentId(111L);
		
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(project1, project2));
		
		Set<IdAndVersion> containerIds = Set.of(IdAndVersion.parse("2"), IdAndVersion.parse("3"));
		long limit = 5;
		long offset = 0;
		
		subTypes = Set.of(SubType.project);
		
		ViewFilter filter = new IdAndVersionFilter(mainType, subTypes, containerIds);
	
		List<ColumnModel> columns = tableIndexDAO.getPossibleColumnModelsForContainers(filter, limit, offset);
		assertNotNull(columns);
		assertEquals(limit, columns.size());
		// one
		ColumnModel cm = columns.get(0);
		assertEquals("key0", cm.getName());
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(new Long(1L), cm.getMaximumSize());
		// two
		cm = columns.get(1);
		assertEquals("key1", cm.getName());
		assertEquals(ColumnType.INTEGER, cm.getColumnType());
		assertEquals(null, cm.getMaximumSize());
		// three
		cm = columns.get(2);
		assertEquals("key10", cm.getName());
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(2, cm.getMaximumSize());
	}
	
	// relevant to PLFM-6344
	@Test
	public void testGetPossibleAnnotationsForContainersWithEmptyStringAnnotation(){
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(2L,3L));
		
		// setup file1 with no annotations
		ObjectDataDTO file1 = createObjectDataDTO(2L, EntityType.file, 0);
		file1.setParentId(333L);
		String testKey1 = "testKey1";
		// add empty string annotation to file1
		ObjectAnnotationDTO annotationDTO1 = new ObjectAnnotationDTO(file1);
		annotationDTO1.setKey(testKey1);
		annotationDTO1.setType(AnnotationType.STRING);
		annotationDTO1.setValue(Arrays.asList(""));
		file1.setAnnotations(Collections.singletonList(annotationDTO1));
		
		// setup file2 with no annotations
		ObjectDataDTO file2 = createObjectDataDTO(3L, EntityType.file, 0);
		file2.setParentId(222L);
		String testKey2 = "testKey2";
		// add empty string list annotation to file2
		ObjectAnnotationDTO annotationDTO2 = new ObjectAnnotationDTO(file2);
		annotationDTO2.setKey(testKey2);
		annotationDTO2.setType(AnnotationType.STRING);
		annotationDTO2.setValue(Arrays.asList("", ""));
		file2.setAnnotations(Collections.singletonList(annotationDTO2));
		
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file1, file2));
		
		Set<Long> containerIds = Set.of(222L, 333L);
		long limit = 5;
		long offset = 0;
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, containerIds);
		
		// call under test
		List<ColumnModel> columns = tableIndexDAO.getPossibleColumnModelsForContainers(filter, limit, offset);
		assertNotNull(columns);
		assertEquals(2, columns.size());
		
		// both column models should have ColumnConstants.DEFAULT_STRING_SIZE as 
		// max size due to empty string(s)
		ColumnModel cm = columns.get(0);
		assertEquals(testKey1, cm.getName());
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(ColumnConstants.DEFAULT_STRING_SIZE, cm.getMaximumSize());
		
		cm = columns.get(1);
		assertEquals(testKey2, cm.getName());
		assertEquals(ColumnType.STRING_LIST, cm.getColumnType());
		assertEquals(ColumnConstants.DEFAULT_STRING_SIZE, cm.getMaximumSize());
	}
	
	@Test
	public void testGetSumOfChildCRCsForEachParent(){
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(2L,3L));
		
		Long parentOneId = 333L;
		Long parentTwoId = 222L;
		Long parentThreeId = 444L;
		// setup some hierarchy.
		ObjectDataDTO file1 = createObjectDataDTO(2L, EntityType.file, 2);
		file1.setParentId(parentOneId);
		ObjectDataDTO file2 = createObjectDataDTO(3L, EntityType.file, 3);
		file2.setParentId(parentTwoId);
		
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file1, file2));
		
		List<Long> parentIds = Lists.newArrayList(parentOneId,parentTwoId,parentThreeId);
		// call under test
		Map<Long, Long> results = tableIndexDAO.getSumOfChildCRCsForEachParent(mainType, parentIds);
		assertNotNull(results);
		assertEquals(2, results.size());
		assertNotNull(results.get(parentOneId));
		assertNotNull(results.get(parentTwoId));
		assertEquals(null, results.get(parentThreeId));
	}
	
	@Test
	public void testGetSumOfChildCRCsForEachParentEmpty(){		
		List<Long> parentIds = new LinkedList<Long>();
		// call under test
		Map<Long, Long> results = tableIndexDAO.getSumOfChildCRCsForEachParent(mainType, parentIds);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
	
	@Test
	public void testGetEntityChildren(){
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(2L,3L));
		
		Long parentOneId = 333L;
		Long parentTwoId = 222L;
		Long parentThreeId = 444L;
		// setup some hierarchy.
		ObjectDataDTO file1 = createObjectDataDTO(2L, EntityType.file, 2);
		file1.setParentId(parentOneId);
		ObjectDataDTO file2 = createObjectDataDTO(3L, EntityType.file, 3);
		file2.setParentId(parentTwoId);
		
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file1, file2));
		
		List<IdAndEtag> results = tableIndexDAO.getObjectChildren(mainType, parentOneId);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(new IdAndEtag(file1.getId(), file1.getEtag(), 2L), results.get(0));
		
		results = tableIndexDAO.getObjectChildren(mainType, parentTwoId);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(new IdAndEtag(file2.getId(), file2.getEtag(), 2L), results.get(0));
		
		results = tableIndexDAO.getObjectChildren(mainType, parentThreeId);
		assertNotNull(results);
		assertEquals(0, results.size());
	}
	
	@Test
	public void testGetSumOfFileSizes(){
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(2L,3L));
		
		Long parentOneId = 333L;
		Long parentTwoId = 222L;
		// setup some hierarchy.
		ObjectDataDTO file1 = createObjectDataDTO(2L, EntityType.file, 2);
		file1.setParentId(parentOneId);
		ObjectDataDTO file2 = createObjectDataDTO(3L, EntityType.file, 3);
		file2.setParentId(parentTwoId);
		
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file1, file2));
		// call under test
		long fileSizes = tableIndexDAO.getSumOfFileSizes(mainType, Arrays.asList(
				IdAndVersion.parse(file1.getIdVersion()),
				IdAndVersion.parse(file2.getIdVersion())
		));
		
		assertEquals(file1.getFileSizeBytes()+ file2.getFileSizeBytes(), fileSizes);
	}
	
	/**
	 * Test added for PLFM-5176
	 */
	@Test
	public void testGetSumOfFileSizesNoFiles(){
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(2L,3L));
		
		Long parentOneId = 333L;
		// setup some hierarchy.
		ObjectDataDTO folder = createObjectDataDTO(2L, EntityType.folder, 2);
		folder.setParentId(parentOneId);
		
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(folder));
		// call under test
		long fileSizes = tableIndexDAO.getSumOfFileSizes(mainType, Arrays.asList(IdAndVersion.parse(folder.getIdVersion())));
		assertEquals(0L, fileSizes);
	}
	
	@Test
	public void testGetSumOfFileSizesNoVersion(){
		List<IdAndVersion> list = Arrays.asList(IdAndVersion.parse("123.1"), IdAndVersion.parse("456"));
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// call under test
			tableIndexDAO.getSumOfFileSizes(mainType, list);
		}).getMessage();
		
		assertEquals("The object with id syn456 must specify a version.", errorMessage);
	}
	
	@Test
	public void testGetSumOfFileSizesEmpty(){
		List<IdAndVersion> list = Collections.emptyList();
		// call under test
		long fileSizes = tableIndexDAO.getSumOfFileSizes(mainType, list);
		assertEquals(0, fileSizes);
	}
	
	@Test
	public void testGetSumOfFileSizesNull(){
		List<IdAndVersion> list = null;
		
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			 tableIndexDAO.getSumOfFileSizes(mainType, list);
		});
	}
	
	@Test
	public void testReplicationExpiration() throws InterruptedException{
		Long one = 111L;

		// lock does not exist so it should be the same as expired.
		assertTrue(tableIndexDAO.isSynchronizationLockExpiredForObject(mainType, one));
		
		long now = System.currentTimeMillis();

		// lock exists but expired in the past.
		tableIndexDAO.setSynchronizationLockExpiredForObject(mainType, one, now-10);
		assertTrue(tableIndexDAO.isSynchronizationLockExpiredForObject(mainType, one));
		
		// setup lock to expire in the future.
		long timeout = 4 * 1000;
		long expires = now + timeout;
		tableIndexDAO.setSynchronizationLockExpiredForObject(mainType, one, expires);
		assertFalse(tableIndexDAO.isSynchronizationLockExpiredForObject(mainType, one));
		// wait for the lock to expire.
		Thread.sleep(timeout+1);
		assertTrue(tableIndexDAO.isSynchronizationLockExpiredForObject(mainType, one));
	}
	
	@Test
	public void testArithmeticSelect() throws ParseException {
		// Create the table
		List<ColumnModel> doubleColumn = Lists.newArrayList(TableModelTestUtils
				.createColumn(1L, "col1", ColumnType.DOUBLE));
		createOrUpdateTable(doubleColumn, indexDescription);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(doubleColumn, 1);
		// insert special values
		rows.get(0).getValues().set(0, "50");
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(doubleColumn);
		set.setHeaders(headers);
		set.setTableId(tableId.toString());
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(tableId, set, doubleColumn);
		// This is our query
		QueryTranslator query = QueryTranslator.builder("select 2 + 2, col1/10 from " + tableId, schemaProvider(doubleColumn), userId).indexDescription(new TableIndexDescription(tableId)).build();
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		System.out.println(results);
		assertNotNull(results.getRows());
		assertEquals(tableId.toString(), results.getTableId());
		assertEquals(1, results.getRows().size());
		assertEquals("4", results.getRows().get(0).getValues().get(0));
		assertEquals("5.0", results.getRows().get(0).getValues().get(1));
	}
	
	@Test
	public void testArithmeticPredicateRightHandSide() throws ParseException {
		// Create the table
		List<ColumnModel> doubleColumn = Lists.newArrayList(TableModelTestUtils
				.createColumn(1L, "col1", ColumnType.DOUBLE));
		createOrUpdateTable(doubleColumn, indexDescription);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(doubleColumn, 1);
		// insert special values
		rows.get(0).getValues().set(0, "-50");
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(doubleColumn);
		set.setHeaders(headers);
		set.setTableId(tableId.toString());
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(tableId, set, doubleColumn);
		// This is our query
		QueryTranslator query = QueryTranslator.builder("select col1 from " + tableId+" where col1 = -5*10", schemaProvider(doubleColumn), userId).indexDescription(new TableIndexDescription(tableId)).build();
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		System.out.println(results);
		assertNotNull(results.getRows());
		assertEquals(tableId.toString(), results.getTableId());
		assertEquals(1, results.getRows().size());
		assertEquals("-50", results.getRows().get(0).getValues().get(0));
	}
	
	/**
	 * Test for PLFM-4575.
	 * @throws ParseException 
	 */
	@Test
	public void testDateTimeFunctions() throws ParseException{
		List<ColumnModel> schema = Lists.newArrayList(TableModelTestUtils
				.createColumn(1L, "aDate", ColumnType.DATE));
		createOrUpdateTable(schema, indexDescription);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 2);
		// The first row is in the past
		long now = System.currentTimeMillis();
		long thritySecondsPast = now - (1000*30);
		long thritySecondsFuture = now + (1000*30);
		// first row is in the past
		rows.get(0).getValues().set(0, ""+thritySecondsPast);
		// second row is in the future
		rows.get(1).getValues().set(0, ""+thritySecondsFuture);
		// apply the rows
		createOrUpdateOrDeleteRows(tableId, rows, schema);
		
		long timeFilter = System.currentTimeMillis() - 1000;
		
		// This is our query
		QueryTranslator query = QueryTranslator.builder("select aDate from " + tableId+" where aDate > " + timeFilter, schemaProvider(schema), userId).indexDescription(new TableIndexDescription(tableId)).build();
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		System.out.println(results);
		assertNotNull(results.getRows());
		assertEquals(tableId.toString(), results.getTableId());
		assertEquals(1, results.getRows().size());
		assertEquals(""+thritySecondsFuture, results.getRows().get(0).getValues().get(0));
	}
	
	/**
	 * PLFM-4028 is an error that occurs when any type of column is changed 
	 * to a type of large text.
	 */
	@Test
	public void testPLFM_4028WithIndex(){
		ColumnModel oldColumn = TableModelTestUtils.createColumn(1L, "foo", ColumnType.INTEGER);
		List<ColumnModel> schema = Lists.newArrayList(oldColumn);
		createOrUpdateTable(schema, indexDescription);
		int maxNumberOfIndices = 2;
		optimizeTableIndices(tableId, maxNumberOfIndices);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 2);
		// apply the rows
		createOrUpdateOrDeleteRows(tableId, rows, schema);
		
		// the new schema has a large text column with the same name
		ColumnModel newColumn = TableModelTestUtils.createColumn(1L, "foo", ColumnType.LARGETEXT);
		
		List<ColumnChangeDetails> changes = Lists.newArrayList(new ColumnChangeDetails(oldColumn, newColumn));
		boolean alterTemp = false;
		alterTableAsNeeded(tableId, changes, alterTemp);
	}
	
	@Test
	public void testPLFM_4028WithoutIndex(){
		ColumnModel oldColumn = TableModelTestUtils.createColumn(1L, "foo", ColumnType.INTEGER);
		List<ColumnModel> schema = Lists.newArrayList(oldColumn);
		createOrUpdateTable(schema, indexDescription);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 2);
		// apply the rows
		createOrUpdateOrDeleteRows(tableId, rows, schema);

		// the new schema has a large text column with the same name
		ColumnModel newColumn = TableModelTestUtils.createColumn(1L, "foo", ColumnType.LARGETEXT);

		List<ColumnChangeDetails> changes = Lists.newArrayList(new ColumnChangeDetails(oldColumn, newColumn));
		boolean alterTemp = false;
		alterTableAsNeeded(tableId, changes, alterTemp);
	}
	
	@Test
	public void generateProjectStatistics() {
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(1L,2L,3L, 4L, 5L, 6L));

		// Set up some data to test
		ObjectDataDTO project1 = createObjectDataDTO(1L, EntityType.project, 0);
		ObjectDataDTO project2 = createObjectDataDTO(2L, EntityType.project, 0);

		project1.setName("Project Name One");
		project2.setName("Project Name Two");

		ObjectDataDTO file1 = createObjectDataDTO(3L, EntityType.file, 0);
		ObjectDataDTO file2 = createObjectDataDTO(4L, EntityType.file, 0);
		ObjectDataDTO file3 = createObjectDataDTO(5L, EntityType.file, 0);
		ObjectDataDTO file4 = createObjectDataDTO(6L, EntityType.file, 0);

		file1.setIsInSynapseStorage(true);
		file2.setIsInSynapseStorage(true);
		file3.setIsInSynapseStorage(true);
		file4.setIsInSynapseStorage(false); // !!

		final Long file1Size = 120L;
		final Long file2Size = 280L;
		final Long file3Size = 492824L;
		final Long file4Size = 100L;
		file1.setFileSizeBytes(file1Size);
		file2.setFileSizeBytes(file2Size);
		file3.setFileSizeBytes(file3Size);
		file4.setFileSizeBytes(file4Size);

		file1.setProjectId(1L);
		file2.setProjectId(1L);
		file3.setProjectId(2L);
		file4.setProjectId(2L);

		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(project1, project2, file1, file2, file3, file4));

		List<SynapseStorageProjectStats> result = new ArrayList<>();

		Callback<SynapseStorageProjectStats> callback = result::add;
		// Call under test
		tableIndexDAO.streamSynapseStorageStats(mainType, callback);

		assertEquals(2, result.size()); // 2 projects
		// Note project 2 is bigger so it will be first
		assertEquals(project2.getId().toString(), result.get(0).getId());
		assertEquals(project2.getName(), result.get(0).getProjectName());
		assertEquals(file3Size, result.get(0).getSizeInBytes()); // Note file4 is not in Synapse storage

		assertEquals(project1.getId().toString(), result.get(1).getId());
		assertEquals(project1.getName(), result.get(1).getProjectName());
		assertEquals((Long) (file1Size + file2Size), result.get(1).getSizeInBytes());
	}
	
	@Test
	public void testPLFM_5445() throws UnsupportedEncodingException, DecoderException {
		List<ColumnModel> schema = Lists.newArrayList(TableModelTestUtils
				.createColumn(1L, "aString", ColumnType.STRING));
		createOrUpdateTable(schema, indexDescription);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(schema, 1);
		// This is the value from the issue.
		String value = new String(Hex.decodeHex("F09D9C85".toCharArray()), "UTF-8");
		// first row is in the past
		rows.get(0).getValues().set(0, value);

		// apply the rows
		createOrUpdateOrDeleteRows(tableId, rows, schema);
	}

	/**
	 * Create update or delete the given rows in the current table.
	 * @param rows
	 * @param schema
	 */
	public void createOrUpdateOrDeleteRows(IdAndVersion tableId, List<Row> rows, List<ColumnModel> schema){
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(schema);
		set.setHeaders(headers);
		set.setTableId(tableId.toString());
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(tableId, set, schema);
	}
	
	/**
	 * Create a view schema using an ObjectDataDTO as a template.
	 * @param dto
	 * 
	 * @return
	 */
	private List<ColumnModel> createSchemaFromObjectDataDTO(ObjectDataDTO dto){
		List<ColumnModel> schema = new LinkedList<>();
		// add a column for each annotation
		if(dto.getAnnotations() != null){
			for(ObjectAnnotationDTO annoDto: dto.getAnnotations()){
				ColumnModel cm = new ColumnModel();
				//double lists are not supported at the moment
				cm.setColumnType(annoDto.getValue().size() > 1 ? ColumnTypeListMappings.listType(annoDto.getType().getColumnType()) : annoDto.getType().getColumnType());
				cm.setName(annoDto.getKey());
				if(cm.getColumnType() == ColumnType.STRING || cm.getColumnType() == ColumnType.STRING_LIST){
					cm.setMaximumSize(50L);
				}
				if(annoDto.getValue().size() > 1){
					cm.setMaximumListLength(21L);
				}
				schema.add(cm);
			}
		}
		// Add all of the default ObjectFields
		schema.addAll(objectFieldModelResolverFactory.getObjectFieldModelResolver(fieldTypeMapper).getAllColumnModels());
		// assign each column an ID
		for(int i=0; i<schema.size(); i++){
			ColumnModel cm = schema.get(i);
			cm.setId(""+i);
		}
		return schema;
	}
	
	/**
	 * Helper to create populated ObjectDataDTO.
	 * @param id
	 * @param type
	 * @param annotationCount
	 * @return
	 */
	private static ObjectDataDTO createObjectDataDTO(long id, EntityType type, int annotationCount){
		ObjectDataDTO objectDataDTO = new ObjectDataDTO();
		objectDataDTO.setId(id);
		objectDataDTO.setVersion(2L);
		objectDataDTO.setCurrentVersion(objectDataDTO.getVersion());
		objectDataDTO.setCreatedBy(222L);
		objectDataDTO.setCreatedOn(new Date());
		objectDataDTO.setEtag("etag"+id);
		objectDataDTO.setName("name"+id);
		objectDataDTO.setDescription("description" + id);
		objectDataDTO.setSubType(SubType.valueOf(type.name()));
		objectDataDTO.setParentId(1L);
		objectDataDTO.setBenefactorId(2L);
		objectDataDTO.setProjectId(3L);
		objectDataDTO.setModifiedBy(333L);
		objectDataDTO.setModifiedOn(new Date());
		if(EntityType.file.equals(type)){
			objectDataDTO.setFileHandleId(888L);
			objectDataDTO.setFileSizeBytes(999L);
			objectDataDTO.setFileMD5(Long.toHexString(id*1000));
			objectDataDTO.setFileBucket("bucket");
			objectDataDTO.setFileKey("key");
			objectDataDTO.setFileConcreteType(S3FileHandle.class.getName());
		}
		if(EntityType.dataset.equals(type)){
			objectDataDTO.setFileSizeBytes(999L);
			objectDataDTO.setFileMD5(Long.toHexString(id*1000));
			objectDataDTO.setItemCount(1L);
		}
		List<ObjectAnnotationDTO> annos = new LinkedList<ObjectAnnotationDTO>();
		for(int i=0; i<annotationCount; i++){
			ObjectAnnotationDTO annoDto = new ObjectAnnotationDTO(objectDataDTO);
			annoDto.setKey("key"+i);
			annoDto.setType(AnnotationType.values()[i%AnnotationType.values().length]);
			annoDto.setValue(""+i);
			annos.add(annoDto);
		}
		if(!annos.isEmpty()){
			objectDataDTO.setAnnotations(annos);
		}
		return objectDataDTO;
	}
	
	/**
	 * Helper to create multiple version of a single object. Each version will have different annotation values.
	 * @param id
	 * @param type
	 * @param annotationCount The number of annotations that each version will have.
	 * @param numberOfVersion The total number of version to create.
	 * @return Results will include one ObjectDataDTO for each version.
	 */
	public List<ObjectDataDTO> createMultipleVersions(long id, EntityType type, int annotationCount, int numberOfVersion){
		List<ObjectDataDTO> list = new ArrayList<>(numberOfVersion);
		for(long version=1; version<numberOfVersion+1; version++) {
			ObjectDataDTO dto = createObjectDataDTO(id, type, annotationCount);
			dto.setVersion(version);
			dto.setCurrentVersion((long)numberOfVersion);
			for(int annoIndex = 0; annoIndex<annotationCount; annoIndex++) {
				ObjectAnnotationDTO anno = dto.getAnnotations().get(annoIndex);
				anno.setObjectVersion(version);
				anno.setValue(""+annoIndex+"-"+version);
			}
			list.add(dto);
		}
		return list;
	}

	@Test
	public void testCreateAndPopulateListColumnIndexTables(){
		// create a table with a long column.
		ColumnModel intColumn = new ColumnModel();
		intColumn.setId("12");
		intColumn.setName("foo");
		intColumn.setColumnType(ColumnType.INTEGER);

		ColumnModel stringListColumn = new ColumnModel();
		stringListColumn.setId("15");
		stringListColumn.setName("myList");
		stringListColumn.setMaximumSize(54L);
		stringListColumn.setColumnType(ColumnType.STRING_LIST);
		stringListColumn.setMaximumListLength(25L);


		ColumnModel booleanColumn = new ColumnModel();
		booleanColumn.setId("13");
		booleanColumn.setName("bar");
		booleanColumn.setColumnType(ColumnType.BOOLEAN);

		List<ColumnModel> schema = Lists.newArrayList(intColumn, stringListColumn ,booleanColumn);

		createOrUpdateTable(schema, indexDescription);

		int numRows = 5;
		List<Row> rows = TableModelTestUtils.createRows(schema, numRows);
		createOrUpdateOrDeleteRows(tableId, rows, schema);

		Set<Long> rowsIds = null;
		tableIndexDAO.populateListColumnIndexTable(tableId, stringListColumn, rowsIds, false);

		//each list value created by createRows has 2 items in the list
		assertEquals(numRows * 2, countRowsInMultiValueIndex(tableId, stringListColumn.getId()));
	}

	//See PLFM-5999
	@Test
	public void testCreateAndPopulateListColumnIndexTables__StringListDataTooLarge(){

		ColumnModel stringListColumn = new ColumnModel();
		stringListColumn.setId("15");
		stringListColumn.setName("myList");
		// initial size needs to be large to allow initial values to be inserted into table
		// in a view, values are replicated from
		stringListColumn.setMaximumSize(54L);
		stringListColumn.setColumnType(ColumnType.STRING_LIST);
		stringListColumn.setMaximumListLength(25L);


		List<ColumnModel> schema = Lists.newArrayList(stringListColumn);

		createOrUpdateTable(schema, indexDescription);

		int numRows = 5;
		List<Row> rows = TableModelTestUtils.createRows(schema, numRows);
		createOrUpdateOrDeleteRows(tableId, rows, schema);

		// make size a very small value to replicate annotation values
		// larger than the defined max size being inserted copied into a view
		stringListColumn.setMaximumSize(1L);
		createOrUpdateTable(schema, indexDescription);

		Set<Long> rowsIds = null;
		String message = assertThrows(IllegalArgumentException.class, ()-> {
			//method under test
			tableIndexDAO.populateListColumnIndexTable(tableId, stringListColumn, rowsIds, false);
		}).getMessage();

		assertEquals("The size of the column 'myList' is too small." +
				" Unable to automatically determine the necessary size to fit all values in a STRING_LIST column", message);
	}

	@Test
	public void testDeleteFromListColumnIndexTable_NullTableId() {
		tableId = null;
		ColumnModel multiValue = new ColumnModel();
		multiValue.setId("886");
		multiValue.setColumnType(ColumnType.STRING_LIST);
		multiValue.setName("multiValue");
		multiValue.setMaximumSize(100L);
		Set<Long> rowIdFilter = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			tableIndexDAO.populateListColumnIndexTable(tableId, multiValue, rowIdFilter, false);
		}).getMessage();
		assertEquals("tableId is required.", message);

	}

	@Test
	public void testDeleteFromListColumnIndexTable_NullColumn() {
		ColumnModel multiValue = null;
		Set<Long> rowIdFilter = Set.of(1L);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			tableIndexDAO.populateListColumnIndexTable(tableId, multiValue, rowIdFilter, false);
		}).getMessage();
		assertEquals("listColumn is required.", message);
	}

	@Test
	public void testDeleteFromListColumnIndexTable_NotListType() {
		ColumnModel multiValue = new ColumnModel();
		multiValue.setId("886");
		multiValue.setColumnType(ColumnType.STRING);
		multiValue.setName("multiValue");
		multiValue.setMaximumSize(100L);
		Set<Long> rowIdFilter = Set.of(1L);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			tableIndexDAO.deleteFromListColumnIndexTable(tableId, multiValue, rowIdFilter);
		}).getMessage();
		assertEquals("Only valid for List type columns", message);
	}

	@Test
	public void testDeleteFromListColumnIndexTable_nullRowFilter() {
		ColumnModel multiValue = new ColumnModel();
		multiValue.setId("886");
		multiValue.setColumnType(ColumnType.STRING_LIST);
		multiValue.setName("multiValue");
		multiValue.setMaximumSize(100L);
		Set<Long> rowIdFilter = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			tableIndexDAO.deleteFromListColumnIndexTable(tableId, multiValue, rowIdFilter);
		}).getMessage();
		assertEquals("rowIds is required and must not be empty.", message);
	}

	@Test
	public void testDeleteFromListColumnIndexTable_EmptyRowFilter() {
		ColumnModel multiValue = new ColumnModel();
		multiValue.setId("886");
		multiValue.setColumnType(ColumnType.STRING_LIST);
		multiValue.setName("multiValue");
		multiValue.setMaximumSize(100L);
		Set<Long> rowIdFilter = Collections.emptySet();
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			tableIndexDAO.deleteFromListColumnIndexTable(tableId, multiValue, rowIdFilter);
		}).getMessage();
		assertEquals("rowIds is required and must not be empty.", message);
	}


	@Test
	public void testDeleteFromListColumnIndexTable() {
		// create a table with a long column.
		ColumnModel intColumn = new ColumnModel();
		intColumn.setId("12");
		intColumn.setName("foo");
		intColumn.setColumnType(ColumnType.INTEGER);

		ColumnModel stringListColumn = new ColumnModel();
		stringListColumn.setId("15");
		stringListColumn.setName("myList");
		stringListColumn.setMaximumSize(54L);
		stringListColumn.setColumnType(ColumnType.STRING_LIST);
		stringListColumn.setMaximumListLength(25L);


		ColumnModel booleanColumn = new ColumnModel();
		booleanColumn.setId("13");
		booleanColumn.setName("bar");
		booleanColumn.setColumnType(ColumnType.BOOLEAN);

		List<ColumnModel> schema = Lists.newArrayList(intColumn, stringListColumn ,booleanColumn);

		createOrUpdateTable(schema, indexDescription);

		int numRows = 5;
		List<Row> rows = TableModelTestUtils.createRows(schema, numRows);
		createOrUpdateOrDeleteRows(tableId, rows, schema);

		tableIndexDAO.populateListColumnIndexTable(tableId, stringListColumn, null, false);

		assertEquals(numRows * 2, countRowsInMultiValueIndex(tableId, stringListColumn.getId()));

		//method under test
		//delete 2 rows from index table
		tableIndexDAO.deleteFromListColumnIndexTable(tableId, stringListColumn, Set.of(rows.get(0).getRowId(), rows.get(2).getRowId()));

		//deleted 2 rows so expect 2*2=4 less rows in the index table
		assertEquals((numRows-2) * 2, countRowsInMultiValueIndex(tableId, stringListColumn.getId()));
	}
	
	/**
	 * Test for the case where an entity is in the replication but not the view.
	 */
	@Test
	public void testGetOutOfDateRowsForView_ReplicationRowsMissingFromView(){
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		int rowCount = 2;
		List<ObjectDataDTO> dtos = createFileEntityObjectDataDTOs(rowCount);
		
		// Make additional object with a different type but same ids
		createObjectDTOs(otherObjectType, EntityType.file, rowCount, false);
		
		Set<Long> scope = dtos.stream().map(ObjectDataDTO::getParentId).collect(Collectors.toSet());
		// first row to define the schema
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(dtos.get(0));
		// Create the empty view
		createOrUpdateTable(schema, indexDescription);
		long limit = 100L;
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);

		// call under test
		Set<Long> results = tableIndexDAO.getOutOfDateRowsForView(tableId, filter, limit);
		
		assertNotNull(results);
		Set<Long> expected = dtos.stream().map(ObjectDataDTO::getId).collect(Collectors.toSet());
		assertEquals(expected, results);
	}
	
	/**
	 * Test for the cases where and entity has been deleted from replication but remains in the view.
	 */
	@Test
	public void testGetOutOfDateRowsForView_DeletedRowsStillInView(){
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		int rowCount = 2;
		List<ObjectDataDTO> dtos = createFileEntityObjectDataDTOs(rowCount);
		
		// Make additional object with a different type but same ids
		createObjectDTOs(otherObjectType, EntityType.file, rowCount, false);
		
		Set<Long> scope = dtos.stream().map(ObjectDataDTO::getParentId).collect(Collectors.toSet());
		// first row to define the schema
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(dtos.get(0));
		// Create the empty view
		createOrUpdateTable(schema, indexDescription);
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);
		
		// add all of the rows to the view.
		tableIndexDAO.copyObjectReplicationToView(tableId.getId(), filter, schema, fieldTypeMapper);
		
		//delete the first row from replication
		List<Long> toDelete = Lists.newArrayList(dtos.get(0).getId());
		tableIndexDAO.deleteObjectData(mainType, toDelete);
		
		long limit = 100L;
		// call under test
		Set<Long> results = tableIndexDAO.getOutOfDateRowsForView(tableId, filter, limit);
		assertNotNull(results);
		Set<Long> expected = new HashSet<Long>(toDelete);
		assertEquals(expected, results);
	}
	
	/**
	 * Test for the cases where and entity has moved out-of-scope but remains in the view.
	 */
	@Test
	public void testGetOutOfDateRowsForView_MovedOutOfScope(){
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		int rowCount = 2;
		List<ObjectDataDTO> dtos = createFileEntityObjectDataDTOs(rowCount);
		
		// Make additional object with a different type but same ids
		createObjectDTOs(otherObjectType, EntityType.file, rowCount, false);
		
		Set<Long> scope = dtos.stream().map(ObjectDataDTO::getParentId).collect(Collectors.toSet());
		// first row to define the schema
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(dtos.get(0));
		// Create the empty view
		createOrUpdateTable(schema, indexDescription);
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);
		
		// add all of the rows to the view.
		tableIndexDAO.copyObjectReplicationToView(tableId.getId(), filter, schema, fieldTypeMapper);
		
		// move the entity out of scope
		ObjectDataDTO first = dtos.get(0);
		first.setParentId(9999L);
		List<Long> toUpdate = Lists.newArrayList(first.getId());
		tableIndexDAO.deleteObjectData(mainType, toUpdate);
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(first));
		
		long limit = 100L;
		// call under test
		Set<Long> results = tableIndexDAO.getOutOfDateRowsForView(tableId, filter, limit);
		assertNotNull(results);
		Set<Long> expected = new HashSet<Long>(toUpdate);
		assertEquals(expected, results);
	}
	
	/**
	 * Test for the cases where an entity etag differs between replication and the view.
	 */
	@Test
	public void testGetOutOfDateRowsForView_EtagDoesNotMatch(){
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		int rowCount = 2;
		List<ObjectDataDTO> dtos = createFileEntityObjectDataDTOs(rowCount);
		
		// Make additional object with a different type but same ids
		createObjectDTOs(otherObjectType, EntityType.file, rowCount, false);
		
		Set<Long> scope = dtos.stream().map(ObjectDataDTO::getParentId).collect(Collectors.toSet());
		// first row to define the schema
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(dtos.get(0));
		// Create the empty view
		createOrUpdateTable(schema, indexDescription);
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);

		// add all of the rows to the view.
		tableIndexDAO.copyObjectReplicationToView(tableId.getId(), filter, schema, fieldTypeMapper);
		
		// change the etag of the first entity
		ObjectDataDTO first = dtos.get(0);
		first.setEtag(first.getEtag()+"updated");
		List<Long> toUpdate = Lists.newArrayList(first.getId());
		tableIndexDAO.deleteObjectData(mainType, toUpdate);
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(first));
		
		long limit = 100L;
		// call under test
		Set<Long> results = tableIndexDAO.getOutOfDateRowsForView(tableId, filter, limit);
		assertNotNull(results);
		Set<Long> expected = new HashSet<Long>(toUpdate);
		assertEquals(expected, results);
	}
	
	/**
	 * Test for the cases where an entity benefactor differs between replication and the view.
	 */
	@Test
	public void testGetOutOfDateRowsForView_BenefactorDoesNotMatch(){
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		int rowCount = 2;
		List<ObjectDataDTO> dtos = createFileEntityObjectDataDTOs(rowCount);
		
		// Make additional object with a different type but same ids
		createObjectDTOs(otherObjectType, EntityType.file, rowCount, false);
		
		Set<Long> scope = dtos.stream().map(ObjectDataDTO::getParentId).collect(Collectors.toSet());
		// first row to define the schema
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(dtos.get(0));
		// Create the empty view
		createOrUpdateTable(schema, indexDescription);
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);

		// add all of the rows to the view.
		tableIndexDAO.copyObjectReplicationToView(tableId.getId(), filter, schema, fieldTypeMapper);
		
		// change the benefactor of the first entity
		ObjectDataDTO first = dtos.get(0);
		first.setBenefactorId(first.getBenefactorId()*100);
		List<Long> toUpdate = Lists.newArrayList(first.getId());
		tableIndexDAO.deleteObjectData(mainType, toUpdate);
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(first));
		
		long limit = 100L;
		// call under test
		Set<Long> results = tableIndexDAO.getOutOfDateRowsForView(tableId, filter, limit);
		assertNotNull(results);
		Set<Long> expected = new HashSet<Long>(toUpdate);
		assertEquals(expected, results);
	}
	
	/**
	 * Test for the case where the view is up-to-date with replication.
	 */
	@Test
	public void testGetOutOfDateRowsForView_ViewUpToDate(){
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		int rowCount = 2;
		List<ObjectDataDTO> dtos = createFileEntityObjectDataDTOs(rowCount);
		
		// Make additional object with a different type but same ids
		createObjectDTOs(otherObjectType, EntityType.file, rowCount, false);
		
		Set<Long> scope = dtos.stream().map(ObjectDataDTO::getParentId).collect(Collectors.toSet());
		// first row to define the schema
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(dtos.get(0));
		// Create the empty view
		createOrUpdateTable(schema, indexDescription);

		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);
		
		// add all of the rows to the view.
		tableIndexDAO.copyObjectReplicationToView(tableId.getId(), filter, schema, fieldTypeMapper);
			
		long limit = 100L;
		// call under test
		Set<Long> results = tableIndexDAO.getOutOfDateRowsForView(tableId, filter, limit);
		assertEquals(Collections.emptySet(), results);
	}
	
	/**
	 * Test the limit for getOutOfDateRowsForView()
	 */
	@Test
	public void testGetOutOfDateRowsForView_Limit(){
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		int rowCount = 4;
		List<ObjectDataDTO> dtos = createFileEntityObjectDataDTOs(rowCount);
		
		// Make additional object with a different type but same ids
		createObjectDTOs(otherObjectType, EntityType.file, rowCount, false);
		
		Set<Long> scope = dtos.stream().map(ObjectDataDTO::getParentId).collect(Collectors.toSet());
		// first row to define the schema
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(dtos.get(0));
		// Create the empty view
		createOrUpdateTable(schema, indexDescription);
		// all of the rows are out-of-date, but only the last should be returned with a limit of one.
		long limit = 1L;

		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);
		
		// call under test
		Set<Long> results = tableIndexDAO.getOutOfDateRowsForView(tableId, filter, limit);
		assertNotNull(results);
		Set<Long> expected = Set.of(dtos.get(3).getId());
		assertEquals(expected, results);
	}
	
	/**
	 * Test that types do not match the view type are excluded from the delta.
	 */
	@Test
	public void testGetOutOfDateRowsForView_FilterTypes(){
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		int rowCount = 4;
		List<ObjectDataDTO> dtos = createFileEntityObjectDataDTOs(rowCount);
		
		// Make additional object with a different type but same ids
		createObjectDTOs(otherObjectType, EntityType.file, rowCount, false);
		
		// add a non-file that should not be part of the view but is in the scope.
		ObjectDataDTO viewDto = createEntityOfType(rowCount, EntityType.entityview, dtos.get(0).getParentId());
		
		Set<Long> scope = dtos.stream().map(ObjectDataDTO::getParentId).collect(Collectors.toSet());
		// first row to define the schema
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(dtos.get(0));
		// Create the empty view
		createOrUpdateTable(schema, indexDescription);
		long limit = 100L;
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);

		// call under test
		Set<Long> results = tableIndexDAO.getOutOfDateRowsForView(tableId, filter, limit);
		assertNotNull(results);
		// The view should not be in the results
		assertFalse(results.contains(viewDto.getId()));
		Set<Long> expectedResults = dtos.stream().map(ObjectDataDTO::getId).collect(Collectors.toSet());
		assertEquals(expectedResults, results);
	}
	
	/**
	 * Test where all types are initially in the view, then the view type is 
	 * changed to only include files.  All non-files must be removed from the view.
	 */
	@Test
	public void testGetOutOfDateRowsForView_RemoveTypesNoLongerInView(){
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		int rowCount = 4;
		List<ObjectDataDTO> dtos = createFileEntityObjectDataDTOs(rowCount);
		
		// Make additional object with a different type but same ids
		createObjectDTOs(otherObjectType, EntityType.file, rowCount, false);
		
		// add a non-file that should not be part of the view but is in the scope.
		ObjectDataDTO folderDto = createEntityOfType(rowCount, EntityType.folder, dtos.get(0).getParentId());
		
		Set<Long> scope = dtos.stream().map(ObjectDataDTO::getParentId).collect(Collectors.toSet());
		// first row to define the schema
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(dtos.get(0));
		// Create the empty view
		createOrUpdateTable(schema, indexDescription);
		
		// start including all types
		
		subTypes = new HashSet<SubType>(Arrays.asList(SubType.values())) ;
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);
		
		// add all of the rows to the view.
		tableIndexDAO.copyObjectReplicationToView(tableId.getId(), filter, schema, fieldTypeMapper);
		
		long limit = 100L;
		// File only type used to indicate the new type is files-only.
		subTypes = Set.of(SubType.file);
		filter = new HierarchicaFilter(mainType, subTypes, scope);
		
		// call under test
		Set<Long> results = tableIndexDAO.getOutOfDateRowsForView(tableId, filter, limit);
		assertNotNull(results);
		// the folder should be removed from the view.
		Set<Long> expectedResults = Set.of(folderDto.getId());
		assertEquals(expectedResults, results);
	}
	
	/**
	 * Test the limit for getOutOfDateRowsForView()
	 */
	@Test
	public void testGetOutOfDateRowsForView_EmptyScope(){
		Set<Long> scope = Collections.emptySet();
		
		int rowCount = 2;
		// Make additional object with a different type
		createObjectDTOs(otherObjectType, EntityType.file, rowCount, false);
		
		long limit = 1L;
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);
		
		// call under test
		Set<Long> results = tableIndexDAO.getOutOfDateRowsForView(tableId, filter, limit);
		assertEquals(Collections.emptySet(), results);
	}
	
	@Test
	public void testGetOutOfDateRowsForView_NullTableId(){
		tableId = null;
		Set<Long> scope = Collections.emptySet();
		long limit = 1L;
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);
		
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			tableIndexDAO.getOutOfDateRowsForView(tableId, filter, limit);
		});
	}
	
	/**
	 * Create n number of ObjectDataDTO with data in both replications and annotations.
	 * @param count
	 * @return
	 */
	List<ObjectDataDTO> createFileEntityObjectDataDTOs(int count){
		boolean includeMultiValue = false;
		return createObjectDTOs(objectType, EntityType.file, count, includeMultiValue);
	}
	
	List<ObjectDataDTO> createFileEntityObjectDataDTOs(int count, boolean includeMultiValue) {
		return createObjectDTOs(objectType, EntityType.file, count, includeMultiValue);
	}
	
	List<ObjectDataDTO> createObjectDTOs(ViewObjectType objectType, EntityType subType, int count, boolean includeMultiValue){
		List<Long> newIds = new ArrayList<Long>(count);
		List<ObjectDataDTO> results = new ArrayList<ObjectDataDTO>(count);
		for(int i=0; i<count; i++) {
			Long entityId = new Long(i+1);
			newIds.add(entityId);
			ObjectDataDTO file = createObjectDataDTO(entityId, subType, 3);
			file.setParentId(entityId*100);
			if(includeMultiValue) {
				ObjectAnnotationDTO multiValue = new ObjectAnnotationDTO(file);
				multiValue.setKey("multiValue");
				multiValue.setType(AnnotationType.STRING);
				multiValue.setValue(Lists.newArrayList("one"+i, "two"+i));
				file.getAnnotations().add(multiValue);
			}
			results.add(file);
		}
		// delete all rows if they already exist
		tableIndexDAO.deleteObjectData(mainType, newIds);
		// create all of the rows in the replication table
		tableIndexDAO.addObjectData(mainType, results);
		return results;
	}
	
	/**
	 * Create an entity of the given type.
	 * @param index
	 * @param type
	 * @param parentId
	 * @return
	 */
	ObjectDataDTO createEntityOfType(int index, EntityType type, long parentId) {
		return createObjectDTO(objectType, type, index, parentId);
	}
	
	ObjectDataDTO createObjectDTO(ViewObjectType objectType, EntityType subtype, int index, long parentId) {
		Long id = new Long(index+1);
		ObjectDataDTO object = createObjectDataDTO(id, subtype, 3);
		object.setParentId(parentId);
		// delete all rows if they already exist
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(id));
		// create all of the rows in the replication table
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(object));
		return object;
	}
	
	@Test
	public void testDeleteRowsFromView() {
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		int rowCount = 4;
		List<ObjectDataDTO> dtos = createFileEntityObjectDataDTOs(rowCount);
		
		// Make additional object with a different type but same ids
		createObjectDTOs(otherObjectType, EntityType.file, rowCount, false);
		
		Set<Long> scope = dtos.stream().map(ObjectDataDTO::getParentId).collect(Collectors.toSet());
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(dtos.get(0));
		createOrUpdateTable(schema, indexDescription);
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);
		
		tableIndexDAO.copyObjectReplicationToView(tableId.getId(), filter, schema, fieldTypeMapper);
		long limit = 100;
		// All rows should be in the view.
		Set<Long> deltas = tableIndexDAO.getOutOfDateRowsForView(tableId, filter, limit);
		assertNotNull(deltas);
		assertTrue(deltas.isEmpty());
		
		// delete the first and last
		Long[] toDelete = new Long[] {dtos.get(0).getId(), dtos.get(3).getId()};
		// call under test
		tableIndexDAO.deleteRowsFromViewBatch(tableId, toDelete);
		// Deleted rows should now show as deltas
		deltas = tableIndexDAO.getOutOfDateRowsForView(tableId, filter, limit);
		assertNotNull(deltas);
		assertEquals(2, deltas.size());
		assertTrue(deltas.contains(toDelete[0]));
		assertTrue(deltas.contains(toDelete[1]));
	}
	
	@Test
	public void testDeleteRowsFromViewEmpty() {
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		int rowCount = 4;
		List<ObjectDataDTO> dtos = createFileEntityObjectDataDTOs(rowCount);
		
		// Make additional object with a different type but same ids
		createObjectDTOs(otherObjectType, EntityType.file, rowCount, false);
		
		Set<Long> scope = dtos.stream().map(ObjectDataDTO::getParentId).collect(Collectors.toSet());
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(dtos.get(0));
		createOrUpdateTable(schema, indexDescription);
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);
		
		tableIndexDAO.copyObjectReplicationToView(tableId.getId(), filter, schema, fieldTypeMapper);
		long limit = 100;
		// All rows should be in the view.
		Set<Long> deltas = tableIndexDAO.getOutOfDateRowsForView(tableId, filter, limit);
		assertNotNull(deltas);
		assertTrue(deltas.isEmpty());
		// delete the first and last
		Long[] toDelete = new Long[0];
		// call under test
		tableIndexDAO.deleteRowsFromViewBatch(tableId, toDelete);
		// nothing should be deleted
		deltas = tableIndexDAO.getOutOfDateRowsForView(tableId, filter, limit);
		assertNotNull(deltas);
		assertTrue(deltas.isEmpty());
	}
	
	@Test
	public void testDeleteRowsFromViewNullTableId() {
		Long[] toDelete = new Long[] {123L};
		tableId = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			tableIndexDAO.deleteRowsFromViewBatch(tableId, toDelete);
		});
	}
	
	@Test
	public void testDeleteRowsFromViewNullIds() {
		Long[] toDelete = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			tableIndexDAO.deleteRowsFromViewBatch(tableId, toDelete);
		});
	}
	
	@Test
	public void testCopyEntityReplicationToViewWithRowFilter() {
		long limit = 100;
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		int rowCount = 4;
		List<ObjectDataDTO> dtos = createFileEntityObjectDataDTOs(rowCount);		

		// Make additional object with a different type but same ids
		createObjectDTOs(otherObjectType, EntityType.file, rowCount, false);
		
		Set<Long> scope = dtos.stream().map(ObjectDataDTO::getParentId).collect(Collectors.toSet());
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(dtos.get(0));
		createOrUpdateTable(schema, indexDescription);
		
		Long idThatDoesNotExist = 999L;
		// Only add the first and last row to the view and a row that does not exist
		Set<Long> rowFilter = Set.of(dtos.get(0).getId(), dtos.get(3).getId(), idThatDoesNotExist);
		

		
		ViewFilter filterNoAdditional = new HierarchicaFilter(mainType, subTypes, scope);
		
		ViewFilter filter = filterNoAdditional.newBuilder().addLimitObjectids(rowFilter).build();
		
		// call under test
		tableIndexDAO.copyObjectReplicationToView(tableId.getId(), filter, schema, fieldTypeMapper);
		
		Set<Long> expectedMissing = Set.of(dtos.get(1).getId(), dtos.get(2).getId());
		filter = new HierarchicaFilter(mainType, subTypes, scope);
		Set<Long> deltas = tableIndexDAO.getOutOfDateRowsForView(tableId, filterNoAdditional, limit);
		assertEquals(expectedMissing, deltas);
		// Add the remaining rows
		rowFilter = Set.of(dtos.get(1).getId(), dtos.get(2).getId());
		filter = filter.newBuilder().addLimitObjectids(rowFilter).build();
		// call under test
		tableIndexDAO.copyObjectReplicationToView(tableId.getId(), filter, schema, fieldTypeMapper);
		deltas = tableIndexDAO.getOutOfDateRowsForView(tableId, filterNoAdditional, limit);
		assertNotNull(deltas);
		assertTrue(deltas.isEmpty());
	}

	@Test
	public void testCopyEntityReplicationToViewWithRowFilterNull() {
		long limit = 100;
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		int rowCount = 4;
		List<ObjectDataDTO> dtos = createFileEntityObjectDataDTOs(rowCount);			
		
		// Make additional object with a different type but same ids
		createObjectDTOs(otherObjectType, EntityType.file, rowCount, false);
		
		Set<Long> scope = dtos.stream().map(ObjectDataDTO::getParentId).collect(Collectors.toSet());
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(dtos.get(0));
		createOrUpdateTable(schema, indexDescription);
	
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);
		// call under test
		tableIndexDAO.copyObjectReplicationToView(tableId.getId(), filter, schema, fieldTypeMapper);
		// all rows should be added
		Set<Long> deltas = tableIndexDAO.getOutOfDateRowsForView(tableId, filter, limit);
		assertNotNull(deltas);
		assertTrue(deltas.isEmpty());
	}
	
	@Test
	public void testPopulateListColumnIndexTableView_NoIdFilter() {
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		int rowCount = 4;
		boolean includeMultiValue = true;
		List<ObjectDataDTO> dtos = createFileEntityObjectDataDTOs(rowCount, includeMultiValue);

		// Make additional object with a different type but same ids
		createObjectDTOs(otherObjectType, EntityType.file, rowCount, includeMultiValue);
		
		Set<Long> scope = dtos.stream().map(ObjectDataDTO::getParentId).collect(Collectors.toSet());
		ColumnModel multiValue = new ColumnModel();
		multiValue.setId("886");
		multiValue.setColumnType(ColumnType.STRING_LIST);
		multiValue.setName("multiValue");
		multiValue.setMaximumSize(100L);
		multiValue.setMaximumListLength(53L);
		List<ColumnModel> schema = Lists.newArrayList(multiValue);
		createOrUpdateTable(schema, indexDescription);

		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);
		// push all of the data to the view
		tableIndexDAO.copyObjectReplicationToView(tableId.getId(), filter, schema, fieldTypeMapper);
		Set<Long> rowIdFilter = null;
		// call under test
		tableIndexDAO.populateListColumnIndexTable(tableId, multiValue, rowIdFilter, false);

		//each list value created by createRows has 2 items in the list
		assertEquals(rowCount * 2, countRowsInMultiValueIndex(tableId, multiValue.getId()));
	}
	
	/**
	 * This test simulate what happens when deltas are applied to an available view.
	 * For such cases, the multi-value index rows will be deleted when rows are
	 * removed from the main view (foreign key cascade). Data is then added back to
	 * the view using an rowIdFilter to both the view and the multi-value index
	 * table.
	 */
	@Test
	public void testPopulateListColumnIndexTableView_WithIdFilter() {
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		int rowCount = 4;
		boolean includeMultiValue = true;
		List<ObjectDataDTO> dtos = createFileEntityObjectDataDTOs(rowCount, includeMultiValue);

		// Make additional object with a different type but same ids
		createObjectDTOs(otherObjectType, EntityType.file, rowCount, includeMultiValue);
		
		Set<Long> scope = dtos.stream().map(ObjectDataDTO::getParentId).collect(Collectors.toSet());
		ColumnModel multiValue = new ColumnModel();
		multiValue.setId("886");
		multiValue.setColumnType(ColumnType.STRING_LIST);
		multiValue.setName("multiValue");
		multiValue.setMaximumSize(100L);
		multiValue.setMaximumListLength(22L);
		List<ColumnModel> schema = Lists.newArrayList(multiValue);
		createOrUpdateTable(schema, indexDescription);

		// Null row filter will add all rows
		Set<Long> rowIdFilter = null;
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);
		
		// push all of the data to the view
		tableIndexDAO.copyObjectReplicationToView(tableId.getId(), filter, schema, fieldTypeMapper);
		// start will all of the data in the secondary table
		tableIndexDAO.populateListColumnIndexTable(tableId, multiValue, rowIdFilter, false);
		// Should start with two row for each entity 
		assertEquals(rowCount * 2,countRowsInMultiValueIndex(tableId, multiValue.getId()));
		
		// delete the first and last rows from the main view
		Long[] toDelete = new Long[] {dtos.get(0).getId(), dtos.get(3).getId()};
		tableIndexDAO.deleteRowsFromViewBatch(tableId, toDelete);
		// cascade delete should reduce the number of rows in the multi-value index
		assertEquals(2 * 2,countRowsInMultiValueIndex(tableId, multiValue.getId()));
		
		// add the two rows back to the view
		rowIdFilter = Set.of(dtos.get(0).getId(), dtos.get(3).getId());
		boolean excludeDerivedKeys = false;
		
		filter = new HierarchicaFilter(mainType, subTypes,rowIdFilter,null, scope, excludeDerivedKeys);
		tableIndexDAO.copyObjectReplicationToView(tableId.getId(), filter, schema, fieldTypeMapper);
		// call under test
		tableIndexDAO.populateListColumnIndexTable(tableId, multiValue, rowIdFilter, false);
		assertEquals(4 * 2,countRowsInMultiValueIndex(tableId, multiValue.getId()));
	}
	
	@Test
	public void testPopulateListColumnIndexTable_NullTableId() {
		tableId = null;
		ColumnModel multiValue = new ColumnModel();
		multiValue.setId("886");
		multiValue.setColumnType(ColumnType.STRING_LIST);
		multiValue.setName("multiValue");
		multiValue.setMaximumSize(100L);
		Set<Long> rowIdFilter = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			tableIndexDAO.populateListColumnIndexTable(tableId, multiValue, rowIdFilter, false);
		});
	}
	
	@Test
	public void testPopulateListColumnIndexTable_NullColumn() {
		ColumnModel multiValue = null;
		Set<Long> rowIdFilter = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			tableIndexDAO.populateListColumnIndexTable(tableId, multiValue, rowIdFilter, false);
		});
	}
	
	@Test
	public void testPopulateListColumnIndexTable_NotListType() {
		ColumnModel multiValue = new ColumnModel();
		multiValue.setId("886");
		multiValue.setColumnType(ColumnType.STRING);
		multiValue.setName("multiValue");
		multiValue.setMaximumSize(100L);
		Set<Long> rowIdFilter = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			tableIndexDAO.populateListColumnIndexTable(tableId, multiValue, rowIdFilter, false);
		}).getMessage();
		assertEquals("Only valid for List type columns", message);
	}
	
	
	@Test
	public void testPopulateListColumnIndexTable_EmptyRowFilter() {
		ColumnModel multiValue = new ColumnModel();
		multiValue.setId("886");
		multiValue.setColumnType(ColumnType.STRING_LIST);
		multiValue.setName("multiValue");
		multiValue.setMaximumSize(100L);
		Set<Long> rowIdFilter = Collections.emptySet();
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			tableIndexDAO.populateListColumnIndexTable(tableId, multiValue, rowIdFilter, false);
		}).getMessage();
		assertEquals("When rowIds is provided (not null) it cannot be empty", message);
	}
	
	/**
	 * Helper to count the number of rows in a mutli-value index table.
	 * @param idAndversion
	 * @param columnId
	 * @return
	 */
	Long countRowsInMultiValueIndex(IdAndVersion idAndversion, String columnId) {
		String listColumnindexTableName = SQLUtils.getTableNameForMultiValueColumnIndex(idAndversion, columnId);
		return tableIndexDAO.countQuery("SELECT COUNT(*) FROM `" + listColumnindexTableName + "`", Collections.emptyMap());
	}

	//See PLFM-6017
	@Test
	public void testCreateAndPopulateListColumnIndexTables__DefaultValue() throws ParseException {
		ColumnModel intColumn = new ColumnModel();
		intColumn.setId("12");
		intColumn.setName("foo");
		intColumn.setColumnType(ColumnType.INTEGER);

		ColumnModel intListColumn = new ColumnModel();
		intListColumn.setId("16");
		intListColumn.setName("intList");
		intListColumn.setColumnType(ColumnType.INTEGER_LIST);
		intListColumn.setDefaultValue("[1,2,3]");
		intListColumn.setMaximumListLength(25L);

		List<ColumnModel> schema = Arrays.asList(intColumn, intListColumn);

		createOrUpdateTable(schema, indexDescription);

		Row row = new Row();
		row.setValues(Arrays.asList("1", null));
		List<Row> rows = Collections.singletonList(row);
		createOrUpdateOrDeleteRows(tableId, rows, schema);

		Set<Long> rowIds = null;
		// call under test
		tableIndexDAO.populateListColumnIndexTable(tableId, intListColumn, rowIds, false);

		QueryTranslator query = QueryTranslator.builder("select * from " + tableId, schemaProvider(schema), userId).indexDescription(new TableIndexDescription(tableId)).build();
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		assertNotNull(results.getRows());
		//expect a single row result
		assertEquals(1, results.getRows().size());
		assertEquals(tableId.toString(), results.getTableId());
		//each row has 2 columns
		assertEquals(2, results.getRows().get(0).getValues().size());
		assertEquals("1", results.getRows().get(0).getValues().get(0));
		//mysql adds spaces between the commas on returned results
		assertEquals("[1, 2, 3]", results.getRows().get(0).getValues().get(1));
	}


	@Test
	public void testGetMultivalueColumnIndexTableColumnIds(){
		ColumnModel strListColumn = new ColumnModel();
		strListColumn.setId("12");
		strListColumn.setName("foo");
		strListColumn.setMaximumSize(14L);
		strListColumn.setColumnType(ColumnType.STRING_LIST);

		ColumnModel intColumn = new ColumnModel();
		intColumn.setId("14");
		intColumn.setName("foo");
		intColumn.setColumnType(ColumnType.INTEGER);

		ColumnModel intListColumn = new ColumnModel();
		intListColumn.setId("16");
		intListColumn.setName("intList");
		intListColumn.setColumnType(ColumnType.INTEGER_LIST);
		intListColumn.setDefaultValue("[1,2,3]");

		List<ColumnModel> schema = Arrays.asList(strListColumn, intColumn, intListColumn);
		createOrUpdateTable(schema, indexDescription);

		//method under test
		Set<Long> columnIds = tableIndexDAO.getMultivalueColumnIndexTableColumnIds(tableId);
		assertEquals(Set.of(12L,16L), columnIds);
	}

	@Test
	public void testGetMultivalueColumnIndexTableColumnIds__emptyList(){
		ColumnModel intColumn = new ColumnModel();
		intColumn.setId("14");
		intColumn.setName("foo");
		intColumn.setColumnType(ColumnType.INTEGER);

		List<ColumnModel> schema = Arrays.asList(intColumn);
		createOrUpdateTable(schema, indexDescription);

		//method under test
		Set<Long> columnIds = tableIndexDAO.getMultivalueColumnIndexTableColumnIds(tableId);
		assertEquals(Collections.emptySet(), columnIds);
	}



	@Test
	public void testCreateUpdateDeleteMultivalueColumnIndexTable(){
		ColumnModel column = new ColumnModel();
		column.setColumnType(ColumnType.STRING_LIST);
		column.setId("1337");
		column.setMaximumSize(50L);
		column.setName("StringList");
		// Create the table
		tableIndexDAO.createTableIfDoesNotExist(indexDescription);
		//add column
		tableIndexDAO.createMultivalueColumnIndexTable(tableId, column, false);


		//check index table was created
		assertNotNull(tableIndexDAO.getConnection().queryForObject("show tables like '" + SQLUtils.getTableNameForMultiValueColumnIndex(tableId, column.getId()) + "'", String.class));

		//update column
		ColumnModel updated = new ColumnModel();
		updated.setColumnType(ColumnType.STRING_LIST);
		updated.setId("44444");
		updated.setMaximumSize(79L);
		updated.setName("newStringList");
		tableIndexDAO.updateMultivalueColumnIndexTable(tableId, Long.parseLong(column.getId()), updated, false);

		//check original no longer exists
		assertThrows(EmptyResultDataAccessException.class, () -> {
			tableIndexDAO.getConnection().queryForObject("show tables like '" + SQLUtils.getTableNameForMultiValueColumnIndex(tableId, column.getId()) + "'", String.class);
		});
		//check updated column's index table exists
		assertNotNull(tableIndexDAO.getConnection().queryForObject("show tables like '" + SQLUtils.getTableNameForMultiValueColumnIndex(tableId, updated.getId()) + "'", String.class));


		//delete column
		tableIndexDAO.deleteMultivalueColumnIndexTable(tableId, Long.parseLong(column.getId()), false);

		//check index table was deleted
		assertThrows(EmptyResultDataAccessException.class, () -> {
			tableIndexDAO.getConnection().queryForObject("show tables like '" + SQLUtils.getTableNameForMultiValueColumnIndex(tableId, column.getId()) + "'", String.class);
		});
	}

	@Test
	public void testTempTableListColumnMaxLength(){
		ColumnModel strListCol = new ColumnModel();
		strListCol.setId("12");
		strListCol.setName("foo");
		strListCol.setMaximumSize(50L);
		strListCol.setColumnType(ColumnType.STRING_LIST);
		strListCol.setMaximumListLength(25L);

		List<ColumnModel> schema = Lists.newArrayList(strListCol);

		createOrUpdateTable(schema, indexDescription);
		// create five rows.
		List<Row> rows = TableModelTestUtils.createRows(schema, 5);
		// add duplicate values
		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getSelectColumns(schema));
		set.setTableId(tableId.toString());

		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);

		createOrUpdateOrDeleteRows(tableId, set, schema);
		//populate the column index table
		tableIndexDAO.populateListColumnIndexTable(tableId, strListCol, null, false);

		String columnId = strListCol.getId();

		tableIndexDAO.deleteAllTemporaryMultiValueColumnIndexTable(tableId);
		tableIndexDAO.deleteTemporaryTable(tableId);

		// Create a copy of the table
		tableIndexDAO.createTemporaryTable(tableId);

		//method under test
		// on an empty table without data, we should get back 0
		assertEquals(0, tableIndexDAO.tempTableListColumnMaxLength(tableId,columnId));

		tableIndexDAO.copyAllDataToTemporaryTable(tableId);

		//method under test
		assertEquals(2, tableIndexDAO.tempTableListColumnMaxLength(tableId,columnId));
	}
	
	@Test
	public void testTempTableColumnExceedsCharacterLimit(){
		ColumnModel largeColumn = new ColumnModel();
		largeColumn.setId("12");
		largeColumn.setName("foo");
		largeColumn.setColumnType(ColumnType.LARGETEXT);
		
		ColumnModel smallColumn = new ColumnModel();
		smallColumn.setId("13");
		smallColumn.setName("bar");
		smallColumn.setMaximumSize(50L);
		smallColumn.setColumnType(ColumnType.STRING);

		List<ColumnModel> schema = List.of(largeColumn, smallColumn);

		createOrUpdateTable(schema, indexDescription);
		
		// create five rows.
		List<Row> rows = TableModelTestUtils.createRows(schema, 5);
		
		RowSet set = new RowSet();
		set.setRows(rows);
		set.setHeaders(TableModelUtils.getSelectColumns(schema));
		set.setTableId(tableId.toString());

		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);

		createOrUpdateOrDeleteRows(tableId, set, schema);
		
		tableIndexDAO.deleteTemporaryTable(tableId);
		// Create a copy of the table
		tableIndexDAO.createTemporaryTable(tableId);
		
		long charLimit = 12;

		// Call under test, on an empty table the limit does not exceed
		assertFalse(tableIndexDAO.tempTableColumnExceedsCharacterLimit(tableId, largeColumn.getId(), charLimit).isPresent());
		assertFalse(tableIndexDAO.tempTableColumnExceedsCharacterLimit(tableId, smallColumn.getId(), charLimit).isPresent());

		tableIndexDAO.copyAllDataToTemporaryTable(tableId);

		// Call under test
		assertEquals(rows.get(0).getRowId(),tableIndexDAO.tempTableColumnExceedsCharacterLimit(tableId, largeColumn.getId(), charLimit).get());
		assertFalse(tableIndexDAO.tempTableColumnExceedsCharacterLimit(tableId, smallColumn.getId(), charLimit).isPresent());
	}
	
	@Test
	public void testRefreshViewBenefactors() throws ParseException{
		tableId = IdAndVersion.parse("syn123");
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(2L,3L));
		tableIndexDAO.deleteTable(tableId);
		
		// setup some hierarchy.
		ObjectDataDTO file1 = createObjectDataDTO(2L, EntityType.file, 2);
		file1.setParentId(333L);
		ObjectAnnotationDTO double1 = new ObjectAnnotationDTO(file1);
		double1.setKey("foo");
		double1.setValue("NaN");
		double1.setType(AnnotationType.DOUBLE);
		file1.setAnnotations(Arrays.asList(double1));
		ObjectDataDTO file2 = createObjectDataDTO(3L, EntityType.file, 3);
		file2.setParentId(222L);
		ObjectAnnotationDTO double2 = new ObjectAnnotationDTO(file2);
		double2.setKey("foo");
		double2.setValue("Infinity");
		double2.setType(AnnotationType.DOUBLE);
		file2.setAnnotations(Arrays.asList(double2));
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file1, file2));
		
		// Create the schema for this table
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(file2);

		// both parents
		Set<Long> scope = Set.of(file1.getParentId(), file2.getParentId());
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);

		createOrUpdateTable(schema, indexDescription);
		
		tableIndexDAO.copyObjectReplicationToView(tableId.getId(), filter, schema, fieldTypeMapper);
		
		QueryTranslator query = QueryTranslator.builder("select ROW_ID, ROW_BENEFACTOR from " + tableId+" ORDER BY ROW_ID ASC", schemaProvider(schema), userId).indexDescription(new TableIndexDescription(tableId)).build();
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		assertEquals(Lists.newArrayList(file1.getId().toString(),file1.getBenefactorId().toString()), results.getRows().get(0).getValues());
		assertEquals(Lists.newArrayList(file2.getId().toString(),file2.getBenefactorId().toString()), results.getRows().get(1).getValues());
		
		// update the benefactors in the replication table
		file1.setBenefactorId(new Long(3));
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(file1.getId()));
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file1));
		
		// call under test
		boolean result = tableIndexDAO.refreshViewBenefactors(tableId, mainType);
		
		assertTrue(result);
		
		// The benefactors should be updated.
		results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		// file one should change while file two should remain the same.
		assertEquals(Lists.newArrayList(file1.getId().toString(),file1.getBenefactorId().toString()), results.getRows().get(0).getValues());
		assertEquals(Lists.newArrayList(file2.getId().toString(),file2.getBenefactorId().toString()), results.getRows().get(1).getValues());
	}
	
	@Test
	public void testRefreshViewBenefactorsWithNoChanges() throws ParseException{
		tableId = IdAndVersion.parse("syn123");
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(2L,3L));
		tableIndexDAO.deleteTable(tableId);
		
		// setup some hierarchy.
		ObjectDataDTO file1 = createObjectDataDTO(2L, EntityType.file, 2);
		file1.setParentId(333L);
		ObjectAnnotationDTO double1 = new ObjectAnnotationDTO(file1);
		double1.setKey("foo");
		double1.setValue("NaN");
		double1.setType(AnnotationType.DOUBLE);
		file1.setAnnotations(Arrays.asList(double1));
		ObjectDataDTO file2 = createObjectDataDTO(3L, EntityType.file, 3);
		file2.setParentId(222L);
		ObjectAnnotationDTO double2 = new ObjectAnnotationDTO(file2);
		double2.setKey("foo");
		double2.setValue("Infinity");
		double2.setType(AnnotationType.DOUBLE);
		file2.setAnnotations(Arrays.asList(double2));
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file1, file2));
		
		// Create the schema for this table
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(file2);

		// both parents
		Set<Long> scope = Set.of(file1.getParentId(), file2.getParentId());
		
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope);

		createOrUpdateTable(schema, indexDescription);
		
		tableIndexDAO.copyObjectReplicationToView(tableId.getId(), filter, schema, fieldTypeMapper);
		
		List<Row> expectedRows = List.of(
			new Row().setRowId(2L).setVersionNumber(2L).setValues(List.of(file1.getId().toString(),file1.getBenefactorId().toString())),
			new Row().setRowId(3L).setVersionNumber(2L).setValues(List.of(file2.getId().toString(),file2.getBenefactorId().toString()))
		);
		
		QueryTranslator query = QueryTranslator.builder("select ROW_ID, ROW_BENEFACTOR from " + tableId+" ORDER BY ROW_ID ASC", schemaProvider(schema), userId).indexDescription(new TableIndexDescription(tableId)).build();
		
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		
		assertEquals(expectedRows, results.getRows());
				
		// call under test
		boolean result = tableIndexDAO.refreshViewBenefactors(tableId, mainType);
		
		assertFalse(result);
		
		// No changes
		assertEquals(expectedRows, tableIndexDAO.query(mockProgressCallback, query).getRows());
	}
	
	@Test
	public void testRefreshViewBenefactorsWithNullId() {
		tableId = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			tableIndexDAO.refreshViewBenefactors(tableId, mainType);
		});
	}
	
	@Test
	public void testRefreshViewBenefactorsWithNullObjectType() {
		mainType = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			tableIndexDAO.refreshViewBenefactors(tableId, mainType);
		});
	}

	@Test
	public void testRoundQuery() throws ParseException {
		// Create the table
		List<ColumnModel> doubleColumn = Lists.newArrayList(TableModelTestUtils
				.createColumn(1L, "col1", ColumnType.DOUBLE));
		createOrUpdateTable(doubleColumn, indexDescription);
		// Now add some data
		List<Row> rows = TableModelTestUtils.createRows(doubleColumn, 2);
		// insert special values
		rows.get(0).getValues().set(0, "1.234");
		rows.get(1).getValues().set(0, "100.1899");
		RowSet set = new RowSet();
		set.setRows(rows);
		List<SelectColumn> headers = TableModelUtils.getSelectColumns(doubleColumn);
		set.setHeaders(headers);
		set.setTableId(tableId.toString());
		IdRange range = new IdRange();
		range.setMinimumId(100L);
		range.setMaximumId(200L);
		range.setVersionNumber(3L);
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, range);
		// Now fill the table with data
		createOrUpdateOrDeleteRows(tableId, set, doubleColumn);
		// This is our query
		QueryTranslator query = QueryTranslator.builder("select round(col1, 2) from " + tableId, schemaProvider(doubleColumn), userId).indexDescription(new TableIndexDescription(tableId)).build();
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		assertNotNull(results.getRows());
		assertEquals(tableId.toString(), results.getTableId());
		assertEquals(2, results.getRows().size());
		assertEquals("1.23", results.getRows().get(0).getValues().get(0));
		assertEquals("100.19", results.getRows().get(1).getValues().get(0));
	}
	
	@Test
	public void testViewWithMultipleVersions() throws ParseException{
		tableId = IdAndVersion.parse("syn123");
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		Long objectIdOne = 22L;
		Long objectIdTwo = 33L;
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(objectIdOne, objectIdTwo));
		tableIndexDAO.deleteTable(tableId);
		
		int annotationCoun = 1;
		int versionCount = 2;
		List<ObjectDataDTO> oneVersion = createMultipleVersions(objectIdOne, EntityType.file, annotationCoun, versionCount);
		List<ObjectDataDTO> twoVersion = createMultipleVersions(objectIdTwo, EntityType.file, annotationCoun, versionCount);
		ObjectDataDTO v1 = oneVersion.get(0);
		ObjectDataDTO v2 = twoVersion.get(1);
		
		// call under test
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(v1, v2));
		
		// Create the schema for this table
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(v2);
		createOrUpdateTable(schema, indexDescription);
		
		Set<SubType> subTypes = Set.of(SubType.file);
		Set<IdAndVersion> scope = Set.of(
				IdAndVersion.newBuilder().setId(v1.getId()).setVersion(v1.getVersion()).build(),
				IdAndVersion.newBuilder().setId(v2.getId()).setVersion(v2.getVersion()).build()
		);
		
		ViewFilter filter = new IdAndVersionFilter(ReplicationType.ENTITY, subTypes, scope);
		
		// Copy the entity data to the table
		tableIndexDAO.copyObjectReplicationToView(tableId.getId(), filter, schema, fieldTypeMapper);
		
		QueryTranslator query = QueryTranslator.builder("select ROW_ID, ROW_VERSION, `key0` from " + tableId+" ORDER BY ROW_ID ASC", schemaProvider(schema), userId).indexDescription(new TableIndexDescription(tableId)).build();
		// Now query for the results
		RowSet results = tableIndexDAO.query(mockProgressCallback, query);
		assertNotNull(results);
		assertEquals(2, results.getRows().size());
		assertEquals(Lists.newArrayList(v1.getId().toString(),v1.getVersion().toString(), "0-1"), results.getRows().get(0).getValues());
		assertEquals(Lists.newArrayList(v2.getId().toString(),v2.getVersion().toString(), "0-2"), results.getRows().get(1).getValues());
		
		// update the benefactors in the replication table
		v1.setBenefactorId(new Long(3));
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(v1.getId()));
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(v1));
	}
	
	@Test
	public void testGetObjectDataWithMultipleVersion() {
		Long objectId = 22L;
		int annotationCoun = 1;
		int versionCount = 2;
		List<ObjectDataDTO> objects = createMultipleVersions(objectId, EntityType.file, annotationCoun, versionCount);
		ObjectDataDTO v1 = objects.get(0);
		ObjectDataDTO v2 = objects.get(1);
		
		tableIndexDAO.addObjectData(mainType, objects);
		
		// Call under test
		assertEquals(v1, tableIndexDAO.getObjectData(mainType, objectId, v1.getVersion()));
		assertEquals(v2, tableIndexDAO.getObjectData(mainType, objectId, v2.getVersion()));
	}
	
	@Test
	public void testGetObjectDataForCurrentVersionWithMultipleVersions() {
		Long objectId = 22L;
		int annotationCoun = 1;
		int versionCount = 2;
		List<ObjectDataDTO> objects = createMultipleVersions(objectId, EntityType.file, annotationCoun, versionCount);
		ObjectDataDTO v2 = objects.get(1);
		
		tableIndexDAO.addObjectData(mainType, objects);
		
		assertEquals(v2, tableIndexDAO.getObjectData(mainType, objectId, v2.getVersion()));
		// call under test
		assertEquals(v2, tableIndexDAO.getObjectDataForCurrentVersion(mainType, objectId));
	}
	
	@Test
	public void testGetIdAndChecksumsForFilterWithFlatIdAndVersionFilter() {
		tableId = IdAndVersion.parse("syn123");
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		Long objectIdOne = 22L;
		Long objectIdTwo = 33L;
		Long objectIdThree = 44L;
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(objectIdOne, objectIdTwo));
		tableIndexDAO.deleteTable(tableId);
		
		int annotationCoun = 1;
		int versionCount = 2;
		List<ObjectDataDTO> oneVersion = createMultipleVersions(objectIdOne, EntityType.file, annotationCoun, versionCount);
		tableIndexDAO.addObjectData(mainType, oneVersion);
		List<ObjectDataDTO> twoVersion = createMultipleVersions(objectIdTwo, EntityType.file, annotationCoun, versionCount);
		tableIndexDAO.addObjectData(mainType, twoVersion);
		List<ObjectDataDTO> threeVersion = createMultipleVersions(objectIdThree, EntityType.file, annotationCoun, versionCount);
		tableIndexDAO.addObjectData(mainType, threeVersion);
		ObjectDataDTO v1 = oneVersion.get(0);
		ObjectDataDTO v2 = twoVersion.get(1);
		ObjectDataDTO v3 = threeVersion.get(0);
		
		// call under test
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(v1, v2, v3));
		
		// Create the schema for this table
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(v2);
		createOrUpdateTable(schema, indexDescription);
		
		Set<SubType> subTypes = Set.of(SubType.file);
		
		Set<IdAndVersion> scope = Set.of(
				IdAndVersion.newBuilder().setId(v1.getId()).setVersion(v1.getVersion()).build(),
				IdAndVersion.newBuilder().setId(v2.getId()).setVersion(v2.getVersion()).build(),
				IdAndVersion.newBuilder().setId(v3.getId()).setVersion(v3.getVersion()).build()
		);
		
		ViewFilter filter = new IdAndVersionFilter(ReplicationType.ENTITY, subTypes, scope);
		
		Long salt = 123L;
		Long limit = 2L;
		Long offset = 1L;
		// call under test
		List<IdAndChecksum> page = tableIndexDAO.getIdAndChecksumsForFilter(salt, filter, limit, offset);
		assertNotNull(page);
		assertEquals(2, page.size());
		assertEquals(objectIdTwo, page.get(0).getId());
		assertEquals(objectIdThree, page.get(1).getId());
		assertEquals(2, page.stream().filter(i->i.getChecksum() != null).count());
	}
	
	@Test
	public void testGetIdAndChecksumsForFilterWithHierarchyFilter() {
		tableId = IdAndVersion.parse("syn123");
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		Long objectIdOne = 22L;
		Long objectIdTwo = 33L;
		Long objectIdThree = 44L;
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(objectIdOne, objectIdTwo));
		tableIndexDAO.deleteTable(tableId);
		
		int annotationCoun = 1;
		int versionCount = 2;
		List<ObjectDataDTO> oneVersion = createMultipleVersions(objectIdOne, EntityType.file, annotationCoun, versionCount);
		tableIndexDAO.addObjectData(mainType, oneVersion);
		List<ObjectDataDTO> twoVersion = createMultipleVersions(objectIdTwo, EntityType.file, annotationCoun, versionCount);
		tableIndexDAO.addObjectData(mainType, twoVersion);
		List<ObjectDataDTO> threeVersion = createMultipleVersions(objectIdThree, EntityType.file, annotationCoun, versionCount);
		tableIndexDAO.addObjectData(mainType, threeVersion);
		
		// Create the schema for this table
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(oneVersion.get(0));
		createOrUpdateTable(schema, indexDescription);
		
		Set<SubType> subTypes = Set.of(SubType.file);
		Set<Long> parentIds = Set.of(1L);
		
		ViewFilter filter = new HierarchicaFilter(ReplicationType.ENTITY, subTypes, parentIds);
		
		Long salt = 123L;
		Long limit = 2L;
		Long offset = 1L;
		// call under test
		List<IdAndChecksum> page = tableIndexDAO.getIdAndChecksumsForFilter(salt, filter, limit, offset);
		assertNotNull(page);
		assertEquals(2, page.size());
		assertEquals(objectIdTwo, page.get(0).getId());
		assertEquals(objectIdThree, page.get(1).getId());
		assertEquals(2, page.stream().filter(i->i.getChecksum() != null).count());
	}
	
	@Test
	public void testGetIdAndChecksumsForFilterWithFlatFilter() {
		tableId = IdAndVersion.parse("syn123");
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		Long objectIdOne = 22L;
		Long objectIdTwo = 33L;
		Long objectIdThree = 44L;
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(objectIdOne, objectIdTwo));
		tableIndexDAO.deleteTable(tableId);
		
		int annotationCoun = 1;
		int versionCount = 2;
		List<ObjectDataDTO> oneVersion = createMultipleVersions(objectIdOne, EntityType.file, annotationCoun, versionCount);
		tableIndexDAO.addObjectData(mainType, oneVersion);
		List<ObjectDataDTO> twoVersion = createMultipleVersions(objectIdTwo, EntityType.file, annotationCoun, versionCount);
		tableIndexDAO.addObjectData(mainType, twoVersion);
		List<ObjectDataDTO> threeVersion = createMultipleVersions(objectIdThree, EntityType.file, annotationCoun, versionCount);
		tableIndexDAO.addObjectData(mainType, threeVersion);
		
		// Create the schema for this table
		List<ColumnModel> schema = createSchemaFromObjectDataDTO(oneVersion.get(0));
		createOrUpdateTable(schema, indexDescription);
		
		Set<SubType> subTypes = Set.of(SubType.file);
		Set<IdAndVersion> scope = Set.of(
				IdAndVersion.newBuilder().setId(objectIdOne).build(),
				IdAndVersion.newBuilder().setId(objectIdTwo).build(),
				IdAndVersion.newBuilder().setId(objectIdThree).build()
		);
		
		ViewFilter filter = new IdAndVersionFilter(ReplicationType.ENTITY, subTypes, scope);
		
		Long salt = 123L;
		Long limit = 2L;
		Long offset = 1L;
		// call under test
		List<IdAndChecksum> page = tableIndexDAO.getIdAndChecksumsForFilter(salt, filter, limit, offset);
		assertNotNull(page);
		assertEquals(2, page.size());
		assertEquals(objectIdTwo, page.get(0).getId());
		assertEquals(objectIdThree, page.get(1).getId());
		assertEquals(2, page.stream().filter(i->i.getChecksum() != null).count());
	}
	
	@Test
	public void testGetIdAndChecksumsForFilterWithEmptyFilter() {
		Set<SubType> subTypes = Set.of(SubType.file);
		Set<IdAndVersion> scope = Collections.emptySet();
		
		ViewFilter filter = new IdAndVersionFilter(ReplicationType.ENTITY, subTypes, scope);
		Long salt = 123L;
		Long limit = 2L;
		Long offset = 1L;
		// call under test
		List<IdAndChecksum> page = tableIndexDAO.getIdAndChecksumsForFilter(salt, filter, limit, offset);
		assertNotNull(page);
		assertTrue(page.isEmpty());
	}
	
	@Test
	public void testGetIdAndChecksumsForFilterWithNullFilter() {
		ViewFilter filter = null;
		Long salt = 123L;
		Long limit = 2L;
		Long offset = 1L;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			tableIndexDAO.getIdAndChecksumsForFilter(salt, filter, limit, offset);
		}).getMessage();
		assertEquals("filter is required.", message);
	}
	
	@Test
	public void testGetIdAndChecksumsForFilterWithNullSalt() {
		Set<SubType> subTypes = Set.of(SubType.file);
		Set<IdAndVersion> scope = Set.of(IdAndVersion.parse("1"));
		ViewFilter filter = new IdAndVersionFilter(ReplicationType.ENTITY, subTypes, scope);
		Long salt = null;
		Long limit = 2L;
		Long offset = 1L;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			tableIndexDAO.getIdAndChecksumsForFilter(salt, filter, limit, offset);
		}).getMessage();
		assertEquals("salt is required.", message);
	}
	
	@Test
	public void testGetIdAndChecksumsForFilterWithNullLimit() {
		Set<SubType> subTypes = Set.of(SubType.file);
		Set<IdAndVersion> scope = Set.of(IdAndVersion.parse("1"));
		ViewFilter filter = new IdAndVersionFilter(ReplicationType.ENTITY, subTypes, scope);
		Long salt = 123L;
		Long limit = null;
		Long offset = 1L;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			tableIndexDAO.getIdAndChecksumsForFilter(salt, filter, limit, offset);
		}).getMessage();
		assertEquals("limit is required.", message);
	}
	
	@Test
	public void testGetIdAndChecksumsForFilterWithNullOffset() {
		Set<SubType> subTypes = Set.of(SubType.file);
		Set<IdAndVersion> scope = Set.of(IdAndVersion.parse("1"));
		ViewFilter filter = new IdAndVersionFilter(ReplicationType.ENTITY, subTypes, scope);
		Long salt = 123L;
		Long limit = 2L;
		Long offset = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			tableIndexDAO.getIdAndChecksumsForFilter(salt, filter, limit, offset);
		}).getMessage();
		assertEquals("offset is required.", message);
	}
	
	@Test
	public void testStreamTableIndexData() {
		tableId = IdAndVersion.parse("syn123");
		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);
		// delete all data
		tableIndexDAO.deleteObjectData(mainType, Lists.newArrayList(2L,3L));
		tableIndexDAO.deleteTable(tableId);
		
		// setup some hierarchy.
		ObjectDataDTO file1 = createObjectDataDTO(2L, EntityType.file, 2);
		file1.setParentId(333L);
		
		ObjectAnnotationDTO int1 = new ObjectAnnotationDTO(file1);
		int1.setKey("foo");
		int1.setValue(Arrays.asList("123", "456", "789"));
		int1.setType(AnnotationType.LONG);
		
		ObjectAnnotationDTO double1 = new ObjectAnnotationDTO(file1);
		double1.setKey("bar");
		double1.setValue("NaN");
		double1.setType(AnnotationType.DOUBLE);
		
		file1.setAnnotations(Arrays.asList(int1, double1));
		ObjectDataDTO file2 = createObjectDataDTO(3L, EntityType.file, 3);
		file2.setParentId(222L);
		
		ObjectAnnotationDTO int2 = new ObjectAnnotationDTO(file2);
		int2.setKey("foo");
		int2.setValue(Arrays.asList("321", "654"));
		int2.setType(AnnotationType.LONG);
		
		ObjectAnnotationDTO double2 = new ObjectAnnotationDTO(file2);
		double2.setKey("bar");
		double2.setValue("Infinity");
		double2.setType(AnnotationType.DOUBLE);
		
		file2.setAnnotations(Arrays.asList(int2, double2));
		
		tableIndexDAO.addObjectData(mainType, Lists.newArrayList(file1, file2));
		
		// Create the schema for this table
		List<ColumnModel> schema = Lists.newArrayList(TableModelTestUtils.createColumn(1L, "foo", ColumnType.INTEGER_LIST), TableModelTestUtils.createColumn(2L, "bar", ColumnType.DOUBLE));

		// both parents
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, Set.of(file1.getParentId(), file2.getParentId()));

		createOrUpdateTable(schema, indexDescription);
		
		tableIndexDAO.copyObjectReplicationToView(tableId.getId(), filter, schema, fieldTypeMapper);
		
		InMemoryCSVWriterStream stream = new InMemoryCSVWriterStream();
		
		// Call under test
		List<String> result = tableIndexDAO.streamTableIndexData(tableId, stream);
		
		assertEquals(schema.stream().map(ColumnModel::getId).collect(Collectors.toList()), result);
		
		List<String[]> rows = stream.getRows();
		
		assertArrayEquals(new String[] {"ROW_ID", "ROW_VERSION", "ROW_ETAG", "ROW_BENEFACTOR" , "_C1_", "_C2_", "_DBL_C2_"}, rows.get(0));
		assertArrayEquals(new String[] {"2", "2", "etag2", "2", "[123, 456, 789]", null, "NaN"}, rows.get(1));
		assertArrayEquals(new String[] {"3", "2", "etag3", "2", "[321, 654]", "1.7976931348623157E308", "Infinity"}, rows.get(2));
	}
	
	// Test to reproduce https://sagebionetworks.jira.com/browse/PLFM-7622
	@Test
	public void testStreamTableIndexDataWithDoubleAndSchemaChange() {
		tableId = IdAndVersion.parse("syn123");
		indexDescription = new TableIndexDescription(tableId);
		// delete all data
		tableIndexDAO.deleteTable(tableId);
		
		tableIndexDAO.createTableIfDoesNotExist(indexDescription);
		
		ColumnModel intColumn = TableModelTestUtils.createColumn(1L, "foo", ColumnType.INTEGER);
		ColumnModel stringColumn = TableModelTestUtils.createColumn(2L, "bar", ColumnType.STRING);
		
		// Start off with an int and a string column
		List<ColumnChangeDetails> schemaChanges = List.of(
			new ColumnChangeDetails(null, intColumn),
			new ColumnChangeDetails(null, stringColumn)
		);
		
		tableIndexDAO.alterTableAsNeeded(tableId, schemaChanges, false);

		ColumnModel doubleColumn = TableModelTestUtils.createColumn(3L, "foo", ColumnType.DOUBLE);
		List<ColumnModel> schema = List.of(doubleColumn, stringColumn);

		// Now change the int column to a double column
		schemaChanges = List.of(
			new ColumnChangeDetails(intColumn, doubleColumn)
		);		
		
		List<DatabaseColumnInfo> currentIndexSchema = tableIndexDAO.getDatabaseInfo(tableId);
		schemaChanges = SQLUtils.matchChangesToCurrentInfo(currentIndexSchema, schemaChanges);
		
		tableIndexDAO.alterTableAsNeeded(tableId, schemaChanges, false);
		
		currentIndexSchema = tableIndexDAO.getDatabaseInfo(tableId);
		
		// The _DBL column is not appended as last
		assertEquals(
			List.of("ROW_ID", "ROW_VERSION", "ROW_SEARCH_CONTENT", "_C3_", "_C2_", "_DBL_C3_"), 
			currentIndexSchema.stream().map(col -> col.getColumnName()).collect(Collectors.toList())
		);
		
		SparseChangeSet rowSet = new SparseChangeSet(tableId.toString(), schema);
		
		SparseRow rowOne = rowSet.addEmptyRow();
		rowOne.setRowId(1L);
		rowOne.setVersionNumber(1L);
		rowOne.setCellValue(doubleColumn.getId(), "1.0");
		rowOne.setCellValue(stringColumn.getId(), "a");
		
		SparseRow rowTow = rowSet.addEmptyRow();
		rowTow.setRowId(2L);
		rowTow.setVersionNumber(1L);
		rowTow.setCellValue(doubleColumn.getId(), "NaN");
		rowTow.setCellValue(stringColumn.getId(), "b");
		
		Grouping data = rowSet.groupByValidValues().iterator().next();
		
		tableIndexDAO.createOrUpdateOrDeleteRows(tableId, data);
		
		InMemoryCSVWriterStream stream = new InMemoryCSVWriterStream();
		
		// Call under test
		List<String> result = tableIndexDAO.streamTableIndexData(tableId, stream);
		
		assertEquals(schema.stream().map(ColumnModel::getId).collect(Collectors.toList()), result);
		
		List<String[]> rows = stream.getRows();
		
		assertArrayEquals(new String[] {"ROW_ID", "ROW_VERSION", "_C3_", "_DBL_C3_", "_C2_"}, rows.get(0));
		assertArrayEquals(new String[] {"1", "1", "1.0", null, "a"}, rows.get(1));
		assertArrayEquals(new String[] {"2", "1", null, "NaN", "b"}, rows.get(2));
	}
	
	@Test
	public void testDoesIndexHashMatchSchemaHash() {
		tableId = IdAndVersion.parse("syn123");
		
		tableIndexDAO.createTableIfDoesNotExist(indexDescription);
		tableIndexDAO.createSecondaryTables(tableId);

		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		String schemaHash = TableModelUtils
				.createSchemaMD5Hex(allTypes.stream().map(ColumnModel::getId).collect(Collectors.toList()));
		tableIndexDAO.setCurrentSchemaMD5Hex(tableId, schemaHash);
		// call under test
		assertTrue(tableIndexDAO.doesIndexHashMatchSchemaHash(tableId, allTypes));
		
		allTypes.add(new ColumnModel().setId("999"));
		// call under test
		assertFalse(tableIndexDAO.doesIndexHashMatchSchemaHash(tableId, allTypes));
	}
	
	@Test
	public void testDoesIndexHashMatchSchemaHashWithTableDoesNotExist() {
		tableId = IdAndVersion.parse("syn123");

		List<ColumnModel> allTypes = TableModelTestUtils.createOneOfEachType();
		// call under test
		assertFalse(tableIndexDAO.doesIndexHashMatchSchemaHash(tableId, allTypes));
	}
	
	@Test
	public void testDoesIndexHashMatchSchemaHashWithTableIdNull() {
		tableId = null;
		List<ColumnModel> schema = TableModelTestUtils.createOneOfEachType();
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			assertFalse(tableIndexDAO.doesIndexHashMatchSchemaHash(tableId, schema));
		}).getMessage();
		assertEquals("tableId is required.",message);
	}
	
	@Test
	public void testDoesIndexHashMatchSchemaHashWithSchemaNull() {
		tableId = IdAndVersion.parse("syn123");
		List<ColumnModel> schema = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			assertFalse(tableIndexDAO.doesIndexHashMatchSchemaHash(tableId, schema));
		}).getMessage();
		assertEquals("newSchema is required.",message);
	}
	
	@Test
	public void testSwapTableIndex() {
		// Create the first table
		List<ColumnModel> schemaOne = List.of(
			TableModelTestUtils.createColumn(1L, "foo", ColumnType.DOUBLE),
			TableModelTestUtils.createColumn(2L, "bar", ColumnType.STRING_LIST),
			TableModelTestUtils.createColumn(3L, "barList", ColumnType.INTEGER_LIST)
		);
		
		createOrUpdateTable(schemaOne, indexDescription);
		tableIndexDAO.createSecondaryTables(indexDescription.getIdAndVersion());
		
		// Now add some data
		RowSet set = new RowSet()
			.setRows(TableModelTestUtils.createRows(schemaOne, 2))
			.setHeaders(TableModelUtils.getSelectColumns(schemaOne))
			.setTableId(indexDescription.getIdAndVersion().toString());
		
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, new IdRange().setMinimumId(100L).setMaximumId(200L).setVersionNumber(3L));
		
		// Now fill the table with data
		createOrUpdateOrDeleteRows(indexDescription.getIdAndVersion(), set, schemaOne);
		
		schemaOne.stream().filter( m -> ColumnTypeListMappings.isList(m.getColumnType())).forEach( column -> {			
			tableIndexDAO.populateListColumnIndexTable(indexDescription.getIdAndVersion(), column, null, false);
		});
		
		// Create another table
		IndexDescription sourceIndexDescription = new TableIndexDescription(IdAndVersion.parse("456"));
		
		List<ColumnModel> schemaTwo = List.of(
			TableModelTestUtils.createColumn(3L, "foo", ColumnType.INTEGER),
			TableModelTestUtils.createColumn(4L, "bar", ColumnType.STRING),
			TableModelTestUtils.createColumn(5L, "fooList", ColumnType.INTEGER_LIST),
			TableModelTestUtils.createColumn(6L, "barList", ColumnType.STRING_LIST)
		);
		
		tableIndexDAO.deleteTable(sourceIndexDescription.getIdAndVersion());
		createOrUpdateTable(schemaTwo, sourceIndexDescription);
		tableIndexDAO.createSecondaryTables(sourceIndexDescription.getIdAndVersion());
		
		set = new RowSet()
			.setRows(TableModelTestUtils.createRows(schemaTwo, 3))
			.setHeaders(TableModelUtils.getSelectColumns(schemaTwo))
			.setTableId(sourceIndexDescription.getIdAndVersion().toString());
		
		TableModelTestUtils.assignRowIdsAndVersionNumbers(set, new IdRange().setMinimumId(100L).setMaximumId(200L).setVersionNumber(4L));
		
		createOrUpdateOrDeleteRows(sourceIndexDescription.getIdAndVersion(), set, schemaTwo);
		
		schemaTwo.stream().filter( m -> ColumnTypeListMappings.isList(m.getColumnType())).forEach( column -> {			
			tableIndexDAO.populateListColumnIndexTable(sourceIndexDescription.getIdAndVersion(), column, null, false);
		});
		
		assertEquals(List.of("ROW_ID", "ROW_VERSION", "ROW_SEARCH_CONTENT", "_C1_", "_DBL_C1_", "_C2_", "_C3_"), 
			tableIndexDAO.getDatabaseInfo(indexDescription.getIdAndVersion()).stream().map(c -> c.getColumnName()).collect(Collectors.toList())
		);
		
		assertEquals(Set.of(2L, 3L), 
			tableIndexDAO.getMultivalueColumnIndexTableColumnIds(indexDescription.getIdAndVersion())
		);
		
		assertEquals(List.of("ROW_ID", "ROW_VERSION", "ROW_SEARCH_CONTENT", "_C3_", "_C4_", "_C5_", "_C6_"), 
			tableIndexDAO.getDatabaseInfo(sourceIndexDescription.getIdAndVersion()).stream().map(c -> c.getColumnName()).collect(Collectors.toList())
		);
		
		assertEquals(Set.of(5L, 6L), 
			tableIndexDAO.getMultivalueColumnIndexTableColumnIds(sourceIndexDescription.getIdAndVersion())
		);
		
		assertEquals(2L, tableIndexDAO.getRowCountForTable(indexDescription.getIdAndVersion()));
		assertEquals(3L, tableIndexDAO.getRowCountForTable(sourceIndexDescription.getIdAndVersion()));
		
		assertEquals(4L, countRowsInMultiValueIndex(indexDescription.getIdAndVersion(), "2"));
		assertEquals(2L, countRowsInMultiValueIndex(indexDescription.getIdAndVersion(), "3"));
		assertEquals(3L, countRowsInMultiValueIndex(sourceIndexDescription.getIdAndVersion(), "5"));
		assertEquals(6L, countRowsInMultiValueIndex(sourceIndexDescription.getIdAndVersion(), "6"));
				
		// Call under test
		tableIndexDAO.swapTableIndex(sourceIndexDescription.getIdAndVersion(), indexDescription.getIdAndVersion());
		
		assertEquals(3L, tableIndexDAO.getRowCountForTable(indexDescription.getIdAndVersion()));
		assertEquals(2L, tableIndexDAO.getRowCountForTable(sourceIndexDescription.getIdAndVersion()));
		
		assertEquals(3L, countRowsInMultiValueIndex(indexDescription.getIdAndVersion(), "5"));
		assertEquals(6L, countRowsInMultiValueIndex(indexDescription.getIdAndVersion(), "6"));
		assertEquals(4L, countRowsInMultiValueIndex(sourceIndexDescription.getIdAndVersion(), "2"));
		assertEquals(2L, countRowsInMultiValueIndex(sourceIndexDescription.getIdAndVersion(), "3"));
		
		assertEquals(List.of("ROW_ID", "ROW_VERSION", "ROW_SEARCH_CONTENT", "_C3_", "_C4_", "_C5_", "_C6_"), 
			tableIndexDAO.getDatabaseInfo(indexDescription.getIdAndVersion()).stream().map(c -> c.getColumnName()).collect(Collectors.toList())
		);
		
		assertEquals(Set.of(5L, 6L), 
			tableIndexDAO.getMultivalueColumnIndexTableColumnIds(indexDescription.getIdAndVersion())
		);
		
		assertEquals(List.of("ROW_ID", "ROW_VERSION", "ROW_SEARCH_CONTENT", "_C1_", "_DBL_C1_", "_C2_", "_C3_"), 
			tableIndexDAO.getDatabaseInfo(sourceIndexDescription.getIdAndVersion()).stream().map(c -> c.getColumnName()).collect(Collectors.toList())
		);
		
		assertEquals(Set.of(2L, 3L), 
			tableIndexDAO.getMultivalueColumnIndexTableColumnIds(sourceIndexDescription.getIdAndVersion())
		);
		
		JdbcTemplate jdbcTemplate = tableIndexDAO.getConnection();
		
		Set<String> renamedMultiValueTables = Set.of("2", "3").stream()
			.map(id -> SQLUtils.getTableNameForMultiValueColumnIndex(sourceIndexDescription.getIdAndVersion(), id)).collect(Collectors.toSet());
				
		// Makes sure the FK constraints were renamed as well, each renamed table will have one renamed FK
		Set<String> expectedForeignKeys = renamedMultiValueTables.stream()
			.map(t ->SQLUtils.getMultiValueIndexTableForeignKeyConstraintName(t))
			.collect(Collectors.toSet());
		
		Set<String> renamedForeignKeys = Set.copyOf(jdbcTemplate.queryForList("SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE "
			+ "TABLE_NAME IN(" + String.join(",", renamedMultiValueTables.stream().map(t-> "'" + t + "'").collect(Collectors.toList())) + ") AND REFERENCED_COLUMN_NAME = 'ROW_ID'", String.class)
		);
		
		assertEquals(expectedForeignKeys, renamedForeignKeys);
		
		tableIndexDAO.deleteTable(sourceIndexDescription.getIdAndVersion());
	}
	
	/**
	 * Helper to create a schema provider for the given schema.
	 * @param schema
	 * @return
	 */
	SchemaProvider schemaProvider(List<ColumnModel> schema) {
		SchemaProvider mockProvider = Mockito.mock(SchemaProvider.class);
		when(mockProvider.getTableSchema(any())).thenReturn(schema);
		return mockProvider;
	}
}
