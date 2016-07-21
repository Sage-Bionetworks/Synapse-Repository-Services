package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.model.docker.DockerCommit;

public interface DockerCommitDao {
	
	void createDockerCommit(String entityId, DockerCommit commit);
	
	/*
	 * returns all the commits for a given Docker repository, choosing just
	 * the *latest* commit for each tag.
	 */
	List<DockerCommit> listDockerCommits(String entityId);
	
}
