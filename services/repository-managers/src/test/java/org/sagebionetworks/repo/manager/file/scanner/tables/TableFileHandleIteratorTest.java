package org.sagebionetworks.repo.manager.file.scanner.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.file.scanner.ScannedFileHandleAssociation;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.manager.table.change.TableChangeMetaData;
import org.sagebionetworks.repo.model.exception.RecoverableException;
import org.sagebionetworks.repo.model.exception.UnrecoverableException;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.SparseRowDto;
import org.sagebionetworks.repo.model.table.TableChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.model.ChangeData;
import org.sagebionetworks.table.model.SparseChangeSet;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonServiceException.ErrorType;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.google.common.collect.ImmutableMap;

@ExtendWith(MockitoExtension.class)
public class TableFileHandleIteratorTest {
	
	@Mock
	private TableEntityManager mockTableManager;
	
	@Mock
	private Iterator<TableChangeMetaData> mockTableChangeIterator;
	
	@Mock
	private TableChangeMetaData mockTableChange;
	
	private TableFileHandleIterator iterator;
	
	private Long tableId;
	
	@BeforeEach
	public void before() {
		when(mockTableManager.newTableChangeIterator(anyString())).thenReturn(mockTableChangeIterator);
		
		tableId = 123L;		
		iterator = new TableFileHandleIterator(mockTableManager, tableId);
	}
	
	@AfterEach
	public void after() {
		verify(mockTableManager).newTableChangeIterator(tableId.toString());
	}
	
	@Test
	public void testHasNext() {
		
		when(mockTableChangeIterator.hasNext()).thenReturn(false);
		
		// Call under test
		boolean result = iterator.hasNext();
		
		assertFalse(result);
		
		verify(mockTableChangeIterator).hasNext();
		
	}
	
	@Test
	public void testNextWithColumnChange() {
		
		when(mockTableChange.getChangeType()).thenReturn(TableChangeType.COLUMN);
		when(mockTableChangeIterator.next()).thenReturn(mockTableChange);
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(tableId);
		
		// Call under test
		ScannedFileHandleAssociation result = iterator.next();
		
		assertEquals(expected, result);
		
		verify(mockTableChangeIterator).next();
		verify(mockTableChange).getChangeType();
		
	}
	
	@Test
	public void testNextWithRowChangeAndNullChangeData() throws NotFoundException, IOException {
		
		ChangeData<SparseChangeSet> changeData = null;
		
		when(mockTableChange.loadChangeData(SparseChangeSet.class)).thenReturn(changeData);
		when(mockTableChange.getChangeType()).thenReturn(TableChangeType.ROW);
		when(mockTableChangeIterator.next()).thenReturn(mockTableChange);
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(tableId);
		
		// Call under test
		ScannedFileHandleAssociation result = iterator.next();
		
		assertEquals(expected, result);
		
		verify(mockTableChangeIterator).next();
		verify(mockTableChange).getChangeType();
		verify(mockTableChange).loadChangeData(SparseChangeSet.class);
		
	}
	
	@Test
	public void testNextWithRowChangeAndNullChangeSet() throws NotFoundException, IOException {
		
		SparseChangeSet change = null;
		
		ChangeData<SparseChangeSet> changeData = new ChangeData<SparseChangeSet>(0, change);
		
		when(mockTableChange.loadChangeData(SparseChangeSet.class)).thenReturn(changeData);
		when(mockTableChange.getChangeType()).thenReturn(TableChangeType.ROW);
		when(mockTableChangeIterator.next()).thenReturn(mockTableChange);
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(tableId);
		
		// Call under test
		ScannedFileHandleAssociation result = iterator.next();
		
		assertEquals(expected, result);
		
		verify(mockTableChangeIterator).next();
		verify(mockTableChange).getChangeType();
		verify(mockTableChange).loadChangeData(SparseChangeSet.class);
		
	}
	
	@Test
	public void testNextWithRowChangeAndEmptyChangeSet() throws NotFoundException, IOException {
		
		SparseChangeSet change = new SparseChangeSet(tableId.toString(), Collections.emptyList());
		
		ChangeData<SparseChangeSet> changeData = new ChangeData<SparseChangeSet>(0, change);
		
		when(mockTableChange.loadChangeData(SparseChangeSet.class)).thenReturn(changeData);
		when(mockTableChange.getChangeType()).thenReturn(TableChangeType.ROW);
		when(mockTableChangeIterator.next()).thenReturn(mockTableChange);
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(tableId).withFileHandleIds(Collections.emptySet());
		
		// Call under test
		ScannedFileHandleAssociation result = iterator.next();
		
		assertEquals(expected, result);
		
		verify(mockTableChangeIterator).next();
		verify(mockTableChange).getChangeType();
		verify(mockTableChange).loadChangeData(SparseChangeSet.class);
		
	}
	
