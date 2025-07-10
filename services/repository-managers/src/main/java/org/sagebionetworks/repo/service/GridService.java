package org.sagebionetworks.repo.service;

import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlRequest;
import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlResponse;
import org.sagebionetworks.repo.model.grid.CreateReplicaRequest;
import org.sagebionetworks.repo.model.grid.CreateReplicaResponse;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.ListGridSessionsRequest;
import org.sagebionetworks.repo.model.grid.ListGridSessionsResponse;

public interface GridService {

	GridSession getGridSession(Long userId, String sessionId);

	CreateReplicaResponse createReplica(Long userId, CreateReplicaRequest request);

	CreateGridPresignedUrlResponse createPresignedUrl(Long userId, CreateGridPresignedUrlRequest request);

	GridReplica getReplica(Long userId, String sessionId, Long replicaId);

	ListGridSessionsResponse listActiveGridSessions(Long userId, ListGridSessionsRequest request);

	void deleteGridSession(Long userId, String gridSessionId);

}
