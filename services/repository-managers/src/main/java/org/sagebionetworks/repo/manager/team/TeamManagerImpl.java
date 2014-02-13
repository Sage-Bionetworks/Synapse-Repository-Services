/**
 * 
 */
package org.sagebionetworks.repo.manager.team;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.manager.util.Validate;
import org.sagebionetworks.repo.manager.AccessRequirementUtil;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipInvtnSubmissionDAO;
import org.sagebionetworks.repo.model.MembershipRqstSubmissionDAO;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.TeamMembershipStatus;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.AuthorizationUtils;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author brucehoff
 *
 *
 */
public class TeamManagerImpl implements TeamManager {
	
	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private TeamDAO teamDAO;
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
	private PrincipalAliasDAO principalAliasDAO;
	
	public TeamManagerImpl() {}
	
	// for testing
	public TeamManagerImpl(
			AuthorizationManager authorizationManager,
			TeamDAO teamDAO,
			GroupMembersDAO groupMembersDAO,
			UserGroupDAO userGroupDAO,
			AccessControlListDAO aclDAO,
			FileHandleManager fileHandlerManager,
			MembershipInvtnSubmissionDAO membershipInvtnSubmissionDAO,
			MembershipRqstSubmissionDAO membershipRqstSubmissionDAO, 
			UserManager userManager,
			AccessRequirementDAO accessRequirementDAO,
			PrincipalAliasDAO principalAliasDAO
			) {
		this.authorizationManager = authorizationManager;
		this.teamDAO = teamDAO;
		this.groupMembersDAO = groupMembersDAO;
	
		this.userGroupDAO = userGroupDAO;
		this.aclDAO = aclDAO;
		this.fileHandleManager = fileHandlerManager;
		this.membershipInvtnSubmissionDAO = membershipInvtnSubmissionDAO;
		this.membershipRqstSubmissionDAO = membershipRqstSubmissionDAO;
		this.userManager = userManager;
		this.accessRequirementDAO = accessRequirementDAO;
		this.principalAliasDAO = principalAliasDAO;
	}
	
	public static void validateForCreate(Team team) {
		Validate.notSpecifiable(team.getCreatedBy(), "createdBy");
		Validate.notSpecifiable(team.getCreatedOn(), "createdOn");
		Validate.notSpecifiable(team.getEtag(), "etag");
		Validate.notSpecifiable(team.getId(), "id");
		Validate.notSpecifiable(team.getModifiedBy(), "modifiedBy");
		Validate.notSpecifiable(team.getModifiedOn(), "modifiedOn");
		Validate.required(team.getName(), "name");
	}
	
	public static void validateForUpdate(Team team) {
		Validate.missing(team.getEtag(), "etag");
		Validate.missing(team.getId(), "id");
		Validate.required(team.getName(), "name");
	}
	
	public static void populateCreationFields(UserInfo userInfo, Team team, Date now) {
		team.setCreatedBy(userInfo.getId().toString());
		team.setCreatedOn(now);
		team.setModifiedBy(userInfo.getId().toString());
		team.setModifiedOn(now);
	}

	public static void populateUpdateFields(UserInfo userInfo, Team team, Date now) {
		team.setCreatedBy(null); // by setting to null we are telling the DAO to use the current values
		team.setCreatedOn(null);
		team.setModifiedBy(userInfo.getId().toString());
		team.setModifiedOn(now);
	}
	
	public static final ACCESS_TYPE[] ADMIN_TEAM_PERMISSIONS = new ACCESS_TYPE[]{
		ACCESS_TYPE.READ, 
		ACCESS_TYPE.UPDATE, 
		ACCESS_TYPE.DELETE, 
		ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE, 
		ACCESS_TYPE.SEND_MESSAGE};

	private static final ACCESS_TYPE[] NON_ADMIN_TEAM_PERMISSIONS = new ACCESS_TYPE[]{
		ACCESS_TYPE.READ, ACCESS_TYPE.SEND_MESSAGE};
	
	public static ResourceAccess createResourceAccess(long principalId, ACCESS_TYPE[] accessTypes) {
		Set<ACCESS_TYPE> accessSet = new HashSet<ACCESS_TYPE>(Arrays.asList(accessTypes));
		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(accessSet);
		ra.setPrincipalId(principalId);
		return ra;
	}

	public static AccessControlList createInitialAcl(
			final UserInfo creator, 
			final String teamId, 
			final Date creationDate) {
		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		ResourceAccess adminRa = createResourceAccess(
				Long.parseLong(creator.getId().toString()),
				ADMIN_TEAM_PERMISSIONS
				);
		raSet.add(adminRa);
		
		ResourceAccess teamRa = createResourceAccess(
				Long.parseLong(teamId),
				NON_ADMIN_TEAM_PERMISSIONS
		);
		raSet.add(teamRa);

		AccessControlList acl = new AccessControlList();
		acl.setId(teamId);
		acl.setCreatedBy(creator.getId().toString());
		acl.setCreationDate(creationDate);
		acl.setModifiedBy(creator.getId().toString());
		acl.setModifiedOn(creationDate);
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
		for (ResourceAccess ra: origRA) {
			if (!principalId.equals(ra.getPrincipalId().toString())) newRA.add(ra);
		}
		acl.setResourceAccess(newRA);
	}

