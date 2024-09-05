package org.sagebionetworks.repo.service;

import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerCommitSortBy;
import org.sagebionetworks.repo.model.docker.DockerRegistryEventList;


public interface DockerService {

	/**
	 * Answer Docker Registry authorization request.
	 * 
	 * @param userId
	 * @param accessToken
	 * @param service
	 * @param scope
	 * @return
	 */
	DockerAuthorizationToken authorizeDockerAccess(Long userId, String accessToken, String service, List<String> scopes);
	
	/**
	 * 
	 * @param userId
	 * @param entityId
	 * @param dockerCommit
	 */
	void addDockerCommit(Long userId, String entityId, DockerCommit dockerCommit);
	
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
	PaginatedResults<DockerCommit> listDockerTags(Long userId,
			String entityId, DockerCommitSortBy sortBy, boolean ascending, long limit, long offset);
	
	
	/**
	 * Process Event notifications from Docker registry
	 * 
	 * @param registryEvents
	 */
	void dockerRegistryNotification(DockerRegistryEventList registryEvents);
	
	/**
	 * @param userId
	 * @param repositoryName
	 * @return The entity id matching the given repository name, not that the repository must be a managed repository
	 */
	EntityId getEntityIdForRepositoryName(Long userId, String repositoryName);


}
