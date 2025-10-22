package org.sagebionetworks.repo.manager.grid.internal.replica.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.GridReplicaSupport;
import org.sagebionetworks.repo.manager.grid.internal.replica.export.GridRecordSetExporterImpl.ValidationSummaryBuilder;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.dbo.schema.EntitySchemaValidationResultDao;
import org.sagebionetworks.repo.model.grid.DownloadFromGridRequest;
import org.sagebionetworks.repo.model.grid.DownloadFromGridResult;
import org.sagebionetworks.repo.model.grid.GridRecordSetExportRequest;
import org.sagebionetworks.repo.model.grid.GridRecordSetExportResponse;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.schema.ValidationSummaryStatistics;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.service.EntityService;

@ExtendWith(MockitoExtension.class)
public class GridRecordSetExporterImplTest {

	@Mock
	private GridManager mockGridManager;
	@Mock
	private GridReplicaSupport mockGridReplicaSupport;
	@Mock
	private GridReplicaCsvExporter mockCsvExporter;
	@Mock
	private EntityService mockEntityService;
	@Mock
	private EntitySchemaValidationResultDao mockValidationResultDao;
	
	@InjectMocks
	private GridRecordSetExporterImpl exporter;

	@Mock
	private GridSession mockGridSession;
	@Mock
	private AsyncJobProgressCallback mockJobCallback;
	@Mock
	private RowView mockRow;
	
	private UserInfo user;
	private Long userId = 101L;
	
	private String sessionId = "123";
	private String recordSetId = "syn456";
	private String fileHandleId = "789";
	
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
		
		when(mockRow.getRowValidationResults()).thenReturn(
			new ValidationResults().setIsValid(true),
			new ValidationResults().setIsValid(false),
			null
		);
		
		DownloadFromGridRequest expectdDownloadRequest = new DownloadFromGridRequest()
			.setSessionId(sessionId)
			.setWriteHeader(true)
			.setIncludeEtag(false)
			.setIncludeRowIdAndRowVersion(false)
			.setCsvTableDescriptor(csvDescriptor);
		
		when(mockCsvExporter.exportGridAsCsv(eq(user), eq(expectdDownloadRequest), eq(mockJobCallback), any(ValidationSummaryBuilder.class)))
			.then(invocation -> {
				// Simulate three rows being processed
				RowViewCallbackHandler rowCallback = invocation.getArgument(3);
				
				rowCallback.next(mockRow);
				rowCallback.next(mockRow);
				rowCallback.next(mockRow);
				
				return new DownloadFromGridResult()
					.setResultsFileHandleId(fileHandleId);
			});
		
		when(mockEntityService.updateEntity(userId, recordSet, true, null))
			.then(invocation -> {
				// Simulate version increment performed by the service
				recordSet.setVersionNumber(2L);
				return recordSet;
			});

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
			);
		
		// Call under test
		GridRecordSetExportResponse response = exporter.exportGrid(user, request, mockJobCallback);
		
		assertNotNull(response.getValidationSummaryStatistics().getGeneratedOn());
		
		expectedResponse.getValidationSummaryStatistics().setGeneratedOn(response.getValidationSummaryStatistics().getGeneratedOn());

		assertEquals(expectedResponse, response);
		
		assertEquals(2L, recordSet.getVersionNumber());
		
		verify(mockValidationResultDao).setRecordSetValidationSummaryStatistics(
			KeyFactory.stringToKey(recordSetId), 2L, expectedResponse.getValidationSummaryStatistics()
		);
	}

	@Test
	public void testExportGridWithIOException() throws Exception {
		when(mockGridManager.getGridSession(user, sessionId)).thenReturn(mockGridSession);
		when(mockGridReplicaSupport.getRecordSetOrThrow(user, mockGridSession)).thenReturn(recordSet);
		when(mockCsvExporter.exportGridAsCsv(any(), any(), any(), any()))		
			.thenThrow(new IOException("nope"));

		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
			exporter.exportGrid(user, request, mockJobCallback);
		});
		
		assertEquals("Could not export the grid to a CSV file.", ex.getMessage());
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
