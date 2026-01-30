package org.sagebionetworks.repo.manager.grid;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import org.sagebionetworks.repo.manager.config.WebsocketApi;
import org.sagebionetworks.repo.manager.grid.create.CreateGridHandler;
import org.sagebionetworks.repo.manager.grid.create.CreateGridHandlerResult;
import org.sagebionetworks.repo.manager.grid.response.InternalReplicaToHubEventPublisher;
import org.sagebionetworks.repo.manager.table.RowHandlerProvider;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
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
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.GridUtils;
import org.sagebionetworks.repo.model.grid.ListGridSessionsRequest;
import org.sagebionetworks.repo.model.grid.ListGridSessionsResponse;
import org.sagebionetworks.repo.model.grid.PatchInfo;
import org.sagebionetworks.repo.model.grid.internal.Connection;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.web.NotFoundException;

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
	private UserInfo mockUser;

	@Mock
	private TableQueryManager mockQueryManager;

	@Mock
	private AsyncJobProgressCallback mockCallback;

	@Mock
	private InternalReplicaToHubEventPublisher mockInternalEventPublisher;
	
	@Mock
	private GridAuthorizationManager mockGridAuthManager;
	
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
	private String patchBody;
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
		patchBody = "[[[66537,1]],[0]]";

		when(mockConfig.getStack()).thenReturn("dev");
		gridManager = new GridManagerImpl(mockCredentialsProvider, mockWebsocketApi, mockGridDao, mockConfig,
			mockS3Client, mockInternalEventPublisher, List.of(mockCreateGridHandler),mockGridAuthManager
		);
		
		gridManager = Mockito.spy(gridManager);
		clock = List.of(patchId);
		query = new Query().setSql("select * from syn123").setIncludeEntityEtag(false);
		tableId = "syn999";

		listGridSessionRequest = new ListGridSessionsRequest().setSourceId(tableId);
		gridSession = new GridSession();
		gridSessions = List.of(gridSession);
		recordSet = new RecordSet().setId("987");
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
		doReturn(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)).when(gridManager)
				.getConnectionInfo(connectionId);
		when(mockS3Client.putObject(putCaptor.capture(), bodyCaptor.capture())).thenReturn(null);
		when(mockGridDao.savePatch(any(), any(), any(), any())).thenReturn(true);
		// call under test
		boolean isNew = gridManager.savePatch(eventContext, patchId, patchBody);
		assertTrue(isNew);

		assertEquals("dev.grid.patch.sagebase.org", putCaptor.getValue().bucket());
		String key = putCaptor.getValue().key();
		assertTrue(key.endsWith(".json"));
		assertEquals(RequestBody.fromString(patchBody, StandardCharsets.UTF_8).optionalContentLength(),
				bodyCaptor.getValue().optionalContentLength());

		verify(mockGridDao).savePatch(gridSessionId, patchId, key, GridManagerImpl.PATCH_DURATION);

	}

	@Test
	public void testSavePatchWithGridId() {
		when(mockS3Client.putObject(putCaptor.capture(), bodyCaptor.capture())).thenReturn(null);
		when(mockGridDao.savePatch(any(), any(), any(), any())).thenReturn(true);
		// call under test
		boolean isNew = gridManager.savePatch(gridSessionId, patchId, patchBody);
		assertTrue(isNew);

		assertEquals("dev.grid.patch.sagebase.org", putCaptor.getValue().bucket());
		String key = putCaptor.getValue().key();
		assertTrue(key.endsWith(".json"));
		assertEquals(RequestBody.fromString(patchBody, StandardCharsets.UTF_8).optionalContentLength(),
				bodyCaptor.getValue().optionalContentLength());

		verify(mockGridDao).savePatch(gridSessionId, patchId, key, GridManagerImpl.PATCH_DURATION);
	}

	@Test
	public void testSavePatchWithNotNew() {
		doReturn(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)).when(gridManager)
				.getConnectionInfo(connectionId);
		when(mockS3Client.putObject(putCaptor.capture(), bodyCaptor.capture())).thenReturn(null);
		when(mockGridDao.savePatch(any(), any(), any(), any())).thenReturn(false);
		// call under test
		boolean isNew = gridManager.savePatch(eventContext, patchId, patchBody);
		assertFalse(isNew);

		assertEquals("dev.grid.patch.sagebase.org", putCaptor.getValue().bucket());
		String key = putCaptor.getValue().key();
		assertTrue(key.endsWith(".json"));
		assertEquals(RequestBody.fromString(patchBody, StandardCharsets.UTF_8).optionalContentLength(),
				bodyCaptor.getValue().optionalContentLength());

		verify(mockGridDao).savePatch(gridSessionId, patchId, key, GridManagerImpl.PATCH_DURATION);

	}

	@Test
	public void testSavePatchWithNullContext() {
		eventContext = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			gridManager.savePatch(eventContext, patchId, patchBody);
		}).getMessage();
		assertEquals("context is required.", message);
	}

	@Test
	public void testSavePatchWithNullPatchId() {
		patchId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			gridManager.savePatch(gridSessionId, patchId, patchBody);
		}).getMessage();
		assertEquals("patchId is required.", message);
	}

	@Test
	public void testSavePatchWithNullPatchBody() {
		patchBody = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			gridManager.savePatch(gridSessionId, patchId, patchBody);
		}).getMessage();
		assertEquals("body is required.", message);
	}

	@Test
	public void testGetPatchBody() {
		Timestamp expires = new Timestamp(System.currentTimeMillis() + 1001L);
		when(mockGridDao.getPatchInfo(gridSessionId, patchId))
				.thenReturn(Optional.of(new PatchInfo().setPatchId(patchId).setS3Key("akey").setExpiresOn(expires)));
		when(mockS3Client.getObjectAsBytes(getObjectRequestCaptor.capture())).thenReturn(ResponseBytes
				.fromByteArray(GetObjectResponse.builder().build(), patchBody.getBytes(StandardCharsets.UTF_8)));
		// call under test
		Optional<String> op = gridManager.getPatchBody(gridSessionId, patchId);
		assertEquals(Optional.of(patchBody), op);

		assertEquals("dev.grid.patch.sagebase.org", getObjectRequestCaptor.getValue().bucket());
		assertEquals("akey", getObjectRequestCaptor.getValue().key());
	}

	@Test
	public void testGetPatchBodyWithExpired() {
		Timestamp expires = new Timestamp(System.currentTimeMillis() - 1001L);
		when(mockGridDao.getPatchInfo(gridSessionId, patchId))
				.thenReturn(Optional.of(new PatchInfo().setPatchId(patchId).setS3Key("akey").setExpiresOn(expires)));

		String message = assertThrows(NotFoundException.class, () -> {
			// Call under test
			gridManager.getPatchBody(gridSessionId, patchId);
		}).getMessage();
		assertEquals("The requested patch has expired: LogicalTimestamp [replicaId=88, sequenceNumber=777]", message);

		verifyZeroInteractions(mockS3Client);
	}

	@Test
	public void testGetPatchBodyWithNoPatch() {
		when(mockGridDao.getPatchInfo(gridSessionId, patchId)).thenReturn(Optional.empty());

		String message = assertThrows(NotFoundException.class, () -> {
			// Call under test
			gridManager.getPatchBody(gridSessionId, patchId);
		}).getMessage();
		assertEquals("Cannot find patch: LogicalTimestamp [replicaId=88, sequenceNumber=777]", message);

		verifyZeroInteractions(mockS3Client);
	}

	@Test
	public void testGetPatchBodyWithNullSessionId() {
		gridSessionId = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			gridManager.getPatchBody(gridSessionId, patchId);
		}).getMessage();
		assertEquals("sessionId is required.", message);

		verifyZeroInteractions(mockS3Client);
	}

	@Test
	public void testGetPatchBodyWithNullPatchId() {
		patchId = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			gridManager.getPatchBody(gridSessionId, patchId);
		}).getMessage();
		assertEquals("patchId is required.", message);

		verifyZeroInteractions(mockS3Client);
	}

	@Test
	public void testGetNextMissingPatch() {
		when(mockGridDao.getConnection(connectionId)).thenReturn(
				Optional.of(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)));
		List<LogicalTimestamp> missing = List.of(new LogicalTimestamp().setReplicaId(44L).setSequenceNumber(90L));

		when(mockGridDao.listMissingPatchIdsForClock(gridSessionId, clock, 1)).thenReturn(missing);
		doReturn(Optional.of(patchBody)).when(gridManager).getPatchBody(gridSessionId, missing.get(0));

		// call under test
		Optional<String> op = gridManager.getNextMissingPatch(eventContext, clock);
		assertEquals(Optional.of(patchBody), op);
	}

	@Test
	public void testGetNextMissingPatchUpToDate() {
		when(mockGridDao.getConnection(connectionId)).thenReturn(
				Optional.of(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)));

		when(mockGridDao.listMissingPatchIdsForClock(gridSessionId, clock, 1)).thenReturn(Collections.emptyList());

		// call under test
		Optional<String> op = gridManager.getNextMissingPatch(eventContext, clock);
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
		userId = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
		when(mockUser.getId()).thenReturn(userId);
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
	
	@ParameterizedTest
	@EnumSource(value = EventSource.class)
    public void testGetDefaultUserInternalConnection(EventSource source) {
		when(mockUser.getId()).thenReturn(userId);
        when(mockGridDao.getSingletonUserConnection(gridSessionId, userId, source)).thenReturn(
                Optional.of(new GridConnectionInfo().setSessionId(gridSessionId).setConnectionId(connectionId)));

        // call under test
        Optional<GridConnectionInfo> actual = gridManager.getSingletonUserConnection(gridSessionId, mockUser, source);
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
}
