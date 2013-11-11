package org.sagebionetworks.bridge.manager.community;

import java.net.URL;
import java.util.*;

import org.sagebionetworks.bridge.manager.Validate;
import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.repo.manager.*;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.*;
import org.sagebionetworks.repo.model.dbo.dao.AuthorizationUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
	private AccessRequirementDAO accessRequirementDAO;
	@Autowired
	private EntityManager entityManager;

	public CommunityManagerImpl() {
	}

	// for testing
	CommunityManagerImpl(AuthorizationManager authorizationManager, GroupMembersDAO groupMembersDAO, UserGroupDAO userGroupDAO,
			AccessControlListDAO aclDAO, UserManager userManager, AccessRequirementDAO accessRequirementDAO, EntityManager entityManager) {
		this.authorizationManager = authorizationManager;
		this.groupMembersDAO = groupMembersDAO;

		this.userGroupDAO = userGroupDAO;
		this.aclDAO = aclDAO;
		this.userManager = userManager;
		this.accessRequirementDAO = accessRequirementDAO;
		this.entityManager = entityManager;
	}

	public static void validateForCreate(Community community) {
		Validate.notSpecifiable(community.getCreatedBy(), "createdBy");
		Validate.notSpecifiable(community.getCreatedOn(), "createdOn");
		Validate.notSpecifiable(community.getEtag(), "etag");
		Validate.notSpecifiable(community.getId(), "id");
		Validate.notSpecifiable(community.getModifiedBy(), "modifiedBy");
		Validate.notSpecifiable(community.getModifiedOn(), "modifiedOn");
		Validate.notSpecifiable(community.getGroupId(), "groupId");

		Validate.required(community.getName(), "name");
		Validate.required(community.getDescription(), "description");
	}

	public static void validateForUpdate(Community community) {
		Validate.missing(community.getEtag(), "etag");
		Validate.missing(community.getId(), "id");
		Validate.missing(community.getGroupId(), "groupId");

		Validate.required(community.getName(), "name");
		Validate.required(community.getDescription(), "description");
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

	public static final ACCESS_TYPE[] ADMIN_COMMUNITY_PERMISSIONS = new ACCESS_TYPE[] { ACCESS_TYPE.READ, ACCESS_TYPE.UPDATE,
			ACCESS_TYPE.DELETE, ACCESS_TYPE.SEND_MESSAGE };

	private static final ACCESS_TYPE[] NON_ADMIN_COMMUNITY_PERMISSIONS = new ACCESS_TYPE[] { ACCESS_TYPE.READ, ACCESS_TYPE.SEND_MESSAGE };

	public static ResourceAccess createResourceAccess(long principalId, ACCESS_TYPE[] accessTypes) {
		Set<ACCESS_TYPE> accessSet = new HashSet<ACCESS_TYPE>(Arrays.asList(accessTypes));
		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(accessSet);
		ra.setPrincipalId(principalId);
		return ra;
	}

	public static AccessControlList createInitialAcl(final UserInfo creator, final String communityId) {
		Date now = new Date();

		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		ResourceAccess adminRa = createResourceAccess(Long.parseLong(creator.getIndividualGroup().getId()), ADMIN_COMMUNITY_PERMISSIONS);
		raSet.add(adminRa);

		ResourceAccess communityRa = createResourceAccess(Long.parseLong(communityId), NON_ADMIN_COMMUNITY_PERMISSIONS);
		raSet.add(communityRa);

		AccessControlList acl = new AccessControlList();
		acl.setId(communityId);
		acl.setCreatedBy(creator.getIndividualGroup().getId());
		acl.setCreationDate(now);
		acl.setModifiedBy(creator.getIndividualGroup().getId());
		acl.setModifiedOn(now);
		acl.setResourceAccess(raSet);

		return acl;
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
			NotFoundException {
		if (AuthorizationUtils.isUserAnonymous(userInfo)) {
			throw new UnauthorizedException("Anonymous user cannot create Community.");
		}
		validateForCreate(community);

		// create UserGroup for community (fail if UG with the given name already exists)
		if (userManager.doesPrincipalExist(community.getName())) {
			throw new InvalidModelException("Name " + community.getName() + " is already used.");
		}

		UserGroup ug = new UserGroup();
		ug.setName(community.getName());
		ug.setIsIndividual(false);
		String groupId = userGroupDAO.create(ug);

		community.setGroupId(groupId);
		populateCreationFields(userInfo, community);

		String communityId = entityManager.createEntity(userInfo, community, null);
		Community created = entityManager.getEntity(userInfo, communityId, Community.class);

		groupMembersDAO.addMembers(communityId, Collections.singletonList(userInfo.getIndividualGroup().getId()));
		// create ACL, adding the current user to the community, as an admin
		AccessControlList acl = createInitialAcl(userInfo, communityId);
		aclDAO.create(acl);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.community.CommunityManager#getByMember(java.lang.String, long, long)
	 */
	// @Override
	// public PaginatedResults<Community> getByMember(String principalId, long limit, long offset) throws
	// DatastoreException {
	// List<Community> results = communityDAO.getForMemberInRange(principalId, limit, offset);
	// long count = communityDAO.getCountForMember(principalId);
	// PaginatedResults<Community> queryResults = new PaginatedResults<Community>();
	// queryResults.setResults(results);
	// queryResults.setTotalNumberOfResults(count);
	// return queryResults;
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.community.CommunityManager#get(java.lang.String)
	 */
	@Override
	public Community get(UserInfo userInfo, String communityId) throws DatastoreException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, communityId, ObjectType.COMMUNITY, ACCESS_TYPE.READ)) {
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
	public Community put(UserInfo userInfo, Community community) throws InvalidModelException, DatastoreException, UnauthorizedException,
			NotFoundException {
		if (!authorizationManager.canAccess(userInfo, community.getId(), ObjectType.COMMUNITY, ACCESS_TYPE.UPDATE)) {
			throw new UnauthorizedException("Cannot update Community.");
		}
		validateForUpdate(community);
		populateUpdateFields(userInfo, community);
		entityManager.updateEntity(userInfo, community, true, null);
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
	public void delete(UserInfo userInfo, Community community) throws DatastoreException, UnauthorizedException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, community.getId(), ObjectType.COMMUNITY, ACCESS_TYPE.DELETE)) {
			throw new UnauthorizedException("Cannot delete Community.");
		}
		// delete userGroup
		userGroupDAO.delete(community.getGroupId());
		entityManager.deleteEntity(userInfo, community.getId());
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
	// /*
	// * (non-Javadoc)
	// *
	// * @see
	// * org.sagebionetworks.repo.manager.community.CommunityManager#addMember(org.sagebionetworks.repo.model.UserInfo,
	// * java.lang.String, java.lang.String)
	// */
	// @Override
	// @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	// public void addMember(UserInfo userInfo, String communityId, UserInfo principalUserInfo) throws
	// DatastoreException,
	// UnauthorizedException, NotFoundException {
	// String principalId = principalUserInfo.getIndividualGroup().getId();
	// if (!canAddCommunityMember(userInfo, communityId, principalUserInfo)) {
	// throw new UnauthorizedException("Cannot add member to Community.");
	// }
	// // check that user is not already in Community
	// if (!userGroupsHasPrincipalId(groupMembersDAO.getMembers(communityId), principalId))
	// groupMembersDAO.addMembers(communityId, Arrays.asList(new String[] { principalId }));
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
