package org.sagebionetworks.repo.manager.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.sagebionetworks.repo.manager.table.TableIndexManager.TableIndexConnection;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.google.common.collect.Sets;

public class TableIndexManagerImplTest {
	
	TableIndexDAO mockIndexDao;
	ConnectionFactory mockConnectionFactory;
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
		mockConnectionFactory = Mockito.mock(ConnectionFactory.class);
		mockTransactionStatus = Mockito.mock(TransactionStatus.class);
		when(mockConnectionFactory.getConnection(anyString())).thenReturn(mockIndexDao);
		manager = new TableIndexManagerImpl();
		ReflectionTestUtils.setField(manager, "tableConnectionFactory", mockConnectionFactory);	
		tableId = "syn123";
		
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

	@Test
	public void testConnectToTableIndexHappy(){
		TableIndexConnection connection = manager.connectToTableIndex(tableId);
		assertNotNull(connection);		
	}
	
	@Test (expected=TableIndexConnectionUnavailableException.class)
	public void testConnectToTableIndexUnavailable(){
		// returning null should trigger a TableIndexConnectionUnavailableException
		when(mockConnectionFactory.getConnection(anyString())).thenReturn(null);
		TableIndexConnection connection = manager.connectToTableIndex(tableId);
		assertNotNull(connection);		
	}
	
	@Test
	public void testApplyChangeSetToIndexHappy(){
		TableIndexConnection con = manager.connectToTableIndex(tableId);
		//call under test.
		manager.applyChangeSetToIndex(con, rowSet, schema, versionNumber);
		// All changes should be executed in a transaction
		verify(mockIndexDao).executeInWriteTransaction(any(TransactionCallback.class));
		// change should be written to the index
		verify(mockIndexDao).createOrUpdateOrDeleteRows(rowSet, schema);
		// files handles should be applied.
		verify(mockIndexDao).applyFileHandleIdsToTable(tableId, Sets.newHashSet("2", "6"));
		// The new version should be set
		verify(mockIndexDao).setMaxCurrentCompleteVersionForTable(tableId, versionNumber);
	}
	
	@Test
	public void testApplyChangeSetToIndexAlreadyApplied(){
		TableIndexConnection con = manager.connectToTableIndex(tableId);
		// For this case the index already has this change set applied.
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(versionNumber+1);
		//call under test.
		manager.applyChangeSetToIndex(con, rowSet, schema, versionNumber);
		// nothing do do.
		verify(mockIndexDao, never()).executeInWriteTransaction(any(TransactionCallback.class));
		verify(mockIndexDao, never()).createOrUpdateOrDeleteRows(any(RowSet.class), anyList());
		verify(mockIndexDao, never()).applyFileHandleIdsToTable(anyString(), anySet());
		verify(mockIndexDao, never()).setMaxCurrentCompleteVersionForTable(anyString(), anyLong());
	}
	
	@Test
	public void testApplyChangeSetToIndexNoFiles(){
		TableIndexConnection con = manager.connectToTableIndex(tableId);
		// no files in the schema
		schema = Arrays.asList(
				TableModelTestUtils.createColumn(99L, "aString", ColumnType.STRING),
				TableModelTestUtils.createColumn(101L, "moreStrings", ColumnType.STRING)
				);
		//call under test.
		manager.applyChangeSetToIndex(con, rowSet, schema, versionNumber);
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
		TableIndexConnection con = manager.connectToTableIndex(tableId);
		// call under test
		manager.setIndexSchema(con, schema);
		// The main table should be created or updated.
		verify(mockIndexDao).createOrUpdateTable(schema, tableId);
		verify(mockIndexDao, never()).deleteTable(tableId);
	}
	
	@Test
	public void testSetIndexSchemaEmpty(){
		TableIndexConnection con = manager.connectToTableIndex(tableId);
		schema = new LinkedList<ColumnModel>();
		// call under test
		manager.setIndexSchema(con, schema);
		// The main table should be created or updated.
		verify(mockIndexDao, never()).createOrUpdateTable(anyList(), anyString());
		// The main table should be deleted.
		verify(mockIndexDao).deleteTable(tableId);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testApplyChangeSetToIndexInvalidVersion(){
		TableIndexConnection con = manager.connectToTableIndex(tableId);
		// use a version that does not match
		versionNumber = versionNumber - 1;
		//call under test.
		manager.applyChangeSetToIndex(con, rowSet, schema, versionNumber);
	}
	
	@Test
	public void testIsVersionAppliedToIndexNoVersionApplied(){
		// no version has been applied for this case
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(-1L);
		versionNumber = 1L;
		TableIndexConnection con = manager.connectToTableIndex(tableId);
		assertFalse(manager.isVersionAppliedToIndex(con, versionNumber));
	}
	
	@Test
	public void testIsVersionAppliedToIndexVersionMatches(){
		versionNumber = 1L;
		// no version has been applied for this case
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(versionNumber);
		TableIndexConnection con = manager.connectToTableIndex(tableId);
		assertTrue(manager.isVersionAppliedToIndex(con, versionNumber));
	}
	
	@Test
	public void testIsVersionAppliedToIndexVersionGreater(){
		versionNumber = 1L;
		// no version has been applied for this case
		when(mockIndexDao.getMaxCurrentCompleteVersionForTable(tableId)).thenReturn(versionNumber+1);
		TableIndexConnection con = manager.connectToTableIndex(tableId);
		assertTrue(manager.isVersionAppliedToIndex(con, versionNumber));
	}
	
	@Test
	public void testDeleteTableIndex(){
		TableIndexConnection con = manager.connectToTableIndex(tableId);
		manager.deleteTableIndex(con);
		verify(mockIndexDao).deleteStatusTable(tableId);
		verify(mockIndexDao).deleteTable(tableId);
	}
	
}
