package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerCommitSortBy;

public interface DockerCommitDao {
	
	/*
	 * Add a new commit to the given entity.  Also update the entity's etag, modifiedOn, modifiedBy.
	 * Return the new etag
	 */
	String createDockerCommit(String entityId, long userId, DockerCommit commit);
	
	/*
	 * returns all the tagged commits for a given Docker repository, choosing just
	 * the *latest* commit for each tag.
	 */
	List<DockerCommit> listDockerTags(String entityId, DockerCommitSortBy sortBy, boolean ascending, long limit, long offset);
	
	/*
	 * Count the number of commits for a repository (just the latest commit for each tag).
	 */
	long countDockerCommits(String entityId);
	
	/*
	 * List the commits (usually zero or one) for the given entity and digest.
	 */
	List<DockerCommit> listCommitsByOwnerAndDigest(String entityId, String digest);
	
}
