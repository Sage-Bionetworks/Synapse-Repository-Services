package org.sagebionetworks.repo.manager.grid;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.agent.handler.grid.SetValueProcessorFactory;
import org.sagebionetworks.repo.manager.config.WebsocketApi;
import org.sagebionetworks.repo.manager.grid.create.CreateGridHandler;
import org.sagebionetworks.repo.manager.grid.create.CreateGridHandlerResult;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangeSet;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.PatchBuilderPublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowData;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.QueryElement;
import org.sagebionetworks.repo.manager.grid.response.InternalReplicaToHubEventPublisher;
import org.sagebionetworks.repo.manager.table.RowHandlerProvider;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlRequest;
import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlResponse;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
import org.sagebionetworks.repo.model.grid.CreateReplicaRequest;
import org.sagebionetworks.repo.model.grid.CreateReplicaResponse;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridQueryJobRequest;
import org.sagebionetworks.repo.model.grid.GridQueryJobResponse;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridReplicaInfo;
import org.sagebionetworks.repo.model.grid.GridReplicaType;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.GridSnapshot;
import org.sagebionetworks.repo.model.grid.GridUpdateJobRequest;
import org.sagebionetworks.repo.model.grid.GridUpdateJobResponse;
import org.sagebionetworks.repo.model.grid.GridUtils;
import org.sagebionetworks.repo.model.grid.ListGridReplicasRequest;
import org.sagebionetworks.repo.model.grid.ListGridReplicasResponse;
import org.sagebionetworks.repo.model.grid.ListGridSessionsRequest;
import org.sagebionetworks.repo.model.grid.ListGridSessionsResponse;
import org.sagebionetworks.repo.model.grid.PatchInfo;
import org.sagebionetworks.repo.model.grid.internal.Connection;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.query.QueryRequest;
import org.sagebionetworks.repo.model.grid.query.result.QueryResult;
import org.sagebionetworks.repo.model.grid.update.GridUpdateRequest;
import org.sagebionetworks.repo.model.grid.update.LiteralSetValue;
import org.sagebionetworks.repo.model.grid.update.Update;
import org.sagebionetworks.repo.model.grid.update.UpdateBatch;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;

import au.com.bytecode.opencsv.CSVReader;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
public class GridManagerUnitTest {

	@Mock
	private GridDao mockGridDao;

	@Mock
	private AwsCredentialsProvider mockCredentialsProvider;

	@Mock
	private WebsocketApi mockWebsocketApi;

	@Mock
	private StackConfiguration mockConfig;

	@Mock
	private S3Client mockS3Client;

	@Mock
	private SynapseS3Client mockSynapseS3Client;

	@Mock
	private UserInfo mockUser;

	@Mock
	private TableQueryManager mockQueryManager;

	@Mock
	private AsyncJobProgressCallback mockCallback;

	@Mock
	private InternalReplicaToHubEventPublisher mockInternalEventPublisher;

	@Mock
	private GridAuthorizationManager mockGridAuthManager;
	
	@Mock
	private TransferManager mockTransferManager;

	@Mock
	private org.sagebionetworks.repo.model.message.TransactionalMessenger mockTransactionalMessenger;

	@Mock
	private Upload mockUpload;

	@Mock
	private File mockSnapshotFile;

	@Captor
	private ArgumentCaptor<PutObjectRequest> putCaptor;

	@Captor
	private ArgumentCaptor<RequestBody> bodyCaptor;

	@Captor
	private ArgumentCaptor<GetObjectRequest> getObjectRequestCaptor;

	@Captor
	private ArgumentCaptor<RowHandlerProvider> rowHandlerProviderCaptor;
	@Captor
	private ArgumentCaptor<String> patchCaptor;
	
	@Captor
	private ArgumentCaptor<EventContext> eventContextCaptor;

	private GridManagerImpl gridManager;

	private Long userId;
	private String gridSessionId;
	private Long gridSessionIdLong;
	private EventSource eventSource;
	private boolean isAgent;
	private CreateReplicaRequest createReplicaRequest;
	private Long replicaId;
	private GridReplica replica;
	private CreateGridPresignedUrlRequest createGridPresignedUrlRequest;
	private EventContext eventContext;
	private String connectionId;
	private LogicalTimestamp patchId;
	private PatchInfo patchInfo;

	private JSONArray patchBody;
	private List<LogicalTimestamp> clock;
	private Query query;
	private String tableId;
	private ListGridSessionsRequest listGridSessionRequest;
	private GridSession gridSession;
	private List<GridSession> gridSessions;

	private RecordSet recordSet;

	@Mock
	private CSVReader mockCsvReader;
	
	@Mock
	private PatchRowHandler mockRowHandler;
	@Mock
	private CreateGridHandler mockCreateGridHandler;

	@Mock
	private GridReplicaViewManager mockGridReplicaViewManager;

	@Mock
	private PatchBuilderPublisher mockPatchBuilderPublisher;

	@Mock
	private SetValueProcessorFactory mockSetValueProcessorFactory;
	
	@BeforeEach
	public void before() {
		userId = 123L;
		gridSessionIdLong = 456L;
		gridSessionId = GridUtils.gridSessionIdAsString(gridSessionIdLong);
		eventSource = EventSource.WEBSOCKET;
		isAgent = false;
		createReplicaRequest = new CreateReplicaRequest().setGridSessionId(gridSessionId);
		replicaId = 88L;
		replica = new GridReplica().setReplicaId(replicaId);
		createGridPresignedUrlRequest = new CreateGridPresignedUrlRequest().setGridSessionId(gridSessionId)
				.setReplicaId(replicaId);
		connectionId = "con444=";
		eventContext = new EventContext(EventType.CONNECT, eventSource, connectionId);
		patchId = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(777L);
		patchBody = new JSONArray("[[[66537,1]],[0]]");

		when(mockConfig.getStack()).thenReturn("dev");
		gridManager = new GridManagerImpl(mockCredentialsProvider, mockWebsocketApi, mockGridDao, mockConfig,
			mockS3Client, mockSynapseS3Client, mockInternalEventPublisher, List.of(mockCreateGridHandler), mockGridAuthManager, mockTransferManager,
			mockTransactionalMessenger, mockGridReplicaViewManager, mockPatchBuilderPublisher, mockSetValueProcessorFactory
		);
		
		gridManager = Mockito.spy(gridManager);
		clock = List.of(patchId);
		query = new Query().setSql("select * from syn123").setIncludeEntityEtag(false);
		tableId = "syn999";

		listGridSessionRequest = new ListGridSessionsRequest().setSourceId(tableId);
		gridSession = new GridSession();
		gridSessions = List.of(gridSession);
		recordSet = new RecordSet().setId("987");

		Timestamp expires = new Timestamp(System.currentTimeMillis() + 1001L);
		patchInfo = new PatchInfo().setPatchId(patchId).setS3Key("akey").setExpiresOn(expires);
	}

	@Test
	public void testCreateGrid() {
		when(mockUser.getId()).thenReturn(userId);
		CreateGridRequest request = new CreateGridRequest().setOwnerPrincipalId(null);
		when(mockGridAuthManager.validateGridOwner(mockUser, null)).thenReturn(userId);
		when(mockCreateGridHandler.canCreate(request)).thenReturn(true);

		GridSession expected = new GridSession().setSessionId(gridSessionId);
		GridReplica replica = new GridReplica().setGridSessionId(expected.getSessionId()).setReplicaId(replicaId);
		
		when(mockCreateGridHandler.createGrid(mockCallback, mockUser, request, gridManager))
				.thenReturn(new CreateGridHandlerResult().setGridSession(expected).setGridReplica(replica));
		
		when(mockGridDao.createReplica(userId, gridSessionId, false, EventSource.USER_SUPPORT))
			.thenReturn(new GridReplica().setGridSessionId(gridSessionId).setReplicaId(replicaId - 1));
		
		when(mockGridDao.getGridSession(gridSessionId)).thenReturn(Optional.of(expected));
		
		// call under test
		CreateGridResponse result = gridManager.createGrid(mockCallback, mockUser, request);
		assertNotNull(result);
		assertEquals(expected, result.getGridSession());
		
		verifyConnectionEvent(EventSource.INTERNAL, replicaId);
		verifyConnectionEvent(EventSource.USER_SUPPORT, replicaId - 1);
		
		verifyNoMoreInteractions(mockInternalEventPublisher);
	}
	
	@Test
	public void testCreateGridWithTeamOwner() {
		when(mockUser.getId()).thenReturn(userId);
		CreateGridRequest request = new CreateGridRequest().setOwnerPrincipalId("456");
		when(mockGridAuthManager.validateGridOwner(mockUser, "456")).thenReturn(456L);
		when(mockCreateGridHandler.canCreate(request)).thenReturn(true);

		GridSession expected = new GridSession().setSessionId(gridSessionId);
		GridReplica replica = new GridReplica().setGridSessionId(expected.getSessionId()).setReplicaId(replicaId);
		
		when(mockCreateGridHandler.createGrid(mockCallback, mockUser, request, gridManager))
				.thenReturn(new CreateGridHandlerResult().setGridSession(expected).setGridReplica(replica));
		
		when(mockGridDao.createReplica(userId, gridSessionId, false, EventSource.USER_SUPPORT))
			.thenReturn(new GridReplica().setGridSessionId(gridSessionId).setReplicaId(replicaId - 1));
		
		when(mockGridDao.getGridSession(gridSessionId)).thenReturn(Optional.of(expected));
		
		// call under test
		CreateGridResponse result = gridManager.createGrid(mockCallback, mockUser, request);
		assertNotNull(result);
		assertEquals(expected, result.getGridSession());
		
		verifyConnectionEvent(EventSource.INTERNAL, replicaId);
		verifyConnectionEvent(EventSource.USER_SUPPORT, replicaId - 1);
		
		verifyNoMoreInteractions(mockInternalEventPublisher);
	}
	
	
	@Test
	public void testCreateGridWithSchema() {
		when(mockUser.getId()).thenReturn(userId);
		CreateGridRequest request = new CreateGridRequest();
		when(mockCreateGridHandler.canCreate(request)).thenReturn(true);
		when(mockGridAuthManager.validateGridOwner(mockUser, null)).thenReturn(userId);

		GridSession expected = new GridSession().setSessionId(gridSessionId).setGridJsonSchema$Id("someSchemaId");
		GridReplica replica = new GridReplica().setGridSessionId(expected.getSessionId()).setReplicaId(replicaId);
		when(mockCreateGridHandler.createGrid(mockCallback, mockUser, request, gridManager))
				.thenReturn(new CreateGridHandlerResult().setGridSession(expected).setGridReplica(replica));
		GridReplica validationReplica = new GridReplica().setGridSessionId(gridSessionId).setReplicaId(replicaId + 1L);
		
		when(mockGridDao.createReplica(userId, gridSessionId, false, EventSource.VALIDATION))
				.thenReturn(validationReplica);
		
		when(mockGridDao.createReplica(userId, gridSessionId, false, EventSource.USER_SUPPORT))
			.thenReturn(new GridReplica().setGridSessionId(gridSessionId).setReplicaId(replicaId - 1));
		
		when(mockGridDao.getGridSession(gridSessionId)).thenReturn(Optional.of(expected));

		// call under test
		CreateGridResponse result = gridManager.createGrid(mockCallback, mockUser, request);

		assertEquals(expected, result.getGridSession());

		verifyConnectionEvent(EventSource.INTERNAL, replicaId);
		verifyConnectionEvent(EventSource.VALIDATION, replicaId + 1);
		verifyConnectionEvent(EventSource.USER_SUPPORT, replicaId - 1);
		
		verifyNoMoreInteractions(mockInternalEventPublisher);
	}
	
