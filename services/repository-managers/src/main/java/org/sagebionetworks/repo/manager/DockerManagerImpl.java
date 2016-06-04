package org.sagebionetworks.repo.manager;

import static org.sagebionetworks.repo.manager.DockerNameUtil.REPO_NAME_PATH_SEP;

import java.security.KeyPair;
import java.util.Arrays;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerRegistryEvent;
import org.sagebionetworks.repo.model.docker.DockerRegistryEventList;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.docker.RegistryEventAction;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;



public class DockerManagerImpl implements DockerManager {
	@Autowired
	private NodeDAO nodeDAO;
	
	@Autowired
	IdGenerator idGenerator;
	
	@Autowired
	UserManager userManager;
	
	@Autowired
	EntityManager entityManager;
	
	@Autowired
	PrincipalAliasDAO principalAliasDAO;

	/**
	 * Answer Docker Registry authorization request.
	 * 
	 * @param userId
	 * @param service
	 * @param scope
	 * @return
	 */
	@Override
	public DockerAuthorizationToken authorizeDockerAccess(String userName, UserInfo userInfo, String service, String scope) {
		// check that 'service' matches a supported registry host
		// scope is repository:repopath:actions
		// check that 'repopath' is a valid path
		// check that 'repopath' starts with a synapse ID (synID)
		// for 'push' access, check canCreate, and UPDATE access in synID
		// for 'pull' access, check READ and DOWNLOAD access in synID
		// now construct the auth response and return it
		
		String[] scopeParts = scope.split(":");
		if (scopeParts.length!=3) throw new RuntimeException("Expected 3 parts but found "+scopeParts.length);
		String type = scopeParts[0];
		String repository = scopeParts[1];
		String accessTypes = scopeParts[2];

		KeyPair keyPair = null; // TODO
		String token = DockerTokenUtil.createToken(keyPair, userName, type, service, repository, Arrays.asList(accessTypes.split(",")));
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
				PrincipalAlias pa = userManager.lookupPrincipalByAlias(username);
				if(AliasType.TEAM_NAME.equals(pa.getType())) throw new RuntimeException(username+" is a Team name.");
				Long userId = pa.getPrincipalId();
				
				String entityId = null;
				try {
					EntityHeader entityHeader = nodeDAO.getEntityHeaderByChildName(parentId, entityName);
					if (entityHeader.getType()!= EntityType.dockerrepo.name()) 
						throw new IllegalArgumentException("Cannot create a Docker repository in container "+parentId+
								". An entity of type "+entityHeader.getType()+" already exists with name "+entityName);
					entityId = entityHeader.getId();
				} catch (NotFoundException nfe) {
					// The node doesn't already exist

					DockerRepository entity = new DockerRepository();
					entity.setIsManaged(true);
					entity.setName(entityName);
					entity.setParentId(parentId);
					// Get the user
					UserInfo userInfo = userManager.getUserInfo(userId);
					// Create a new id for this entity
					long newId = idGenerator.generateNewId();
					entity.setId(KeyFactory.keyToString(newId));
					entityManager.createEntity(userInfo, entity, null);
					entityId =  KeyFactory.keyToString(newId);
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
