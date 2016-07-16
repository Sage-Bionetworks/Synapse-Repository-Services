package org.sagebionetworks.repo.model;

public interface DockerNodeDao {
	
	/*
	 * Create the relation between the given entityId and repository name.
	 * This is only used for managed Docker repositories.
	 */
	void createRepositoryName(String entityId, String repositoryName);
	
	/*
	 * Return the unique entity ID for the given (managed) repository name or null if none exists.
	 */
	String getEntityIdForRepositoryName(String repositoryName);

}