	// returns true iff the the ACL has an entry for a Team admin
	public static boolean aclHasTeamAdmin(AccessControlList acl) {
		Set<ResourceAccess> ras = acl.getResourceAccess();
		for (ResourceAccess ra: ras) {
			if (ra.getAccessType().contains(ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)) return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.TeamManager#create(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.repo.model.Team)
	 * 
	 * Note:  This method must execute within a transaction, since it makes calls to two different DAOs
	 */
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Team create(UserInfo userInfo, Team team) throws DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException {
		if (AuthorizationUtils.isUserAnonymous(userInfo))
				throw new UnauthorizedException("Anonymous user cannot create Team.");
		validateForCreate(team);
		// create UserGroup (fail if UG with the given name already exists)
		UserGroup ug = new UserGroup();
		ug.setIsIndividual(false);
		Long id = userGroupDAO.create(ug);
		// bind the team name to this principal
		bindTeamName(team.getName(), id);
		
		team.setId(id.toString());
		Date now = new Date();
		populateCreationFields(userInfo, team, now);
		Team created = teamDAO.create(team);
		groupMembersDAO.addMembers(id.toString(), Arrays.asList(new String[]{userInfo.getId().toString()}));
		// create ACL, adding the current user to the team, as an admin
		AccessControlList acl = createInitialAcl(userInfo, id.toString(), now);
		aclDAO.create(acl, ObjectType.TEAM);
		return created;
	}
	
	private void bindTeamName(String name, Long teamId){
		// Determine if the email already exists
		PrincipalAlias alias = principalAliasDAO.findPrincipalWithAlias(name);
		if(alias != null && !alias.getPrincipalId().equals(teamId)){
			throw new NameConflictException("Name "+name+" is already used.");
		}
		// Bind the team name
		alias = new PrincipalAlias();
		alias.setAlias(name);
		alias.setIsValidated(true);
		alias.setPrincipalId(teamId);
		alias.setType(AliasType.TEAM_NAME);
		// bind this alias
		try {
			principalAliasDAO.bindAliasToPrincipal(alias);
		} catch (NotFoundException e1) {
			throw new DatastoreException(e1);
		}
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.TeamManager#get(long, long)
	 */
	@Override
	public PaginatedResults<Team> get(long limit, long offset)
			throws DatastoreException {
		List<Team> results = teamDAO.getInRange(limit, offset);
		long count = teamDAO.getCount();
		PaginatedResults<Team> queryResults = new PaginatedResults<Team>();
		queryResults.setResults(results);
		queryResults.setTotalNumberOfResults(count);
		return queryResults;
	}

	

	/**
	 * 
	 */
	@Override
	public PaginatedResults<TeamMember> getMembers(String teamId, long limit,
			long offset) throws DatastoreException {
		List<TeamMember> results = teamDAO.getMembersInRange(teamId, limit, offset);
		long count = teamDAO.getMembersCount(teamId);
		PaginatedResults<TeamMember> queryResults = new PaginatedResults<TeamMember>();
		queryResults.setResults(results);
		queryResults.setTotalNumberOfResults(count);
		return queryResults;
	}
	
	@Override
	public TeamMember getMember(String teamId, String principalId) throws NotFoundException, DatastoreException {
		return teamDAO.getMember(teamId, principalId);
	}


	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.TeamManager#getByMember(java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<Team> getByMember(String principalId,
			long limit, long offset) throws DatastoreException {
		List<Team> results = teamDAO.getForMemberInRange(principalId, limit, offset);
		long count = teamDAO.getCountForMember(principalId);
		PaginatedResults<Team> queryResults = new PaginatedResults<Team>();
		queryResults.setResults(results);
		queryResults.setTotalNumberOfResults(count);
		return queryResults;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.TeamManager#get(java.lang.String)
	 */
	@Override
	public Team get(String id) throws DatastoreException, NotFoundException {
		return teamDAO.get(id);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.TeamManager#put(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.repo.model.Team)
	 */
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Team put(UserInfo userInfo, Team team) throws InvalidModelException,
			DatastoreException, UnauthorizedException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, team.getId(), ObjectType.TEAM, ACCESS_TYPE.UPDATE)) throw new UnauthorizedException("Cannot update Team.");
		validateForUpdate(team);
		populateUpdateFields(userInfo, team, new Date());
		// bind the team name to this principal
		bindTeamName(team.getName(), Long.parseLong(team.getId()));
		return teamDAO.update(team);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.TeamManager#delete(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void delete(UserInfo userInfo, String id) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		try {
			teamDAO.get(id);
		} catch (NotFoundException e) {
			return;
		}
		if (!authorizationManager.canAccess(userInfo, id, ObjectType.TEAM, ACCESS_TYPE.DELETE)) throw new UnauthorizedException("Cannot delete Team.");
		// delete ACL
		aclDAO.delete(id, ObjectType.TEAM);
		// delete Team
		teamDAO.delete(id);
		// delete userGroup
		userGroupDAO.delete(id);
	}
	
	
	private boolean hasUnmetAccessRequirements(UserInfo memberUserInfo, String teamId) throws NotFoundException {
		List<Long> unmetRequirements = AccessRequirementUtil.unmetAccessRequirementIdsForTeam(
				memberUserInfo, teamId, accessRequirementDAO);
		return !unmetRequirements.isEmpty();

	}
	/**
	 * Either:
		principalId is self and membership invitation has been extended (and not yet accepted), or
    	principalId is self and have MEMBERSHIP permission on Team, or
    	have MEMBERSHIP permission on Team and membership request has been created (but not yet accepted) for principalId
	 * @param userInfo
	 * @param teamId the ID of the team
	 * @param principalId the ID of the one to be added to the team
	 * @return
	 */
	public boolean canAddTeamMember(UserInfo userInfo, String teamId, UserInfo principalUserInfo) throws NotFoundException {
		if (userInfo.isAdmin()) return true;
		if (hasUnmetAccessRequirements(principalUserInfo, teamId)) return false;
		String principalId = principalUserInfo.getId().toString();
		boolean principalIsSelf = userInfo.getId().toString().equals(principalId);
		boolean amTeamAdmin = authorizationManager.canAccess(userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE);
		long now = System.currentTimeMillis();
		if (principalIsSelf) {
			// trying to add myself to Team.  
			if (amTeamAdmin) return true;
			// if the team is open, I can join
			Team team = teamDAO.get(teamId);
			if (team.getCanPublicJoin()!=null && team.getCanPublicJoin()==true) return true;
			// if I'm not a team admin and the team is not open, then I need to have an open invitation
			long openInvitationCount = membershipInvtnSubmissionDAO.getOpenByTeamAndUserCount(Long.parseLong(teamId), Long.parseLong(principalId), now);
			return openInvitationCount>0L;
		} else {
			// the member to be added is someone other than me
			if (!amTeamAdmin) return false; // can't add somone unless I'm a Team administrator
			// can't add someone unless they are asking to be added
			long openRequestCount = membershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(Long.parseLong(teamId), Long.parseLong(principalId), now);
			return openRequestCount>0L;
		}
	}
	
