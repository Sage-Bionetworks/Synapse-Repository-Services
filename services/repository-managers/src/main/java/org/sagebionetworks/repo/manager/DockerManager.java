package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.sagebionetworks.repo.model.docker.DockerRegistryEventList;


public interface DockerManager {

	/**
	 * Answer Docker Registry authorization request.

	 * @param userName
	 * @param userInfo
	 * @param service
	 * @param scope
	 * @return
	 */
	public DockerAuthorizationToken authorizeDockerAccess(String userName, UserInfo userInfo, String service, String scope);

	/**
	 * Process (push, pull) event notifications from Docker Registry
	 * @param registryEvents
	 */
	public void dockerRegistryNotification(DockerRegistryEventList registryEvents);
}
