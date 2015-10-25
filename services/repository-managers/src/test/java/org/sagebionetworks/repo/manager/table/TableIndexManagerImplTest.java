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
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.google.common.collect.Sets;

public class TableIndexManagerImplTest {
	
	TableIndexDAO mockIndexDao;
	TransactionStatus mockTransactionStatus;
	TableIndexManagerImpl manager;
	String tableId;
	Long versionNumber;
	List<Row> rows;
	RowSet rowSet;
	List<ColumnModel> schema;
	
	@SuppressWarnings("unchecked")
	@Before
	public void before(){
		mockIndexDao = Mockito.mock(TableIndexDAO.class);
		mockTransactionStatus = Mockito.mock(TransactionStatus.class);
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
		
		rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setTableId(tableId);	
		
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
		// The main table should be created or updated.
		verify(mockIndexDao).createOrUpdateTable(schema, tableId);
		verify(mockIndexDao, never()).deleteTable(tableId);
		verify(mockIndexDao).createSecondaryTables(tableId);
	}
	
	@Test
	public void testSetIndexSchemaEmpty(){
		schema = new LinkedList<ColumnModel>();
		// call under test
		manager.setIndexSchema(schema);
		// The main table should be created or updated.
		verify(mockIndexDao, never()).createOrUpdateTable(anyList(), anyString());
		// The main table should be deleted.
		verify(mockIndexDao).deleteTable(tableId);
		// The status tables should be created even if the schema is empty
		verify(mockIndexDao).createSecondaryTables(tableId);
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
	
}
