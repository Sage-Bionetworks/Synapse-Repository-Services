package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerCommitSortBy;
import org.sagebionetworks.repo.model.docker.DockerRegistryEventList;


public interface DockerManager {

	/**
	 * Answer Docker Registry authorization request.

	 * @param userInfo
	 * @param accessToken
	 * @param service
	 * @param scope
	 * @return
	 */
	DockerAuthorizationToken authorizeDockerAccess(UserInfo userInfo, String accessToken, String service, List<String> scopes);

	/**
	 * Process (push, pull) event notifications from Docker Registry
	 * @param registryEvents
	 */
	void dockerRegistryNotification(DockerRegistryEventList registryEvents);
	
	/**
	 * 
	 * @param entityId
	 * @param userId
	 * @param commit
	 */
	void addDockerCommitToUnmanagedRespository(UserInfo userInfo, String entityId, DockerCommit commit);
	
	/**
	 * 
	 * @param userInfo
	 * @param entityId
	 * @param sortBy
	 * @param ascending
	 * @param limit
	 * @param offset
	 * @returnall the commits for a given Docker repository, choosing just
	 * the *latest* commit for each tag.
	 */
	PaginatedResults<DockerCommit> listDockerTags(UserInfo userInfo, String entityId, DockerCommitSortBy sortBy, boolean ascending, long limit, long offset);
	
	/**
	 * @param userInfo
	 * @param repositoryName
	 * @return The id of entity matching the given repository name, the name of the repository must be a managed repository
	 */
	EntityId getEntityIdForRepositoryName(UserInfo userInfo, String repositoryName);

}
