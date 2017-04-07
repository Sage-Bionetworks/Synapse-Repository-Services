package org.sagebionetworks.repo.manager;

import static org.sagebionetworks.repo.model.docker.RegistryEventAction.pull;
import static org.sagebionetworks.repo.model.util.DockerNameUtil.REPO_NAME_PATH_SEP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DockerCommitDao;
import org.sagebionetworks.repo.model.DockerNodeDao;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerCommitSortBy;
import org.sagebionetworks.repo.model.docker.DockerRegistryEvent;
import org.sagebionetworks.repo.model.docker.DockerRegistryEventList;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.docker.RegistryEventAction;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.util.DockerNameUtil;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;



public class DockerManagerImpl implements DockerManager {
	@Autowired
	private NodeDAO nodeDAO;
	
	@Autowired
	private DockerNodeDao dockerNodeDao;

	@Autowired
	IdGenerator idGenerator;

	@Autowired
	UserManager userManager;

	@Autowired
	EntityManager entityManager;

	@Autowired
	AuthorizationManager authorizationManager;
	
	@Autowired
	DockerCommitDao dockerCommitDao;
	
	@Autowired
	private TransactionalMessenger transactionalMessenger;
	
	public static String MANIFEST_MEDIA_TYPE = "application/vnd.docker.distribution.manifest.v2+json";

	/**
	 * Answer Docker Registry authorization request.
	 * 
	 * @param userInfo
	 * @param service
	 * @param scope optional parameter used to authenticate access to docker repositories.
	 *			pass null if the docker client only needs to check login credentials
	 * @return
	 */
	@Override
	public DockerAuthorizationToken authorizeDockerAccess(UserInfo userInfo, String service, List<String> scopes) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(service, "service");
		
		List<DockerScopePermission> accessPermissions = new ArrayList<DockerScopePermission>();
		
		if (scopes != null) {
			for(String scope : scopes){
				String[] scopeParts = scope.split(":");
				if (scopeParts.length!=3) throw new RuntimeException("Expected 3 parts in scope param but found "+scopeParts.length + ". Scope param was " + scope);
				String type = scopeParts[0]; // type='repository'
				
				String repositoryPath = scopeParts[1]; // i.e. the 'path'
				
				String actionTypes = scopeParts[2]; // e.g. push, pull
				Set<RegistryEventAction> permittedActions = getPermittedActions(userInfo,  service, repositoryPath, actionTypes);
				
				accessPermissions.add(new DockerScopePermission(type, repositoryPath, permittedActions));
			}
		}

		// now construct the auth response and return it
		long now = System.currentTimeMillis();
		String uuid = UUID.randomUUID().toString();

