package org.sagebionetworks.repo.manager.file.scanner.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.file.scanner.ScannedFileHandleAssociation;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonServiceException.ErrorType;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.google.common.collect.ImmutableSet;

@ExtendWith(MockitoExtension.class)
public class TableFileHandleAssociationMapperTest {
	
	@Mock
	private TableEntityManager mockTableManager;
	
	@InjectMocks
	private TableFileHandleAssociationMapper mapper;
	
	@Mock
	private ResultSet mockResultSet;
	
	@Mock
	private SparseChangeSet mockChangeSet;
	
	private Long tableId;

	@BeforeEach
	public void before() {
		tableId = 1L;
	}
	
	@Test
	public void testMapRow() throws Exception {
		
		Set<Long> fileHandles = ImmutableSet.of(1L, 2L);
		
		when(mockResultSet.getLong(anyString())).thenReturn(tableId);
		when(mockResultSet.getString(anyString())).thenReturn("ROW");
		when(mockTableManager.getSparseChangeSet(any())).thenReturn(mockChangeSet);
		when(mockChangeSet.getFileHandleIdsInSparseChangeSet()).thenReturn(fileHandles);
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(tableId).withFileHandleIds(fileHandles);
		
		// Call under test
		ScannedFileHandleAssociation association = mapper.mapRow(mockResultSet, 0);
		
		assertEquals(expected, association);
	}
	
	@Test
	public void testMapRowWithIOException() throws Exception {
		
		IOException ex = new IOException("failed");
		
		when(mockResultSet.getLong(anyString())).thenReturn(tableId);
		when(mockResultSet.getString(anyString())).thenReturn("ROW");
		when(mockTableManager.getSparseChangeSet(any())).thenThrow(ex);
		
		IllegalStateException result = assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			mapper.mapRow(mockResultSet, 0);
		});
		
		assertEquals(ex, result.getCause());
	}
	
	@Test
	public void testMapRowWithNotFoundException() throws Exception {
		
		NotFoundException ex = new NotFoundException("failed");
		
		when(mockResultSet.getLong(anyString())).thenReturn(tableId);
		when(mockResultSet.getString(anyString())).thenReturn("ROW");
		when(mockTableManager.getSparseChangeSet(any())).thenThrow(ex);
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(tableId);
		
		// Call under test
		ScannedFileHandleAssociation association = mapper.mapRow(mockResultSet, 0);
		
		assertEquals(expected, association);
	}
	
	@Test
	public void testMapRowWithAmazonServiceExceptionAndS3NotFound() throws Exception {
		
		AmazonS3Exception ex = new AmazonS3Exception("failed");
		ex.setStatusCode(HttpStatus.SC_NOT_FOUND);
		
		when(mockResultSet.getLong(anyString())).thenReturn(tableId);
		when(mockResultSet.getString(anyString())).thenReturn("ROW");
		when(mockTableManager.getSparseChangeSet(any())).thenThrow(ex);
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(tableId);
		
		// Call under test
		ScannedFileHandleAssociation association = mapper.mapRow(mockResultSet, 0);
		
		assertEquals(expected, association);
	}
	
	@Test
	public void testMapRowWithAmazonServiceException() throws Exception {
		
		AmazonServiceException ex = new AmazonServiceException("fail");
		ex.setErrorType(ErrorType.Service);
		
		when(mockResultSet.getLong(anyString())).thenReturn(tableId);
		when(mockResultSet.getString(anyString())).thenReturn("ROW");
		when(mockTableManager.getSparseChangeSet(any())).thenThrow(ex);
		
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {			
			// Call under test
			mapper.mapRow(mockResultSet, 0);
		});
		
		assertEquals(ex, result.getCause());
	}
	
	@Test
	public void testMapRowWithAmazonServiceExceptionUnhandled() throws Exception {
		
		AmazonServiceException ex = new AmazonServiceException("fail");
		ex.setErrorType(ErrorType.Client);
		
		when(mockResultSet.getLong(anyString())).thenReturn(tableId);
		when(mockResultSet.getString(anyString())).thenReturn("ROW");
		when(mockTableManager.getSparseChangeSet(any())).thenThrow(ex);
		
		AmazonServiceException result = assertThrows(AmazonServiceException.class, () -> {			
			// Call under test
			mapper.mapRow(mockResultSet, 0);
		});
		
		assertEquals(ex, result);
	}

}
