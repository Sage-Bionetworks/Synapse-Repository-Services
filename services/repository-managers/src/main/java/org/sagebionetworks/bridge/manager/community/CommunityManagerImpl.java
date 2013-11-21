package org.sagebionetworks.bridge.manager.community;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.apache.commons.fileupload.FileItemStream;
import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.manager.util.Validate;
import org.sagebionetworks.repo.manager.*;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.manager.wiki.V2WikiManager;
import org.sagebionetworks.repo.model.*;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.model.dbo.dao.AuthorizationUtils;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.jdo.NodeField;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.util.StringInputStream;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class CommunityManagerImpl implements CommunityManager {
	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private TeamManager teamManager;
	@Autowired
	private V2WikiManager wikiManager;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private EntityPermissionsManager entityPermissionsManager;
	
	@Autowired
	private NodeQueryDao nodeQueryDao;

	public CommunityManagerImpl() {
	}

	// for testing
	CommunityManagerImpl(AuthorizationManager authorizationManager, FileHandleManager fileHandleManager, UserManager userManager,
			TeamManager teamManager, EntityManager entityManager, EntityPermissionsManager entityPermissionsManager, V2WikiManager wikiManager) {
		this.authorizationManager = authorizationManager;
		this.fileHandleManager = fileHandleManager;
		this.userManager = userManager;
		this.teamManager = teamManager;
		this.entityManager = entityManager;
		this.entityPermissionsManager = entityPermissionsManager;
		this.wikiManager = wikiManager;
	}

	public static void validateForCreate(Community community) {
		Validate.notSpecifiable(community.getCreatedBy(), "createdBy");
		Validate.notSpecifiable(community.getCreatedOn(), "createdOn");
		Validate.notSpecifiable(community.getEtag(), "etag");
		Validate.notSpecifiable(community.getId(), "id");
		Validate.notSpecifiable(community.getModifiedBy(), "modifiedBy");
		Validate.notSpecifiable(community.getModifiedOn(), "modifiedOn");
		Validate.notSpecifiable(community.getTeamId(), "teamId");
		Validate.notSpecifiable(community.getWelcomePageWikiId(), "welcomePageWikiId");
		Validate.notSpecifiable(community.getIndexPageWikiId(), "indexPageWikiId");
		Validate.notSpecifiable(community.getTeamId(), "teamId");

		Validate.required(community.getName(), "name");
		Validate.optional(community.getDescription(), "description");
	}

	public static void validateForUpdate(Community community) {
		Validate.missing(community.getEtag(), "etag");
		Validate.missing(community.getId(), "id");
		Validate.missing(community.getTeamId(), "teamId");

		Validate.required(community.getName(), "name");
		Validate.optional(community.getDescription(), "description");
	}

	public static void populateCreationFields(UserInfo userInfo, Community community) {
		Date now = new Date();
		community.setCreatedBy(userInfo.getIndividualGroup().getId());
		community.setCreatedOn(now);
		community.setModifiedBy(userInfo.getIndividualGroup().getId());
		community.setModifiedOn(now);
	}

	public static void populateUpdateFields(UserInfo userInfo, Community community) {
		Date now = new Date();
		community.setCreatedBy(null); // by setting to null we are telling the DAO to use the current values
		community.setCreatedOn(null);
		community.setModifiedBy(userInfo.getIndividualGroup().getId());
		community.setModifiedOn(now);
	}

	private static final ACCESS_TYPE[] ANONYMOUS_PERMISSIONS = { ACCESS_TYPE.READ };
	private static final ACCESS_TYPE[] SIGNEDIN_PERMISSIONS = { ACCESS_TYPE.READ, ACCESS_TYPE.PARTICIPATE };
	private static final ACCESS_TYPE[] COMMUNITY_MEMBER_PERMISSIONS = { ACCESS_TYPE.READ, ACCESS_TYPE.SEND_MESSAGE, ACCESS_TYPE.PARTICIPATE };
	private static final ACCESS_TYPE[] COMMUNITY_ADMIN_PERMISSIONS = { ACCESS_TYPE.READ, ACCESS_TYPE.UPDATE, ACCESS_TYPE.CREATE,
			ACCESS_TYPE.DELETE, ACCESS_TYPE.SEND_MESSAGE };

	public static ResourceAccess createResourceAccess(long principalId, ACCESS_TYPE[] accessTypes) {
		Set<ACCESS_TYPE> accessSet = new HashSet<ACCESS_TYPE>(Arrays.asList(accessTypes));
		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(accessSet);
		ra.setPrincipalId(principalId);
		return ra;
	}

	public static void addToACL(AccessControlList acl, String principalId, ACCESS_TYPE[] accessTypes) {
		ResourceAccess ra = new ResourceAccess();
		Set<ACCESS_TYPE> accessSet = new HashSet<ACCESS_TYPE>(Arrays.asList(accessTypes));
		ra.setAccessType(accessSet);
		ra.setPrincipalId(Long.parseLong(principalId));
		acl.getResourceAccess().add(ra);
	}

	public static void removeFromACL(AccessControlList acl, String principalId) {
		Set<ResourceAccess> origRA = acl.getResourceAccess();
		Set<ResourceAccess> newRA = new HashSet<ResourceAccess>();
		for (ResourceAccess ra : origRA) {
			if (!principalId.equals(ra.getPrincipalId().toString()))
				newRA.add(ra);
		}
		acl.setResourceAccess(newRA);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.community.CommunityManager#create(org.sagebionetworks.repo.model.UserInfo,
	 * org.sagebionetworks.repo.model.Community)
	 * 
	 * Note: This method must execute within a transaction, since it makes calls to two different DAOs
	 */
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Community create(UserInfo userInfo, Community community) throws DatastoreException, InvalidModelException, UnauthorizedException,
			NotFoundException, NameConflictException, ACLInheritanceException, IOException, ServiceUnavailableException {
		if (AuthorizationUtils.isUserAnonymous(userInfo)) {
			throw new UnauthorizedException("Anonymous user cannot create Community.");
		}
		validateForCreate(community);

		// create the team (this will do a uniqueness check on the team name
		Team team = new Team();
		team.setName(community.getName());
		team.setDescription(community.getDescription());
		team.setCanPublicJoin(true);
		team = teamManager.create(userInfo, team);
		community.setTeamId(team.getId());

		populateCreationFields(userInfo, community);

		String communityId = entityManager.createEntity(userInfo, community, null);
		community = entityManager.getEntity(userInfo, communityId, Community.class);

		// adding the current user to the community as an admin, and the team as a non-admin
		AccessControlList acl = entityPermissionsManager.getACL(communityId, userInfo);
		UserGroup authenticatedUsers = userManager.getDefaultUserGroup(DEFAULT_GROUPS.AUTHENTICATED_USERS);
		UserGroup allUsers = userManager.getDefaultUserGroup(DEFAULT_GROUPS.PUBLIC);
		Set<ResourceAccess> raSet = Sets.newHashSet(createResourceAccess(Long.parseLong(allUsers.getId()), ANONYMOUS_PERMISSIONS),
				createResourceAccess(Long.parseLong(authenticatedUsers.getId()), SIGNEDIN_PERMISSIONS),
				createResourceAccess(Long.parseLong(team.getId()), COMMUNITY_MEMBER_PERMISSIONS),
				createResourceAccess(Long.parseLong(userInfo.getIndividualGroup().getId()), COMMUNITY_ADMIN_PERMISSIONS));
		acl.setResourceAccess(raSet);
		entityPermissionsManager.updateACL(acl, userInfo);

		V2WikiPage rootPage = createWikiPage(userInfo, community, communityId, null, community.getName(), "Root");
		V2WikiPage welcomePage = createWikiPage(userInfo, community, communityId, rootPage, "Welcome to " + community.getName(), "Welcome");
		V2WikiPage indexPage = createWikiPage(userInfo, community, communityId, rootPage, "Index of " + community.getName(), "index");

		community.setWelcomePageWikiId(welcomePage.getId());
		community.setIndexPageWikiId(indexPage.getId());
		entityManager.updateEntity(userInfo, community, false, null);

		community = entityManager.getEntity(userInfo, communityId, Community.class);
		return community;
	}

	private V2WikiPage createWikiPage(UserInfo userInfo, Community community, String communityId, V2WikiPage rootPage, String title,
			final String content) throws NotFoundException, IOException, ServiceUnavailableException {

		FileItemStream fis = new FileItemStream() {
			@Override
			public InputStream openStream() throws IOException {
				return new StringInputStream(content);
			}

			@Override
			public boolean isFormField() {
				return false;
			}

			@Override
			public String getName() {
				return "initial_markdown.txt";
			}

			@Override
			public String getFieldName() {
				return "none";
			}

			@Override
			public String getContentType() {
				return "application/text";
			}
		};
		S3FileHandle uploadedFile = fileHandleManager.uploadFile(userInfo.getIndividualGroup().getId(), fis);

		V2WikiPage page = new V2WikiPage();
		page.setTitle(title);
		page.setParentWikiId(rootPage == null ? null : rootPage.getId());
		page.setMarkdownFileHandleId(uploadedFile.getId());
		page = wikiManager.createWikiPage(userInfo, communityId, ObjectType.ENTITY, page);
		return page;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.community.CommunityManager#getByMember(java.lang.String, int, int)
	 */
	@Override
	public PaginatedResults<Community> getCommunitiesByMember(UserInfo userInfo, String principalId, int limit, int offset) throws DatastoreException,
			NotFoundException {
		
		// temp code. Will add new table to connect community to teams and visa versa.
		BasicQuery basicQuery = createCommunityQuery();
		
		// This exposes the user's community memberships, though that appears to be the intent of this method.
		NodeQueryResults nodeResults = nodeQueryDao.executeQuery(basicQuery, userInfo);

		List<Community> communities = Lists.newArrayList();
		UserInfo principal = userManager.getUserInfo(principalId);
		for (String id : nodeResults.getResultIds()) {
			Community community = entityManager.getEntity(userInfo, id, Community.class);
			String teamId = community.getTeamId();
			
			TeamMembershipStatus status = teamManager.getTeamMembershipStatus(userInfo, teamId, principal);
			if (status.getIsMember()) {
				communities.add(community);	
			}
		}
		
		PaginatedResults<Community> page = new PaginatedResults<Community>();
		int start = Math.min(offset, communities.size());
		int end = Math.min(start + limit, communities.size());
		page.setResults(communities.subList(start, end));
		page.setTotalNumberOfResults(communities.size());
		return page;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.community.CommunityManager#getAll(java.lang.String, int, int)
	 */
	@Override
	public PaginatedResults<Community> getAll(UserInfo userInfo, int limit, int offset) throws DatastoreException, NotFoundException {
		// temp code. Will add new table to connect community to teams and visa versa.
		
		BasicQuery basicQuery = createCommunityQuery();
		
		// userGroupDao. get those two groups, and then remove the
		// DEFAULT_GROUPS.AUTHENTICATED_USERS
		// DEFAULT_GROUPS.PUBLIC;
		//userInfo.getGroups().remove(DEFAULT_GROUPS.AUTHENTICATED_USERS);
		//userInfo.getGroups().remove(DEFAULT_GROUPS.PUBLIC);
		
		NodeQueryResults nodeResults = nodeQueryDao.executeQuery(basicQuery, userInfo);
		
		List<Community> communities = Lists.newArrayList();
		for (String id : nodeResults.getResultIds()) {
			Community community = entityManager.getEntity(userInfo, id, Community.class);
			communities.add(community);
		}
		
		PaginatedResults<Community> page = new PaginatedResults<Community>();
		int start = Math.min(offset, communities.size());
		int end = Math.min(start + limit, communities.size());
		page.setResults(communities.subList(start, end));
		page.setTotalNumberOfResults(communities.size());
		return page;
	}
	
	private BasicQuery createCommunityQuery() {
		EntityType type =  EntityType.getNodeTypeForClass(Community.class);
		BasicQuery basicQuery = new BasicQuery();
		basicQuery.setSelect(new ArrayList<String>());
		basicQuery.getSelect().add(NodeField.ID.getFieldName());
		basicQuery.setFrom(type.name());
		basicQuery.setLimit(100);
		basicQuery.setOffset(0);
		basicQuery.setAscending(true);
		basicQuery.setSort("name");
		return basicQuery;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.community.CommunityManager#get(java.lang.String)
	 */
	@Override
	public Community get(UserInfo userInfo, String communityId) throws DatastoreException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, communityId, ObjectType.ENTITY, ACCESS_TYPE.READ)) {
			throw new UnauthorizedException("Cannot read Community.");
		}
		Community community = entityManager.getEntity(userInfo, communityId, Community.class);
		return community;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.community.CommunityManager#put(org.sagebionetworks.repo.model.UserInfo,
	 * org.sagebionetworks.repo.model.Community)
	 */
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Community update(UserInfo userInfo, Community community) throws InvalidModelException, DatastoreException, UnauthorizedException,
			NotFoundException {
		if (!authorizationManager.canAccess(userInfo, community.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE)) {
			throw new UnauthorizedException("Cannot update Community.");
		}
		validateForUpdate(community);
		populateUpdateFields(userInfo, community);
		entityManager.updateEntity(userInfo, community, false, null);
		community = entityManager.getEntity(userInfo, community.getId(), Community.class);
		return community;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.community.CommunityManager#delete(org.sagebionetworks.repo.model.UserInfo,
	 * java.lang.String)
	 */
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void delete(UserInfo userInfo, String communityId) throws DatastoreException, UnauthorizedException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, communityId, ObjectType.ENTITY, ACCESS_TYPE.DELETE)) {
			throw new UnauthorizedException("Cannot delete Community.");
		}

		Community community = entityManager.getEntity(userInfo, communityId, Community.class);
		teamManager.delete(userInfo, community.getTeamId());
		entityManager.deleteEntity(userInfo, communityId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.community.CommunityManager#join(org.sagebionetworks.repo.model.UserInfo,
	 * java.lang.String)
	 */
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void join(UserInfo userInfo, String communityId) throws DatastoreException, UnauthorizedException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, communityId, ObjectType.ENTITY, ACCESS_TYPE.PARTICIPATE)) {
			throw new UnauthorizedException("Cannot join Community.");
		}
		Community community = entityManager.getEntity(userInfo, communityId, Community.class);
		teamManager.addMember(userInfo, community.getTeamId(), userInfo);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.community.CommunityManager#leave(org.sagebionetworks.repo.model.UserInfo,
	 * java.lang.String)
	 */
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void leave(UserInfo userInfo, String communityId) throws DatastoreException, UnauthorizedException, NotFoundException {
		Community community = entityManager.getEntity(userInfo, communityId, Community.class);
		teamManager.removeMember(userInfo, community.getTeamId(), userInfo.getIndividualGroup().getId());
	}
}
