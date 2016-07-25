package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerCommitSortBy;

public interface DockerCommitDao {
	
	void createDockerCommit(String entityId, long userId, DockerCommit commit);
	
	/*
	 * returns all the commits for a given Docker repository, choosing just
	 * the *latest* commit for each tag.
	 */
	List<DockerCommit> listDockerCommits(String entityId, 
			DockerCommitSortBy sortBy, boolean ascending,long limit, long offset);
	
	/*
	 * Count the number of commits for a repository (just the latest commit for each tag).
	 */
	long countDockerCommits(String entityId);
	
}
