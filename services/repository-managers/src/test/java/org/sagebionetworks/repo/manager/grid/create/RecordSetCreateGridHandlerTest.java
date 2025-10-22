package org.sagebionetworks.repo.manager.grid.create;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.file.CsvFileHandleProvider;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.grid.PatchRowHandler;
import org.sagebionetworks.repo.manager.grid.PatchStore;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.dbo.grid.CreateGridSession;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.GridUtils;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.compact.PatchCompactSerializable;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;

import au.com.bytecode.opencsv.CSVReader;

@ExtendWith(MockitoExtension.class)
public class RecordSetCreateGridHandlerTest {

	@Mock
	private GridDao mockGridDao;

	@Mock
	private UserInfo mockUser;

	@Mock
	private AsyncJobProgressCallback mockCallback;

	@Mock
	private PatchStore mockPatchStore;

	@Mock
	private EntityManager mockEntityManager;

	@Mock
	private FileHandleManager mockFileHandleManager;
	
	@Mock
	private EntityAuthorizationManager mockAuthorizationManager;
	
	@Mock
	private CsvFileHandleProvider mockCsvProvider;

	@Captor
	private ArgumentCaptor<String> patchCaptor;

	@Spy
	@InjectMocks
	RecordSetCreateGridHandler handler;

	private Long userId;
	private String gridSessionId;
	private Long gridSessionIdLong;
	private boolean isAgent;
	private Long replicaId;
	private GridReplica replica;
	private String schema$id;
	private GridSession gridSession;

	private S3FileHandle csvFile;
	private CsvTableDescriptor csvDescriptor;
	private List<ColumnModel> csvSchema;
	private RecordSet recordSet;

	@Mock
	private CSVReader mockCsvReader;

	@Mock
	private PatchRowHandler mockRowHandler;

	@BeforeEach
	public void before() {
		userId = 123L;
		gridSessionIdLong = 456L;
		gridSessionId = GridUtils.gridSessionIdAsString(gridSessionIdLong);
		isAgent = false;

		replicaId = 88L;
		replica = new GridReplica().setReplicaId(replicaId);
		schema$id = "someorg-somename";

		gridSession = new GridSession();

		csvFile = new S3FileHandle().setId("789").setBucketName("someBucket").setKey("someKey");

		csvSchema = List.of(new ColumnModel().setColumnType(ColumnType.INTEGER).setName("foo"),
				new ColumnModel().setColumnType(ColumnType.STRING).setName("bar").setMaximumSize(50L));

		csvDescriptor = new CsvTableDescriptor().setIsFirstLineHeader(true).setQuoteCharacter("'");

		recordSet = new RecordSet().setId("syn456").setDataFileHandleId(csvFile.getId())
				.setCsvDescriptor(csvDescriptor);
	}
	
	@Test
	public void testCanCreate() {
		assertFalse(handler.canCreate(new CreateGridRequest()));
		assertTrue(handler.canCreate(new CreateGridRequest().setRecordSetId("syn123")));
		assertFalse(handler.canCreate(new CreateGridRequest().setInitialQuery(new Query())));
	}

