package org.sagebionetworks.repo.web.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerRegistryEvent;
import org.sagebionetworks.repo.model.docker.DockerRegistryEventList;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.docker.RegistryEventAction;



public class DockerServiceImpl implements DockerService {

	/**
	 * Answer Docker Registry authorization request.
	 * 
	 * @param userId
	 * @param service
	 * @param scope
	 * @return
	 */
	@Override
	public DockerAuthorizationToken authorizeDockerAccess(Long userId, String service, String scope) {
		// check that 'service' matches a supported registry host
		// scope is repository:repopath:actions
		// check that 'repopath' is a valid path
		// check that 'repopath' starts with a synapse ID (synID)
		// for 'push' access, check canCreate, and UPDATE access in synID
		// for 'pull' access, check READ and DOWNLOAD access in synID
		// now construct the auth response and return it
		String token = null; 
		DockerAuthorizationToken result = new DockerAuthorizationToken();
		result.setToken(token);
		return result;
	}
	
	private static final String REPO_NAME_PATH_SEP = "/"; // TODO combine with parsing util's
	
	public static String getParentIdFromRepositoryName(String name) {
		int i = name.indexOf(REPO_NAME_PATH_SEP);
		String result = name;
		if (i>0) result = name.substring(0, i);
		// TODO validate that the string is a valid ID (i.e. "syn" followed by a number)
		return result;
	}

	/**
	 * Process (push, pull) event notifications from Docker Registry
	 * @param registryEvents
	 */
	@Override
	public void dockerRegistryNotification(DockerRegistryEventList registryEvents) {
		for (DockerRegistryEvent event : registryEvents.getEvents()) {
			RegistryEventAction action = event.getAction();
			switch (action) {
			case push:
				// need to make sure this is a registry we support
				String host = event.getRequest().getHost();
				// note the username was authenticated in the authorization check
				String username = event.getActor().getName();
				// the 'repository name' does not include the registry host or the tag
				String repositoryName = event.getTarget().getRepository();
				String entityName = host+REPO_NAME_PATH_SEP+repositoryName;
				String parentId = getParentIdFromRepositoryName(repositoryName);
				DockerCommit commit = new DockerCommit();
				commit.setTag(event.getTarget().getTag());
				commit.setDigest(event.getTarget().getDigest());
				// create or update the given object
				// TODO search for the Repo with the given name and parent
				boolean alreadyExists = false;
				if (alreadyExists) {
					DockerRepository entity =null; // TODO get current version
					Set<DockerCommit> commits = new HashSet<DockerCommit>(entity.getCommits());
					commits.add(commit);
					entity.setCommits(commits);
					// TODO update entity
				} else {
					DockerRepository entity = new DockerRepository();
					entity.setCommits(Collections.singleton(commit));
					entity.setIsManaged(true);
					entity.setName(host+"/"+repositoryName);
					entity.setParentId(parentId);
					// TODO create entity
				}
			case pull:
				// nothing to do. We are being notified that someone has pulled a repository image
			default:
				throw new IllegalArgumentException("Unexpected action "+action);
			}
		}
	}
}
