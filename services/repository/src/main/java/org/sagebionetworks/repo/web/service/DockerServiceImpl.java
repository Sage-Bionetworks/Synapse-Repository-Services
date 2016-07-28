package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.DockerManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerCommitSortBy;
import org.springframework.beans.factory.annotation.Autowired;

public class DockerServiceImpl implements DockerService {

	@Autowired
	private UserManager userManager;
	
	@Autowired
	private DockerManager dockerManager;

	@Override
	public DockerAuthorizationToken authorizeDockerAccess(Long userId,
			String service, String scope) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return dockerManager.authorizeDockerAccess(userInfo, service, scope);
	}

	@Override
	public void addDockerCommit(Long userId, String entityId, DockerCommit dockerCommit) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		dockerManager.addDockerCommitToUnmanagedRespository(userInfo, entityId, dockerCommit);
	}

	@Override
	public PaginatedResults<DockerCommit> listDockerCommits(Long userId, 
			String entityId, DockerCommitSortBy sortBy, boolean ascending, long limit, long offset) {
				UserInfo userInfo = userManager.getUserInfo(userId);
		return dockerManager.listDockerCommits(userInfo, entityId, sortBy, ascending, limit, offset);
	}

}
