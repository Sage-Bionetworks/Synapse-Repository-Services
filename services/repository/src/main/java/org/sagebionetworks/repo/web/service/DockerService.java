package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;


public interface DockerService {

	/**
	 * Answer Docker Registry authorization request.
	 * 
	 * @param userId
	 * @param service
	 * @param scope
	 * @return
	 */
	public DockerAuthorizationToken authorizeDockerAccess(Long userId, String service, String scope);

}