		String token = DockerTokenUtil.createToken(userInfo.getId().toString(), service, accessPermissions, now, uuid);
		DockerAuthorizationToken result = new DockerAuthorizationToken();
		result.setToken(token);
		return result;

	}

	public Set<RegistryEventAction> getPermittedActions(UserInfo userInfo, 
			String service, String repositoryPath, String actionTypes) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(service, "service");
		ValidateArgument.required(repositoryPath, "repositoryPath");
		ValidateArgument.required(actionTypes, "actionTypes");

		Set<RegistryEventAction> permittedActions = new HashSet<RegistryEventAction>();

		String parentId = validParentProjectId(repositoryPath);

		if (parentId==null) return permittedActions;

		String repositoryName = service+DockerNameUtil.REPO_NAME_PATH_SEP+repositoryPath;

		String existingDockerRepoId = dockerNodeDao.getEntityIdForRepositoryName(repositoryName);

		for (String requestedActionString : actionTypes.split(",")) {
			RegistryEventAction requestedAction = RegistryEventAction.valueOf(requestedActionString);
			switch (requestedAction) {
			case push:
				// check CREATE or UPDATE permission and add to permittedActions
				AuthorizationStatus as = null;
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
				if (as!=null && as.getAuthorized()) {
					permittedActions.add(requestedAction);
					if (existingDockerRepoId==null) permittedActions.add(pull);
				}
				break;
			case pull:
				// check DOWNLOAD permission and add to permittedActions
				if (existingDockerRepoId!=null) {
					if (authorizationManager.canAccess(
							userInfo, existingDockerRepoId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD).getAuthorized()
					) permittedActions.add(requestedAction);
				}
				break;
			default:
				throw new RuntimeException("Unexpected action type: " + requestedAction);
			}
		}
		return permittedActions;
	}


	/*
	 * Given a repository path, return a valid parent Id, 
	 * a project which has been verified to exist. 
	 * If there is no such valid parent then return null
	 */
	public String validParentProjectId(String repositoryPath) {
		// check that 'repositoryPath' is a valid path
		try {
			DockerNameUtil.validateName(repositoryPath);
		} catch (IllegalArgumentException e) {
			return null;
		}
		// check that 'repopath' starts with a synapse ID (synID) and synID is a project or folder
		String parentId;
		try {
			parentId = getParentIdFromRepositoryPath(repositoryPath);
		} catch (IllegalArgumentException e) {
			return null;
		}

		Long parentIdAsLong = KeyFactory.stringToKey(parentId);
		List<EntityHeader> headers = nodeDAO.getEntityHeader(Collections.singleton(parentIdAsLong));
		if (headers.size()==0) return null; // Not found!
		if (headers.size()>1) throw new IllegalStateException("Expected one node with ID "+parentId+" but found "+headers.size());
		EntityHeader header = headers.get(0);
		if(EntityTypeUtils.getEntityTypeForClassName(header.getType())!=EntityType.project) return null; // parent must be a project!
		return header.getId();
	}

	private static String getParentIdFromRepositoryPath(String name) {
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
	@WriteTransactionReadCommitted
	@Override
	public void dockerRegistryNotification(DockerRegistryEventList registryEvents) {
		for (DockerRegistryEvent event : registryEvents.getEvents()) {
			RegistryEventAction action = event.getAction();
			switch (action) {
			case push:
				//we only care about the notification if it is a manifest push, which contains metadata about the repository
				//the other type of push would be layer blob pushes ("mediaType": "application/octet-stream"), which we don't care about
				if(!event.getTarget().getMediaType().equals(MANIFEST_MEDIA_TYPE))
					continue;
				
				// need to make sure this is a registry we support
				String host = event.getRequest().getHost();
				if (!StackConfiguration.getDockerRegistryHosts().contains(host)) continue;
				// note the user ID was authenticated in the authorization check
				Long userId = Long.parseLong(event.getActor().getName());
				// the 'repository path' does not include the registry host or the tag
				String repositoryPath = event.getTarget().getRepository();
				String repositoryName = host+REPO_NAME_PATH_SEP+repositoryPath;
				String parentId = getParentIdFromRepositoryPath(repositoryPath);
				if (parentId==null) throw new IllegalArgumentException("parentId is required.");
				DockerCommit commit = new DockerCommit();
				commit.setTag(event.getTarget().getTag());
				commit.setDigest(event.getTarget().getDigest());
				commit.setCreatedOn(new Date());

				String entityId =  dockerNodeDao.getEntityIdForRepositoryName(repositoryName);
				if (entityId==null) {
					// The node doesn't already exist
					List<EntityHeader> headers = nodeDAO.getEntityHeader(Collections.singleton(KeyFactory.stringToKey(parentId)));
					if (headers.size()==0) throw new NotFoundException("parentId "+parentId+" does not exist.");
					if (headers.size()>1) throw new IllegalStateException("Expected 0-1 result for "+parentId+" but found "+headers.size());
					if (EntityTypeUtils.getEntityTypeForClassName(headers.get(0).getType())!=EntityType.project) {
						throw new IllegalArgumentException("Parent must be a project.");
					}
					DockerRepository entity = new DockerRepository();
					entity.setIsManaged(true);
					entity.setRepositoryName(repositoryName);
					entity.setParentId(parentId);
					// Get the user
					UserInfo userInfo = userManager.getUserInfo(userId);
					// Create a new id for this entity
					long newId = idGenerator.generateNewId(IdType.ENTITY_ID);
					entity.setId(KeyFactory.keyToString(newId));
					entityManager.createEntity(userInfo, entity, null);
					entityId =  KeyFactory.keyToString(newId);
					dockerNodeDao.createRepositoryName(entityId, repositoryName);
				}
				// Add commit to entity
				dockerCommitDao.createDockerCommit(entityId, userId, commit);
				break;
			case pull:
				// nothing to do. We are being notified that someone has pulled a repository image
				break;
			case mount:
				//nothing to do. We are being notified that another repository is being mounted to prevent re-upload
				break;
			default:
				throw new IllegalArgumentException("Unexpected action "+action);
			}
		}
	}

	@Override
	public void addDockerCommitToUnmanagedRespository(UserInfo userInfo, String entityId,
			DockerCommit commit) {
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(commit.getDigest(), "digest");
		if (dockerNodeDao.getRepositoryNameForEntityId(entityId)!=null) {
			throw new IllegalArgumentException("Commits for managed Docker repositories are created using the Docker client rather than the Synapse client.");
		}
		AuthorizationStatus authStatus = authorizationManager.canAccess(userInfo, entityId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(authStatus);
		commit.setCreatedOn(new Date());
		String newEntityEtag = dockerCommitDao.createDockerCommit(entityId, userInfo.getId(), commit);
		transactionalMessenger.sendMessageAfterCommit(entityId, ObjectType.ENTITY, newEntityEtag, ChangeType.UPDATE);
	}

	@Override
	public PaginatedResults<DockerCommit> listDockerCommits(UserInfo userInfo,
			String entityId, DockerCommitSortBy sortBy, boolean ascending,
			long limit, long offset) {
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(sortBy, "sortBy");
		AuthorizationStatus authStatus = authorizationManager.canAccess(userInfo, entityId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(authStatus);
		EntityType entityType = entityManager.getEntityType(userInfo, entityId);
		if (!entityType.equals(EntityType.dockerrepo)) throw new IllegalArgumentException("Only Docker reposiory entities have commits.");
		List<DockerCommit> commits = dockerCommitDao.listDockerCommits(entityId, sortBy, ascending, limit, offset);
		return PaginatedResults.createWithLimitAndOffset(commits, limit, offset);
	}
}
