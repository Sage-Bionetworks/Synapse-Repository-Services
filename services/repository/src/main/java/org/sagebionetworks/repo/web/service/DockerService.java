package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerCommitSortBy;


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
	public PaginatedResults<DockerCommit> listDockerCommits(Long userId, 
			String entityId, DockerCommitSortBy sortBy, boolean ascending, long limit, long offset);

}
