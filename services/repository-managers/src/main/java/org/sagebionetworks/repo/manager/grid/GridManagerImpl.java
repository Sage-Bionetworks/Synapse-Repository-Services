package org.sagebionetworks.repo.manager.grid;

import static org.sagebionetworks.repo.manager.file.FileHandleManagerImpl.PRESIGNED_URL_EXPIRE_TIME_MS;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.SynapseS3Client;
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
import org.sagebionetworks.repo.model.dbo.grid.GridSource;
import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlRequest;
import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlResponse;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
import org.sagebionetworks.repo.model.grid.CreateReplicaRequest;
import org.sagebionetworks.repo.model.grid.CreateReplicaResponse;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.GridSnapshot;
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

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
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

	/*
	 * Note: The S3 bucket that store patches will automatically delete all patch
	 * files that are 120 days old. We expire each patch in the database after 119
	 * days to ensure we never try to read a files that is about to be deleted
	 */
	public static final Duration PATCH_DURATION = Duration.ofDays(119);

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

	@Autowired
	public GridManagerImpl(AwsCredentialsProvider awsCredentialsProvider, WebsocketApi websocketApi, GridDao gridDao,
	   StackConfiguration config, S3Client s3Client, SynapseS3Client synapseS3Client, InternalReplicaToHubEventPublisher internalEventPublisher,
	   List<CreateGridHandler> createHandlers, GridAuthorizationManager gridAuthorizationManager, TransferManager transferManager) {
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

}
