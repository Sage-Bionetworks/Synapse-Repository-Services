package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.sagebionetworks.repo.model.docker.DockerRegistryEventList;


public interface DockerManager {

	/**
	 * Answer Docker Registry authorization request.

	 * @param userInfo
	 * @param service
	 * @param scope
	 * @return
	 */
	public DockerAuthorizationToken authorizeDockerAccess(UserInfo userInfo, String service, String scope);

	/**
	 * Process (push, pull) event notifications from Docker Registry
	 * @param registryEvents
	 */
	public void dockerRegistryNotification(DockerRegistryEventList registryEvents);
}
