package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;


public interface DockerService {

	/**
	 * Answer Docker Registry authorization request.
	 * 
	 * @param userName
	 * @param userId
	 * @param service
	 * @param scope
	 * @return
	 */
	public DockerAuthorizationToken authorizeDockerAccess(String userName, Long userId, String service, String scope);

}
