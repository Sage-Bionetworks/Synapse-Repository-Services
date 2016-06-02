package org.sagebionetworks.repo.web.service;

import static org.sagebionetworks.repo.web.service.DockerNameUtil.REPO_NAME_PATH_SEP;

import org.sagebionetworks.auth.services.AuthenticationService;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerRegistryEvent;
import org.sagebionetworks.repo.model.docker.DockerRegistryEventList;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.docker.RegistryEventAction;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;



public class DockerServiceImpl implements DockerService {
	@Autowired
	private AuthenticationService authenticationService;

	@Autowired
	private EntityService entityService;

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
	
	public static String getParentIdFromRepositoryName(String name) {
		int i = name.indexOf(REPO_NAME_PATH_SEP);
		String result = name;
		if (i>0) result = name.substring(0, i);
		// validate that the string is a valid ID (i.e. "syn" followed by a number)
		KeyFactory.stringToKey(result);
		return result;
	}

	/**
	 * Process (push, pull) event notifications from Docker Registry
	 * @param registryEvents
	 */
	@WriteTransaction
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
				Long userId = authenticationService.getUserId(username);
				String entityId = null;
				// TODO what if you have update but not create permission and 
				// the repo' already exists?  If so you should not be trying to create.
				try {
					DockerRepository entity = new DockerRepository();
					entity.setIsManaged(true);
					entity.setName(entityName);
					entity.setParentId(parentId);
					entityId = entityService.createManagedDockerRepo(userId, entity);
				} catch (NameConflictException e) {
					// already exists
					// TODO find entityId for the given ID and name
				}
				// TODO Add commit to entity
			case pull:
				// nothing to do. We are being notified that someone has pulled a repository image
			default:
				throw new IllegalArgumentException("Unexpected action "+action);
			}
		}
	}
}
