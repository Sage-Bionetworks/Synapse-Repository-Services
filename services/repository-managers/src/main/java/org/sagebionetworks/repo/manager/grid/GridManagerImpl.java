package org.sagebionetworks.repo.manager.grid;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.config.WebsocketApi;
import org.sagebionetworks.repo.manager.grid.create.CreateGridHandler;
import org.sagebionetworks.repo.manager.grid.create.CreateGridHandlerResult;
import org.sagebionetworks.repo.manager.grid.response.InternalReplicaToHubEventPublisher;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
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
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.AuthLocation;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class GridManagerImpl implements GridManager {

	public static final String GRID_REPLICA_NOT_FOUND = "Grid replica not found.";
	public static final String GRID_SESSION_NOT_FOUND = "Grid session not found.";

	/*
	 * Note: The S3 bucket that store patches will automatically delete all patch
	 * files that are 120 days old. We expire each patch in the database after 119
	 * days to ensure we never try to read a files that is about to be deleted
	 */
	public static final Duration PATCH_DURATION = Duration.ofDays(119);

	private final AwsCredentialsProvider awsCredentialsProvider;
	private final WebsocketApi websocketApi;
	private final GridDao gridDao;
	private final String gridPatchBucket;
	private final S3Client s3Client;
	private final InternalReplicaToHubEventPublisher internalEventPublisher;
	private final List<CreateGridHandler> createGridHandlers;

	@Autowired
	public GridManagerImpl(AwsCredentialsProvider awsCredentialsProvider, WebsocketApi websocketApi, GridDao gridDao,
			StackConfiguration config, S3Client s3Client, InternalReplicaToHubEventPublisher internalEventPublisher,
			List<CreateGridHandler> createHandlers) {
		super();
		this.awsCredentialsProvider = awsCredentialsProvider;
		this.websocketApi = websocketApi;
		this.gridDao = gridDao;
		this.gridPatchBucket = String.format("%s.grid.patch.sagebase.org", config.getStack());
		this.s3Client = s3Client;
		this.internalEventPublisher = internalEventPublisher;
		this.createGridHandlers = createHandlers;
	}

	@WriteTransaction
	@Override
	public CreateGridResponse createGrid(AsyncJobProgressCallback callback, UserInfo user, CreateGridRequest request) {
		ValidateArgument.required(callback, "callback");
		ValidateArgument.required(user, "user");
		ValidateArgument.required(request, "request");
		ValidateArgument.requirement(request.getInitialQuery() == null || request.getRecordSetId() == null,
				"Cannot set both initialQuery and recordSetId.");

		// Must authenticate to create a grid session.
		AuthorizationUtils.disallowAnonymous(user);

		CreateGridHandler handler = createGridHandlers.stream()
			.filter(h -> h.canCreate(request))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Cannot find a handler for: " + request));
		
		CreateGridHandlerResult result = handler.createGrid(callback, user, request, this);
		
		if (result == null || result.getGridSession() == null) {
			throw new IllegalStateException("Handler must provide a grid session");
		}
		
		GridSession session = result.getGridSession();
		
		if (result.getGridReplica() != null) {
			GridReplica replica = result.getGridReplica();
			/*
			 * This call will establish a new internal connection to this replica. It will
			 * also trigger a new [8,"connected"] event to be sent to the replica's worker.
			 */
			sendInternalConnectEvent(user, session, replica, EventSource.INTERNAL);
		}
		
		if (session.getGridJsonSchema$Id() != null) {
			// establish the connection to be used the validation worker.
			GridReplica validationReplica = gridDao.createReplica(user.getId(), session.getSessionId(), false, EventSource.VALIDATION);
			
			sendInternalConnectEvent(user, session, validationReplica, EventSource.VALIDATION);
		}
		
		// establish the connection to be used for jobs triggered by the user

		GridReplica supportReplica = gridDao.createReplica(user.getId(), session.getSessionId(), false, EventSource.USER_SUPPORT);
		
		sendInternalConnectEvent(user, session, supportReplica, EventSource.USER_SUPPORT);
		
		// Creating replicas modify the session ETAG, so we reload it from the database
		session = gridDao.getGridSession(session.getSessionId()).orElseThrow();
		
		return new CreateGridResponse().setGridSession(session);
	}
	
	void sendInternalConnectEvent(UserInfo user, GridSession session, GridReplica replica, EventSource source) {
		// establish the connection to be used the validation worker.
		internalEventPublisher.publishEventAfterCommit(
				new EventContext(EventType.CONNECT, source, UUID.randomUUID().toString()),
				JsonRxMessageType.Notification, "connection",
				new Connection().setGridSessionId(GridUtils.gridSessionIdAsLong(session.getSessionId()))
						.setReplicaId(replica.getReplicaId()).setUserId(user.getId()));
	}

	/**
	 * Currently, only the user that started the session may access it.
	 * 
	 * @param user
	 * @param gridSessionId
	 */
	void validGridSessionAccess(UserInfo user, String gridSessionId) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(gridSessionId, "gridSessionId");

		Long startedBy = gridDao.getGridSessionStartedBy(gridSessionId)
				.orElseThrow(() -> new NotFoundException(GRID_SESSION_NOT_FOUND));
		if (!AuthorizationUtils.isUserCreatorOrAdmin(user, startedBy.toString())) {
			throw new UnauthorizedException("You are not authorized to access this resource.");
		}
	}

	@Override
	public GridSession getGridSession(UserInfo user, String gridSessionId) {
		validGridSessionAccess(user, gridSessionId);
		return gridDao.getGridSession(gridSessionId).orElseThrow(() -> new NotFoundException(GRID_SESSION_NOT_FOUND));
	}

	@WriteTransaction
	@Override
	public CreateReplicaResponse createReplica(UserInfo user, CreateReplicaRequest request) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(request, "request");
		boolean isAgent = false;
		EventSource soure = EventSource.WEBSOCKET;
		return createReplica(user, request.getGridSessionId(), isAgent, soure);
	}

	@WriteTransaction
	@Override
	public CreateReplicaResponse createReplica(UserInfo user, String gridSessionId, boolean isAgent,
			EventSource source) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(gridSessionId, "gridSessionId");
		ValidateArgument.required(source, "source");
		// User must have access to the session in order to create a replica
		validGridSessionAccess(user, gridSessionId);
		GridReplica replia = gridDao.createReplica(user.getId(), gridSessionId, isAgent, source);
		return new CreateReplicaResponse().setReplica(replia);
	}

	@Override
	public GridReplica getReplica(UserInfo user, String sessionId, Long replicaId) {
		ValidateArgument.required(replicaId, "replicaId");
		// User must have access to the session in order to create a replica
		validGridSessionAccess(user, sessionId);
		return gridDao.getGridReplica(sessionId, replicaId)
				.orElseThrow(() -> new NotFoundException(GRID_REPLICA_NOT_FOUND));
	}

	/**
	 * Validate that the user created the replica.
	 * 
	 * @param user
	 * @param sesisonId
	 * @param replicaId
	 */
	void validateRepicaOwner(UserInfo user, String sesisonId, Long replicaId) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(sesisonId, "gridSessionId");
		ValidateArgument.required(replicaId, "replicaId");

		boolean isAgentReplica = false;
		Long replicaCreatedBy = gridDao.getReplicaCreatedBy(sesisonId, replicaId, isAgentReplica)
				.orElseThrow(() -> new NotFoundException(GRID_REPLICA_NOT_FOUND));
		if (!AuthorizationUtils.isUserCreatorOrAdmin(user, replicaCreatedBy.toString())) {
			throw new UnauthorizedException("You are not authorized to access this resource.");
		}
	}

	@Override
	public CreateGridPresignedUrlResponse createWebsocketPresignedUrl(UserInfo user,
			CreateGridPresignedUrlRequest request) {
		ValidateArgument.required(request, "request");

		validateRepicaOwner(user, request.getGridSessionId(), request.getReplicaId());

		Long longSessionId = GridUtils.gridSessionIdAsLong(request.getGridSessionId());

		AwsV4HttpSigner signer = AwsV4HttpSigner.create();
		String startUrl = String.format(
				"https://%s.execute-api.us-east-1.amazonaws.com/%s/?gridSessionId=%s&replicaId=%d&userId=%d",
				websocketApi.getApiId(), websocketApi.getStageName(), longSessionId, request.getReplicaId(),
				user.getId());
		SdkHttpRequest httpRequest = SdkHttpRequest.builder().uri(startUrl).method(SdkHttpMethod.GET).build();

		SignedRequest signedRequest = signer.sign(SignRequest.builder(awsCredentialsProvider.resolveCredentials())
				.request(httpRequest).putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "execute-api")
				.putProperty(AwsV4HttpSigner.REGION_NAME, Region.US_EAST_1.toString())
				.putProperty(AwsV4HttpSigner.AUTH_LOCATION, AuthLocation.QUERY_STRING)
				.putProperty(AwsV4HttpSigner.EXPIRATION_DURATION, Duration.ofMinutes(15)).build());

		String url = signedRequest.request().getUri().toString();
		StringBuilder builder = new StringBuilder("wss").append(url.substring(5, url.length()));
		return new CreateGridPresignedUrlResponse().setPresignedUrl(builder.toString());
	}

	@WriteTransaction
	@Override
	public void createReplicaConnection(UserInfo user, EventContext context, Connection connection) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(context, "context");
		ValidateArgument.required(connection, "connection");

		if (!EventType.CONNECT.equals(context.getEventType())) {
			throw new UnauthorizedException("Invalid request");
		}
		String sessionIdAsString = GridUtils.gridSessionIdAsString(connection.getGridSessionId());
		validateRepicaOwner(user, sessionIdAsString, connection.getReplicaId());
		gridDao.createConnection(new GridConnectionInfo().setConnectionId(context.getConnectionId())
				.setCreatedBy(user.getId()).setReplicaId(connection.getReplicaId()).setSessionId(sessionIdAsString)
				.setSource(context.getEventSource()));
	}

	@WriteTransaction
	@Override
	public void removeReplicatConnection(EventType type, String connectionId) {
		ValidateArgument.required(type, "type");
		if (!EventType.DISCONNECT.equals(type)) {
			throw new UnauthorizedException("Invalid request");
		}
		removeReplicaConnection(connectionId);
	}

	@Override
	public void removeReplicaConnection(String connectionId) {
		ValidateArgument.required(connectionId, "connectionId");
		gridDao.removeConnection(connectionId);
	}

	@Override
	public Optional<GridConnectionInfo> getSingletonConnection(String sessionId, EventSource source) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(source, "source");
		return gridDao.getSingletonConnection(sessionId, source);
	}
	
	@Override
	public Optional<GridConnectionInfo> getSingletonUserConnection(String sessionId, UserInfo user, EventSource source) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(user, "user");
		ValidateArgument.required(source, "source");
		return gridDao.getSingletonUserConnection(sessionId, user.getId(), source);
	}

	@WriteTransaction
	@Override
	public boolean savePatch(EventContext context, LogicalTimestamp patchId, String body) {
		ValidateArgument.required(context, "context");
		GridConnectionInfo thisCon = getConnectionInfo(context.getConnectionId());
		return savePatch(thisCon.getSessionId(), patchId, body);
	}

	@WriteTransaction
	@Override
	public boolean savePatch(String sessionId, LogicalTimestamp patchId, String body) {
		ValidateArgument.required(patchId, "patchId");
		ValidateArgument.required(patchId, "patchId");
		ValidateArgument.required(body, "body");
		String s3Key = String.format("%s.json", UUID.randomUUID().toString());
		s3Client.putObject(PutObjectRequest.builder().bucket(gridPatchBucket).key(s3Key).build(),
				RequestBody.fromString(body, StandardCharsets.UTF_8));
		return gridDao.savePatch(sessionId, patchId, s3Key, PATCH_DURATION);
	}

	@Override
	public List<GridConnectionInfo> listActiveConnections(String connectionId) {
		ValidateArgument.required(connectionId, "connectionId");
		// Lookup the grid session for the provide connection Id.
		GridConnectionInfo thisCon = getConnectionInfo(connectionId);
		return gridDao.listConnections(thisCon.getSessionId());
	}

	GridConnectionInfo getConnectionInfo(String connectionId) {
		return gridDao.getConnection(connectionId)
				.orElseThrow(() -> new NotFoundException("No Connection Found: " + connectionId));
	}

	@Override
	public Optional<String> getNextMissingPatch(EventContext context, List<LogicalTimestamp> clock) {
		ValidateArgument.required(context, "context");
		ValidateArgument.required(clock, "clock");
		GridConnectionInfo thisCon = getConnectionInfo(context.getConnectionId());
		// Get the first patch ID that this clock is missing.
		List<LogicalTimestamp> missing = gridDao.listMissingPatchIdsForClock(thisCon.getSessionId(), clock, 1);
		if (missing.isEmpty()) {
			return Optional.empty();
		}
		LogicalTimestamp nextPatchId = missing.get(0);

		return getPatchBody(thisCon.getSessionId(), nextPatchId);
	}

	/**
	 * Get the body of a patch for the given patch ID.
	 * 
	 * @param sessionId
	 * @param patchId
	 * @return
	 */
	Optional<String> getPatchBody(String sessionId, LogicalTimestamp patchId) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(patchId, "patchId");
		PatchInfo patch = gridDao.getPatchInfo(sessionId, patchId)
				.orElseThrow(() -> new NotFoundException("Cannot find patch: " + patchId));
		if (Instant.now().isAfter(patch.getExpiresOn().toInstant())) {
			throw new NotFoundException("The requested patch has expired: " + patchId);
		}

		return Optional.of(s3Client
				.getObjectAsBytes(GetObjectRequest.builder().bucket(gridPatchBucket).key(patch.getS3Key()).build())
				.asString(StandardCharsets.UTF_8));
	}

	@Override
	public ListGridSessionsResponse listActiveGridSessions(UserInfo user, ListGridSessionsRequest request) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(request, "request");
		AuthorizationUtils.disallowAnonymous(user);
		NextPageToken nextPageToken = new NextPageToken(request.getNextPageToken());
		List<GridSession> page = request.getSourceId() != null
				? gridDao.listActiveGridSession(user.getId(), request.getSourceId(), nextPageToken.getLimitForQuery(),
						nextPageToken.getOffset())
				: gridDao.listActiveGridSession(user.getId(), nextPageToken.getLimitForQuery(),
						nextPageToken.getOffset());
		return new ListGridSessionsResponse().setPage(page)
				.setNextPageToken(nextPageToken.getNextPageTokenForCurrentResults(page));
	}

	@Override
	public void deleteGridSession(UserInfo user, String sessionId) {
		// User must have access to the session in order to delete it.
		validGridSessionAccess(user, sessionId);
		gridDao.deleteGridSession(sessionId);
	}

	@Override
	public Optional<GridConnectionInfo> getConnectionInfoOptional(String connectionId) {
		return gridDao.getConnection(connectionId);
	}

}
