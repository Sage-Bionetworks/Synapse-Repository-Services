package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.file.CsvFileHandleProvider;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.GridReplicaSupport;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowData;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridCsvImportRequest;
import org.sagebionetworks.repo.model.grid.GridCsvImportResponse;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.table.cluster.utils.CSVUtils;

import au.com.bytecode.opencsv.CSVReader;

@ExtendWith(MockitoExtension.class)
public class GridCsvImporterImplTest {
	
	@Mock
	private GridManager mockGridManager;
	@Mock
	private GridReplicaViewManager mockGridViewManager;
	@Mock
	private GridReplicaSupport mockGridReplicaSupport;
	@Mock
	private CsvFileHandleProvider mockCsvProvider;
	@Mock
	private GridCsvImportDao mockImportDao;
	@Mock
	private JoinedRowChangePublisher mockChangePublisher;
	@Mock
	private JsonSchemaManager mockJsonSchemaManager;

	@InjectMocks
	private GridCsvImporterImpl importer;

	private UserInfo user;
	private String sessionId = "sessionId";
	private CsvTableDescriptor descriptor;
	private GridCsvImportRequest request;
	private GridSession session;
	private GridConnectionInfo connectionInfo;
	private GridHeader gridHeader;
	private List<String> upsertKey;
	private RecordSet recordSet;
	private String csvContent;
	private List<RowView> gridRows;
	private List<JoinedRow> joinedRows;
	private GridCsvImportResponse response;
	
	@Mock
	private AsyncJobProgressCallback mockCallback;
	
	@Captor
	private ArgumentCaptor<DataStream> streamCaptor;
	
	@Captor
	private ArgumentCaptor<Iterator<JoinedRow>> joinedRowCaptor;
	