	@Test
	public void testBuildSessionFromRecordSet() throws IOException {
		when(mockUser.getId()).thenReturn(userId);
		when(mockEntityManager.getEntity(mockUser, recordSet.getId(), RecordSet.class)).thenReturn(recordSet);
		when(mockAuthorizationManager.hasAccess(mockUser, recordSet.getId(), ACCESS_TYPE.DOWNLOAD)).thenReturn(AuthorizationStatus.authorized());
		when(mockEntityManager.findBoundSchema(recordSet.getId())).thenReturn(Optional.of(
				new JsonSchemaObjectBinding().setJsonSchemaVersionInfo(new JsonSchemaVersionInfo().set$id(schema$id))));

		gridSession = new GridSession().setSessionId(gridSessionId);

		when(mockGridDao.createGridSession(
				new CreateGridSession().setUserId(userId).setSourceId(recordSet.getId()).setSchemaId(schema$id)))
				.thenReturn(gridSession);

		when(mockGridDao.createReplica(userId, gridSessionId, isAgent, EventSource.INTERNAL)).thenReturn(replica);

		when(mockFileHandleManager.getRawFileHandleUnchecked(recordSet.getDataFileHandleId())).thenReturn(csvFile);

		Long maxRowSize = (long) TableModelUtils.calculateMaxRowSize(csvSchema);

		doReturn(csvSchema).when(handler).getSchemaFromCsv(csvFile, csvDescriptor);
		doReturn(mockCsvReader).when(mockCsvProvider).getCsvReader(csvFile, csvDescriptor);
		doReturn(mockRowHandler).when(handler).getPatchRowHandler(mockPatchStore, gridSession, replica, csvSchema,
				maxRowSize);

		when(mockCsvReader.readNext()).thenReturn(new String[] { "foo", "bar" }, new String[] { "1", "one" },
				new String[] { "2", "two" }, new String[] { null, "three" }, null);

		// Call under test
		handler.createGrid(mockCallback, mockUser, new CreateGridRequest().setRecordSetId(recordSet.getId()),
				mockPatchStore);

		verify(mockCsvReader, times(5)).readNext();
		verify(mockCsvReader).close();

		verify(mockRowHandler).nextRow(new Row().setValues(Arrays.asList("1", "one")));
		verify(mockRowHandler).nextRow(new Row().setValues(Arrays.asList("2", "two")));
		verify(mockRowHandler).nextRow(new Row().setValues(Arrays.asList(null, "three")));
		verify(mockRowHandler).close();

		verifyNoMoreInteractions(mockCsvReader, mockRowHandler);

	}

	@Test
	public void testBuildSessionFromRecordSetWithNoValidationSchema() throws IOException {
		when(mockUser.getId()).thenReturn(userId);
		when(mockEntityManager.getEntity(mockUser, recordSet.getId(), RecordSet.class)).thenReturn(recordSet);
		when(mockAuthorizationManager.hasAccess(mockUser, recordSet.getId(), ACCESS_TYPE.DOWNLOAD)).thenReturn(AuthorizationStatus.authorized());
		when(mockEntityManager.findBoundSchema(recordSet.getId())).thenReturn(Optional.empty());

		gridSession = new GridSession().setSessionId(gridSessionId);

		when(mockGridDao.createGridSession(new CreateGridSession().setUserId(userId).setSourceId(recordSet.getId())))
				.thenReturn(gridSession);

		when(mockGridDao.createReplica(userId, gridSessionId, isAgent, EventSource.INTERNAL)).thenReturn(replica);

		when(mockFileHandleManager.getRawFileHandleUnchecked(recordSet.getDataFileHandleId())).thenReturn(csvFile);

		Long maxRowSize = (long) TableModelUtils.calculateMaxRowSize(csvSchema);

		doReturn(csvSchema).when(handler).getSchemaFromCsv(csvFile, csvDescriptor);
		doReturn(mockCsvReader).when(mockCsvProvider).getCsvReader(csvFile, csvDescriptor);

		doReturn(mockRowHandler).when(handler).getPatchRowHandler(mockPatchStore, gridSession, replica, csvSchema,
				maxRowSize);

		when(mockCsvReader.readNext()).thenReturn(new String[] { "foo", "bar" }, new String[] { "1", "one" },
				new String[] { "2", "two" }, new String[] { null, "three" }, null);

		// Call under test
		handler.createGrid(mockCallback, mockUser, new CreateGridRequest().setRecordSetId(recordSet.getId()),
				mockPatchStore);

		verify(mockCsvReader, times(5)).readNext();
		verify(mockCsvReader).close();

		verify(mockRowHandler).nextRow(new Row().setValues(Arrays.asList("1", "one")));
		verify(mockRowHandler).nextRow(new Row().setValues(Arrays.asList("2", "two")));
		verify(mockRowHandler).nextRow(new Row().setValues(Arrays.asList(null, "three")));
		verify(mockRowHandler).close();

		verifyNoMoreInteractions(mockCsvReader, mockRowHandler);
	}

