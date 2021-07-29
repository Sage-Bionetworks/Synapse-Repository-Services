package org.sagebionetworks.repo.manager.file.scanner.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.file.scanner.ScannedFileHandleAssociation;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.model.IdRange;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonServiceException.ErrorType;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.google.common.collect.ImmutableSet;

@ExtendWith(MockitoExtension.class)
public class TableFileHandleScannerTest {
	
	@Mock
	private TableEntityManager mockTableManager;
	
	@InjectMocks
	private TableFileHandleScanner scanner;
	
	@Mock
	private TableRowChange mockRowChange;
	
	@Mock
	private SparseChangeSet mockChangeSet;
	
	private Long tableId;

	@BeforeEach
	public void before() {
		tableId = 1L;
	}
	
	@Test
	public void testGetIdRange() {
		
		IdRange expected = new IdRange(0, 10);
		
		when(mockTableManager.getTableRowChangeIdRange()).thenReturn(expected);
		
		// Call under test
		IdRange result = scanner.getIdRange();
		
		assertEquals(expected, result);
		
		verify(mockTableManager).getTableRowChangeIdRange();
	}
	
	@Test
	public void testScanRange() throws Exception {
		
		IdRange idRange = new IdRange(0, 10);
		
		Set<Long> fileHandles = ImmutableSet.of(1L, 2L);
		
		when(mockRowChange.getTableId()).thenReturn(tableId.toString());
		when(mockTableManager.getSparseChangeSet(any())).thenReturn(mockChangeSet);
		when(mockChangeSet.getFileHandleIdsInSparseChangeSet()).thenReturn(fileHandles);
		when(mockTableManager.newTableRowChangeWithFileRefsIterator(any())).thenReturn(Arrays.asList(mockRowChange).iterator());
		
		// Call under test
		Iterable<ScannedFileHandleAssociation> result = scanner.scanRange(idRange);
		
		assertNotNull(result.iterator());
		
		verify(mockTableManager).newTableRowChangeWithFileRefsIterator(idRange);
		
		// At this point we didn't consume the iterable, so no other interactions should happen with the manager
		verifyNoMoreInteractions(mockTableManager);
		
		List<ScannedFileHandleAssociation> expected = Arrays.asList(
			new ScannedFileHandleAssociation(tableId).withFileHandleIds(fileHandles)
		);
		
		// Consume the iterator
		List<ScannedFileHandleAssociation> resultList = StreamSupport.stream(result.spliterator(), false).collect(Collectors.toList());
		
		assertEquals(expected, resultList);
		
		verify(mockTableManager).getSparseChangeSet(mockRowChange);
	}
	
	@Test
	public void testMapRowChange() throws Exception {
		
		Set<Long> fileHandles = ImmutableSet.of(1L, 2L);
		
		when(mockRowChange.getTableId()).thenReturn(tableId.toString());
		when(mockTableManager.getSparseChangeSet(any())).thenReturn(mockChangeSet);
		when(mockChangeSet.getFileHandleIdsInSparseChangeSet()).thenReturn(fileHandles);
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(tableId).withFileHandleIds(fileHandles);
		
		// Call under test
		ScannedFileHandleAssociation association = scanner.mapTableRowChange(mockRowChange);
		
		assertEquals(expected, association);
		
		verify(mockTableManager).getSparseChangeSet(mockRowChange);
	}
	
	@Test
	public void testMapRowChangeWithIOException() throws Exception {
		
		IOException ex = new IOException("failed");
		
		when(mockRowChange.getTableId()).thenReturn(tableId.toString());
		when(mockTableManager.getSparseChangeSet(any())).thenThrow(ex);
		
		IllegalStateException result = assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			scanner.mapTableRowChange(mockRowChange);
		});
		
		assertEquals(ex, result.getCause());
		
		verify(mockTableManager).getSparseChangeSet(mockRowChange);
	}
	
	@Test
	public void testMapRowChangeWithNotFoundException() throws Exception {
		
		NotFoundException ex = new NotFoundException("failed");
		
		when(mockRowChange.getTableId()).thenReturn(tableId.toString());
		when(mockTableManager.getSparseChangeSet(any())).thenThrow(ex);
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(tableId);
		
		// Call under test
		ScannedFileHandleAssociation association = scanner.mapTableRowChange(mockRowChange);
		
		assertEquals(expected, association);
		
		verify(mockTableManager).getSparseChangeSet(mockRowChange);
	}
	
	@Test
	public void testMapRowChangeWithAmazonServiceExceptionAndS3NotFound() throws Exception {
		
		AmazonS3Exception ex = new AmazonS3Exception("failed");
		ex.setStatusCode(HttpStatus.SC_NOT_FOUND);
		
		when(mockRowChange.getTableId()).thenReturn(tableId.toString());
		when(mockTableManager.getSparseChangeSet(any())).thenThrow(ex);
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(tableId);
		
		// Call under test
		ScannedFileHandleAssociation association = scanner.mapTableRowChange(mockRowChange);
		
		assertEquals(expected, association);
		
		verify(mockTableManager).getSparseChangeSet(mockRowChange);
	}
	
	@Test
	public void testMapRowChangeWithAmazonServiceException() throws Exception {
		
		AmazonServiceException ex = new AmazonServiceException("fail");
		ex.setErrorType(ErrorType.Service);
		
		when(mockRowChange.getTableId()).thenReturn(tableId.toString());
		when(mockTableManager.getSparseChangeSet(any())).thenThrow(ex);
		
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {			
			// Call under test
			scanner.mapTableRowChange(mockRowChange);
		});
		
		assertEquals(ex, result.getCause());
		
		verify(mockTableManager).getSparseChangeSet(mockRowChange);
	}
	
	@Test
	public void testMapRowChangeWithAmazonServiceExceptionUnhandled() throws Exception {
		
		AmazonServiceException ex = new AmazonServiceException("fail");
		ex.setErrorType(ErrorType.Client);
		
		when(mockRowChange.getTableId()).thenReturn(tableId.toString());
		when(mockTableManager.getSparseChangeSet(any())).thenThrow(ex);
		
		AmazonServiceException result = assertThrows(AmazonServiceException.class, () -> {			
			// Call under test
			scanner.mapTableRowChange(mockRowChange);
		});
		
		assertEquals(ex, result);
		
		verify(mockTableManager).getSparseChangeSet(mockRowChange);
	}

}
