package org.sagebionetworks.repo.manager.grid;

import static org.sagebionetworks.repo.manager.file.FileHandleManagerImpl.PRESIGNED_URL_EXPIRE_TIME_MS;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.agent.handler.grid.SetValueProcessorFactory;
import org.sagebionetworks.repo.manager.config.WebsocketApi;
import org.sagebionetworks.repo.manager.grid.create.CreateGridHandler;
import org.sagebionetworks.repo.manager.grid.create.CreateGridHandlerResult;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangePublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.PatchBuilderPublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.UpdateRowChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.QueryElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.FilterElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.FilterTranslation;
import org.sagebionetworks.repo.manager.grid.response.InternalReplicaToHubEventPublisher;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.dbo.grid.GridSource;
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
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.query.QueryRequest;
import org.sagebionetworks.repo.model.grid.query.result.QueryResult;
import org.sagebionetworks.repo.model.grid.update.GridUpdateResponse;
import org.sagebionetworks.repo.model.grid.update.SetValue;
import org.sagebionetworks.repo.model.grid.update.Update;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.model.UploadResult;

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

	private final AwsCredentialsProvider awsCredentialsProvider;
	private final WebsocketApi websocketApi;
	private final GridDao gridDao;
	private final String gridSnapshotBucket;
	private final String gridPatchBucket;
	private final S3Client s3Client;
	private final SynapseS3Client synapseS3Client;
	private final InternalReplicaToHubEventPublisher internalEventPublisher;
	private final List<CreateGridHandler> createGridHandlers;
	private final GridAuthorizationManager gridAuthorizationManager;
	private final TransferManager transferManager;
	private final TransactionalMessenger transactionalMessenger;
	private final GridReplicaViewManager gridReplicaViewManager;
	private final PatchBuilderPublisher patchBuilderPublisher;
	private final SetValueProcessorFactory setValueProcessorFactory;

	@Autowired
	public GridManagerImpl(AwsCredentialsProvider awsCredentialsProvider, WebsocketApi websocketApi, GridDao gridDao,
	   StackConfiguration config, S3Client s3Client, SynapseS3Client synapseS3Client, InternalReplicaToHubEventPublisher internalEventPublisher,
	   List<CreateGridHandler> createHandlers, GridAuthorizationManager gridAuthorizationManager, TransferManager transferManager,
	   TransactionalMessenger transactionalMessenger, GridReplicaViewManager gridReplicaViewManager,
	   PatchBuilderPublisher patchBuilderPublisher, SetValueProcessorFactory setValueProcessorFactory) {
		super();
		this.awsCredentialsProvider = awsCredentialsProvider;
		this.websocketApi = websocketApi;
		this.gridDao = gridDao;
		this.gridSnapshotBucket = String.format("%s.grid.snapshot.sagebase.org", config.getStack());
		this.gridPatchBucket = String.format("%s.grid.patch.sagebase.org", config.getStack());
		this.s3Client = s3Client;
		this.synapseS3Client = synapseS3Client;
		this.internalEventPublisher = internalEventPublisher;
		this.createGridHandlers = createHandlers;
		this.gridAuthorizationManager = gridAuthorizationManager;
		this.transferManager = transferManager;
		this.transactionalMessenger = transactionalMessenger;
		this.gridReplicaViewManager = gridReplicaViewManager;
		this.patchBuilderPublisher = patchBuilderPublisher;
		this.setValueProcessorFactory = setValueProcessorFactory;
	}

	@WriteTransaction
	@Override
	public CreateGridResponse createGrid(AsyncJobProgressCallback callback, UserInfo user, CreateGridRequest request) {
		ValidateArgument.required(callback, "callback");
		ValidateArgument.required(user, "user");
		ValidateArgument.required(request, "request");
		ValidateArgument.requirement(request.getInitialQuery() == null || request.getRecordSetId() == null,
				"Cannot set both initialQuery and recordSetId.");

		Long ownerId = gridAuthorizationManager.validateGridOwner(user, request.getOwnerPrincipalId());
		request.setOwnerPrincipalId(ownerId.toString());

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
	 * If the grid owner is a team, the user must belong to the team, or the user must be the owner user.
	 * 
	 * @param user
	 * @param gridSessionId
	 */
	void validGridSessionAccess(UserInfo user, String gridSessionId) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(gridSessionId, "gridSessionId");
		gridAuthorizationManager.hasGridSessionAccess(user, gridSessionId).checkAuthorizationOrElseThrow();
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

		Long replicaCreatedBy = gridDao.getReplicaCreatedBy(sesisonId, replicaId)
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
	
	@WriteTransaction
	@Override
	public boolean savePatch(EventContext context, LogicalTimestamp patchId, String body) {
		ValidateArgument.required(context, "context");
		ValidateArgument.required(patchId, "patchId");
		GridConnectionInfo thisCon = getConnectionInfo(context.getConnectionId());
		if (!Objects.equals(patchId.getReplicaId(), thisCon.getReplicaId())) {
			throw new UnauthorizedException("Patch replicaId does not match the connection replicaId.");
		}
		return savePatch(thisCon.getSessionId(), patchId, body);
	}

	@WriteTransaction
	@Override
	public boolean savePatch(String sessionId, LogicalTimestamp patchId, String body) {
		ValidateArgument.required(patchId, "patchId");
		ValidateArgument.required(patchId, "patchId");
		ValidateArgument.required(body, "body");
		String s3Key = String.format("%s.json", UUID.randomUUID().toString());
		byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
		s3Client.putObject(PutObjectRequest.builder().bucket(gridPatchBucket).key(s3Key).build(),
				RequestBody.fromBytes(bodyBytes));
		boolean isNew = gridDao.savePatch(sessionId, patchId, s3Key, bodyBytes.length);
		if (isNew) {
			transactionalMessenger.sendMessageAfterCommit(
					GridUtils.gridSessionIdAsLong(sessionId).toString(), ObjectType.GRID_SESSION,
					ChangeType.UPDATE);
		}
		return isNew;
	}

	@Override
	public List<GridConnectionInfo> listActiveConnections(String connectionId) {
		ValidateArgument.required(connectionId, "connectionId");
		// Lookup the grid session for the provide connection Id.
		GridConnectionInfo thisCon = getConnectionInfo(connectionId);
		return gridDao.listConnections(thisCon.getSessionId());
	}

	static final long PATCH_BATCH_BUDGET_BYTES = PatchUtils.MAX_BYTES_PER_PATCH - 1_000L; // WebSocket message limit, minus ~1KB for overhead
	static final long PATCH_BATCH_CANDIDATE_LIMIT = PATCH_BATCH_BUDGET_BYTES / 10; // Assume minimum patch size of ~10B

	@Override
	public Optional<String> getNextSynchronizeResponse(EventContext context, List<LogicalTimestamp> clock) {
		// Always start a new replica with a snapshot
		boolean getSnapshot = clock == null || clock.isEmpty();

		if (getSnapshot) {
			Optional<URL> snapshotPresignedUrl = this.getLatestSnapshotPresignedUrl(context);
			if (snapshotPresignedUrl.isPresent()) {
				// Send the snapshot URL to the caller
				JSONObject messageBody = new JSONObject();
				messageBody.put("type", "snapshot");
				messageBody.put("body", snapshotPresignedUrl.get().toString());
				return Optional.of(messageBody.toString());
			}
		}

		// The replica already has data, or there is no snapshot. Send patches
		ValidateArgument.required(context, "context");
		GridConnectionInfo thisCon = getConnectionInfo(context.getConnectionId());
		String sessionId = thisCon.getSessionId();
		List<LogicalTimestamp> effectiveClock = clock != null ? clock : List.of();

		List<PatchInfo> missingPatches = gridDao.listMissingPatchInfoForClock(sessionId, effectiveClock, PATCH_BATCH_CANDIDATE_LIMIT);
		if (missingPatches.isEmpty()) {
			return Optional.empty();
		}
		JSONArray patches = createMaxSizedPatchArray(sessionId, missingPatches);
		JSONObject response = new JSONObject();
		response.put("type", "patches");
		response.put("body", patches);
		return Optional.of(response.toString());
	}

	JSONArray createMaxSizedPatchArray(String sessionId, List<PatchInfo> candidatePatches) {
		long cumulativeSize = 0;
		JSONArray arrayOfPatches = new JSONArray();

		for (PatchInfo candidate : candidatePatches) {
			// Temporary code - there are patches in the database that were created before size was tracked.
			// Once all patches have a size field, this check can be removed, and we can require size for all patches.
			// This can happen once all patches without a size expire, or if we backfill the size for all existing patches.
			if (candidate.getSizeBytes() == null) {
				// Null size means pre-existing record — fallback to single-patch behavior
				if (arrayOfPatches.length() == 0) {
					Optional<JSONArray> body = getPatchBody(sessionId, candidate);
					if (body.isPresent()) {
						arrayOfPatches.put(body.get());
					}
					return arrayOfPatches;
				}
				// Stop accumulating; send what we have
				break;
			}

			if (!(arrayOfPatches.length() == 0) && cumulativeSize + candidate.getSizeBytes() > PATCH_BATCH_BUDGET_BYTES) {
				break;
			}

			Optional<JSONArray> body = getPatchBody(sessionId, candidate);
			if (body.isEmpty()) {
				break;
			}
			arrayOfPatches.put(body.get());
			cumulativeSize += candidate.getSizeBytes();
		}

		return arrayOfPatches;
	}

	GridConnectionInfo getConnectionInfo(String connectionId) {
		return gridDao.getConnection(connectionId)
				.orElseThrow(() -> new NotFoundException("No Connection Found: " + connectionId));
	}

	@Override
	public Optional<JSONArray> getNextMissingPatch(EventContext context, List<LogicalTimestamp> clock) {
		ValidateArgument.required(context, "context");
		ValidateArgument.required(clock, "clock");
		GridConnectionInfo thisCon = getConnectionInfo(context.getConnectionId());
		// Get the first patch ID that this clock is missing.
		List<PatchInfo> missing = gridDao.listMissingPatchInfoForClock(thisCon.getSessionId(), clock, 1);
		if (missing.isEmpty()) {
			return Optional.empty();
		}
		PatchInfo nextPatch = missing.get(0);

		return getPatchBody(thisCon.getSessionId(), nextPatch);
	}

	/**
	 * Get the body of a patch for the given patch ID.
	 * 
	 * @param sessionId
	 * @param patch
	 * @return
	 */
	Optional<JSONArray> getPatchBody(String sessionId, PatchInfo patch) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(patch, "patch");
		if (patch.getExpiresOn() != null && Instant.now().isAfter(patch.getExpiresOn().toInstant())) {
			throw new NotFoundException("The requested patch has expired: " + patch.getPatchId());
		}

		return Optional.of(s3Client
				.getObjectAsBytes(GetObjectRequest.builder().bucket(gridPatchBucket).key(patch.getS3Key()).build())
				.asString(StandardCharsets.UTF_8)).map(JSONArray::new);
	}

	@Override
	public Optional<URL> getLatestSnapshotPresignedUrl(EventContext context) {
		ValidateArgument.required(context, "context");
		GridConnectionInfo thisCon = getConnectionInfo(context.getConnectionId());

		Optional<GridSnapshot> snapshot = gridDao.getLatestSnapshot(thisCon.getSessionId());

		if (snapshot.isEmpty()) {
			return Optional.empty();
		}

		GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(gridSnapshotBucket, snapshot.get().getS3Key(), HttpMethod.GET);
		request.setExpiration(new Date(System.currentTimeMillis() + PRESIGNED_URL_EXPIRE_TIME_MS));

		return Optional.of(synapseS3Client.generatePresignedUrl(request));
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
	public ListGridReplicasResponse listReplicas(UserInfo user, ListGridReplicasRequest request) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getGridSessionId(), "request.gridSessionId");
		validGridSessionAccess(user, request.getGridSessionId());
		NextPageToken nextPageToken = new NextPageToken(request.getNextPageToken());
		List<GridReplicaInfo> page = gridDao.listReplicas(request.getGridSessionId(), nextPageToken.getLimitForQuery(),
				nextPageToken.getOffset());
		return new ListGridReplicasResponse().setPage(page)
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

	@Override
	public GridReplica createAgentReplica(UserInfo user, GridSession session) {
		GridReplica replica = gridDao.createReplica(user.getId(), session.getSessionId(), true, EventSource.AGENT);
		sendInternalConnectEvent(user, session, replica, EventSource.AGENT);
		return replica;
	}

	@Override
	public Optional<GridConnectionInfo> getConnection(String gridSessionId, Long agentsReplicaId) {
		return gridDao.getConnection(gridSessionId, agentsReplicaId);
	}

	@Override
	public Optional<GridSource> getSessionSource(String sessionId) {
		return gridDao.getSessionSource(sessionId);
	}

	@WriteTransaction
	@Override
	public void saveSnapshot(String sessionId, ClockTable clockTable, Long createdBy, File snapshotFile) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(clockTable, "clockTable");
		ValidateArgument.required(createdBy, "createdBy");
		ValidateArgument.required(snapshotFile, "snapshotFile");

		String s3Key = String.format("snapshot/%s/%d-%s.cbor", sessionId, System.currentTimeMillis(), UUID.randomUUID());
		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setContentType("application/cbor");

		UploadResult uploadResult;
		try {
			uploadResult = transferManager.upload(gridSnapshotBucket, s3Key, snapshotFile).waitForUploadResult();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Snapshot upload was interrupted", e);
		} catch (Exception e) {
			throw new RuntimeException("Failed to upload snapshot to S3", e);
		}
		gridDao.saveSnapshot(sessionId, clockTable, uploadResult.getKey(), createdBy);
	}

	@WriteTransaction
	@Override
	public long backfillGridSessionChanges() {
		long count = 0;
		long offset = 0;
		long limit = 100;
		List<String> sessionIds;
		while (!(sessionIds = gridDao.listAllSessionIds(limit, offset)).isEmpty()) {
			for (String sessionId : sessionIds) {
				transactionalMessenger.sendMessageAfterCommit(
						GridUtils.gridSessionIdAsLong(sessionId).toString(), ObjectType.GRID_SESSION,
						ChangeType.UPDATE);
				count++;
			}
			offset += limit;
		}
		return count;
	}

	static final long DEFAULT_QUERY_LIMIT = 100L;

	@Override
	public GridQueryJobResponse queryGrid(UserInfo user, GridQueryJobRequest request) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getSessionId(), "request.sessionId");
		ValidateArgument.required(request.getReplicaId(), "request.replicaId");
		ValidateArgument.required(request.getQueryRequest(), "request.queryRequest");
		ValidateArgument.required(request.getQueryRequest().getQuery(), "request.queryRequest.query");
		String sessionId = request.getSessionId();
		Long replicaId = request.getReplicaId();
		QueryRequest queryRequest = request.getQueryRequest();
		if (queryRequest.getQuery().getLimit() == null) {
			queryRequest.getQuery().setLimit(DEFAULT_QUERY_LIMIT);
		}
		validGridSessionAccess(user, sessionId);
		validateRepicaOwner(user, sessionId, replicaId);
		GridConnectionInfo internalConnection = gridDao.getSingletonConnection(sessionId, EventSource.INTERNAL)
				.orElseThrow(() -> new RecoverableMessageException("No internal connection exists for this grid session."));
		GridHeader header = gridReplicaViewManager
				.readHeader(sessionId, internalConnection.getReplicaId(), replicaId)
				.orElseThrow(() -> new RecoverableMessageException("Grid session does not exist."));
		QueryResult queryResult = gridReplicaViewManager.querySinglePageAsQueryResult(header,
				new QueryElement(queryRequest.getQuery()));
		return new GridQueryJobResponse().setQueryResult(queryResult);
	}

	@Override
	public long executeGridUpdate(GridHeader header, GridConnectionInfo publishingConnection,
			JSONObject rawUpdate) throws Exception {
		Update update = JDOSecondaryPropertyUtils.createEntityFromJSONObject(rawUpdate, Update.class);
		JSONArray rawSetValueArray = rawUpdate.getJSONArray("set");
		List<SetValue> set = update.getSet();
		List<FilterElement> filters = update.getFilters() == null ? Collections.emptyList()
				: update.getFilters().stream().map(FilterTranslation::translate).collect(Collectors.toList());
		Map<String, Integer> indexByName = header.getOrderedColumns().stream()
				.collect(Collectors.toMap(Column::getName, Column::getVectorIndex));
		Integer[] indexArray = set.stream().map(s -> {
			Integer idx = indexByName.get(s.getColumnName());
			if (idx == null) {
				throw new IllegalArgumentException("Column name: " + s.getColumnName() + " not found.");
			}
			return idx;
		}).toArray(Integer[]::new);

		long updateCount = 0;
		Iterator<RowView> rows = gridReplicaViewManager.getQueryIterator(header,
				new QueryElement().setWhere(filters).setLimit(update.getLimit()));
		try (IntendedChangePublisher icp = new IntendedChangePublisher(publishingConnection,
				header.getClockSequenceMaximum(), patchBuilderPublisher, PatchUtils.MAX_CHANGE_SET_SIZE)) {
			while (rows.hasNext()) {
				List<ConValue> updates = new ArrayList<>();
				List<Integer> finalIndex = new ArrayList<>();
				RowView row = rows.next();
				for (int i = 0; i < set.size(); i++) {
					JSONObject rawSetValue = rawSetValueArray.optJSONObject(i);
					Optional<ConValue> op = setValueProcessorFactory.createConValue(row, set.get(i), rawSetValue);
					if (op.isPresent()) {
						finalIndex.add(indexArray[i]);
						updates.add(op.get());
					}
				}
				if (!updates.isEmpty()) {
					icp.publish(new UpdateRowChange(row.getRowObject().getData().getVectorId(), updates,
							finalIndex.toArray(new Integer[finalIndex.size()])));
					updateCount++;
				}
			}
		}
		return updateCount;
	}

	@WriteTransaction
	@Override
	public GridUpdateJobResponse updateGrid(UserInfo user, GridUpdateJobRequest request) throws Exception {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getSessionId(), "request.sessionId");
		ValidateArgument.required(request.getReplicaId(), "request.replicaId");
		ValidateArgument.required(request.getUpdateRequest(), "request.updateRequest");
		ValidateArgument.required(request.getUpdateRequest().getUpdate(), "request.updateRequest.update");
		ValidateArgument.required(request.getUpdateRequest().getUpdate().getBatch(),
				"request.updateRequest.update.batch");
		String sessionId = request.getSessionId();
		Long replicaId = request.getReplicaId();
		validGridSessionAccess(user, sessionId);
		validateRepicaOwner(user, sessionId, replicaId);
		GridConnectionInfo internalConnection = gridDao.getSingletonConnection(sessionId, EventSource.INTERNAL)
				.orElseThrow(() -> new RecoverableMessageException("No internal connection exists for this grid session."));
		GridHeader header = gridReplicaViewManager
				.readHeader(sessionId, internalConnection.getReplicaId(), replicaId)
				.orElseThrow(() -> new RecoverableMessageException("Grid session does not exist."));
		GridConnectionInfo publishingConnection = gridDao.getConnection(sessionId, replicaId)
				.orElseGet(() -> {
					String connectionId = UUID.randomUUID().toString();
					gridDao.createConnection(new GridConnectionInfo()
							.setConnectionId(connectionId)
							.setSessionId(sessionId)
							.setReplicaId(replicaId)
							.setCreatedBy(user.getId())
							.setSource(EventSource.API));
					return gridDao.getConnection(sessionId, replicaId)
							.orElseThrow(() -> new IllegalStateException("Failed to create connection."));
				});
		// Re-serialize to JSON so that the existing raw-JSON processing logic in
		// executeGridUpdate can distinguish absent vs. null in LiteralSetValue.value.
		String updateRequestJson = JDOSecondaryPropertyUtils.createJSONFromObject(request.getUpdateRequest());
		JSONArray updateBatch = new JSONObject(updateRequestJson).getJSONObject("update").getJSONArray("batch");
		List<Long> updateCounts = new ArrayList<>();
		for (int i = 0; i < updateBatch.length(); i++) {
			updateCounts.add(executeGridUpdate(header, publishingConnection, updateBatch.getJSONObject(i)));
		}
		GridUpdateResponse updateResponse = new GridUpdateResponse()
				.setUpdateResults(updateCounts)
				.setTotalRowsUpdated(updateCounts.stream().mapToLong(Long::longValue).sum());
		return new GridUpdateJobResponse().setUpdateResponse(updateResponse);
	}

}
