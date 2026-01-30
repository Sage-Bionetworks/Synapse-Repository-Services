package org.sagebionetworks.repo.manager.grid.internal.replica.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.GridReplicaSupport;
import org.sagebionetworks.repo.manager.grid.internal.replica.export.GridRecordSetExporterImpl.ValidationSummaryBuilder;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.dbo.schema.EntitySchemaValidationResultDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.grid.DownloadFromGridRequest;
import org.sagebionetworks.repo.model.grid.DownloadFromGridResult;
import org.sagebionetworks.repo.model.grid.GridRecordSetExportRequest;
import org.sagebionetworks.repo.model.grid.GridRecordSetExportResponse;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.schema.ValidationSummaryStatistics;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.util.csv.CSVWriterProvider;

import au.com.bytecode.opencsv.CSVWriter;

@ExtendWith(MockitoExtension.class)
public class GridRecordSetExporterImplTest {

	@Mock
	private GridManager mockGridManager;
	@Mock
	private GridReplicaSupport mockGridReplicaSupport;
	@Mock
	private GridReplicaCsvExporter mockCsvExporter;
	@Mock
	private EntityManager mockEntityManager;
	@Mock
	private EntitySchemaValidationResultDao mockValidationResultDao;
	@Mock
	private CSVWriterProvider mockCsvWriterProvider;
	@Mock
    private FileHandleManager mockFileHandleManager;
	
	@InjectMocks
	private GridRecordSetExporterImpl exporter;

	@Mock
	private GridSession mockGridSession;
	@Mock
	private AsyncJobProgressCallback mockJobCallback;
	@Mock
	private RowView mockRow;
	@Mock
	private CSVWriter mockCsvWriter;
	
	private UserInfo user;
	private Long userId = 101L;
	
	private String sessionId = "123";
	private String recordSetId = "syn456";
	private String fileHandleId = "789";
	private String validationFileHandleId = "987";
	
	private RecordSet recordSet;
	private CsvTableDescriptor csvDescriptor;
	private GridRecordSetExportRequest request;

	@BeforeEach
	public void setup() {
		user = new UserInfo(false, userId);
		
		csvDescriptor = new CsvTableDescriptor();

		recordSet = new RecordSet()
			.setId(recordSetId)
			.setVersionNumber(1L)
			.setCsvDescriptor(csvDescriptor);
		
		request = new GridRecordSetExportRequest()
			.setSessionId(sessionId);
	}

	@Test
	public void testExportGrid() throws Exception {
		when(mockGridSession.getSessionId()).thenReturn(sessionId);
		when(mockGridManager.getGridSession(user, sessionId)).thenReturn(mockGridSession);
		when(mockGridReplicaSupport.getRecordSetOrThrow(user, mockGridSession)).thenReturn(recordSet);
		when(mockCsvWriterProvider.createWriter(any(), any())).thenReturn(mockCsvWriter);
		
		when(mockRow.getRowValidationResults()).thenReturn(
			new ValidationResults().setIsValid(true),
			new ValidationResults().setIsValid(false).setValidationErrorMessage("error").setAllValidationMessages(List.of("err1", "err2")),
			null
		);
		
		DownloadFromGridRequest expectedDownloadRequest = new DownloadFromGridRequest()
			.setSessionId(sessionId)
			.setWriteHeader(true)
			.setIncludeEtag(false)
			.setIncludeRowIdAndRowVersion(false)
			.setCsvTableDescriptor(csvDescriptor);
		
		when(mockCsvExporter.exportGridAsCsv(eq(user), eq(expectedDownloadRequest), eq(mockJobCallback), any(ValidationSummaryBuilder.class)))
			.then(invocation -> {
				// Simulate three rows being processed
				RowViewCallbackHandler rowCallback = invocation.getArgument(3);
				
				rowCallback.next(mockRow);
				rowCallback.next(mockRow);
				rowCallback.next(mockRow);
				
				return new DownloadFromGridResult()
					.setResultsFileHandleId(fileHandleId);
			});
		
		ArgumentCaptor<LocalFileUploadRequest> uploadRequestCaptor = ArgumentCaptor.forClass(LocalFileUploadRequest.class);
		
		when(mockFileHandleManager.uploadLocalFile(uploadRequestCaptor.capture())).thenReturn(new S3FileHandle().setId(validationFileHandleId));
		
		when(mockEntityManager.updateEntity(user, recordSet, true, null))
			.then(invocation -> {
				// Simulate version increment performed by the service
				recordSet.setVersionNumber(2L);
				return true;
			});
		
		when(mockEntityManager.getEntity(user, recordSetId, RecordSet.class)).thenReturn(recordSet);

		GridRecordSetExportResponse expectedResponse = new GridRecordSetExportResponse()
			.setSessionId(sessionId)
			.setRecordSetId(recordSetId)
			.setRecordSetVersionNumber(2L)
			.setValidationSummaryStatistics(new ValidationSummaryStatistics()
				.setContainerId(recordSetId)
				.setTotalNumberOfChildren(3L)
				.setNumberOfValidChildren(1L)
				.setNumberOfInvalidChildren(1L)
				.setNumberOfUnknownChildren(1L)
			)
			.setValidationFileHandleId(validationFileHandleId);
		
		// Call under test
		GridRecordSetExportResponse response = exporter.exportGrid(user, request, mockJobCallback);
		
		assertNotNull(response.getValidationSummaryStatistics().getGeneratedOn());
		
		expectedResponse.getValidationSummaryStatistics().setGeneratedOn(response.getValidationSummaryStatistics().getGeneratedOn());

		assertEquals(expectedResponse, response);
		
		assertEquals(2L, recordSet.getVersionNumber());
		
		LocalFileUploadRequest uploadRequest = uploadRequestCaptor.getValue();
		
		assertEquals(user.getId().toString(), uploadRequest.getUserId());
		assertEquals("grid_validation_details.csv", uploadRequest.getFileName());
		assertEquals("text/csv", uploadRequest.getContentType());
		assertNotNull(uploadRequest.getFileToUpload());
		
		verify(mockValidationResultDao).setRecordSetValidationSummaryStatistics(
			KeyFactory.stringToKey(recordSetId), 2L, expectedResponse.getValidationSummaryStatistics()
		);
		
		verify(mockCsvWriter).writeNext(new String[] {"row_index", "is_valid", "validation_error_message", "all_validation_messages"});
		verify(mockCsvWriter).writeNext(new String[] {"0", "true", null, null});
		verify(mockCsvWriter).writeNext(new String[] {"1", "false", "error", "[\"err1\",\"err2\"]"});
		verify(mockCsvWriter).writeNext(new String[] {"2", null, null, null});
		verify(mockCsvWriter).close();
		verifyNoMoreInteractions(mockCsvWriter);
	}

