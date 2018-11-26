package org.sagebionetworks.repo.web.service;

import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerCommitSortBy;
import org.sagebionetworks.repo.model.docker.DockerRegistryEventList;


public interface DockerService {

	/**
	 * Answer Docker Registry authorization request.
	 * 
	 * @param userId
	 * @param service
	 * @param scope
	 * @return
	 */
	public DockerAuthorizationToken authorizeDockerAccess(Long userId, String service, List<String> scopes);
	
	/**
	 * 
	 * @param userId
	 * @param entityId
	 * @param dockerCommit
	 */
	public void addDockerCommit(Long userId, String entityId, DockerCommit dockerCommit);
	
	/**
	 * 
	 * @param userId
	 * @param entityId
	 * @param sortBy
	 * @param ascending
	 * @param limit
	 * @param offset
	 * @return
	 */
	public PaginatedResults<DockerCommit> listDockerTags(Long userId,
			String entityId, DockerCommitSortBy sortBy, boolean ascending, long limit, long offset);
	
	
	/**
	 * Process Event notifications from Docker registry
	 * 
	 * @param registryEvents
	 */
	public void dockerRegistryNotification(DockerRegistryEventList registryEvents);


}