	@Test
	public void testBuildSessionFromRecordSetWithNoCsvDescriptor() throws IOException {
		recordSet.setCsvDescriptor(null);

		when(mockUser.getId()).thenReturn(userId);
		when(mockEntityManager.getEntity(mockUser, recordSet.getId(), RecordSet.class)).thenReturn(recordSet);
		when(mockAuthorizationManager.hasAccess(mockUser, recordSet.getId(), ACCESS_TYPE.DOWNLOAD)).thenReturn(AuthorizationStatus.authorized());
		when(mockEntityManager.findBoundSchema(recordSet.getId())).thenReturn(Optional.empty());

		gridSession = new GridSession().setSessionId(gridSessionId);

		when(mockGridDao.createGridSession(new CreateGridSession().setUserId(userId).setSourceId(recordSet.getId())))
				.thenReturn(gridSession);

		when(mockGridDao.createReplica(userId, gridSessionId, isAgent, EventSource.INTERNAL)).thenReturn(replica);

		when(mockFileHandleManager.getRawFileHandleUnchecked(recordSet.getDataFileHandleId())).thenReturn(csvFile);

		Long maxRowSize = (long) TableModelUtils.calculateMaxRowSize(csvSchema);

		csvDescriptor = new CsvTableDescriptor().setIsFirstLineHeader(true);

		doReturn(csvSchema).when(handler).getSchemaFromCsv(csvFile, csvDescriptor);
		doReturn(mockCsvReader).when(mockCsvProvider).getCsvReader(csvFile, csvDescriptor);
		doReturn(mockRowHandler).when(handler).getPatchRowHandler(mockPatchStore, gridSession, replica, csvSchema,
				maxRowSize);

		when(mockCsvReader.readNext()).thenReturn(new String[] { "foo", "bar" }, new String[] { "1", "one" },
				new String[] { "2", "two" }, new String[] { null, "three" }, null);

		// Call under test
		handler.createGrid(mockCallback, mockUser, new CreateGridRequest().setRecordSetId(recordSet.getId()),
				mockPatchStore);

		verify(mockCsvReader, times(5)).readNext();
		verify(mockCsvReader).close();

		verify(mockRowHandler).nextRow(new Row().setValues(Arrays.asList("1", "one")));
		verify(mockRowHandler).nextRow(new Row().setValues(Arrays.asList("2", "two")));
		verify(mockRowHandler).nextRow(new Row().setValues(Arrays.asList(null, "three")));
		verify(mockRowHandler).close();

		verifyNoMoreInteractions(mockCsvReader, mockRowHandler);
	}

	@Test
	public void testBuildSessionFromRecordSetWithIOException() throws IOException {
		when(mockUser.getId()).thenReturn(userId);
		when(mockEntityManager.getEntity(mockUser, recordSet.getId(), RecordSet.class)).thenReturn(recordSet);
		when(mockAuthorizationManager.hasAccess(mockUser, recordSet.getId(), ACCESS_TYPE.DOWNLOAD)).thenReturn(AuthorizationStatus.authorized());
		when(mockEntityManager.findBoundSchema(recordSet.getId())).thenReturn(Optional.empty());

		gridSession = new GridSession().setSessionId(gridSessionId);

		when(mockGridDao.createGridSession(new CreateGridSession().setUserId(userId).setSourceId(recordSet.getId())))
				.thenReturn(gridSession);

		when(mockGridDao.createReplica(userId, gridSessionId, isAgent, EventSource.INTERNAL)).thenReturn(replica);

		when(mockFileHandleManager.getRawFileHandleUnchecked(recordSet.getDataFileHandleId())).thenReturn(csvFile);

		IOException ioe = new IOException("nope");

		Long maxRowSize = (long) TableModelUtils.calculateMaxRowSize(csvSchema);

		doReturn(csvSchema).when(handler).getSchemaFromCsv(csvFile, csvDescriptor);
		doReturn(mockCsvReader).when(mockCsvProvider).getCsvReader(csvFile, csvDescriptor);
		doReturn(mockRowHandler).when(handler).getPatchRowHandler(mockPatchStore, gridSession, replica, csvSchema,
				maxRowSize);

		when(mockCsvReader.readNext()).thenThrow(ioe);

		IllegalStateException result = assertThrows(IllegalStateException.class, () -> {
			// Call under test
			handler.createGrid(mockCallback, mockUser, new CreateGridRequest().setRecordSetId(recordSet.getId()),
					mockPatchStore);
		});

		assertEquals(ioe, result.getCause());

		verify(mockCsvReader).close();
		verify(mockRowHandler).close();

		verifyNoMoreInteractions(mockCsvReader, mockRowHandler);
	}

