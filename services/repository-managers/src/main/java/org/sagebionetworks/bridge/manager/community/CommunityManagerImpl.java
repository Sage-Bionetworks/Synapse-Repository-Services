package org.sagebionetworks.bridge.manager.community;

import java.util.*;

import org.sagebionetworks.bridge.manager.Validate;
import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.repo.manager.*;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.*;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.model.dbo.dao.AuthorizationUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class CommunityManagerImpl implements CommunityManager {
	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private AccessControlListDAO aclDAO;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private MembershipInvtnSubmissionDAO membershipInvtnSubmissionDAO;
	@Autowired
	private MembershipRqstSubmissionDAO membershipRqstSubmissionDAO;
	@Autowired
	private UserManager userManager;
	@Autowired
	private TeamManager teamManager;
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private EntityPermissionsManager entityPermissionsManager;

	public CommunityManagerImpl() {
	}

	// for testing
	CommunityManagerImpl(AuthorizationManager authorizationManager, GroupMembersDAO groupMembersDAO, UserGroupDAO userGroupDAO,
			AccessControlListDAO aclDAO, UserManager userManager, TeamManager teamManager, AccessRequirementDAO accessRequirementDAO,
			EntityManager entityManager, EntityPermissionsManager entityPermissionsManager) {
		this.authorizationManager = authorizationManager;
		this.groupMembersDAO = groupMembersDAO;

		this.userGroupDAO = userGroupDAO;
		this.aclDAO = aclDAO;
		this.userManager = userManager;
		this.teamManager = teamManager;
		this.accessRequirementDAO = accessRequirementDAO;
		this.entityManager = entityManager;
		this.entityPermissionsManager = entityPermissionsManager;
	}

	public static void validateForCreate(Community community) {
		Validate.notSpecifiable(community.getCreatedBy(), "createdBy");
		Validate.notSpecifiable(community.getCreatedOn(), "createdOn");
		Validate.notSpecifiable(community.getEtag(), "etag");
		Validate.notSpecifiable(community.getId(), "id");
		Validate.notSpecifiable(community.getModifiedBy(), "modifiedBy");
		Validate.notSpecifiable(community.getModifiedOn(), "modifiedOn");
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
	private static final ACCESS_TYPE[] COMMUNITY_ADMIN_PERMISSIONS = { ACCESS_TYPE.READ, ACCESS_TYPE.UPDATE,
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
			NotFoundException, NameConflictException, ACLInheritanceException {
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
		Community created = entityManager.getEntity(userInfo, communityId, Community.class);

		// adding the current user to the community as an admin, and the team as a non-admin
		AccessControlList acl = entityPermissionsManager.getACL(communityId, userInfo);
		UserGroup authenticatedUsers = userManager.getDefaultUserGroup(DEFAULT_GROUPS.AUTHENTICATED_USERS);
		UserGroup allUsers = userManager.getDefaultUserGroup(DEFAULT_GROUPS.PUBLIC);
		Set<ResourceAccess> raSet = Sets.newHashSet(
				createResourceAccess(Long.parseLong(allUsers.getId()), ANONYMOUS_PERMISSIONS),
				createResourceAccess(Long.parseLong(authenticatedUsers.getId()), SIGNEDIN_PERMISSIONS),
				createResourceAccess(Long.parseLong(team.getId()), COMMUNITY_MEMBER_PERMISSIONS),
				createResourceAccess(Long.parseLong(userInfo.getIndividualGroup().getId()), COMMUNITY_ADMIN_PERMISSIONS));
		acl.setResourceAccess(raSet);
		entityPermissionsManager.updateACL(acl, userInfo);

		return created;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.community.CommunityManager#get(long, long)
	 */
	// @Override
	// public PaginatedResults<Community> get(long limit, long offset) throws DatastoreException {
	// List<Community> results = communityDAO.getInRange(limit, offset);
	// long count = communityDAO.getCount();
	// PaginatedResults<Community> queryResults = new PaginatedResults<Community>();
	// queryResults.setResults(results);
	// queryResults.setTotalNumberOfResults(count);
	// return queryResults;
	// }

	/**
	 * 
	 */
	// @Override
	// public PaginatedResults<CommunityMember> getMembers(String communityId, long limit, long offset) throws
	// DatastoreException {
	// List<CommunityMember> results = communityDAO.getMembersInRange(communityId, limit, offset);
	// long count = communityDAO.getMembersCount(communityId);
	// PaginatedResults<CommunityMember> queryResults = new PaginatedResults<CommunityMember>();
	// queryResults.setResults(results);
	// queryResults.setTotalNumberOfResults(count);
	// return queryResults;
	// }

	// @Override
	// public PaginatedResults<Community> getByMember(String principalId, long limit, long offset) throws
	// DatastoreException {
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.community.CommunityManager#getByMember(java.lang.String, int, int)
	 */
	@Override
	public PaginatedResults<Community> getByMember(UserInfo userInfo, String principalId, int limit, int offset) throws DatastoreException,
			NotFoundException {
		// temp code. Will add new table to connect community to teams and visa versa.
		List<Community> communities = Lists.newArrayList();

		for (Team team : teamManager.getByMember(principalId, 0, Long.MAX_VALUE).getResults()) {
			try {
				Community community = entityManager.getEntity(userInfo, team.getName(), Community.class);
				communities.add(community);
			} catch (NotFoundException e) {
			}
		}

		PaginatedResults<Community> queryResults = new PaginatedResults<Community>();
		int start = Math.min(offset, communities.size());
		int end = Math.min(start + limit, communities.size());
		queryResults.setResults(communities.subList(start, end));
		queryResults.setTotalNumberOfResults(communities.size());
		return queryResults;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.community.CommunityManager#getAll(java.lang.String, int, int)
	 */
	@Override
	public PaginatedResults<Community> getAll(UserInfo userInfo, int limit, int offset) throws DatastoreException, NotFoundException {
		// temp code. Will add new table to connect community to teams and visa versa.
		List<Community> communities = Lists.newArrayList();

		for (Team team : teamManager.getAllTeamsAndMembers().keySet()) {
			try {
				Community community = entityManager.getEntity(userInfo, team.getName(), Community.class);
				communities.add(community);
			} catch (NotFoundException e) {
			}
		}

		PaginatedResults<Community> queryResults = new PaginatedResults<Community>();
		int start = Math.min(offset, communities.size());
		int end = Math.min(start + limit, communities.size());
		queryResults.setResults(communities.subList(start, end));
		queryResults.setTotalNumberOfResults(communities.size());
		return queryResults;
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
	public void join(UserInfo userInfo, String communityId) throws DatastoreException,
			UnauthorizedException, NotFoundException {
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

	/**
	 * Either: principalId is self and membership invitation has been extended (and not yet accepted), or principalId is
	 * self and have MEMBERSHIP permission on Community, or have MEMBERSHIP permission on Community and membership
	 * request has been created (but not yet accepted) for principalId
	 * 
	 * @param userInfo
	 * @param communityId the ID of the community
	 * @param principalId the ID of the one to be added to the community
	 * @return
	 */
	// public boolean canAddCommunityMember(UserInfo userInfo, String communityId, UserInfo principalUserInfo) throws
	// NotFoundException {
	// if (userInfo.isAdmin())
	// return true;
	// if (hasUnmetAccessRequirements(principalUserInfo, communityId))
	// return false;
	// String principalId = principalUserInfo.getIndividualGroup().getId();
	// boolean principalIsSelf = userInfo.getIndividualGroup().getId().equals(principalId);
	// boolean amCommunityAdmin = authorizationManager.canAccess(userInfo, communityId, ObjectType.COMMUNITY,
	// ACCESS_TYPE.COMMUNITY_MEMBERSHIP_UPDATE);
	// long now = System.currentTimeMillis();
	// if (principalIsSelf) {
	// // trying to add myself to Community.
	// if (amCommunityAdmin)
	// return true;
	// // if the community is open, I can join
	// Community community = communityDAO.get(communityId);
	// if (community.getCanPublicJoin() != null && community.getCanPublicJoin() == true)
	// return true;
	// // if I'm not a community admin and the community is not open, then I need to have an open invitation
	// long openInvitationCount =
	// membershipInvtnSubmissionDAO.getOpenByCommunityAndUserCount(Long.parseLong(communityId),
	// Long.parseLong(principalId), now);
	// return openInvitationCount > 0L;
	// } else {
	// // the member to be added is someone other than me
	// if (!amCommunityAdmin)
	// return false; // can't add somone unless I'm a Community administrator
	// // can't add someone unless they are asking to be added
	// long openRequestCount =
	// membershipRqstSubmissionDAO.getOpenByCommunityAndRequestorCount(Long.parseLong(communityId),
	// Long.parseLong(principalId), now);
	// return openRequestCount > 0L;
	// }
	// }
	//
	// public static boolean userGroupsHasPrincipalId(Collection<UserGroup> userGroups, String principalId) {
	// for (UserGroup ug : userGroups)
	// if (ug.getId().equals(principalId))
	// return true;
	// return false;
	// }
	//
	// /**
	// * MEMBERSHIP permission on group OR user issuing request is the one being removed.
	// *
	// * @param userInfo
	// * @param communityId
	// * @param principalId
	// * @return
	// */
	// public boolean canRemoveCommunityMember(UserInfo userInfo, String communityId, String principalId) throws
	// NotFoundException {
	// if (userInfo.isAdmin())
	// return true;
	// boolean principalIsSelf = userInfo.getIndividualGroup().getId().equals(principalId);
	// if (principalIsSelf)
	// return true;
	// boolean amCommunityAdmin = authorizationManager.canAccess(userInfo, communityId, ObjectType.COMMUNITY,
	// ACCESS_TYPE.COMMUNITY_MEMBERSHIP_UPDATE);
	// if (amCommunityAdmin)
	// return true;
	// return false;
	// }
	//
	// /*
	// * (non-Javadoc)
	// *
	// * @see
	// *
	// org.sagebionetworks.repo.manager.community.CommunityManager#removeMember(org.sagebionetworks.repo.model.UserInfo,
	// * java.lang.String, java.lang.String)
	// */
	// @Override
	// @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	// public void removeMember(UserInfo userInfo, String communityId, String principalId) throws DatastoreException,
	// UnauthorizedException,
	// NotFoundException {
	// if (!canRemoveCommunityMember(userInfo, communityId, principalId)) {
	// throw new UnauthorizedException("Cannot remove member from Community.");
	// }
	// // check that member is actually in Community
	// if (userGroupsHasPrincipalId(groupMembersDAO.getMembers(communityId), principalId)) {
	// groupMembersDAO.removeMembers(communityId, Arrays.asList(new String[] { principalId }));
	// // remove from ACL
	// AccessControlList acl = aclDAO.get(communityId, ObjectType.COMMUNITY);
	// removeFromACL(acl, principalId);
	// aclDAO.update(acl);
	// }
	// }
	//
	// /*
	// * (non-Javadoc)
	// *
	// * @see
	// org.sagebionetworks.repo.manager.community.CommunityManager#getACL(org.sagebionetworks.repo.model.UserInfo,
	// * java.lang.String)
	// */
	// @Override
	// public AccessControlList getACL(UserInfo userInfo, String communityId) throws DatastoreException,
	// UnauthorizedException,
	// NotFoundException {
	// if (!authorizationManager.canAccess(userInfo, communityId, ObjectType.COMMUNITY, ACCESS_TYPE.READ)) {
	// throw new UnauthorizedException("Cannot read Community ACL.");
	// }
	// return aclDAO.get(communityId, ObjectType.COMMUNITY);
	// }
	//
	// /*
	// * (non-Javadoc)
	// *
	// * @see
	// * org.sagebionetworks.repo.manager.community.CommunityManager#updateACL(org.sagebionetworks.repo.model.UserInfo,
	// * org.sagebionetworks.repo.model.AccessControlList)
	// */
	// @Override
	// public void updateACL(UserInfo userInfo, AccessControlList acl) throws DatastoreException, UnauthorizedException,
	// NotFoundException {
	// if (!authorizationManager.canAccess(userInfo, acl.getId(), ObjectType.COMMUNITY, ACCESS_TYPE.UPDATE)) {
	// throw new UnauthorizedException("Cannot change Community permissions.");
	// }
	// aclDAO.update(acl);
	// }
	//
	// @Override
	// public URL getIconURL(String communityId) throws NotFoundException {
	// Community community = communityDAO.get(communityId);
	// String handleId = community.getIcon();
	// if (handleId == null) {
	// throw new NotFoundException("Community " + communityId + " has no icon file handle.");
	// }
	// return fileHandleManager.getRedirectURLForFileHandle(handleId);
	// }
	//
	// @Override
	// public Map<Community, Collection<CommunityMember>> getAllCommunitysAndMembers() throws DatastoreException {
	// return communityDAO.getAllCommunitysAndMembers();
	// }
	//
	// @Override
	// @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	// public void setPermissions(UserInfo userInfo, String communityId, String principalId, boolean isAdmin) throws
	// DatastoreException,
	// UnauthorizedException, NotFoundException {
	// if (!authorizationManager.canAccess(userInfo, communityId, ObjectType.COMMUNITY, ACCESS_TYPE.UPDATE)) {
	// throw new UnauthorizedException("Cannot change Community permissions.");
	// }
	// AccessControlList acl = aclDAO.get(communityId, ObjectType.COMMUNITY);
	// // first, remove the principal's entries from the ACL
	// removeFromACL(acl, principalId);
	// // now, if isAdmin is false, the community membership is enough to give the user basic permissions
	// if (isAdmin) {
	// // if isAdmin is true, then we add the specified admin permissions
	// addToACL(acl, principalId, ADMIN_COMMUNITY_PERMISSIONS);
	// }
	// // finally, update the ACL
	// aclDAO.update(acl);
	// }
	//
	// // answers the question about whether membership approval is required to add principal to community
	// // the logic is !userIsSynapseAdmin && !userIsCommunityAdmin && !publicCanJoinCommunity
	// public boolean isMembershipApprovalRequired(UserInfo principalUserInfo, String communityId) throws
	// DatastoreException, NotFoundException {
	// boolean userIsSynapseAdmin = principalUserInfo.isAdmin();
	// if (userIsSynapseAdmin)
	// return false;
	// boolean userIsCommunityAdmin = authorizationManager.canAccess(principalUserInfo, communityId,
	// ObjectType.COMMUNITY,
	// ACCESS_TYPE.COMMUNITY_MEMBERSHIP_UPDATE);
	// if (userIsCommunityAdmin)
	// return false;
	// Community community = communityDAO.get(communityId);
	// boolean publicCanJoinCommunity = community.getCanPublicJoin() != null && community.getCanPublicJoin() == true;
	// return !publicCanJoinCommunity;
	// }
	//
	// @Override
	// public CommunityMembershipStatus getCommunityMembershipStatus(UserInfo userInfo, String communityId, UserInfo
	// principalUserInfo)
	// throws DatastoreException, NotFoundException {
	// CommunityMembershipStatus tms = new CommunityMembershipStatus();
	// tms.setCommunityId(communityId);
	// String principalId = principalUserInfo.getIndividualGroup().getId();
	// tms.setUserId(principalId);
	// tms.setIsMember(userGroupsHasPrincipalId(groupMembersDAO.getMembers(communityId), principalId));
	// long now = System.currentTimeMillis();
	// long openInvitationCount =
	// membershipInvtnSubmissionDAO.getOpenByCommunityAndUserCount(Long.parseLong(communityId),
	// Long.parseLong(principalId), now);
	// tms.setHasOpenInvitation(openInvitationCount > 0L);
	// long openRequestCount =
	// membershipRqstSubmissionDAO.getOpenByCommunityAndRequestorCount(Long.parseLong(communityId),
	// Long.parseLong(principalId), now);
	// tms.setHasOpenRequest(openRequestCount > 0L);
	// tms.setCanJoin(canAddCommunityMember(userInfo, communityId, principalUserInfo));
	// tms.setHasUnmetAccessRequirement(hasUnmetAccessRequirements(principalUserInfo, communityId));
	// tms.setMembershipApprovalRequired(isMembershipApprovalRequired(principalUserInfo, communityId));
	// return tms;
	// }
	//
}