	public static boolean userGroupsHasPrincipalId(Collection<UserGroup> userGroups, String principalId) {
		for (UserGroup ug : userGroups) if (ug.getId().equals(principalId)) return true;
		return false;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.TeamManager#addMember(org.sagebionetworks.repo.model.UserInfo, java.lang.String, java.lang.String)
	 */
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void addMember(UserInfo userInfo, String teamId, UserInfo principalUserInfo)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		String principalId = principalUserInfo.getId().toString();
		if (!canAddTeamMember(userInfo, teamId, principalUserInfo)) throw new UnauthorizedException("Cannot add member to Team.");
		// check that user is not already in Team
		if (!userGroupsHasPrincipalId(groupMembersDAO.getMembers(teamId), principalId))
			groupMembersDAO.addMembers(teamId, Arrays.asList(new String[]{principalId}));
		// clean up any invitations
		membershipInvtnSubmissionDAO.deleteByTeamAndUser(Long.parseLong(teamId), principalUserInfo.getId());
		// clean up and membership requests
		membershipRqstSubmissionDAO.deleteByTeamAndRequester(Long.parseLong(teamId), principalUserInfo.getId());
		
	}
	
	/**
	 * MEMBERSHIP permission on group OR user issuing request is the one being removed.
	 * @param userInfo
	 * @param teamId
	 * @param principalId
	 * @return
	 */
	public boolean canRemoveTeamMember(UserInfo userInfo, String teamId, String principalId) throws NotFoundException {
		if (userInfo.isAdmin()) return true;
		boolean principalIsSelf = userInfo.getId().toString().equals(principalId);
		if (principalIsSelf) return true;
		boolean amTeamAdmin = authorizationManager.canAccess(userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE);
		if (amTeamAdmin) return true;
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.TeamManager#removeMember(org.sagebionetworks.repo.model.UserInfo, java.lang.String, java.lang.String)
	 */
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeMember(UserInfo userInfo, String teamId,
			String principalId) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		if (!canRemoveTeamMember(userInfo, teamId, principalId)) throw new UnauthorizedException("Cannot remove member from Team.");
		// check that member is actually in Team
		List<UserGroup> currentMembers = groupMembersDAO.getMembers(teamId);
		if (userGroupsHasPrincipalId(currentMembers, principalId)) {
			// remove from ACL
			AccessControlList acl = aclDAO.get(teamId, ObjectType.TEAM);
			removeFromACL(acl, principalId);
			if (!userInfo.isAdmin() && !aclHasTeamAdmin(acl)) throw new InvalidModelException("Team must have at least one administrator.");
			groupMembersDAO.removeMembers(teamId, Arrays.asList(new String[]{principalId}));
			aclDAO.update(acl, ObjectType.TEAM);
		}
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.TeamManager#getACL(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@Override
	public AccessControlList getACL(UserInfo userInfo, String teamId)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.READ)) throw new UnauthorizedException("Cannot read Team ACL.");
		return aclDAO.get(teamId, ObjectType.TEAM);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.TeamManager#updateACL(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.repo.model.AccessControlList)
	 */
	@Override
	public void updateACL(UserInfo userInfo, AccessControlList acl)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, acl.getId(), ObjectType.TEAM, ACCESS_TYPE.UPDATE)) throw new UnauthorizedException("Cannot change Team permissions.");
		aclDAO.update(acl, ObjectType.TEAM);
	}

	@Override
	public URL getIconURL(String teamId) throws NotFoundException {
		Team team = teamDAO.get(teamId);
		String handleId = team.getIcon();
		if (handleId==null) throw new NotFoundException("Team "+teamId+" has no icon file handle.");
		return fileHandleManager.getRedirectURLForFileHandle(handleId);
	}

	@Override
	public Map<Team, Collection<TeamMember>> getAllTeamsAndMembers()
			throws DatastoreException {
		return teamDAO.getAllTeamsAndMembers();
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void setPermissions(UserInfo userInfo, String teamId,
			String principalId, boolean isAdmin) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.UPDATE)) throw new UnauthorizedException("Cannot change Team permissions.");
		AccessControlList acl = aclDAO.get(teamId, ObjectType.TEAM);
		// first, remove the principal's entries from the ACL
		removeFromACL(acl, principalId);
		// now, if isAdmin is false, the team membership is enough to give the user basic permissions
		if (isAdmin) {
			// if isAdmin is true, then we add the specified admin permissions
			addToACL(acl, principalId, ADMIN_TEAM_PERMISSIONS);
		}
		if (!userInfo.isAdmin() && !aclHasTeamAdmin(acl)) throw new InvalidModelException("Team must have at least one administrator.");
		// finally, update the ACL
		aclDAO.update(acl, ObjectType.TEAM);
	}
	
	// answers the question about whether membership approval is required to add principal to team
	// the logic is !userIsSynapseAdmin && !userIsTeamAdmin && !publicCanJoinTeam
	public boolean isMembershipApprovalRequired(UserInfo principalUserInfo, String teamId) throws DatastoreException, NotFoundException {
		boolean userIsSynapseAdmin = principalUserInfo.isAdmin();
		if (userIsSynapseAdmin) return false;
		boolean userIsTeamAdmin = authorizationManager.canAccess(principalUserInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE);
		if (userIsTeamAdmin) return false;
		Team team = teamDAO.get(teamId);
		boolean publicCanJoinTeam = team.getCanPublicJoin()!=null && team.getCanPublicJoin()==true;
		return !publicCanJoinTeam;
	}

	@Override
	public TeamMembershipStatus getTeamMembershipStatus(UserInfo userInfo,
			String teamId, UserInfo principalUserInfo) throws DatastoreException,
			NotFoundException {
		TeamMembershipStatus tms = new TeamMembershipStatus();
		tms.setTeamId(teamId);
		String principalId = principalUserInfo.getId().toString();
		tms.setUserId(principalId);
		tms.setIsMember(userGroupsHasPrincipalId(groupMembersDAO.getMembers(teamId), principalId));
		long now = System.currentTimeMillis();
		long openInvitationCount = membershipInvtnSubmissionDAO.getOpenByTeamAndUserCount(Long.parseLong(teamId), Long.parseLong(principalId), now);
		tms.setHasOpenInvitation(openInvitationCount>0L);
		long openRequestCount = membershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(Long.parseLong(teamId), Long.parseLong(principalId), now);
		tms.setHasOpenRequest(openRequestCount>0L);
		tms.setCanJoin(canAddTeamMember(userInfo, teamId, principalUserInfo));
		tms.setHasUnmetAccessRequirement(hasUnmetAccessRequirements(principalUserInfo, teamId));
		tms.setMembershipApprovalRequired(isMembershipApprovalRequired(principalUserInfo, teamId));
		return tms;
	}
	

}