	@Test
	public void testBuildSessionFromRecordSetWithEmptyCsvSchema() throws IOException {
		when(mockUser.getId()).thenReturn(userId);
		when(mockEntityManager.getEntity(mockUser, recordSet.getId(), RecordSet.class)).thenReturn(recordSet);
		when(mockAuthorizationManager.hasAccess(mockUser, recordSet.getId(), ACCESS_TYPE.DOWNLOAD)).thenReturn(AuthorizationStatus.authorized());
		when(mockEntityManager.findBoundSchema(recordSet.getId())).thenReturn(Optional.empty());

		gridSession = new GridSession().setSessionId(gridSessionId);

		when(mockGridDao.createGridSession(new CreateGridSession().setUserId(userId).setSourceId(recordSet.getId())))
				.thenReturn(gridSession);

		when(mockGridDao.createReplica(userId, gridSessionId, isAgent, EventSource.INTERNAL)).thenReturn(replica);

		when(mockFileHandleManager.getRawFileHandleUnchecked(recordSet.getDataFileHandleId())).thenReturn(csvFile);

		csvSchema = Collections.emptyList();

		doReturn(csvSchema).when(handler).getSchemaFromCsv(csvFile, csvDescriptor);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			handler.createGrid(mockCallback, mockUser, new CreateGridRequest().setRecordSetId(recordSet.getId()),
					mockPatchStore);
		}).getMessage();

		assertEquals("Cannot determine the schema from the CSV file, at least one column header must be present.",
				errorMessage);

		verifyNoMoreInteractions(mockCsvReader, mockRowHandler);
	}

	@Test
	public void testGetPatchRowHandler() throws IOException {
		gridSession = new GridSession().setSessionId(gridSessionId);

		long maxRowSize = (long) TableModelUtils.calculateMaxRowSize(csvSchema);

		// Call under test
		PatchRowHandler patchHandler = handler.getPatchRowHandler(mockPatchStore, gridSession, replica, csvSchema,
				maxRowSize);

		assertNotNull(patchHandler);

		// Call under test
		patchHandler.nextRow(new Row().setValues(Arrays.asList("1", "one")));
		patchHandler.close();

		verify(mockPatchStore).savePatch(eq(gridSessionId), any(), patchCaptor.capture());

		Patch patch = PatchCompactSerializable.deserialize(new JSONArray(patchCaptor.getValue()));

		assertEquals(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(1L), patch.getPatchId());
	}

	@Test
	public void testGetSchemaFromCsv() throws IOException {
		csvDescriptor = new CsvTableDescriptor().setIsFirstLineHeader(true);
		
		doReturn(mockCsvReader).when(mockCsvProvider).getCsvReader(csvFile, csvDescriptor);
		
		when(mockCsvReader.readNext()).thenReturn(
			new String[] { "foo", "bar" }, 
			new String[] { "1", "one" },
			new String[] { "2", "two" }, 
			new String[] { null, "three" }, 
			null);

		// Call under test
		List<ColumnModel> schema = handler.getSchemaFromCsv(csvFile, csvDescriptor);

		assertEquals(List.of(
			new ColumnModel().setName("foo").setColumnType(ColumnType.INTEGER),
			new ColumnModel().setName("bar").setColumnType(ColumnType.STRING).setMaximumSize(5L)
			), schema);

	}

}
