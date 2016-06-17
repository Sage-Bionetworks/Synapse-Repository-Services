package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.manager.DockerManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.springframework.beans.factory.annotation.Autowired;

public class DockerServiceImpl implements DockerService {

	@Autowired
	private UserManager userManager;
	
	@Autowired
	private DockerManager dockerManager;

	@Override
	public DockerAuthorizationToken authorizeDockerAccess(String userName, Long userId,
			String service, String scope) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return dockerManager.authorizeDockerAccess(userName, userInfo, service, scope);
	}

}
