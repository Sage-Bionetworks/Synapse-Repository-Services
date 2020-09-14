package org.sagebionetworks.repo.manager;

import static org.sagebionetworks.repo.model.util.DockerNameUtil.REPO_NAME_PATH_SEP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.oauth.ClaimsJsonUtil;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DockerCommitDao;
import org.sagebionetworks.repo.model.DockerNodeDao;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerCommitSortBy;
import org.sagebionetworks.repo.model.docker.DockerRegistryEvent;
import org.sagebionetworks.repo.model.docker.DockerRegistryEventList;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.docker.RegistryEventAction;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.util.DockerNameUtil;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;

public class DockerManagerImpl implements DockerManager {
	@Autowired
	private NodeDAO nodeDao;
	
	@Autowired
	private DockerNodeDao dockerNodeDao;

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
	
	@Autowired
	StackConfiguration stackConfiguration;
	
	@Autowired
	private OIDCTokenHelper oidcTokenHelper;
	
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
	public DockerAuthorizationToken authorizeDockerAccess(UserInfo userInfo, String accessToken, String service, List<String> scopes) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(service, "service");
		
		List<DockerScopePermission> accessPermissions = new ArrayList<DockerScopePermission>();
		
		if (scopes != null) {
			for(String scope : scopes){
				String[] scopeParts = scope.split(":");
				if (scopeParts.length!=3) throw new RuntimeException("Expected 3 parts in scope param but found "+scopeParts.length + ". Scope param was " + scope);
				String type = scopeParts[0]; // type='repository' or 'registry'
				
				String name = scopeParts[1]; // if type is 'repository' then this is the name. if type is 'registry' this might be 'catalog'
				
				String actionTypes = scopeParts[2]; // e.g. push, pull
				
				// now get the scopes permitted in the Synapse access token
				List<OAuthScope> oauthScopes = Collections.EMPTY_LIST;
				if (accessToken!=null) {
					Jwt<JwsHeader, Claims> jwt = oidcTokenHelper.parseJWT(accessToken);
					oauthScopes = ClaimsJsonUtil.getScopeFromClaims(jwt.getBody());
				}
				
				Set<String> permittedActions = authorizationManager.getPermittedDockerActions(userInfo, oauthScopes, service, type, name, actionTypes);
				
				accessPermissions.add(new DockerScopePermission(type, name, permittedActions));
			}
		}

		// now construct the auth response and return it
		long now = System.currentTimeMillis();
		String uuid = UUID.randomUUID().toString();

		String token = DockerTokenUtil.createDockerAuthorizationToken(userInfo.getId().toString(), service, accessPermissions, now, uuid);
		DockerAuthorizationToken result = new DockerAuthorizationToken();
		result.setToken(token);
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
				//we only care about the notification if it is a manifest push, which contains metadata about the repository
				//the other type of push would be layer blob pushes ("mediaType": "application/octet-stream"), which we don't care about
				if(!event.getTarget().getMediaType().equals(MANIFEST_MEDIA_TYPE))
					continue;
				
				// need to make sure this is a registry we support
				String host = event.getRequest().getHost();
				if (!stackConfiguration.getDockerRegistryHosts().contains(host)) continue;
				// note the user ID was authenticated in the authorization check
				Long userId = Long.parseLong(event.getActor().getName());
				// the 'repository path' does not include the registry host or the tag
				String repositoryPath = event.getTarget().getRepository();
				String repositoryName = host+REPO_NAME_PATH_SEP+repositoryPath;
				String parentId = DockerNameUtil.getParentIdFromRepositoryPath(repositoryPath);
				if (parentId==null) throw new IllegalArgumentException("parentId is required.");
				DockerCommit commit = new DockerCommit();
				commit.setTag(event.getTarget().getTag());
				commit.setDigest(event.getTarget().getDigest());
				commit.setCreatedOn(new Date());

				String entityId =  dockerNodeDao.getEntityIdForRepositoryName(repositoryName);
				if (entityId==null) {
					// The node doesn't already exist
					try {
						EntityType parentType = nodeDao.getNodeTypeById(parentId);
						if (parentType!=EntityType.project) {
							throw new IllegalArgumentException("Parent must be a project.");
						}
					} catch (NotFoundException e) {
						throw new NotFoundException("parentId "+parentId+" does not exist.", e);
					}
					DockerRepository entity = new DockerRepository();
					entity.setIsManaged(true);
					entity.setRepositoryName(repositoryName);
					entity.setParentId(parentId);
					// Get the user
					UserInfo userInfo = userManager.getUserInfo(userId);
					entityId = entityManager.createEntity(userInfo, entity, null);
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
		authStatus.checkAuthorizationOrElseThrow();
		commit.setCreatedOn(new Date());
		String newEntityEtag = dockerCommitDao.createDockerCommit(entityId, userInfo.getId(), commit);
		transactionalMessenger.sendMessageAfterCommit(entityId, ObjectType.ENTITY, ChangeType.UPDATE);
	}

	@Override
	public PaginatedResults<DockerCommit> listDockerTags(UserInfo userInfo,
			String entityId, DockerCommitSortBy sortBy, boolean ascending,
			long limit, long offset) {
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(sortBy, "sortBy");
		AuthorizationStatus authStatus = authorizationManager.canAccess(userInfo, entityId, ObjectType.ENTITY, ACCESS_TYPE.READ);
		authStatus.checkAuthorizationOrElseThrow();
		EntityType entityType = entityManager.getEntityType(userInfo, entityId);
		if (!entityType.equals(EntityType.dockerrepo)) throw new IllegalArgumentException("Only Docker reposiory entities have commits.");
		List<DockerCommit> commits = dockerCommitDao.listDockerTags(entityId, sortBy, ascending, limit, offset);
		return PaginatedResults.createWithLimitAndOffset(commits, limit, offset);
	}
}
