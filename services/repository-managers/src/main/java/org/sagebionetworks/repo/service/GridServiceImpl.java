package org.sagebionetworks.repo.service;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlRequest;
import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlResponse;
import org.sagebionetworks.repo.model.grid.CreateReplicaRequest;
import org.sagebionetworks.repo.model.grid.CreateReplicaResponse;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.ListGridReplicasRequest;
import org.sagebionetworks.repo.model.grid.ListGridReplicasResponse;
import org.sagebionetworks.repo.model.grid.ListGridSessionsRequest;
import org.sagebionetworks.repo.model.grid.ListGridSessionsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GridServiceImpl implements GridService {

	@Autowired
	private UserManager userManager;

	@Autowired
	private GridManager gridManager;

	@Override
	public GridSession getGridSession(Long userId, String sessionId) {
		UserInfo user = userManager.getUserInfo(userId);
		return gridManager.getGridSession(user, sessionId);
	}

	@Override
	public CreateReplicaResponse createReplica(Long userId, CreateReplicaRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return gridManager.createReplica(user, request);
	}

	@Override
	public CreateGridPresignedUrlResponse createPresignedUrl(Long userId, CreateGridPresignedUrlRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return gridManager.createWebsocketPresignedUrl(user, request);
	}

	@Override
	public GridReplica getReplica(Long userId, String sessionId, Long replicaId) {
		UserInfo user = userManager.getUserInfo(userId);
		return gridManager.getReplica(user, sessionId, replicaId);
	}

	@Override
	public ListGridSessionsResponse listActiveGridSessions(Long userId, ListGridSessionsRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return gridManager.listActiveGridSessions(user, request);
	}

	@Override
	public ListGridReplicasResponse listReplicas(Long userId, ListGridReplicasRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return gridManager.listReplicas(user, request);
	}

	@Override
	public void deleteGridSession(Long userId, String gridSessionId) {
		UserInfo user = userManager.getUserInfo(userId);
		gridManager.deleteGridSession(user, gridSessionId);
	}

}