	@Test
	public void testNextWithRowChangeAndNonEmptyChangeSet() throws NotFoundException, IOException {
		
		ColumnModel fileModel = new ColumnModel();
		
		fileModel.setId("123");
		fileModel.setColumnType(ColumnType.FILEHANDLEID);
		fileModel.setName("FILE_ID");
		
		SparseRowDto row = new SparseRowDto();
		
		row.setValues(ImmutableMap.of(fileModel.getId(), "456"));
		
		SparseChangeSet change = new SparseChangeSet(tableId.toString(), Arrays.asList(fileModel));
		
		change.addAllRows(Arrays.asList(row));
		
		ChangeData<SparseChangeSet> changeData = new ChangeData<SparseChangeSet>(0, change);
		
		when(mockTableChange.loadChangeData(SparseChangeSet.class)).thenReturn(changeData);
		when(mockTableChange.getChangeType()).thenReturn(TableChangeType.ROW);
		when(mockTableChangeIterator.next()).thenReturn(mockTableChange);
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(tableId).withFileHandleIds(Collections.singleton(456L));
		
		// Call under test
		ScannedFileHandleAssociation result = iterator.next();
		
		assertEquals(expected, result);
		
		verify(mockTableChangeIterator).next();
		verify(mockTableChange).getChangeType();
		verify(mockTableChange).loadChangeData(SparseChangeSet.class);
		
	}
	
	@Test
	public void testNextWithRowChangeAndNotFoundException() throws NotFoundException, IOException {
		
		NotFoundException ex = new NotFoundException("Error");
		
		doThrow(ex).when(mockTableChange).loadChangeData(any());
		
		Long changeNumber = 123L;
		
		when(mockTableChange.getChangeType()).thenReturn(TableChangeType.ROW);
		when(mockTableChange.getChangeNumber()).thenReturn(changeNumber);
		when(mockTableChangeIterator.next()).thenReturn(mockTableChange);
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(tableId);
		
		// Call under test
		ScannedFileHandleAssociation result = iterator.next();
		
		assertEquals(expected, result);
		
		verify(mockTableChangeIterator).next();
		verify(mockTableChange).getChangeType();
		verify(mockTableChange).loadChangeData(SparseChangeSet.class);
		verify(mockTableChange).getChangeNumber();
		
	}
	
	@Test
	public void testNextWithRowChangeAndAmazonNotFoundS3Exception() throws NotFoundException, IOException {
		
		AmazonS3Exception ex = new AmazonS3Exception("Not found");
		
		ex.setStatusCode(404);
		
		doThrow(ex).when(mockTableChange).loadChangeData(any());
		
		Long changeNumber = 123L;
		
		when(mockTableChange.getChangeType()).thenReturn(TableChangeType.ROW);
		when(mockTableChange.getChangeNumber()).thenReturn(changeNumber);
		when(mockTableChangeIterator.next()).thenReturn(mockTableChange);
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(tableId);
		
		// Call under test
		ScannedFileHandleAssociation result = iterator.next();
		
		assertEquals(expected, result);
		
		verify(mockTableChangeIterator).next();
		verify(mockTableChange).getChangeType();
		verify(mockTableChange).loadChangeData(SparseChangeSet.class);
		verify(mockTableChange).getChangeNumber();
		
	}
	
	@Test
	public void testNextWithRowChangeAndIOException() throws NotFoundException, IOException {
		
		IOException ex = new IOException("Error");
		
		doThrow(ex).when(mockTableChange).loadChangeData(any());
		
		when(mockTableChange.getChangeType()).thenReturn(TableChangeType.ROW);
		when(mockTableChangeIterator.next()).thenReturn(mockTableChange);
		
		UnrecoverableException result = assertThrows(UnrecoverableException.class, () -> {			
			// Call under test
			iterator.next();
		});
		
		assertEquals(ex, result.getCause());
		
		verify(mockTableChangeIterator).next();
		verify(mockTableChange).getChangeType();
		verify(mockTableChange).loadChangeData(SparseChangeSet.class);
		
	}
	
	@Test
	public void testNextWithRowChangeAndCallerAmazonServiceException() throws NotFoundException, IOException {
		
		AmazonServiceException ex = new AmazonServiceException("Error");
		
		ex.setErrorType(ErrorType.Client);
		
		doThrow(ex).when(mockTableChange).loadChangeData(any());
		
		when(mockTableChange.getChangeType()).thenReturn(TableChangeType.ROW);
		when(mockTableChangeIterator.next()).thenReturn(mockTableChange);
		
		UnrecoverableException result = assertThrows(UnrecoverableException.class, () -> {			
			// Call under test
			iterator.next();
		});

		assertEquals(ex, result.getCause());
		
		verify(mockTableChangeIterator).next();
		verify(mockTableChange).getChangeType();
		verify(mockTableChange).loadChangeData(SparseChangeSet.class);
		
	}
	
	@Test
	public void testNextWithRowChangeAndServiceAmazonServiceException() throws NotFoundException, IOException {
		
		AmazonServiceException ex = new AmazonServiceException("Error");
		
		ex.setErrorType(ErrorType.Service);
		
		doThrow(ex).when(mockTableChange).loadChangeData(any());
		
		when(mockTableChange.getChangeType()).thenReturn(TableChangeType.ROW);
		when(mockTableChangeIterator.next()).thenReturn(mockTableChange);
		
		RecoverableException result = assertThrows(RecoverableException.class, () -> {			
			// Call under test
			iterator.next();
		});

		assertEquals(ex, result.getCause());
		
		verify(mockTableChangeIterator).next();
		verify(mockTableChange).getChangeType();
		verify(mockTableChange).loadChangeData(SparseChangeSet.class);
		
	}
	
}