	@Test
	public void testExportGridWithIOExceptionDuringExport() throws Exception {
		when(mockGridManager.getGridSession(user, sessionId)).thenReturn(mockGridSession);
		when(mockGridReplicaSupport.getRecordSetOrThrow(user, mockGridSession)).thenReturn(recordSet);
		when(mockCsvWriterProvider.createWriter(any(), any())).thenReturn(mockCsvWriter);
		when(mockCsvExporter.exportGridAsCsv(any(), any(), any(), any()))		
			.thenThrow(new IOException("nope"));

		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
			// Call under test
			exporter.exportGrid(user, request, mockJobCallback);
		});
		
		assertEquals("Could not export the grid to a CSV file.", ex.getMessage());
	}
	
	@Test
	public void testExportGridWithIOExceptionDuringValidationExport() throws Exception {
		when(mockGridManager.getGridSession(user, sessionId)).thenReturn(mockGridSession);
		when(mockGridReplicaSupport.getRecordSetOrThrow(user, mockGridSession)).thenReturn(recordSet);
		when(mockCsvWriterProvider.createWriter(any(), any())).thenReturn(mockCsvWriter);
		
		when(mockCsvExporter.exportGridAsCsv(eq(user), any(), eq(mockJobCallback), any(ValidationSummaryBuilder.class)))
			.then(invocation -> {
				// Simulate rows being processed
				RowViewCallbackHandler rowCallback = invocation.getArgument(3);
				
				rowCallback.next(mockRow);
				
				return new DownloadFromGridResult()
					.setResultsFileHandleId(fileHandleId);
			});
		
		// The first writeNext is for the header, throw on the next call
		doNothing().doThrow(new IOException("nope")).when(mockCsvWriter).writeNext(any());
		
		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
			// Call under test
			exporter.exportGrid(user, request, mockJobCallback);
		});
		
		assertEquals("Could not write validation details to CSV file.", ex.getMessage());
	}

	@Test
	public void testExportGridWithNullUser() {
		assertThrows(IllegalArgumentException.class, () -> exporter.exportGrid(null, request, mockJobCallback));
	}

	@Test
	public void testExportGridWithNullRequest() {
		assertThrows(IllegalArgumentException.class, () -> exporter.exportGrid(user, null, mockJobCallback));
	}

	@Test
	public void testExportGridWithNullSessionIdInRequest() {
		request.setSessionId(null);
		assertThrows(IllegalArgumentException.class, () -> exporter.exportGrid(user, request, mockJobCallback));
	}
}
