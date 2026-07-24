package org.sagebionetworks.repo.manager.grid.internal.replica;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.grid.db.GridIndexManager;
import org.sagebionetworks.grid.db.MessageChain;
import org.sagebionetworks.repo.manager.grid.response.InternalReplicaToHubEventPublisher;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessage;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;
import org.sagebionetworks.util.RetryException;
import org.sagebionetworks.util.TimeUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.springframework.stereotype.Component;

@Component
public class GridReplicaManagerImpl implements GridReplicaManager {

	private static final Logger log = LogManager.getLogger(GridReplicaManagerImpl.class);

	private final GridIndexManager gridIndexManager;
	private final GridReplicaSnapshotManager snapshotManager;
	private final InternalReplicaToHubEventPublisher publisher;
	private final HttpClient httpClient;
	private final GridReplicaConnectionManager gridReplicaConnectionManager;

	public GridReplicaManagerImpl(GridIndexManager gridIndexManager,
			GridReplicaSnapshotManager snapshotManager,
			InternalReplicaToHubEventPublisher publisher,
			HttpClient httpClient,
			GridReplicaConnectionManager gridReplicaConnectionManager) {
		this.gridIndexManager = gridIndexManager;
		this.snapshotManager = snapshotManager;
		this.publisher = publisher;
		this.httpClient = httpClient;
		this.gridReplicaConnectionManager = gridReplicaConnectionManager;
	}

	void synchronizeClock(ProgressCallback callback, GridConnectionInfo connection) {
		// Ignore this request if an active 'synchronize-clock' is in progress.
		Optional<MessageChain> mo = gridIndexManager.getNonExpiredMessageChain(connection.getSessionId(),
				connection.getReplicaId(), SYNCHRONIZE_CLOCK);
		if (mo.isPresent()) {
			log.info("Non-expired message chain already exists for session: {} replica: {} method: {}",
					connection.getSessionId(), connection.getReplicaId(), SYNCHRONIZE_CLOCK);
			return;
		}
		MessageChain chain = gridIndexManager.startMessageChain(connection.getSessionId(), connection.getReplicaId(),
				SYNCHRONIZE_CLOCK);
		List<LogicalTimestamp> clock = gridIndexManager.getClock(connection.getSessionId(), connection.getReplicaId());
		sendClockMessage(chain.getId(), connection.getConnectionId(), clock);
	}

	void sendClockMessage(Integer methodId, String connectionId, List<LogicalTimestamp> clock) {
		publisher.publishEvent(new EventContext(EventType.MESSAGE, EventSource.INTERNAL, connectionId),
				new JsonRxMessage(JsonRxMessageType.RequestData).setMethod(SYNCHRONIZE_CLOCK).setId(methodId)
						.setBody(LogicalTimestampCompactSerializable.serializeClock(clock)));
	}

	@Override
	public void onResponseComplete(ProgressCallback callback, GridConnectionInfo connection, Integer methodId) {
		gridIndexManager.completeMessageChain(connection.getSessionId(), connection.getReplicaId(), methodId);
	}

	@Override
	public void onApplyPatches(ProgressCallback callback, GridConnectionInfo connection, Integer messageId, List<Patch> patches) {
		gridIndexManager.refreshMessageChain(connection.getSessionId(), connection.getReplicaId(), messageId);
		Map<IndexType, Set<LogicalTimestamp>> cumulativeChanges = new LinkedHashMap<>();
		patches.forEach(patch -> {
			Map<IndexType, Set<LogicalTimestamp>> patchChanges = gridIndexManager.applyPatch(connection.getSessionId(), connection.getReplicaId(), patch);
			patchChanges.forEach((indexType, timestamps) -> cumulativeChanges.computeIfAbsent(indexType, k -> new LinkedHashSet<>()).addAll(timestamps));
		});

		gridReplicaConnectionManager.sendChangesToTopic(ReplicaChangeSet.fromPatch(connection, cumulativeChanges));
		List<LogicalTimestamp> clock = gridIndexManager.getClock(connection.getSessionId(), connection.getReplicaId());
		sendClockMessage(messageId, connection.getConnectionId(), clock);
	}

	@Override
	public void onApplySnapshot(ProgressCallback callback, GridConnectionInfo connection, Integer messageId, URL snapshotPresignedUrl) {
		gridIndexManager.refreshMessageChain(connection.getSessionId(), connection.getReplicaId(), messageId);

		Path snapshotFile = null;
		try {
			snapshotFile = downloadSnapshotFile(snapshotPresignedUrl);
			gridIndexManager.applySnapshot(connection.getSessionId(), connection.getReplicaId(), snapshotFile);
		} finally {
			if (snapshotFile != null) {
				try {
					Files.deleteIfExists(snapshotFile);
				} catch (IOException e) {
					log.warn("Failed to delete temp file: {}", snapshotFile, e);
				}
			}
		}
		List<LogicalTimestamp> clock = gridIndexManager.getClock(connection.getSessionId(), connection.getReplicaId());
		sendClockMessage(messageId, connection.getConnectionId(), clock);
		gridReplicaConnectionManager.sendChangesToTopic(ReplicaChangeSet.fromSnapshot(connection));
	}

	Path downloadSnapshotFile(URL snapshotPresignedUrl) {
		ValidateArgument.required(snapshotPresignedUrl, "snapshotPresignedUrl");
		Path tempFile;
		try {
			tempFile = Files.createTempFile("grid-snapshot-", ".cbor");

			HttpRequest request = HttpRequest.newBuilder()
					.uri(snapshotPresignedUrl.toURI())
					.GET()
					.build();

			return TimeUtils.waitForExponentialMaxRetry(5, 1000, () -> {
				final Set<Integer> RETRY_STATUS_CODES = Set.of(429, 500, 502, 503, 504, 509);
				HttpResponse<Path> response;
				try {
					response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(tempFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE));
					int statusCode = response.statusCode();
					if (RETRY_STATUS_CODES.contains(statusCode)) {
						throw new RetryException("Failed to download snapshot. Status: " + statusCode);
					} else if (statusCode >= 300) {
						throw new RuntimeException("Failed to download snapshot. Status: " + statusCode);
					}
					return response.body();
				} catch (SocketTimeoutException ste) {
					throw new RetryException(ste);
				}
			});

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while downloading snapshot from: " + snapshotPresignedUrl, e);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid snapshot URL: " + snapshotPresignedUrl, e);
		} catch (Exception e) {
			throw new RuntimeException("Failed to download snapshot from: " + snapshotPresignedUrl, e);
		}
	}

	@Override
	public void onConnected(ProgressCallback callback, GridConnectionInfo connection) {
		synchronizeClock(callback, connection);
	}

	@Override
	public void onNewPatch(ProgressCallback callback, GridConnectionInfo connection) {
		synchronizeClock(callback, connection);
	}

	@Override
	public void onExportSnapshot(ProgressCallback callback, GridConnectionInfo connection) {
		snapshotManager.createSnapshotIfPatchCountIsExceeded(connection);
	}
}
