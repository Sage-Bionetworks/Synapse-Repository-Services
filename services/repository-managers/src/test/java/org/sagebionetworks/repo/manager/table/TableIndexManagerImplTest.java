package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.table.cluster.ColumnChange;
import org.sagebionetworks.table.cluster.DatabaseColumnInfo;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class TableIndexManagerImplTest {
	
	@Mock
	TableIndexDAO mockIndexDao;
	@Mock
	TransactionStatus mockTransactionStatus;
	TableIndexManagerImpl manager;
	String tableId;
	Long versionNumber;
	List<Row> rows;
	RowSet rowSet;
	List<ColumnModel> schema;
	List<SelectColumn> selectColumns;
	
	@SuppressWarnings("unchecked")
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		tableId = "syn123";
		manager = new TableIndexManagerImpl(mockIndexDao, tableId);
		
		versionNumber = 99L;
		rows = new ArrayList<Row>();
		rows.add(TableModelTestUtils.createRow(1L, versionNumber, "1","2"));
		rows.add(TableModelTestUtils.createRow(2L, versionNumber, "5","6"));
		
		schema = Arrays.asList(
				TableModelTestUtils.createColumn(99L, "aString", ColumnType.STRING),
				TableModelTestUtils.createColumn(101L, "aFile", ColumnType.FILEHANDLEID)
				);
		
		selectColumns = Arrays.asList(
				TableModelTestUtils.createSelectColumn(99L, "aString", ColumnType.STRING),
				TableModelTestUtils.createSelectColumn(101L, "aFile", ColumnType.FILEHANDLEID)
				);
		
		rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setTableId(tableId);
		rowSet.setHeaders(selectColumns);
		
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);
		
		// When a write transaction callback is used, we need to call the callback.
		doAnswer(new Answer<Void>(){

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				TransactionCallback callback = (TransactionCallback) invocation.getArguments()[0];
				callback.doInTransaction(mockTransactionStatus);
				return null;
			}}).when(mockIndexDao).executeInWriteTransaction(any(TransactionCallback.class));

	}

	@Test (expected=IllegalArgumentException.class)
	public void testNullDao(){
		new TableIndexManagerImpl(null, tableId);	
	}
	
	
	@Test (expected=IllegalArgumentException.class)
	public void testNullTableId(){
		new TableIndexManagerImpl(mockIndexDao, null);			
	}
	
	@Test
	public void testApplyChangeSetToIndexHappy(){
		//call under test.
		manager.applyChangeSetToIndex(rowSet, schema, versionNumber);
		// All changes should be executed in a transaction
		verify(mockIndexDao).executeInWriteTransaction(any(TransactionCallback.class));
		// change should be written to the index
		verify(mockIndexDao).createOrUpdateOrDeleteRows(rowSet, schema);
		// files handles should be applied.
		verify(mockIndexDao).applyFileHandleIdsToTable(tableId, Sets.newHashSet(2L, 6L));
		// The new version should be set
		verify(mockIndexDao).setMaxCurrentCompleteVersionForTable(tableId, versionNumber);
	}
	
	@Test
	public void testApplyChangeSetToIndexAlreadyApplied(){
		// For this case the index already has this change set applied.
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(versionNumber+1);
		//call under test.
		manager.applyChangeSetToIndex(rowSet, schema, versionNumber);
		// nothing do do.
		verify(mockIndexDao, never()).executeInWriteTransaction(any(TransactionCallback.class));
		verify(mockIndexDao, never()).createOrUpdateOrDeleteRows(any(RowSet.class), anyList());
		verify(mockIndexDao, never()).applyFileHandleIdsToTable(anyString(), anySet());
		verify(mockIndexDao, never()).setMaxCurrentCompleteVersionForTable(anyString(), anyLong());
	}
	
	@Test
	public void testApplyChangeSetToIndexNoFiles(){
		// no files in the schema
		schema = Arrays.asList(
				TableModelTestUtils.createColumn(99L, "aString", ColumnType.STRING),
				TableModelTestUtils.createColumn(101L, "moreStrings", ColumnType.STRING)
				);
		selectColumns = Arrays.asList(
				TableModelTestUtils.createSelectColumn(99L, "aString", ColumnType.STRING),
				TableModelTestUtils.createSelectColumn(101L, "moreStrings", ColumnType.STRING)
				);
		rowSet.setHeaders(selectColumns);
		
		//call under test.
		manager.applyChangeSetToIndex(rowSet, schema, versionNumber);
		// All changes should be executed in a transaction
		verify(mockIndexDao).executeInWriteTransaction(any(TransactionCallback.class));
		// change should be written to the index
		verify(mockIndexDao).createOrUpdateOrDeleteRows(rowSet, schema);
		// there are no files
		verify(mockIndexDao, never()).applyFileHandleIdsToTable(anyString(), anySet());
		// The new version should be set
		verify(mockIndexDao).setMaxCurrentCompleteVersionForTable(tableId, versionNumber);
	}
	
	@Test
	public void testSetIndexSchemaWithColumns(){
		// call under test
		manager.setIndexSchema(schema);
		String schemaMD5Hex = TableModelUtils. createSchemaMD5HexCM(Lists.newArrayList(schema));
		verify(mockIndexDao).setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testApplyChangeSetToIndexInvalidVersion(){
		// use a version that does not match
		versionNumber = versionNumber - 1;
		//call under test.
		manager.applyChangeSetToIndex(rowSet, schema, versionNumber);
	}
	
	@Test
	public void testIsVersionAppliedToIndexNoVersionApplied(){
		// no version has been applied for this case
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);
		versionNumber = 1L;
		assertFalse(manager.isVersionAppliedToIndex(versionNumber));
	}
	
	@Test
	public void testIsVersionAppliedToIndexVersionMatches(){
		versionNumber = 1L;
		// no version has been applied for this case
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(versionNumber);
		assertTrue(manager.isVersionAppliedToIndex(versionNumber));
	}
	
	@Test
	public void testIsVersionAppliedToIndexVersionGreater(){
		versionNumber = 1L;
		// no version has been applied for this case
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(versionNumber+1);
		assertTrue(manager.isVersionAppliedToIndex(versionNumber));
	}
	
	@Test
	public void testDeleteTableIndex(){
		manager.deleteTableIndex();
		verify(mockIndexDao).deleteSecondayTables(tableId);
		verify(mockIndexDao).deleteTable(tableId);
	}
	
	@Test
	public void testOptimizeTableIndices(){
		List<DatabaseColumnInfo> infoList = new LinkedList<DatabaseColumnInfo>();
		when(mockIndexDao.getDatabaseInfo(tableId)).thenReturn(infoList);
		// call under test
		manager.optimizeTableIndices();
		// column data must be gathered.
		verify(mockIndexDao).getDatabaseInfo(tableId);
		verify(mockIndexDao).provideCardinality(infoList, tableId);
		verify(mockIndexDao).provideIndexName(infoList, tableId);
		// optimization called.
		verify(mockIndexDao).optimizeTableIndices(infoList, tableId, 63);
	}
	
	@Test
	public void testUpdateTableSchemaAddColumn(){
		ColumnModel oldColumn = null;
		ColumnModel newColumn = new ColumnModel();
		newColumn.setId("12");
		List<ColumnChange> changes = Lists.newArrayList(new ColumnChange(oldColumn, newColumn));
		// call under test
		manager.updateTableSchema(changes);
		verify(mockIndexDao).createTableIfDoesNotExist(tableId);
		verify(mockIndexDao).createSecondaryTables(tableId);
		// The new schema is not empty so do not truncate.
		verify(mockIndexDao, never()).truncateTable(tableId);
		verify(mockIndexDao).alterTableAsNeeded(tableId, changes);
		
		String schemaMD5Hex = TableModelUtils. createSchemaMD5HexCM(Lists.newArrayList(newColumn));
		verify(mockIndexDao).setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
	}
	
	@Test
	public void testUpdateTableSchemaRemoveAllColumns(){
		ColumnModel oldColumn = new ColumnModel();
		oldColumn.setId("12");
		ColumnModel newColumn = null;

		List<ColumnChange> changes = Lists.newArrayList(new ColumnChange(oldColumn, newColumn));
		// call under test
		manager.updateTableSchema(changes);
		verify(mockIndexDao).createTableIfDoesNotExist(tableId);
		verify(mockIndexDao).createSecondaryTables(tableId);
		// The new schema is empty so the table is truncated.
		verify(mockIndexDao).truncateTable(tableId);
		verify(mockIndexDao).alterTableAsNeeded(tableId, changes);
		
		String schemaMD5Hex = TableModelUtils. createSchemaMD5HexCM(new LinkedList<ColumnModel>());
		verify(mockIndexDao).setCurrentSchemaMD5Hex(tableId, schemaMD5Hex);
	}
}