	@Test
	public void testCreateGridWithNoReplica() {
		when(mockUser.getId()).thenReturn(userId);
		CreateGridRequest request = new CreateGridRequest();
		when(mockCreateGridHandler.canCreate(request)).thenReturn(true);

		GridSession expected = new GridSession().setSessionId(gridSessionId);
		GridReplica replica = null;
		when(mockCreateGridHandler.createGrid(mockCallback, mockUser, request, gridManager))
				.thenReturn(new CreateGridHandlerResult().setGridSession(expected).setGridReplica(replica));

		when(mockGridDao.createReplica(userId, gridSessionId, false, EventSource.USER_SUPPORT))
			.thenReturn(new GridReplica().setGridSessionId(gridSessionId).setReplicaId(replicaId - 1));
		
		when(mockGridDao.getGridSession(gridSessionId)).thenReturn(Optional.of(expected));
		
		// call under test
		CreateGridResponse result = gridManager.createGrid(mockCallback, mockUser, request);

		assertEquals(expected, result.getGridSession());
		
		verifyConnectionEvent(EventSource.USER_SUPPORT, replicaId - 1);
		
		verifyNoMoreInteractions(mockInternalEventPublisher);
	}
	
	
	@Test
	public void testCreateGridWithNoHandler() {
		CreateGridRequest request = new CreateGridRequest();
		when(mockCreateGridHandler.canCreate(request)).thenReturn(false);

		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			gridManager.createGrid(mockCallback, mockUser, request);
		}).getMessage();
		assertTrue(message.startsWith("Cannot find a handler for:"));
	}

	
	@Test
	public void testCreateGridWithInitialQueryAndRecordSetId() {
		CreateGridRequest request = new CreateGridRequest().setInitialQuery(query).setRecordSetId(recordSet.getId());
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// call under test
			gridManager.createGrid(mockCallback, mockUser, request);
		}).getMessage();
		
		assertEquals("Cannot set both initialQuery and recordSetId.", errorMessage);
	}


	@Test
	public void testCreateGridWithNullUser() {
		CreateGridRequest request = new CreateGridRequest();

		String message = assertThrows(IllegalArgumentException.class, () -> {

			// call under test
			gridManager.createGrid(mockCallback, null, request);

		}).getMessage();
		assertEquals("user is required.", message);
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testCreateGridWithNullRequest() {
		CreateGridRequest request = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {

			// call under test
			gridManager.createGrid(mockCallback, mockUser, request);

		}).getMessage();
		assertEquals("request is required.", message);
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testValidGridSessionAccess() {
		when(mockGridAuthManager.hasGridSessionAccess(mockUser, gridSessionId)).thenReturn(AuthorizationStatus.authorized());
		// call under test
		gridManager.validGridSessionAccess(mockUser, gridSessionId);
	}

	@Test
	public void testValidGridSessionAccessWithNullUser() {
		mockUser = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.validGridSessionAccess(mockUser, gridSessionId);
		}).getMessage();
		assertEquals("user is required.", message);
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testValidGridSessionAccessWithNulSessionId() {
		String gridSessionId = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.validGridSessionAccess(mockUser, gridSessionId);
		}).getMessage();
		assertEquals("gridSessionId is required.", message);
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testGetGridSession() {

		doNothing().when(gridManager).validGridSessionAccess(mockUser, gridSessionId);

		GridSession expected = new GridSession().setSessionId("gs123").setStartedBy(userId.toString());
		when(mockGridDao.getGridSession(gridSessionId)).thenReturn(Optional.of(expected));

		// call under test
		GridSession session = gridManager.getGridSession(mockUser, gridSessionId);
		assertEquals(expected, session);
	}

	@Test
	public void testGetGridSessionNotFound() {

		doNothing().when(gridManager).validGridSessionAccess(mockUser, gridSessionId);

		when(mockGridDao.getGridSession(gridSessionId)).thenReturn(Optional.empty());

		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			gridManager.getGridSession(mockUser, gridSessionId);
		}).getMessage();
		assertEquals("Grid session not found.", message);
	}

	@Test
	public void testCreateReplica() {
		when(mockUser.getId()).thenReturn(userId);
		// must have access to create a replica.
		doNothing().when(gridManager).validGridSessionAccess(mockUser, gridSessionId);
		GridReplica replica = new GridReplica().setReplicaId(333L);
		when(mockGridDao.createReplica(userId, gridSessionId, isAgent, eventSource)).thenReturn(replica);

		// call under test
		CreateReplicaResponse response = gridManager.createReplica(mockUser, gridSessionId, isAgent, eventSource);
		assertNotNull(response);
		assertEquals(replica, response.getReplica());
	}

	@Test
	public void testCreateReplicaWithNullUser() {
		mockUser = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.createReplica(mockUser, gridSessionId, isAgent, eventSource);
		}).getMessage();
		assertEquals("user is required.", message);

		verify(gridManager, never()).validGridSessionAccess(any(), any());
	}

	@Test
	public void testCreateReplicaWithNullSessionId() {
		gridSessionId = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.createReplica(mockUser, gridSessionId, isAgent, eventSource);
		}).getMessage();
		assertEquals("gridSessionId is required.", message);

		verify(gridManager, never()).validGridSessionAccess(any(), any());
	}

	@Test
	public void testCreateReplicaWithNullEventSource() {
		eventSource = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.createReplica(mockUser, gridSessionId, isAgent, eventSource);
		}).getMessage();
		assertEquals("source is required.", message);

		verify(gridManager, never()).validGridSessionAccess(any(), any());
	}

	@Test
	public void testCreateReplicaWithRequest() {

		doReturn(new CreateReplicaResponse().setReplica(replica)).when(gridManager).createReplica(mockUser,
				gridSessionId, false, EventSource.WEBSOCKET);

		// call under test
		CreateReplicaResponse response = gridManager.createReplica(mockUser, createReplicaRequest);
		assertNotNull(response);
		assertEquals(replica, response.getReplica());
	}

	@Test
	public void testCreateReplicaWithRequestNullRequest() {
		createReplicaRequest = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.createReplica(mockUser, createReplicaRequest);
		}).getMessage();
		assertEquals("request is required.", message);

		verify(gridManager, never()).createReplica(any(), any(), any(Boolean.class), any());
	}

	@Test
	public void testGetReplica() {
		// must have access to create a replica.
		doNothing().when(gridManager).validGridSessionAccess(mockUser, gridSessionId);
		when(mockGridDao.getGridReplica(gridSessionId, replicaId)).thenReturn(Optional.of(replica));

		// call under test
		GridReplica result = gridManager.getReplica(mockUser, gridSessionId, replicaId);
		assertEquals(replica, result);
	}

	@Test
	public void testGetReplicaWithNotFound() {
		// must have access to create a replica.
		doNothing().when(gridManager).validGridSessionAccess(mockUser, gridSessionId);
		when(mockGridDao.getGridReplica(gridSessionId, replicaId)).thenReturn(Optional.empty());

		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			gridManager.getReplica(mockUser, gridSessionId, replicaId);
		}).getMessage();
		assertEquals("Grid replica not found.", message);
	}

	@Test
	public void testGetReplicaWithNullReplicaId() {
		replicaId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.getReplica(mockUser, gridSessionId, replicaId);
		}).getMessage();
		assertEquals("replicaId is required.", message);
	}

	@Test
	public void testValidateReplicaOwnerWithOwner() {
		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.isAdmin()).thenReturn(false);
		when(mockGridDao.getReplicaCreatedBy(gridSessionId, replicaId)).thenReturn(Optional.of(userId));
		// call under test
		gridManager.validateRepicaOwner(mockUser, gridSessionId, replicaId);
	}

	@Test
	public void testValidateReplicaOwnerWithOther() {
		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.isAdmin()).thenReturn(false);
		when(mockGridDao.getReplicaCreatedBy(gridSessionId, replicaId)).thenReturn(Optional.of(77777L));

		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			gridManager.validateRepicaOwner(mockUser, gridSessionId, replicaId);
		}).getMessage();
		assertEquals("You are not authorized to access this resource.", message);
	}

	@Test
	public void testValidateReplicaOwnerWithOtherAdmin() {
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockGridDao.getReplicaCreatedBy(gridSessionId, replicaId)).thenReturn(Optional.of(555L));
		// call under test
		gridManager.validateRepicaOwner(mockUser, gridSessionId, replicaId);
	}

	@Test
	public void testValidateReplicaOwnerWithNotFound() {
		when(mockGridDao.getReplicaCreatedBy(gridSessionId, replicaId)).thenReturn(Optional.empty());
		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			gridManager.validateRepicaOwner(mockUser, gridSessionId, replicaId);
		}).getMessage();
		assertEquals("Grid replica not found.", message);
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testValidateReplicaOwnerWithNullUser() {
		mockUser = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.validateRepicaOwner(mockUser, gridSessionId, replicaId);
		}).getMessage();
		assertEquals("user is required.", message);
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testValidateReplicaOwnerWithNullSessionId() {
		gridSessionId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.validateRepicaOwner(mockUser, gridSessionId, replicaId);
		}).getMessage();
		assertEquals("gridSessionId is required.", message);
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testValidateReplicaOwnerWithNullReplicaId() {
		replicaId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.validateRepicaOwner(mockUser, gridSessionId, replicaId);
		}).getMessage();
		assertEquals("replicaId is required.", message);
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testCreateWebsocketPresignedUrl() {
		when(mockUser.getId()).thenReturn(userId);
		when(mockWebsocketApi.getApiId()).thenReturn("abcde");
		when(mockWebsocketApi.getStageName()).thenReturn("stage");
		AwsBasicCredentials creds = AwsBasicCredentials.builder().accessKeyId("accessKey").secretAccessKey("secret")
				.build();
		when(mockCredentialsProvider.resolveCredentials()).thenReturn(creds);
		doNothing().when(gridManager).validateRepicaOwner(mockUser, gridSessionId, replicaId);

		// call under test
		CreateGridPresignedUrlResponse response = gridManager.createWebsocketPresignedUrl(mockUser,
				createGridPresignedUrlRequest);
		String presigned = response.getPresignedUrl();
		assertTrue(presigned.startsWith(
				"wss://abcde.execute-api.us-east-1.amazonaws.com/stage/?gridSessionId=456&replicaId=88&userId=123"));
		// note the date and signature change with each run.
		assertTrue(presigned.contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
		assertTrue(presigned.contains("X-Amz-Date"));
		assertTrue(presigned.contains("X-Amz-SignedHeaders=host"));
		assertTrue(presigned.contains("X-Amz-Credential=accessKey"));
		assertTrue(presigned.contains("X-Amz-Expires"));
		assertTrue(presigned.contains("X-Amz-Signature"));

	}

	@Test
	public void testCreateWebsocketPresignedUrlWithNullRequest() {
		createGridPresignedUrlRequest = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.createWebsocketPresignedUrl(mockUser, createGridPresignedUrlRequest);
		}).getMessage();
		assertEquals("request is required.", message);
		verify(gridManager, never()).validateRepicaOwner(any(), any(), any());

	}

	@Test
	public void testCreateReplicaConnection() {
		when(mockUser.getId()).thenReturn(userId);
		doNothing().when(gridManager).validateRepicaOwner(mockUser, gridSessionId, replicaId);

		// call under test
		gridManager.createReplicaConnection(mockUser, eventContext,
				new Connection().setGridSessionId(gridSessionIdLong).setReplicaId(replicaId).setUserId(userId));

		verify(mockGridDao)
				.createConnection(new GridConnectionInfo().setConnectionId(connectionId).setSessionId(gridSessionId)
						.setReplicaId(replicaId).setCreatedBy(userId).setSource(EventSource.WEBSOCKET));
	}

	@Test
	public void testCreateReplicaConnectionWithNonConnectionType() {
		// Only connection type is allowed.
		eventContext = new EventContext(EventType.MESSAGE, EventSource.WEBSOCKET, connectionId);
		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			gridManager.createReplicaConnection(mockUser, eventContext,
					new Connection().setGridSessionId(gridSessionIdLong).setReplicaId(replicaId).setUserId(userId));
		}).getMessage();
		assertEquals("Invalid request", message);

		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testCreateReplicaConnectionWithNullUser() {
		mockUser = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.createReplicaConnection(mockUser, eventContext,
					new Connection().setGridSessionId(gridSessionIdLong).setReplicaId(replicaId).setUserId(userId));
		}).getMessage();
		assertEquals("user is required.", message);

		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testCreateReplicaConnectionWithNullContext() {
		eventContext = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.createReplicaConnection(mockUser, eventContext,
					new Connection().setGridSessionId(gridSessionIdLong).setReplicaId(replicaId).setUserId(userId));
		}).getMessage();
		assertEquals("context is required.", message);

		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testCreateReplicaConnectionWithNullConnection() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.createReplicaConnection(mockUser, eventContext, null);
		}).getMessage();
		assertEquals("connection is required.", message);

		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testRemoveReplicaConnection() {
		// call under test
		gridManager.removeReplicatConnection(EventType.DISCONNECT, connectionId);
		verify(mockGridDao).removeConnection(connectionId);
	}

	@Test
	public void testRemoveReplicaConnectionWithNonDisconnect() {
		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			gridManager.removeReplicatConnection(EventType.MESSAGE, connectionId);
		}).getMessage();
		assertEquals("Invalid request", message);
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testRemoveReplicaConnectionWithNullType() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.removeReplicatConnection(null, connectionId);
		}).getMessage();
		assertEquals("type is required.", message);
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testRemoveReplicaConnectionWithNullConnectionId() {
		connectionId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.removeReplicatConnection(EventType.DISCONNECT, connectionId);
		}).getMessage();
		assertEquals("connectionId is required.", message);
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testRemoveReplicaConnectionInternal() {
		// call under test
		gridManager.removeReplicaConnection(connectionId);
		verify(mockGridDao).removeConnection(connectionId);
	}

	@Test
	public void testRemoveReplicaConnectionInternalWithNullId() {
		connectionId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.removeReplicaConnection(connectionId);
		}).getMessage();
		assertEquals("connectionId is required.", message);
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testListActiveConnections() {
		when(mockGridDao.getConnection(connectionId)).thenReturn(
				Optional.of(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)));
		List<GridConnectionInfo> otherCons = List
				.of(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId("con22"));
		when(mockGridDao.listConnections(gridSessionId)).thenReturn(otherCons);

		// call under test
		List<GridConnectionInfo> cons = gridManager.listActiveConnections(connectionId);
		assertEquals(otherCons, cons);
	}

	@Test
	public void testListActiveConnectionsWithNotFound() {
		when(mockGridDao.getConnection(connectionId)).thenReturn(Optional.empty());
		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			gridManager.listActiveConnections(connectionId);
		}).getMessage();
		assertEquals("No Connection Found: con444=", message);
	}

	@Test
	public void testListActiveConnectionsWithNullId() {
		connectionId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.listActiveConnections(connectionId);
		}).getMessage();
		assertEquals("connectionId is required.", message);
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testSavePatch() {
		doReturn(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId).setReplicaId(replicaId)).when(gridManager)
				.getConnectionInfo(connectionId);
		when(mockS3Client.putObject(putCaptor.capture(), bodyCaptor.capture())).thenReturn(null);
		when(mockGridDao.savePatch(any(), any(), any(), anyLong())).thenReturn(true);
		// call under test
		boolean isNew = gridManager.savePatch(eventContext, patchId, patchBody.toString());
		assertTrue(isNew);

		assertEquals("dev.grid.patch.sagebase.org", putCaptor.getValue().bucket());
		String key = putCaptor.getValue().key();
		assertTrue(key.endsWith(".json"));
		assertEquals(RequestBody.fromString(patchBody.toString(), StandardCharsets.UTF_8).optionalContentLength(),
				bodyCaptor.getValue().optionalContentLength());

		verify(mockGridDao).savePatch(eq(gridSessionId), eq(patchId), eq(key), anyLong());
		verify(mockTransactionalMessenger).sendMessageAfterCommit(
				gridSessionIdLong.toString(), org.sagebionetworks.repo.model.ObjectType.GRID_SESSION,
				org.sagebionetworks.repo.model.message.ChangeType.UPDATE);

	}

	@Test
	public void testSavePatchWithGridId() {
		when(mockS3Client.putObject(putCaptor.capture(), bodyCaptor.capture())).thenReturn(null);
		when(mockGridDao.savePatch(any(), any(), any(), anyLong())).thenReturn(true);
		// call under test
		boolean isNew = gridManager.savePatch(gridSessionId, patchId, patchBody.toString());
		assertTrue(isNew);

		assertEquals("dev.grid.patch.sagebase.org", putCaptor.getValue().bucket());
		String key = putCaptor.getValue().key();
		assertTrue(key.endsWith(".json"));
		assertEquals(RequestBody.fromString(patchBody.toString(), StandardCharsets.UTF_8).optionalContentLength(),
				bodyCaptor.getValue().optionalContentLength());

		verify(mockGridDao).savePatch(eq(gridSessionId), eq(patchId), eq(key), anyLong());
		verify(mockTransactionalMessenger).sendMessageAfterCommit(
				gridSessionIdLong.toString(), org.sagebionetworks.repo.model.ObjectType.GRID_SESSION,
				org.sagebionetworks.repo.model.message.ChangeType.UPDATE);
	}

	@Test
	public void testSavePatchWithNotNew() {
		doReturn(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId).setReplicaId(replicaId)).when(gridManager)
				.getConnectionInfo(connectionId);
		when(mockS3Client.putObject(putCaptor.capture(), bodyCaptor.capture())).thenReturn(null);
		when(mockGridDao.savePatch(any(), any(), any(), anyLong())).thenReturn(false);
		// call under test
		boolean isNew = gridManager.savePatch(eventContext, patchId, patchBody.toString());
		assertFalse(isNew);

		assertEquals("dev.grid.patch.sagebase.org", putCaptor.getValue().bucket());
		String key = putCaptor.getValue().key();
		assertTrue(key.endsWith(".json"));
		assertEquals(RequestBody.fromString(patchBody.toString(), StandardCharsets.UTF_8).optionalContentLength(),
				bodyCaptor.getValue().optionalContentLength());

		verify(mockGridDao).savePatch(eq(gridSessionId), eq(patchId), eq(key), anyLong());
		verify(mockTransactionalMessenger, never()).sendMessageAfterCommit(any(), any(), any());
	}

	@Test
	public void testSavePatchWithNullContext() {
		eventContext = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			gridManager.savePatch(eventContext, patchId, patchBody.toString());
		}).getMessage();
		assertEquals("context is required.", message);
	}

	@Test
	public void testSavePatchWithMismatchedReplicaId() {
		doReturn(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)
				.setReplicaId(replicaId + 1)).when(gridManager).getConnectionInfo(connectionId);
		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			gridManager.savePatch(eventContext, patchId, patchBody.toString());
		}).getMessage();
		assertEquals("Patch replicaId does not match the connection replicaId.", message);
		verifyZeroInteractions(mockS3Client);
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testSavePatchWithNullPatchId() {
		patchId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			gridManager.savePatch(gridSessionId, patchId, patchBody.toString());
		}).getMessage();
		assertEquals("patchId is required.", message);
	}

	@Test
	public void testSavePatchWithNullPatchBody() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			gridManager.savePatch(gridSessionId, patchId, null);
		}).getMessage();
		assertEquals("body is required.", message);
	}

	@Test
	public void testGetPatchBody() {
		when(mockS3Client.getObjectAsBytes(getObjectRequestCaptor.capture())).thenReturn(ResponseBytes
				.fromByteArray(GetObjectResponse.builder().build(), patchBody.toString().getBytes(StandardCharsets.UTF_8)));
		// call under test
		Optional<JSONArray> op = gridManager.getPatchBody(gridSessionId, patchInfo);
		assertEquals(patchBody.toString(), op.get().toString());

		assertEquals("dev.grid.patch.sagebase.org", getObjectRequestCaptor.getValue().bucket());
		assertEquals("akey", getObjectRequestCaptor.getValue().key());
	}

	@Test
	public void testGetPatchBodyWithExpired() {
		Timestamp expires = new Timestamp(System.currentTimeMillis() - 1001L);
		PatchInfo expiredPatchInfo = new PatchInfo().setPatchId(patchId).setS3Key("akey").setExpiresOn(expires);

		String message = assertThrows(NotFoundException.class, () -> {
			// Call under test
			gridManager.getPatchBody(gridSessionId, expiredPatchInfo);
		}).getMessage();
		assertEquals("The requested patch has expired: LogicalTimestamp [replicaId=88, sequenceNumber=777]", message);

		verifyZeroInteractions(mockS3Client);
	}

	@Test
	public void testGetPatchBodyWithNullSessionId() {
		gridSessionId = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			gridManager.getPatchBody(gridSessionId, patchInfo);
		}).getMessage();
		assertEquals("sessionId is required.", message);

		verifyZeroInteractions(mockS3Client);
	}

	@Test
	public void testGetPatchBodyWithNullPatchInfo() {
		PatchInfo patchInfo = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			gridManager.getPatchBody(gridSessionId, patchInfo);
		}).getMessage();
		assertEquals("patch is required.", message);

		verifyZeroInteractions(mockS3Client);
	}

	@Test
	public void testGetNextMissingPatch() {
		when(mockGridDao.getConnection(connectionId)).thenReturn(
				Optional.of(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)));
		List<PatchInfo> missing = List.of(new PatchInfo().setPatchId(new LogicalTimestamp().setReplicaId(44L).setSequenceNumber(90L)));

		when(mockGridDao.listMissingPatchInfoForClock(gridSessionId, clock, 1)).thenReturn(missing);
		doReturn(Optional.of(patchBody)).when(gridManager).getPatchBody(gridSessionId, missing.get(0));

		// call under test
		Optional<JSONArray> op = gridManager.getNextMissingPatch(eventContext, clock);
		assertEquals(Optional.of(patchBody), op);
	}

	@Test
	public void testGetNextMissingPatchUpToDate() {
		when(mockGridDao.getConnection(connectionId)).thenReturn(
				Optional.of(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)));

		when(mockGridDao.listMissingPatchInfoForClock(gridSessionId, clock, 1)).thenReturn(Collections.emptyList());

		// call under test
		Optional<JSONArray> op = gridManager.getNextMissingPatch(eventContext, clock);
		assertEquals(Optional.empty(), op);
	}

	@Test
	public void testGetNextMissingPatchWithNullContext() {
		eventContext = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			gridManager.getNextMissingPatch(eventContext, clock);
		}).getMessage();
		assertEquals("context is required.", message);
	}

	@Test
	public void testGetNextMissingPatchWithNullClock() {
		clock = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			gridManager.getNextMissingPatch(eventContext, clock);
		}).getMessage();
		assertEquals("clock is required.", message);
	}

	@Test
	public void testListGridSessionsWithSource() {
		when(mockUser.getId()).thenReturn(userId);
		when(mockGridDao.listActiveGridSession(userId, tableId, 51L, 0L)).thenReturn(gridSessions);

		// call under test
		ListGridSessionsResponse response = gridManager.listActiveGridSessions(mockUser, listGridSessionRequest);
		ListGridSessionsResponse expected = new ListGridSessionsResponse().setPage(gridSessions).setNextPageToken(null);
		assertEquals(expected, response);
	}

	@Test
	public void testListGridSessionsWithAnonymous() {
		when(mockUser.isUserAnonymous()).thenReturn(true);
		
		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			gridManager.listActiveGridSessions(mockUser, listGridSessionRequest);
		}).getMessage();
		assertEquals("Must login to perform this action", message);
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testListGridSessionsWithNullUser() {
		mockUser = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.listActiveGridSessions(mockUser, listGridSessionRequest);
		}).getMessage();
		assertEquals("user is required.", message);
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testListGridSessionsWithNullRequest() {
		listGridSessionRequest = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.listActiveGridSessions(mockUser, listGridSessionRequest);
		}).getMessage();
		assertEquals("request is required.", message);
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testListGridSessionsWithoutSource() {
		listGridSessionRequest.setSourceId(null);
		when(mockUser.getId()).thenReturn(userId);
		when(mockGridDao.listActiveGridSession(userId, 51L, 0L)).thenReturn(gridSessions);

		// call under test
		ListGridSessionsResponse response = gridManager.listActiveGridSessions(mockUser, listGridSessionRequest);
		ListGridSessionsResponse expected = new ListGridSessionsResponse().setPage(gridSessions).setNextPageToken(null);
		assertEquals(expected, response);
	}

	@Test
	public void testListGridSessionsWithNextPageToken() {
		when(mockUser.getId()).thenReturn(userId);
		List<GridSession> page = IntStream.range(0, 51).mapToObj(i -> new GridSession().setSessionId("s" + i))
				.collect(Collectors.toList());
		when(mockGridDao.listActiveGridSession(userId, tableId, 51L, 0L)).thenReturn(page);

		// call under test
		ListGridSessionsResponse response = gridManager.listActiveGridSessions(mockUser, listGridSessionRequest);
		ListGridSessionsResponse expected = new ListGridSessionsResponse().setPage(page.subList(0, 50))
				.setNextPageToken("50a50");
		assertEquals(expected, response);

		when(mockGridDao.listActiveGridSession(userId, tableId, 51L, 50L)).thenReturn(gridSessions);
		// call under test
		response = gridManager.listActiveGridSessions(mockUser,
				listGridSessionRequest.setNextPageToken(response.getNextPageToken()));
		expected = new ListGridSessionsResponse().setPage(gridSessions).setNextPageToken(null);
		assertEquals(expected, response);
	}

	@Test
	public void testListGridSessionsWithoutSourceNextPageToken() {
		listGridSessionRequest.setSourceId(null);
		when(mockUser.getId()).thenReturn(userId);
		List<GridSession> page = IntStream.range(0, 51).mapToObj(i -> new GridSession().setSessionId("s" + i))
				.collect(Collectors.toList());
		when(mockGridDao.listActiveGridSession(userId, 51L, 0L)).thenReturn(page);

		// call under test
		ListGridSessionsResponse response = gridManager.listActiveGridSessions(mockUser, listGridSessionRequest);
		ListGridSessionsResponse expected = new ListGridSessionsResponse().setPage(page.subList(0, 50))
				.setNextPageToken("50a50");
		assertEquals(expected, response);

		when(mockGridDao.listActiveGridSession(userId, 51L, 50L)).thenReturn(gridSessions);
		// call under test
		response = gridManager.listActiveGridSessions(mockUser,
				listGridSessionRequest.setNextPageToken(response.getNextPageToken()));
		expected = new ListGridSessionsResponse().setPage(gridSessions).setNextPageToken(null);
		assertEquals(expected, response);
	}

	@Test
	public void testDeleteSession() {
		doNothing().when(gridManager).validGridSessionAccess(mockUser, gridSessionId);
		// call udner test
		gridManager.deleteGridSession(mockUser, gridSessionId);
		verify(mockGridDao).deleteGridSession(gridSessionId);
	}

	@ParameterizedTest
	@EnumSource(value = EventSource.class)
    public void testGetDefaultInternalConnection(EventSource source) {
        when(mockGridDao.getSingletonConnection(gridSessionId, source)).thenReturn(
                Optional.of(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)));

        // call under test
        Optional<GridConnectionInfo> actual = gridManager.getSingletonConnection(gridSessionId,source);
        assertEquals(Optional.of(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)), actual);
        verifyNoMoreInteractions(mockGridDao);
    }
	
	void verifyConnectionEvent(EventSource expectedSource, Long replicaId) {
		verify(mockInternalEventPublisher).publishEventAfterCommit(eventContextCaptor.capture(),
				eq(JsonRxMessageType.Notification), eq("connection"),
				eq(new Connection().setGridSessionId(gridSessionIdLong).setReplicaId(replicaId).setUserId(userId)));

		EventContext capturedContext = eventContextCaptor.getValue();
		assertEquals(EventType.CONNECT, capturedContext.getEventType());
		assertEquals(expectedSource, capturedContext.getEventSource());
		String connectionId = capturedContext.getConnectionId();
		assertNotNull(connectionId);
		assertDoesNotThrow(() -> UUID.fromString(connectionId));
	}

	@Test
	public void testSaveSnapshot() throws Exception {
		ClockTable clockTable = new ClockTable(List.of(patchId));
		Long createdBy = userId;
		String expectedS3Key = "snapshot-key-123";

		UploadResult uploadResult = new UploadResult();
		uploadResult.setKey(expectedS3Key);

		when(mockTransferManager.upload(any(String.class), any(String.class), any(File.class))).thenReturn(mockUpload);
		when(mockUpload.waitForUploadResult()).thenReturn(uploadResult);

		// call under test
		gridManager.saveSnapshot(gridSessionId, clockTable, createdBy, mockSnapshotFile);

		verify(mockGridDao).saveSnapshot(eq(gridSessionId), eq(clockTable), any(String.class), eq(createdBy));
		verify(mockTransferManager).upload(eq("dev.grid.snapshot.sagebase.org"), any(String.class), eq(mockSnapshotFile));
	}

	@Test
	public void testSaveSnapshotWithNullSessionId() {
		ClockTable clockTable = new ClockTable(List.of(patchId));
		Long createdBy = userId;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.saveSnapshot(null, clockTable, createdBy, mockSnapshotFile);
		}).getMessage();
		assertEquals("sessionId is required.", message);
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testSaveSnapshotWithNullClockTable() {
		Long createdBy = userId;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.saveSnapshot(gridSessionId, null, createdBy, mockSnapshotFile);
		}).getMessage();
		assertEquals("clockTable is required.", message);
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testSaveSnapshotWithNullCreatedBy() {
		ClockTable clockTable = new ClockTable(List.of(patchId));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.saveSnapshot(gridSessionId, clockTable, null, mockSnapshotFile);
		}).getMessage();
		assertEquals("createdBy is required.", message);
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testSaveSnapshotWithNullSnapshotFile() {
		ClockTable clockTable = new ClockTable(List.of(patchId));
		Long createdBy = userId;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.saveSnapshot(gridSessionId, clockTable, createdBy, null);
		}).getMessage();
		assertEquals("snapshotFile is required.", message);
		verifyZeroInteractions(mockGridDao);
	}


	@Test
	public void testSaveSnapshotWithException() {
		when(mockTransferManager.upload(any(String.class), any(String.class), any(File.class)))
				.thenThrow(new RuntimeException("Upload failed"));
		ClockTable clockTable = new ClockTable(List.of(patchId));
		Long createdBy = userId;

		assertThrows(RuntimeException.class, () -> {
			// call under test
			gridManager.saveSnapshot(gridSessionId, clockTable, createdBy, mockSnapshotFile);
		});
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testGetLatestSnapshotPresignedUrl() throws MalformedURLException {
		when(mockGridDao.getConnection(connectionId)).thenReturn(
				Optional.of(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)));

		GridSnapshot snapshot = new GridSnapshot().setS3Key("snapshot-key-123");
		when(mockGridDao.getLatestSnapshot(gridSessionId)).thenReturn(Optional.of(snapshot));

		URL presignedUrl = new URL("https://example.com/snapshot");
		when(mockSynapseS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(presignedUrl);

		// call under test
		Optional<URL> result = gridManager.getLatestSnapshotPresignedUrl(eventContext);

		assertTrue(result.isPresent());
		assertEquals(presignedUrl, result.get());
	}

	@Test
	public void testGetLatestSnapshotPresignedUrlWithNoSnapshot() {
		when(mockGridDao.getConnection(connectionId)).thenReturn(
				Optional.of(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)));

		when(mockGridDao.getLatestSnapshot(gridSessionId)).thenReturn(Optional.empty());

		// call under test
		Optional<URL> result = gridManager.getLatestSnapshotPresignedUrl(eventContext);

		assertTrue(result.isEmpty());
		verifyZeroInteractions(mockSynapseS3Client);
	}

	@Test
	public void testGetLatestSnapshotPresignedUrlWithNullContext() {
		eventContext = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.getLatestSnapshotPresignedUrl(eventContext);
		}).getMessage();
		assertEquals("context is required.", message);
		verifyZeroInteractions(mockGridDao, mockSynapseS3Client);
	}

	@Test
	public void testGetNextSynchronizeResponseWithNullClockAndSnapshot() throws MalformedURLException {
		when(mockGridDao.getConnection(connectionId)).thenReturn(
				Optional.of(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)));

		GridSnapshot snapshot = new GridSnapshot().setS3Key("snapshot-key-123");
		when(mockGridDao.getLatestSnapshot(gridSessionId)).thenReturn(Optional.of(snapshot));

		URL presignedUrl = new URL("https://example.com/snapshot");
		when(mockSynapseS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(presignedUrl);

		// call under test
		Optional<String> result = gridManager.getNextSynchronizeResponse(eventContext, null);

		assertTrue(result.isPresent());
		String response = result.get();
		JSONObject json = new JSONObject(response);
		assertEquals("snapshot", json.getString("type"));
		assertEquals("https://example.com/snapshot", json.getString("body"));
	}

	@Test
	public void testGetNextSynchronizeResponseWithEmptyClockAndSnapshot() throws MalformedURLException {
		when(mockGridDao.getConnection(connectionId)).thenReturn(
				Optional.of(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)));

		GridSnapshot snapshot = new GridSnapshot().setS3Key("snapshot-key-123");
		when(mockGridDao.getLatestSnapshot(gridSessionId)).thenReturn(Optional.of(snapshot));

		URL presignedUrl = new URL("https://example.com/snapshot");
		when(mockSynapseS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(presignedUrl);

		// call under test
		Optional<String> result = gridManager.getNextSynchronizeResponse(eventContext, Collections.emptyList());

		assertTrue(result.isPresent());
		String response = result.get();
		JSONObject json = new JSONObject(response);
		assertEquals("snapshot", json.getString("type"));
		assertEquals("https://example.com/snapshot", json.getString("body"));
	}

	@Test
	public void testGetNextSynchronizeResponseWithEmptyClockAndNoSnapshotButHasPatch() {
		when(mockGridDao.getConnection(connectionId)).thenReturn(
				Optional.of(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)));

		when(mockGridDao.getLatestSnapshot(gridSessionId)).thenReturn(Optional.empty());

		// A single patch is available with known size
		LogicalTimestamp patchTs = new LogicalTimestamp().setReplicaId(44L).setSequenceNumber(90L);
		PatchInfo patchInfo = new PatchInfo().setSessionId(gridSessionId).setPatchId(patchTs)
				.setS3Key("key1").setSizeBytes(500L).setExpiresOn(new Timestamp(System.currentTimeMillis() + 10000));
		List<PatchInfo> candidates = List.of(patchInfo);
		when(mockGridDao.listMissingPatchInfoForClock(gridSessionId, Collections.emptyList(), GridManagerImpl.PATCH_BATCH_CANDIDATE_LIMIT))
				.thenReturn(candidates);
		doReturn(Optional.of(patchBody)).when(gridManager).getPatchBody(gridSessionId, patchInfo);

		// call under test
		Optional<String> result = gridManager.getNextSynchronizeResponse(eventContext, Collections.emptyList());

		assertTrue(result.isPresent());
		String response = result.get();
		JSONObject json = new JSONObject(result.get());
		assertEquals("patches", json.getString("type"));
		assertEquals(1, json.getJSONArray("body").length());
		assertEquals(patchBody.toString(), json.getJSONArray("body").getJSONArray(0).toString());

		verifyZeroInteractions(mockSynapseS3Client);
	}

	@Test
	public void testGetNextSynchronizeResponseWithEmptyClockNoSnapshotNoPatch() {
		when(mockGridDao.getConnection(connectionId)).thenReturn(
				Optional.of(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)));

		when(mockGridDao.getLatestSnapshot(gridSessionId)).thenReturn(Optional.empty());

		// No patches available
		when(mockGridDao.listMissingPatchInfoForClock(gridSessionId, Collections.emptyList(), GridManagerImpl.PATCH_BATCH_CANDIDATE_LIMIT))
				.thenReturn(Collections.emptyList());

		// call under test
		Optional<String> result = gridManager.getNextSynchronizeResponse(eventContext, Collections.emptyList());

		assertTrue(result.isEmpty());
		verifyZeroInteractions(mockSynapseS3Client);
	}

	@Test
	public void testGetNextSynchronizeResponseWithNonEmptyClockAndPatch() {
		when(mockGridDao.getConnection(connectionId)).thenReturn(
				Optional.of(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)));

		LogicalTimestamp patchTs = new LogicalTimestamp().setReplicaId(44L).setSequenceNumber(90L);
		PatchInfo patchInfo = new PatchInfo().setSessionId(gridSessionId).setPatchId(patchTs)
				.setS3Key("key1").setSizeBytes(500L).setExpiresOn(new Timestamp(System.currentTimeMillis() + 10000));
		List<PatchInfo> candidates = List.of(patchInfo);
		when(mockGridDao.listMissingPatchInfoForClock(gridSessionId, clock, GridManagerImpl.PATCH_BATCH_CANDIDATE_LIMIT))
				.thenReturn(candidates);
		doReturn(Optional.of(patchBody)).when(gridManager).getPatchBody(gridSessionId, patchInfo);

		// call under test
		Optional<String> result = gridManager.getNextSynchronizeResponse(eventContext, clock);

		assertTrue(result.isPresent());
		String response = result.get();
		JSONObject json = new JSONObject(result.get());
		assertEquals("patches", json.getString("type"));
		assertEquals(1, json.getJSONArray("body").length());
		assertEquals(patchBody.toString(), json.getJSONArray("body").getJSONArray(0).toString());


		// Should not check for snapshot when clock is non-empty
		verify(mockGridDao, never()).getLatestSnapshot(any());
	}

	@Test
	public void testGetNextSynchronizeResponseWithNonEmptyClockAndNoPatch() {
		when(mockGridDao.getConnection(connectionId)).thenReturn(
				Optional.of(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)));

		when(mockGridDao.listMissingPatchInfoForClock(gridSessionId, clock, GridManagerImpl.PATCH_BATCH_CANDIDATE_LIMIT))
				.thenReturn(Collections.emptyList());

		// call under test
		Optional<String> result = gridManager.getNextSynchronizeResponse(eventContext, clock);

		assertTrue(result.isEmpty());

		// Should not check for snapshot when clock is non-empty
		verify(mockGridDao, never()).getLatestSnapshot(any());
	}

	@Test
	public void testGetNextSynchronizeResponseWithMultipleSmallPatches() {
		when(mockGridDao.getConnection(connectionId)).thenReturn(
				Optional.of(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)));

		String patchBody1 = "[[[1,1]],[0]]";
		String patchBody2 = "[[[2,2]],[0]]";
		LogicalTimestamp ts1 = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L);
		LogicalTimestamp ts2 = new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(2L);
		PatchInfo patchInfo1 = new PatchInfo().setSessionId(gridSessionId).setPatchId(ts1).setS3Key("k1").setSizeBytes(100L)
				.setExpiresOn(new Timestamp(System.currentTimeMillis() + 10000));
		PatchInfo patchInfo2 = new PatchInfo().setSessionId(gridSessionId).setPatchId(ts2).setS3Key("k2").setSizeBytes(100L)
				.setExpiresOn(new Timestamp(System.currentTimeMillis() + 10000));
		List<PatchInfo> candidates = List.of(patchInfo1, patchInfo2);
		when(mockGridDao.listMissingPatchInfoForClock(gridSessionId, clock, GridManagerImpl.PATCH_BATCH_CANDIDATE_LIMIT))
				.thenReturn(candidates);
		doReturn(Optional.of(patchBody1)).when(gridManager).getPatchBody(gridSessionId, patchInfo1);
		doReturn(Optional.of(patchBody2)).when(gridManager).getPatchBody(gridSessionId, patchInfo2);

		// call under test
		Optional<String> result = gridManager.getNextSynchronizeResponse(eventContext, clock);

		assertTrue(result.isPresent());
		JSONObject json = new JSONObject(result.get());
		assertEquals("patches", json.getString("type"));
		assertEquals(2, json.getJSONArray("body").length());
		assertEquals(patchBody1, json.getJSONArray("body").getString(0));
		assertEquals(patchBody2, json.getJSONArray("body").getString(1));
	}

	@Test
	public void testGetNextSynchronizeResponseWithNullSizeFallback() {
		when(mockGridDao.getConnection(connectionId)).thenReturn(
				Optional.of(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)));

		// First candidate has null sizeBytes (pre-existing record)
		LogicalTimestamp ts1 = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L);
		PatchInfo patchInfo1 = new PatchInfo().setSessionId(gridSessionId).setPatchId(ts1).setS3Key("k1").setSizeBytes(null)
				.setExpiresOn(new Timestamp(System.currentTimeMillis() + 10000));
		List<PatchInfo> candidates = List.of(patchInfo1);
		when(mockGridDao.listMissingPatchInfoForClock(gridSessionId, clock, GridManagerImpl.PATCH_BATCH_CANDIDATE_LIMIT))
				.thenReturn(candidates);
		doReturn(Optional.of(patchBody)).when(gridManager).getPatchBody(gridSessionId, patchInfo1);

		// call under test
		Optional<String> result = gridManager.getNextSynchronizeResponse(eventContext, clock);

		assertTrue(result.isPresent());
		JSONObject json = new JSONObject(result.get());
		assertEquals("patches", json.getString("type"));
		assertEquals(1, json.getJSONArray("body").length());
		assertEquals(patchBody.toString(), json.getJSONArray("body").getJSONArray(0).toString());
	}

	@Test
	public void testGetNextSynchronizeResponseWithNullSizeMidBatchStops() {
		when(mockGridDao.getConnection(connectionId)).thenReturn(
				Optional.of(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)));

		String patchBody1 = "[[[1,1]],[0]]";
		LogicalTimestamp ts1 = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L);
		LogicalTimestamp ts2 = new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(2L);
		PatchInfo patchInfo1 = new PatchInfo().setSessionId(gridSessionId).setPatchId(ts1).setS3Key("k1").setSizeBytes(100L)
				.setExpiresOn(new Timestamp(System.currentTimeMillis() + 10000));
		PatchInfo patchInfo2 = new PatchInfo().setSessionId(gridSessionId).setPatchId(ts2).setS3Key("k2").setSizeBytes(null)
				.setExpiresOn(new Timestamp(System.currentTimeMillis() + 10000));
		List<PatchInfo> candidates = List.of(patchInfo1, patchInfo2);
		when(mockGridDao.listMissingPatchInfoForClock(gridSessionId, clock, GridManagerImpl.PATCH_BATCH_CANDIDATE_LIMIT))
				.thenReturn(candidates);
		doReturn(Optional.of(patchBody1)).when(gridManager).getPatchBody(gridSessionId, patchInfo1);

		// call under test
		Optional<String> result = gridManager.getNextSynchronizeResponse(eventContext, clock);

		assertTrue(result.isPresent());
		JSONObject json = new JSONObject(result.get());
		assertEquals("patches", json.getString("type"));
		assertEquals(1, json.getJSONArray("body").length());
		assertEquals(patchBody1, json.getJSONArray("body").getString(0));

		// Second patch should not be fetched
		verify(gridManager, never()).getPatchBody(gridSessionId, patchInfo2);
	}

	@Test
	public void testGetNextSynchronizeResponseWithBudgetExceeded() {
		when(mockGridDao.getConnection(connectionId)).thenReturn(
				Optional.of(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)));

		String patchBody1 = "[[[1,1]],[0]]";
		LogicalTimestamp ts1 = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L);
		LogicalTimestamp ts2 = new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(2L);
		PatchInfo patchInfo1 = new PatchInfo().setSessionId(gridSessionId).setPatchId(ts1).setS3Key("k1")
				.setSizeBytes(GridManagerImpl.PATCH_BATCH_BUDGET_BYTES - 10)
				.setExpiresOn(new Timestamp(System.currentTimeMillis() + 10000));
		PatchInfo patchInfo2 = new PatchInfo().setSessionId(gridSessionId).setPatchId(ts2).setS3Key("k2")
				.setSizeBytes(500L)
				.setExpiresOn(new Timestamp(System.currentTimeMillis() + 10000));
		// First patch nearly fills the budget
		List<PatchInfo> candidates = List.of(patchInfo1, patchInfo2);
		when(mockGridDao.listMissingPatchInfoForClock(gridSessionId, clock, GridManagerImpl.PATCH_BATCH_CANDIDATE_LIMIT))
				.thenReturn(candidates);
		doReturn(Optional.of(patchBody1)).when(gridManager).getPatchBody(gridSessionId, patchInfo1);

		// call under test
		Optional<String> result = gridManager.getNextSynchronizeResponse(eventContext, clock);

		assertTrue(result.isPresent());
		// Only first patch fits, so single-patch format
		JSONObject json = new JSONObject(result.get());
		assertEquals("patches", json.getString("type"));
		assertEquals(1, json.getJSONArray("body").length());
		assertEquals(patchBody1, json.getJSONArray("body").getString(0));

		// Second patch should not be fetched
		verify(gridManager, never()).getPatchBody(gridSessionId, patchInfo2);
	}

	@Test
	public void testBackfillGridSessionChanges() {
		List<String> firstBatch = List.of(
				GridUtils.gridSessionIdAsString(100L),
				GridUtils.gridSessionIdAsString(200L));
		List<String> emptyBatch = Collections.emptyList();
		when(mockGridDao.listAllSessionIds(100, 0)).thenReturn(firstBatch);
		when(mockGridDao.listAllSessionIds(100, 100)).thenReturn(emptyBatch);

		// call under test
		long count = gridManager.backfillGridSessionChanges();

		assertEquals(2L, count);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(
				"100", org.sagebionetworks.repo.model.ObjectType.GRID_SESSION,
				org.sagebionetworks.repo.model.message.ChangeType.UPDATE);
		verify(mockTransactionalMessenger).sendMessageAfterCommit(
				"200", org.sagebionetworks.repo.model.ObjectType.GRID_SESSION,
				org.sagebionetworks.repo.model.message.ChangeType.UPDATE);
	}

	@Test
	public void testBackfillGridSessionChangesWithNoSessions() {
		when(mockGridDao.listAllSessionIds(100, 0)).thenReturn(Collections.emptyList());

		// call under test
		long count = gridManager.backfillGridSessionChanges();

		assertEquals(0L, count);
		verify(mockTransactionalMessenger, never()).sendMessageAfterCommit(any(), any(), any());
	}

	@Test
	public void testListReplicas() {
		doNothing().when(gridManager).validGridSessionAccess(mockUser, gridSessionId);
		List<GridReplicaInfo> replicas = List.of(
				new GridReplicaInfo().setReplicaId(1L).setCreatedBy("123").setIsConnected(true)
						.setReplicaType(GridReplicaType.USER),
				new GridReplicaInfo().setReplicaId(2L).setCreatedBy("456").setIsConnected(false)
						.setReplicaType(GridReplicaType.AGENT));
		when(mockGridDao.listReplicas(gridSessionId, 51L, 0L)).thenReturn(replicas);

		ListGridReplicasRequest request = new ListGridReplicasRequest().setGridSessionId(gridSessionId);

		// call under test
		ListGridReplicasResponse response = gridManager.listReplicas(mockUser, request);

		ListGridReplicasResponse expected = new ListGridReplicasResponse().setPage(replicas).setNextPageToken(null);
		assertEquals(expected, response);
	}

	@Test
	public void testListReplicasWithNextPageToken() {
		doNothing().when(gridManager).validGridSessionAccess(mockUser, gridSessionId);
		List<GridReplicaInfo> page = IntStream.range(0, 51)
				.mapToObj(i -> new GridReplicaInfo().setReplicaId((long) i).setCreatedBy("123").setIsConnected(false)
						.setReplicaType(GridReplicaType.USER))
				.collect(Collectors.toList());
		when(mockGridDao.listReplicas(gridSessionId, 51L, 0L)).thenReturn(page);

		ListGridReplicasRequest request = new ListGridReplicasRequest().setGridSessionId(gridSessionId);

		// call under test
		ListGridReplicasResponse response = gridManager.listReplicas(mockUser, request);

		assertEquals(50, response.getPage().size());
		assertNotNull(response.getNextPageToken());
		assertEquals("50a50", response.getNextPageToken());
	}

	@Test
	public void testListReplicasWithNullUser() {
		mockUser = null;
		ListGridReplicasRequest request = new ListGridReplicasRequest().setGridSessionId(gridSessionId);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.listReplicas(mockUser, request);
		}).getMessage();
		assertEquals("user is required.", message);
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testListReplicasWithNullRequest() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.listReplicas(mockUser, null);
		}).getMessage();
		assertEquals("request is required.", message);
		verifyZeroInteractions(mockGridDao);
	}

	@Test
	public void testListReplicasWithNullGridSessionId() {
		ListGridReplicasRequest request = new ListGridReplicasRequest().setGridSessionId(null);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.listReplicas(mockUser, request);
		}).getMessage();
		assertEquals("request.gridSessionId is required.", message);
		verifyZeroInteractions(mockGridDao);
	}

	private GridQueryJobRequest buildQueryRequest(Long replicaId) {
		return new GridQueryJobRequest()
				.setSessionId(gridSessionId)
				.setReplicaId(replicaId)
				.setQueryRequest(new QueryRequest().setQuery(new org.sagebionetworks.repo.model.grid.query.Query()));
	}

	@Test
	public void testQueryGridWithValidRequest() {
		doNothing().when(gridManager).validGridSessionAccess(mockUser, gridSessionId);
		doNothing().when(gridManager).validateRepicaOwner(mockUser, gridSessionId, replicaId);
		GridConnectionInfo internalConnection = new GridConnectionInfo().setReplicaId(replicaId);
		when(mockGridDao.getSingletonConnection(gridSessionId, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));
		GridHeader header = new GridHeader().setReplicaId(replicaId);
		when(mockGridReplicaViewManager.readHeader(gridSessionId, replicaId, replicaId)).thenReturn(Optional.of(header));
		QueryResult expectedQueryResult = new QueryResult();
		when(mockGridReplicaViewManager.querySinglePageAsQueryResult(eq(header), any(QueryElement.class)))
				.thenReturn(expectedQueryResult);

		// call under test
		GridQueryJobResponse result = gridManager.queryGrid(mockUser, buildQueryRequest(replicaId));

		assertEquals(expectedQueryResult, result.getQueryResult());
		verify(gridManager).validGridSessionAccess(mockUser, gridSessionId);
		verify(mockGridDao).getSingletonConnection(gridSessionId, EventSource.INTERNAL);
		verify(mockGridReplicaViewManager).readHeader(gridSessionId, replicaId, replicaId);
		verify(mockGridReplicaViewManager).querySinglePageAsQueryResult(eq(header), any(QueryElement.class));
	}

	@Test
	public void testQueryGridWithNullUser() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.queryGrid(null, buildQueryRequest(replicaId));
		}).getMessage();
		assertEquals("user is required.", message);
		verifyZeroInteractions(mockGridDao, mockGridReplicaViewManager);
	}

	@Test
	public void testQueryGridWithNullRequest() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.queryGrid(mockUser, null);
		}).getMessage();
		assertEquals("request is required.", message);
		verifyZeroInteractions(mockGridDao, mockGridReplicaViewManager);
	}

	@Test
	public void testQueryGridWithNullSessionId() {
		GridQueryJobRequest request = buildQueryRequest(replicaId).setSessionId(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.queryGrid(mockUser, request);
		}).getMessage();
		assertEquals("request.sessionId is required.", message);
		verifyZeroInteractions(mockGridDao, mockGridReplicaViewManager);
	}

	@Test
	public void testQueryGridWithNullReplicaId() {
		GridQueryJobRequest request = buildQueryRequest(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.queryGrid(mockUser, request);
		}).getMessage();
		assertEquals("request.replicaId is required.", message);
		verifyZeroInteractions(mockGridDao, mockGridReplicaViewManager);
	}

	@Test
	public void testQueryGridWithNullQueryRequest() {
		GridQueryJobRequest request = buildQueryRequest(replicaId).setQueryRequest(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.queryGrid(mockUser, request);
		}).getMessage();
		assertEquals("request.queryRequest is required.", message);
		verifyZeroInteractions(mockGridDao, mockGridReplicaViewManager);
	}

	@Test
	public void testQueryGridWithNullQuery() {
		GridQueryJobRequest request = buildQueryRequest(replicaId)
				.setQueryRequest(new QueryRequest());
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.queryGrid(mockUser, request);
		}).getMessage();
		assertEquals("request.queryRequest.query is required.", message);
		verifyZeroInteractions(mockGridDao, mockGridReplicaViewManager);
	}

	@Test
	public void testQueryGridWithNoLimitDefaultsTo100() {
		doNothing().when(gridManager).validGridSessionAccess(mockUser, gridSessionId);
		doNothing().when(gridManager).validateRepicaOwner(mockUser, gridSessionId, replicaId);
		GridConnectionInfo internalConnection = new GridConnectionInfo().setReplicaId(replicaId);
		when(mockGridDao.getSingletonConnection(gridSessionId, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));
		GridHeader header = new GridHeader().setReplicaId(replicaId);
		when(mockGridReplicaViewManager.readHeader(gridSessionId, replicaId, replicaId)).thenReturn(Optional.of(header));
		when(mockGridReplicaViewManager.querySinglePageAsQueryResult(eq(header), any(QueryElement.class)))
				.thenReturn(new QueryResult());

		GridQueryJobRequest request = buildQueryRequest(replicaId);

		// call under test
		gridManager.queryGrid(mockUser, request);

		assertEquals(GridManagerImpl.DEFAULT_QUERY_LIMIT, request.getQueryRequest().getQuery().getLimit());
	}

	@Test
	public void testQueryGridWithUnauthorizedUser() {
		when(mockGridAuthManager.hasGridSessionAccess(mockUser, gridSessionId))
				.thenReturn(AuthorizationStatus.accessDenied("not allowed"));

		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			gridManager.queryGrid(mockUser, buildQueryRequest(replicaId));
		});
		verifyZeroInteractions(mockGridReplicaViewManager);
		verify(mockGridDao, never()).getSingletonConnection(any(), any());
	}

	@Test
	public void testQueryGridWithNoInternalConnection() {
		doNothing().when(gridManager).validGridSessionAccess(mockUser, gridSessionId);
		doNothing().when(gridManager).validateRepicaOwner(mockUser, gridSessionId, replicaId);
		when(mockGridDao.getSingletonConnection(gridSessionId, EventSource.INTERNAL)).thenReturn(Optional.empty());

		assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			gridManager.queryGrid(mockUser, buildQueryRequest(replicaId));
		});
		verifyZeroInteractions(mockGridReplicaViewManager);
	}

	@Test
	public void testQueryGridWithLimitAlreadySet() {
		doNothing().when(gridManager).validGridSessionAccess(mockUser, gridSessionId);
		doNothing().when(gridManager).validateRepicaOwner(mockUser, gridSessionId, replicaId);
		GridConnectionInfo internalConnection = new GridConnectionInfo().setReplicaId(replicaId);
		when(mockGridDao.getSingletonConnection(gridSessionId, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));
		when(mockGridReplicaViewManager.readHeader(gridSessionId, replicaId, replicaId))
				.thenReturn(Optional.of(new GridHeader().setReplicaId(replicaId)));
		when(mockGridReplicaViewManager.querySinglePageAsQueryResult(any(), any(QueryElement.class)))
				.thenReturn(new QueryResult());

		Long existingLimit = 50L;
		GridQueryJobRequest request = new GridQueryJobRequest()
				.setSessionId(gridSessionId)
				.setReplicaId(replicaId)
				.setQueryRequest(new QueryRequest().setQuery(
						new org.sagebionetworks.repo.model.grid.query.Query().setLimit(existingLimit)));

		// call under test
		gridManager.queryGrid(mockUser, request);

		assertEquals(existingLimit, request.getQueryRequest().getQuery().getLimit());
	}

	@Test
	public void testQueryGridWithNoHeader() {
		doNothing().when(gridManager).validGridSessionAccess(mockUser, gridSessionId);
		doNothing().when(gridManager).validateRepicaOwner(mockUser, gridSessionId, replicaId);
		GridConnectionInfo internalConnection = new GridConnectionInfo().setReplicaId(replicaId);
		when(mockGridDao.getSingletonConnection(gridSessionId, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));
		when(mockGridReplicaViewManager.readHeader(gridSessionId, replicaId, replicaId))
				.thenReturn(Optional.empty());

		assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			gridManager.queryGrid(mockUser, buildQueryRequest(replicaId));
		});
		verify(mockGridReplicaViewManager, never()).querySinglePageAsQueryResult(any(), any());
	}

	// ─── executeGridUpdate ───────────────────────────────────────────────────────

	private GridHeader buildUpdateHeader() {
		return new GridHeader()
				.setOrderedColumns(List.of(new Column().setName("colA").setVectorIndex(0),
						new Column().setName("colB").setVectorIndex(1)))
				.setClockSequenceMaximum(500L);
	}

	private RowView buildRowView(long rep, long seq) {
		LogicalTimestamp vectorId = new LogicalTimestamp().setReplicaId(rep).setSequenceNumber(seq);
		return new RowView().setRowObject(
				new RowObject().setData(new RowData().setVectorId(vectorId).setRowJsonDocument(new JSONObject())));
	}

	private JSONObject buildRawUpdate(String columnName, Object value) {
		return JDOSecondaryPropertyUtils.createJSONObjectForEntity(
				new Update().setSet(List.of(new LiteralSetValue().setColumnName(columnName).setValue(value))));
	}

	@Test
	public void testExecuteGridUpdateWithValidUpdate() throws Exception {
		GridHeader header = buildUpdateHeader();
		GridConnectionInfo connection = new GridConnectionInfo().setReplicaId(replicaId).setConnectionId("conn1");
		JSONObject rawUpdate = buildRawUpdate("colA", "newValue");
		List<RowView> rows = List.of(buildRowView(1L, 1L), buildRowView(1L, 2L));
		when(mockGridReplicaViewManager.getQueryIterator(eq(header), any(QueryElement.class)))
				.thenReturn(rows.iterator());
		when(mockSetValueProcessorFactory.createConValue(any(), any(), any()))
				.thenReturn(Optional.of(new ConValue(ConType.STRING, "newValue")));

		// call under test
		long count = gridManager.executeGridUpdate(header, connection, rawUpdate);

		assertEquals(2L, count);
		ArgumentCaptor<IntendedChangeSet> captor = ArgumentCaptor.forClass(IntendedChangeSet.class);
		verify(mockPatchBuilderPublisher).sendChangesToPatchBuilder(captor.capture());
		assertEquals(2, captor.getValue().getChanges().size());
	}

	@Test
	public void testExecuteGridUpdateWithSomeRowsSkipped() throws Exception {
		GridHeader header = buildUpdateHeader();
		GridConnectionInfo connection = new GridConnectionInfo().setReplicaId(replicaId).setConnectionId("conn1");
		JSONObject rawUpdate = buildRawUpdate("colA", "newValue");
		List<RowView> rows = List.of(buildRowView(1L, 1L), buildRowView(1L, 2L));
		when(mockGridReplicaViewManager.getQueryIterator(eq(header), any(QueryElement.class)))
				.thenReturn(rows.iterator());
		when(mockSetValueProcessorFactory.createConValue(any(), any(), any()))
				.thenReturn(Optional.of(new ConValue(ConType.STRING, "newValue")))
				.thenReturn(Optional.empty());

		// call under test
		long count = gridManager.executeGridUpdate(header, connection, rawUpdate);

		assertEquals(1L, count);
	}

	@Test
	public void testExecuteGridUpdateWithAllRowsSkipped() throws Exception {
		GridHeader header = buildUpdateHeader();
		GridConnectionInfo connection = new GridConnectionInfo().setReplicaId(replicaId).setConnectionId("conn1");
		JSONObject rawUpdate = buildRawUpdate("colA", "newValue");
		List<RowView> rows = List.of(buildRowView(1L, 1L), buildRowView(1L, 2L));
		when(mockGridReplicaViewManager.getQueryIterator(eq(header), any(QueryElement.class)))
				.thenReturn(rows.iterator());
		when(mockSetValueProcessorFactory.createConValue(any(), any(), any()))
				.thenReturn(Optional.empty());

		// call under test
		long count = gridManager.executeGridUpdate(header, connection, rawUpdate);

		assertEquals(0L, count);
		verify(mockPatchBuilderPublisher, never()).sendChangesToPatchBuilder(any());
	}

	@Test
	public void testExecuteGridUpdateWithFilters() throws Exception {
		GridHeader header = buildUpdateHeader();
		GridConnectionInfo connection = new GridConnectionInfo().setReplicaId(replicaId).setConnectionId("conn1");
		// Build a raw update that includes a filter
		JSONObject rawUpdate = JDOSecondaryPropertyUtils.createJSONObjectForEntity(
				new Update()
						.setSet(List.of(new LiteralSetValue().setColumnName("colA").setValue("newValue")))
						.setFilters(List.of(
								new org.sagebionetworks.repo.model.grid.query.RowSelectionFilter().setIsSelected(true))));
		when(mockGridReplicaViewManager.getQueryIterator(eq(header), any(QueryElement.class)))
				.thenReturn(List.of(buildRowView(1L, 1L)).iterator());
		when(mockSetValueProcessorFactory.createConValue(any(), any(), any()))
				.thenReturn(Optional.of(new ConValue(ConType.STRING, "newValue")));

		// call under test
		long count = gridManager.executeGridUpdate(header, connection, rawUpdate);

		assertEquals(1L, count);
		// verify the query iterator was called with a QueryElement that has the filter
		ArgumentCaptor<QueryElement> queryCaptor = ArgumentCaptor.forClass(QueryElement.class);
		verify(mockGridReplicaViewManager).getQueryIterator(eq(header), queryCaptor.capture());
		assertNotNull(queryCaptor.getValue().getWhere());
		assertFalse(queryCaptor.getValue().getWhere().isEmpty());
	}

	@Test
	public void testExecuteGridUpdateWithUnknownColumnName() throws Exception {
		GridHeader header = buildUpdateHeader(); // has colA and colB
		GridConnectionInfo connection = new GridConnectionInfo().setReplicaId(replicaId).setConnectionId("conn1");
		// Set value targets a column that does not exist in the header
		JSONObject rawUpdate = buildRawUpdate("unknownCol", "value");

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.executeGridUpdate(header, connection, rawUpdate);
		}).getMessage();

		assertEquals("Column name: unknownCol not found.", message);
	}

	@Test
	public void testExecuteGridUpdateWithNoRows() throws Exception {
		GridHeader header = buildUpdateHeader();
		GridConnectionInfo connection = new GridConnectionInfo().setReplicaId(replicaId).setConnectionId("conn1");
		JSONObject rawUpdate = buildRawUpdate("colA", "newValue");
		when(mockGridReplicaViewManager.getQueryIterator(eq(header), any(QueryElement.class)))
				.thenReturn(Collections.emptyIterator());

		// call under test
		long count = gridManager.executeGridUpdate(header, connection, rawUpdate);

		assertEquals(0L, count);
		verify(mockPatchBuilderPublisher, never()).sendChangesToPatchBuilder(any());
	}

	// ─── updateGrid ──────────────────────────────────────────────────────────────

	private GridUpdateJobRequest buildUpdateRequest(Long replicaId) {
		return new GridUpdateJobRequest()
				.setSessionId(gridSessionId)
				.setReplicaId(replicaId)
				.setUpdateRequest(new GridUpdateRequest()
						.setUpdate(new UpdateBatch().setBatch(List.of(
								new Update().setSet(List.of(
										new LiteralSetValue().setColumnName("colA").setValue("val1")))))));
	}

	@Test
	public void testUpdateGridWithValidRequestAndExistingConnection() throws Exception {
		GridConnectionInfo internalConnection = new GridConnectionInfo().setReplicaId(replicaId);
		GridConnectionInfo apiConnection = new GridConnectionInfo().setReplicaId(replicaId).setConnectionId("api-conn");
		GridHeader header = buildUpdateHeader();
		doNothing().when(gridManager).validGridSessionAccess(mockUser, gridSessionId);
		doNothing().when(gridManager).validateRepicaOwner(mockUser, gridSessionId, replicaId);
		when(mockGridDao.getSingletonConnection(gridSessionId, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));
		when(mockGridReplicaViewManager.readHeader(gridSessionId, replicaId, replicaId))
				.thenReturn(Optional.of(header));
		when(mockGridDao.getConnection(gridSessionId, replicaId)).thenReturn(Optional.of(apiConnection));
		doReturn(2L).when(gridManager).executeGridUpdate(eq(header), eq(apiConnection), any(JSONObject.class));

		// call under test
		GridUpdateJobResponse response = gridManager.updateGrid(mockUser, buildUpdateRequest(replicaId));

		assertNotNull(response.getUpdateResponse());
		assertEquals(2L, (long) response.getUpdateResponse().getTotalRowsUpdated());
		verify(mockGridDao, never()).createConnection(any());
	}

	@Test
	public void testUpdateGridWithNoExistingConnectionCreatesOne() throws Exception {
		GridConnectionInfo internalConnection = new GridConnectionInfo().setReplicaId(replicaId);
		GridConnectionInfo apiConnection = new GridConnectionInfo().setReplicaId(replicaId).setConnectionId("api-conn");
		GridHeader header = buildUpdateHeader();
		doNothing().when(gridManager).validGridSessionAccess(mockUser, gridSessionId);
		doNothing().when(gridManager).validateRepicaOwner(mockUser, gridSessionId, replicaId);
		when(mockGridDao.getSingletonConnection(gridSessionId, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));
		when(mockGridReplicaViewManager.readHeader(gridSessionId, replicaId, replicaId))
				.thenReturn(Optional.of(header));
		when(mockGridDao.getConnection(gridSessionId, replicaId))
				.thenReturn(Optional.empty())
				.thenReturn(Optional.of(apiConnection));
		doReturn(1L).when(gridManager).executeGridUpdate(eq(header), eq(apiConnection), any(JSONObject.class));

		// call under test
		GridUpdateJobResponse response = gridManager.updateGrid(mockUser, buildUpdateRequest(replicaId));

		assertEquals(1L, (long) response.getUpdateResponse().getTotalRowsUpdated());
		ArgumentCaptor<GridConnectionInfo> connCaptor = ArgumentCaptor.forClass(GridConnectionInfo.class);
		verify(mockGridDao).createConnection(connCaptor.capture());
		assertEquals(EventSource.API, connCaptor.getValue().getSource());
		assertEquals(gridSessionId, connCaptor.getValue().getSessionId());
		assertEquals(replicaId, connCaptor.getValue().getReplicaId());
	}

	@Test
	public void testUpdateGridWithNullUser() {
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.updateGrid(null, buildUpdateRequest(replicaId));
		});
		verifyZeroInteractions(mockGridDao, mockGridReplicaViewManager);
	}

	@Test
	public void testUpdateGridWithNullSessionId() {
		GridUpdateJobRequest request = buildUpdateRequest(replicaId).setSessionId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.updateGrid(mockUser, request);
		});
		verifyZeroInteractions(mockGridDao, mockGridReplicaViewManager);
	}

	@Test
	public void testUpdateGridWithNullReplicaId() {
		GridUpdateJobRequest request = buildUpdateRequest(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.updateGrid(mockUser, request);
		});
		verifyZeroInteractions(mockGridDao, mockGridReplicaViewManager);
	}

	@Test
	public void testUpdateGridWithNullRequest() {
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			gridManager.updateGrid(mockUser, null);
		});
		verifyZeroInteractions(mockGridDao, mockGridReplicaViewManager);
	}

	@Test
	public void testUpdateGridWithUnauthorizedUser() {
		when(mockGridAuthManager.hasGridSessionAccess(mockUser, gridSessionId))
				.thenReturn(AuthorizationStatus.accessDenied("not allowed"));
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			gridManager.updateGrid(mockUser, buildUpdateRequest(replicaId));
		});
		verifyZeroInteractions(mockGridReplicaViewManager);
		verify(mockGridDao, never()).getSingletonConnection(any(), any());
	}

	@Test
	public void testUpdateGridWithNoInternalConnection() {
		doNothing().when(gridManager).validGridSessionAccess(mockUser, gridSessionId);
		doNothing().when(gridManager).validateRepicaOwner(mockUser, gridSessionId, replicaId);
		when(mockGridDao.getSingletonConnection(gridSessionId, EventSource.INTERNAL)).thenReturn(Optional.empty());
		assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			gridManager.updateGrid(mockUser, buildUpdateRequest(replicaId));
		});
		verifyZeroInteractions(mockGridReplicaViewManager);
	}

	@Test
	public void testUpdateGridWithNoHeader() throws Exception {
		GridConnectionInfo internalConnection = new GridConnectionInfo().setReplicaId(replicaId);
		doNothing().when(gridManager).validGridSessionAccess(mockUser, gridSessionId);
		doNothing().when(gridManager).validateRepicaOwner(mockUser, gridSessionId, replicaId);
		when(mockGridDao.getSingletonConnection(gridSessionId, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));
		when(mockGridReplicaViewManager.readHeader(gridSessionId, replicaId, replicaId))
				.thenReturn(Optional.empty());

		assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			gridManager.updateGrid(mockUser, buildUpdateRequest(replicaId));
		});
		verify(mockGridDao, never()).getConnection(any(), any());
	}

	@Test
	public void testUpdateGridWithConnectionCreationFailure() throws Exception {
		GridConnectionInfo internalConnection = new GridConnectionInfo().setReplicaId(replicaId);
		GridHeader header = buildUpdateHeader();
		doNothing().when(gridManager).validGridSessionAccess(mockUser, gridSessionId);
		doNothing().when(gridManager).validateRepicaOwner(mockUser, gridSessionId, replicaId);
		when(mockGridDao.getSingletonConnection(gridSessionId, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));
		when(mockGridReplicaViewManager.readHeader(gridSessionId, replicaId, replicaId))
				.thenReturn(Optional.of(header));
		// both getConnection calls return empty — simulates a failed lazy creation
		when(mockGridDao.getConnection(gridSessionId, replicaId)).thenReturn(Optional.empty());

		assertThrows(IllegalStateException.class, () -> {
			// call under test
			gridManager.updateGrid(mockUser, buildUpdateRequest(replicaId));
		});
	}

}
