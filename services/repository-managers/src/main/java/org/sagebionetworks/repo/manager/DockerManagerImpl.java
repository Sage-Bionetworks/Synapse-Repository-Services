package org.sagebionetworks.repo.manager;

import static org.sagebionetworks.repo.model.util.DockerNameUtil.REPO_NAME_PATH_SEP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
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
import org.sagebionetworks.repo.model.util.DockerNameUtil;
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
	AuthorizationManager authorizationManager;

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
		String[] scopeParts = scope.split(":");
		if (scopeParts.length!=3) throw new RuntimeException("Expected 3 parts but found "+scopeParts.length);
		String type = scopeParts[0]; // type='repository'
		String repositoryPath = scopeParts[1]; // i.e. the 'path'
		String accessTypes = scopeParts[2]; // e.g. push, pull

		List<String> permittedAccessTypes = getPermittedAccessTypes( userName,  userInfo,  service,  type, repositoryPath, accessTypes);
		// now construct the auth response and return it
		long now = System.currentTimeMillis();
		String uuid = UUID.randomUUID().toString();

		String token = DockerTokenUtil.createToken(userName, type, service, repositoryPath, 
				permittedAccessTypes, now, uuid);
		DockerAuthorizationToken result = new DockerAuthorizationToken();
		result.setToken(token);
		return result;

	}

	public List<String> getPermittedAccessTypes(String userName, UserInfo userInfo, 
			String service, String type, String repositoryPath, String accessTypes) {

		List<String> permittedAccessTypes = new ArrayList<String>();

		String parentId = validParentProjectId(repositoryPath);

		if (parentId==null) return permittedAccessTypes;

		String entityName = service+DockerNameUtil.REPO_NAME_PATH_SEP+repositoryPath;
		EntityHeader entityHeader = null;
		try {
			entityHeader = nodeDAO.getEntityHeaderByChildName(parentId, entityName);
		} catch (NotFoundException nfe) {
			entityHeader = null;
		}
		String existingDockerRepoId = null;
		if (entityHeader!=null && EntityType.valueOf(entityHeader.getType()) == EntityType.dockerrepo) {
			existingDockerRepoId = entityHeader.getId();
		}

		for (String requestedAccessTypeString : accessTypes.split(",")) {
			RegistryEventAction requestedAccessType = RegistryEventAction.valueOf(requestedAccessTypeString);
			AuthorizationStatus as = null;
			switch (requestedAccessType) {
			case push:
				// check CREATE or UPDATE permission and add to permittedAccessTypes
				if (existingDockerRepoId==null) {
					// check for create permission on parent
					Node node = new Node();
					node.setParentId(parentId);
					node.setNodeType(EntityType.dockerrepo);
					as = authorizationManager.canCreate(userInfo, node);
				} else {
					// check update permission on this entity
					as = authorizationManager.canAccess(
							userInfo, existingDockerRepoId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
				}
				break;
			case pull:
				// check DOWNLOAD permission and add to permittedAccessTypes
				if (existingDockerRepoId!=null) {
					as = authorizationManager.canAccess(
							userInfo, existingDockerRepoId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD);
					// TODO if denied here see if the repo is in a submission and, if so, 
					// check the ACL on the Evaluation owning the Submission
				}
				break;
			default:
				throw new RuntimeException("Unexpected access type: "+requestedAccessType);
			}
			if (as!=null && as.getAuthorized()) permittedAccessTypes.add(requestedAccessType.name());
		}
		return permittedAccessTypes;
	}


	/*
	 * Given a repository path, return a valid parent Id, 
	 * a project which has been verified to exist. 
	 * If there is no such valid parent then return null
	 */
	public String validParentProjectId(String repositoryName) {
		// check that 'repopath' is a valid path
		try {
			DockerNameUtil.validateName(repositoryName);
		} catch (IllegalArgumentException e) {
			return null;
		}
		// check that 'repopath' starts with a synapse ID (synID) and synID is a project or folder
		String parentId;
		try {
			parentId = getParentIdFromRepositoryName(repositoryName);
		} catch (DatastoreException e) {
			return null;
		}

		Long parentIdAsLong = KeyFactory.stringToKey(parentId);
		List<EntityHeader> headers = nodeDAO.getEntityHeader(Collections.singleton(parentIdAsLong));
		if (headers.size()==0) return null; // Not found!
		if (headers.size()>1) throw new IllegalStateException("Expected one node with ID "+parentId+" but found "+headers.size());
		EntityHeader header = headers.get(0);
		if (!header.getType().equals(EntityType.project.name())) return null; // parent must be a project!
		return header.getId();
	}

	private static String getParentIdFromRepositoryName(String name) {
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
				if (parentId==null) throw new IllegalArgumentException("parentId is required.");
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
					List<EntityHeader> headers = nodeDAO.getEntityHeader(Collections.singleton(KeyFactory.stringToKey(parentId)));
					if (headers.size()==0) throw new NotFoundException("parentId "+parentId+" does not exist.");
					if (headers.size()>1) throw new IllegalStateException("Expected 0-1 result for "+parentId+" but found "+headers.size());
					if (EntityType.valueOf(headers.get(0).getType())!=EntityType.project) {
						throw new IllegalArgumentException("Parent must be a project.");
					}
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
				break;
			case pull:
				// nothing to do. We are being notified that someone has pulled a repository image
				break;
			default:
				throw new IllegalArgumentException("Unexpected action "+action);
			}
		}
	}
}