	@BeforeEach
	public void before() {
		user = new UserInfo(false, 123L, AuthorizationConstants.DEFAULT_REALM_ID);
		
		descriptor = new CsvTableDescriptor().setIsFirstLineHeader(true);
		
		request = new GridCsvImportRequest()
			.setSessionId(sessionId)
			.setFileHandleId("123")
			.setCsvDescriptor(descriptor)
			.setSchema( List.of(
				new ColumnModel().setName("a").setColumnType(ColumnType.INTEGER),
				new ColumnModel().setName("b").setColumnType(ColumnType.STRING),
				new ColumnModel().setName("c").setColumnType(ColumnType.BOOLEAN)
			));
		
		session = new GridSession()
			.setSessionId(sessionId)
			.setSourceEntityId("syn123");
		
		connectionInfo = new GridConnectionInfo()
			.setSessionId(sessionId);
		
		gridHeader = new GridHeader().setOrderedColumns(List.of(
			new Column().setName("a").setVectorIndex(0),
			new Column().setName("b").setVectorIndex(1),
			new Column().setName("c").setVectorIndex(2)
		));
		
		upsertKey = List.of("a");
		recordSet = new RecordSet()
			.setUpsertKey(upsertKey)
			.setDataFileHandleId("syn123");
		
		csvContent = 
			"a,b,c" + System.lineSeparator() +
			"0,0,false" + System.lineSeparator() +
			"1,1,false" + System.lineSeparator() + 
			"2,2,false" + System.lineSeparator();
		
		gridRows = List.of(
			new RowView().setRowObject(new RowObject().setData(new RowData()
					.setNodes(new ConstantNode[] {
						new ConstantNode().setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(100L)).setValue(new ConValue(ConType.LONG, 0)),
						new ConstantNode().setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(102L)).setValue(new ConValue(ConType.LONG, 1)),
						new ConstantNode().setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(103L)).setValue(new ConValue(ConType.BOOLEAN, true))
					}).setVectorId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(98L))
			)),
			new RowView().setRowObject(new RowObject().setData(new RowData()
					.setNodes(new ConstantNode[] {
						new ConstantNode().setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(104L)).setValue(new ConValue(ConType.LONG, 2)),
						new ConstantNode().setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(105L)).setValue(new ConValue(ConType.LONG, 3)),
						new ConstantNode().setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(106L)).setValue(new ConValue(ConType.BOOLEAN, true))
					}).setVectorId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(99L)))
			)
		);
		
		joinedRows = List.of(
			new JoinedRow(List.of(new ConValue(ConType.LONG, 0), new ConValue(ConType.LONG, 0), new ConValue(ConType.BOOLEAN, false)), new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(1L)),
			new JoinedRow(List.of(new ConValue(ConType.LONG, 1), new ConValue(ConType.LONG, 1), new ConValue(ConType.BOOLEAN, false)), null),
			new JoinedRow(List.of(new ConValue(ConType.LONG, 2), new ConValue(ConType.LONG, 2), new ConValue(ConType.BOOLEAN, false)), new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(2L))
		);
		
		response = new GridCsvImportResponse()
			.setSessionId(sessionId)
			.setTotalCount(3L)
			.setUpdatedCount(2L)
			.setCreatedCount(1L);
	}
	
	@Test
	public void testImportCsv() {
		setupFullMocks();
				
		// Call under test
		GridCsvImportResponse result = importer.importCsv(user, request, mockCallback);
		
		assertEquals(response, result);
		
		ColumnMapping[] expectedMapping = new ColumnMapping[] {
			new ColumnMapping("a", ColumnType.INTEGER, 0, 0, true),
			new ColumnMapping("b", ColumnType.STRING, 1, 1, false),
			new ColumnMapping("c", ColumnType.BOOLEAN, 2, 2, false)
		};
		
		verifyImportSequence(expectedMapping);
	}
	
	@Test
	public void testImportCsvWithDifferentGridOrder() {
		
		gridHeader = new GridHeader().setOrderedColumns(List.of(
			new Column().setName("b").setVectorIndex(0),
			new Column().setName("a").setVectorIndex(1),
			new Column().setName("c").setVectorIndex(2)
		));
		
		setupFullMocks();
				
		// Call under test
		GridCsvImportResponse result = importer.importCsv(user, request, mockCallback);
		
		assertEquals(response, result);
		
		ColumnMapping[] expectedMapping = new ColumnMapping[] {
			new ColumnMapping("a", ColumnType.INTEGER, 0, 1, true),
			new ColumnMapping("b", ColumnType.STRING, 1, 0, false),
			new ColumnMapping("c", ColumnType.BOOLEAN, 2, 2, false)
		};
		
		verifyImportSequence(expectedMapping);
	}
	
	@Test
	public void testImportCsvWithDifferentUpsertKeyOrder() {
		recordSet.setUpsertKey(List.of("b", "a"));
		
		setupFullMocks();
				
		// Call under test
		GridCsvImportResponse result = importer.importCsv(user, request, mockCallback);
		
		assertEquals(response, result);
		
		ColumnMapping[] expectedMapping = new ColumnMapping[] {
			new ColumnMapping("b", ColumnType.STRING, 1, 1, true),
			new ColumnMapping("a", ColumnType.INTEGER, 0, 0, true),
			new ColumnMapping("c", ColumnType.BOOLEAN, 2, 2, false)
		};
		
		verifyImportSequence(expectedMapping);
	}
	
	@Test
	public void testImportCsvWithNonExistingGridColumn() {
		csvContent = 
			"a,b,c,d" + System.lineSeparator() +
			"0,0,false,0.1" + System.lineSeparator() +
			"1,1,false,0.2" + System.lineSeparator() + 
			"2,2,false,0.3" + System.lineSeparator();
		
		request.setSchema(List.of(
			new ColumnModel().setName("a").setColumnType(ColumnType.INTEGER),
			new ColumnModel().setName("b").setColumnType(ColumnType.STRING),
			new ColumnModel().setName("c").setColumnType(ColumnType.BOOLEAN),
			// Extra column not in the grid
			new ColumnModel().setName("d").setColumnType(ColumnType.DOUBLE)
		));		
		
		setupFullMocks();
				
		// Call under test
		GridCsvImportResponse result = importer.importCsv(user, request, mockCallback);
		
		assertEquals(response, result);
		
		ColumnMapping[] expectedMapping = new ColumnMapping[] {
			new ColumnMapping("a", ColumnType.INTEGER, 0, 0, true),
			new ColumnMapping("b", ColumnType.STRING, 1, 1, false),
			new ColumnMapping("c", ColumnType.BOOLEAN, 2, 2, false)
		};
		
		verifyImportSequence(expectedMapping);
	}
	
	@Test
	public void testImportCsvWithEmptyCsv() {
		csvContent = "" + System.lineSeparator();
		
		when(mockGridManager.getGridSession(user, sessionId)).thenReturn(session);
		when(mockGridReplicaSupport.getGridHeaderOrThrow(session)).thenReturn(gridHeader);
		when(mockGridManager.getSingletonConnection(sessionId, EventSource.USER_SUPPORT)).thenReturn(Optional.of(connectionInfo));
		when(mockGridReplicaSupport.getRecordSetOrThrow(user, session)).thenReturn(recordSet);
		when(mockCsvProvider.getCsvReader(user, request.getFileHandleId(), descriptor)).thenReturn(csvReader(csvContent));
		
		assertEquals("The CSV file cannot be empty.", assertThrows(IllegalArgumentException.class, () -> {	
			// Call under test
			importer.importCsv(user, request, mockCallback);
		}).getMessage());
		
		verify(mockImportDao).dropTemporaryTables(sessionId);
		
		verifyNoMoreInteractions(mockImportDao);
	}
	
	@Test
	public void testImportCsvWithMismatchingSchemaSize() {
		request.setSchema(List.of(
			new ColumnModel().setName("a").setColumnType(ColumnType.INTEGER),
			new ColumnModel().setName("b").setColumnType(ColumnType.STRING)
			// Missing column "c"
		));
		
		when(mockGridManager.getGridSession(user, sessionId)).thenReturn(session);
		when(mockGridReplicaSupport.getGridHeaderOrThrow(session)).thenReturn(gridHeader);
		when(mockGridManager.getSingletonConnection(sessionId, EventSource.USER_SUPPORT)).thenReturn(Optional.of(connectionInfo));
		when(mockGridReplicaSupport.getRecordSetOrThrow(user, session)).thenReturn(recordSet);
		when(mockCsvProvider.getCsvReader(user, request.getFileHandleId(), descriptor)).thenReturn(csvReader(csvContent));
		
		assertEquals("The CSV header does not match the schema size.", assertThrows(IllegalArgumentException.class, () -> {	
			// Call under test
			importer.importCsv(user, request, mockCallback);
		}).getMessage());
		
		verify(mockImportDao).dropTemporaryTables(sessionId);
		
		verifyNoMoreInteractions(mockImportDao);
	}

	@Test
	public void testImportCsvWithMismatchingSchemaColumns() {
		request.setSchema(List.of(
			new ColumnModel().setName("a").setColumnType(ColumnType.INTEGER),
			new ColumnModel().setName("b").setColumnType(ColumnType.STRING),
			// Different column "d" instead of "c"
			new ColumnModel().setName("d").setColumnType(ColumnType.STRING)
		));
		
		when(mockGridManager.getGridSession(user, sessionId)).thenReturn(session);
		when(mockGridReplicaSupport.getGridHeaderOrThrow(session)).thenReturn(gridHeader);
		when(mockGridManager.getSingletonConnection(sessionId, EventSource.USER_SUPPORT)).thenReturn(Optional.of(connectionInfo));
		when(mockGridReplicaSupport.getRecordSetOrThrow(user, session)).thenReturn(recordSet);
		when(mockCsvProvider.getCsvReader(user, request.getFileHandleId(), descriptor)).thenReturn(csvReader(csvContent));
		
		assertEquals("The CSV header column \"c\" does not match the schema column \"d\" at index: 2", assertThrows(IllegalArgumentException.class, () -> {	
			// Call under test
			importer.importCsv(user, request, mockCallback);
		}).getMessage());
		
		verify(mockImportDao).dropTemporaryTables(sessionId);
		
		verifyNoMoreInteractions(mockImportDao);
	}
	
	@Test
	public void testImportCsvWithUpsertKeyNotInCsvSchema() {
		recordSet.setUpsertKey(List.of("d"));
		
		when(mockGridManager.getGridSession(user, sessionId)).thenReturn(session);
		when(mockGridReplicaSupport.getGridHeaderOrThrow(session)).thenReturn(gridHeader);
		when(mockGridManager.getSingletonConnection(sessionId, EventSource.USER_SUPPORT)).thenReturn(Optional.of(connectionInfo));
		when(mockGridReplicaSupport.getRecordSetOrThrow(user, session)).thenReturn(recordSet);
		when(mockCsvProvider.getCsvReader(user, request.getFileHandleId(), descriptor)).thenReturn(csvReader(csvContent));
		
		assertEquals("The upsert key column \"d\" does not exist in the CSV schema.", assertThrows(IllegalArgumentException.class, () -> {	
			// Call under test
			importer.importCsv(user, request, mockCallback);
		}).getMessage());
		
		verify(mockImportDao).dropTemporaryTables(sessionId);
		
		verifyNoMoreInteractions(mockImportDao);
	}
	
	@Test
	public void testImportCsvWithUpsertKeyNotInGridSchema() {
		gridHeader = new GridHeader().setOrderedColumns(List.of(
			new Column().setName("b").setVectorIndex(0),
			new Column().setName("c").setVectorIndex(1),
			new Column().setName("d").setVectorIndex(2)
		));
		
		when(mockGridManager.getGridSession(user, sessionId)).thenReturn(session);
		when(mockGridReplicaSupport.getGridHeaderOrThrow(session)).thenReturn(gridHeader);
		when(mockGridManager.getSingletonConnection(sessionId, EventSource.USER_SUPPORT)).thenReturn(Optional.of(connectionInfo));
		when(mockGridReplicaSupport.getRecordSetOrThrow(user, session)).thenReturn(recordSet);
		when(mockCsvProvider.getCsvReader(user, request.getFileHandleId(), descriptor)).thenReturn(csvReader(csvContent));
		
		assertEquals("The upsert key column \"a\" does not exist in the grid schema.", assertThrows(IllegalArgumentException.class, () -> {	
			// Call under test
			importer.importCsv(user, request, mockCallback);
		}).getMessage());
		
		verify(mockImportDao).dropTemporaryTables(sessionId);
		
		verifyNoMoreInteractions(mockImportDao);
	}
	
	private void setupFullMocks() {
		when(mockGridManager.getGridSession(user, sessionId)).thenReturn(session);
		when(mockGridReplicaSupport.getGridHeaderOrThrow(session)).thenReturn(gridHeader);
		when(mockGridManager.getSingletonConnection(sessionId, EventSource.USER_SUPPORT)).thenReturn(Optional.of(connectionInfo));
		when(mockGridReplicaSupport.getRecordSetOrThrow(user, session)).thenReturn(recordSet);
		when(mockCsvProvider.getCsvReader(user, request.getFileHandleId(), descriptor)).thenReturn(csvReader(csvContent));
		when(mockGridViewManager.getQueryIterator(gridHeader, Collections.emptyList())).thenReturn(gridRows.iterator());
		when(mockImportDao.getJoinedTempTableIterator(eq(sessionId), any())).thenReturn(joinedRows.iterator());
		when(mockChangePublisher.processJoinedRows(eq(gridHeader), eq(connectionInfo), any(), any())).thenReturn(response);
	}

	private void verifyImportSequence(ColumnMapping[] expectedMapping) {
		verify(mockImportDao).streamToCsvTempTable(eq(sessionId), streamCaptor.capture(), eq(expectedMapping));
		
		assertTrue(streamCaptor.getValue() instanceof CsvDataStream);

		verify(mockImportDao).streamToGridTempTable(eq(sessionId), streamCaptor.capture(), eq(expectedMapping));

		assertTrue(streamCaptor.getValue() instanceof GridDataStream);
		
		verify(mockImportDao).getJoinedTempTableIterator(sessionId, expectedMapping);
	
		verify(mockChangePublisher).processJoinedRows(eq(gridHeader), eq(connectionInfo), joinedRowCaptor.capture(), eq(expectedMapping));
		
		List<JoinedRow> capturedJoinedRows = new ArrayList<>();
		
		joinedRowCaptor.getValue().forEachRemaining(capturedJoinedRows::add);

		assertEquals(joinedRows, capturedJoinedRows);
		
		verify(mockImportDao).dropTemporaryTables(sessionId);
	}
	
	private CSVReader csvReader(String csvContent) {
		return CSVUtils.createCSVReader(new InputStreamReader(
			new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8
			), descriptor, null);
	}

}
