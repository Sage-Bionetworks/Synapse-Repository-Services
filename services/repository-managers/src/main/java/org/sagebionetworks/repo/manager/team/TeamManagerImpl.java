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

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamHeader;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserInfo;
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
	private UserManager userManager;
	@Autowired
	private AccessControlListDAO aclDAO;
	@Autowired
	private FileHandleManager fileHandlerManager;
	@Autowired
	private MembershipInvitationManager membershipInvitationManager;
	@Autowired
	private MembershipRequestManager membershipRequestManager;
	
	public static void validateForCreate(Team team) {
		if (team.getCreatedBy()!=null) throw new InvalidModelException("'createdBy' field is not user specifiable.");
		if (team.getCreatedOn()!=null) throw new InvalidModelException("'createdOn' field is not user specifiable.");
		if(team.getEtag()!=null) throw new InvalidModelException("'etag' field is not user specifiable.");
		if(team.getId()!=null) throw new InvalidModelException("'id' field is not user specifiable.");
		if(team.getModifiedBy()!=null) throw new InvalidModelException("'modifiedBy' field is not user specifiable.");
		if(team.getModifiedOn()!=null) throw new InvalidModelException("'modifiedOn' field is not user specifiable.");
		if(team.getName()==null) throw new InvalidModelException("'name' field is required.");
	}
	
	public static void validateForUpdate(Team team) {
		if(team.getEtag()==null) throw new InvalidModelException("'etag' field is missing.");
		if(team.getId()==null) throw new InvalidModelException("'id' field is missing.");
		if(team.getName()==null) throw new InvalidModelException("'name' field is required.");
	}
	
	public static void populateCreationFields(UserInfo userInfo, Team team, Date now) {
		team.setCreatedBy(userInfo.getIndividualGroup().getId());
		team.setCreatedOn(now);
		team.setModifiedBy(userInfo.getIndividualGroup().getId());
		team.setModifiedOn(now);
	}

	public static void populateUpdateFields(UserInfo userInfo, Team team, Date now) {
		team.setCreatedBy(null); // by setting to null we are telling the DAO to use the current values
		team.setCreatedOn(null);
		team.setModifiedBy(userInfo.getIndividualGroup().getId());
		team.setModifiedOn(now);
	}
	
	private static final ACCESS_TYPE[] ADMIN_TEAM_PERMISSIONS = new ACCESS_TYPE[]{
		ACCESS_TYPE.READ, 
		ACCESS_TYPE.UPDATE, 
		ACCESS_TYPE.DELETE, 
		ACCESS_TYPE.MEMBERSHIP, 
		ACCESS_TYPE.SEND_MESSAGE};

	private static final ACCESS_TYPE[] NON_ADMIN_TEAM_PERMISSIONS = new ACCESS_TYPE[]{
		ACCESS_TYPE.READ, ACCESS_TYPE.SEND_MESSAGE};

	private static AccessControlList createAdminAcl(
			final UserInfo creator, 
			final String teamId, 
			final Date creationDate) {
		Set<ACCESS_TYPE> accessSet = new HashSet<ACCESS_TYPE>(Arrays.asList(ADMIN_TEAM_PERMISSIONS));

		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(accessSet);
		String userId = creator.getIndividualGroup().getId();
		ra.setPrincipalId(Long.parseLong(userId));

		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		raSet.add(ra);

		AccessControlList acl = new AccessControlList();
		acl.setId(teamId);
		acl.setCreationDate(creationDate);
		acl.setResourceAccess(raSet);

		return acl;
	}
	
	public static void addToACL(AccessControlList acl, String principalId, ACCESS_TYPE[] accessTypes) {
		ResourceAccess ra = new ResourceAccess();
		Set<ACCESS_TYPE> accessSet = new HashSet<ACCESS_TYPE>(Arrays.asList(accessTypes));
		ra.setAccessType(accessSet);
		ra.setPrincipalId(Long.parseLong(principalId));
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
		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(userInfo.getIndividualGroup().getId()))
				throw new UnauthorizedException("Anonymous user cannot create Team.");
		validateForCreate(team);
		// create UserGroup (fail if UG with the given name already exists)
		String id = userManager.createPrincipal(team.getName(), /*isIndividual*/false);
		team.setId(id);
		Date now = new Date();
		populateCreationFields(userInfo, team, now);
		Team created = teamDAO.create(team);
		// create ACL, adding the current user to the team, as an admin
		AccessControlList acl = createAdminAcl(userInfo, id, now);
		aclDAO.create(acl);
		return created;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.TeamManager#get(long, long)
	 */
	@Override
	public QueryResults<Team> get(long offset, long limit)
			throws DatastoreException {
		List<Team> results = teamDAO.getInRange(offset, limit);
		long count = teamDAO.getCount();
		QueryResults<Team> queryResults = new QueryResults<Team>();
		queryResults.setResults(results);
		queryResults.setTotalNumberOfResults(count);
		return queryResults;
	}


	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.TeamManager#getByMember(java.lang.String, long, long)
	 */
	@Override
	public QueryResults<Team> getByMember(String principalId, long offset,
			long limit) throws DatastoreException {
		List<Team> results = teamDAO.getForMemberInRange(principalId, offset, limit);
		long count = teamDAO.getCountForMember(principalId);
		QueryResults<Team> queryResults = new QueryResults<Team>();
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
	public Team put(UserInfo userInfo, Team team) throws InvalidModelException,
			DatastoreException, UnauthorizedException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, team.getId(), ObjectType.TEAM, ACCESS_TYPE.UPDATE)) throw new UnauthorizedException("Cannot update Team.");
		validateForUpdate(team);
		populateUpdateFields(userInfo, team, new Date());
		return teamDAO.update(team);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.TeamManager#delete(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void delete(UserInfo userInfo, String id) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, id, ObjectType.TEAM, ACCESS_TYPE.DELETE)) throw new UnauthorizedException("Cannot delete Team.");
		// delete ACL
		aclDAO.delete(id);
		// delete Team
		teamDAO.delete(id);
		// delete userGroup
		userGroupDAO.delete(id);
	}
	
	/**
	 * Either:
		principalId is self and membership invitation has been extended, or
    	principalId is self and have MEMBERSHIP permission on Team, or
    	have MEMBERSHIP permission on Team and membership request has been created for principalId
	 * @param userInfo
	 * @param teamId the ID of the team
	 * @param principalId the ID of the one to be added to the team
	 * @return
	 */
	public boolean canAddTeamMember(UserInfo userInfo, String teamId, String principalId) throws NotFoundException {
		if (userInfo.isAdmin()) return true;
		boolean principalIsSelf = userInfo.getIndividualGroup().getId().equals(principalId);
		boolean amTeamAdmin = authorizationManager.canAccess(userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.MEMBERSHIP);
		if (principalIsSelf) {
			// trying to add myself to Team.  
			if (amTeamAdmin) return true;
			// if I'm not a team admin, then I need to have an invitation
			QueryResults<MembershipInvitation> openInvitations = membershipInvitationManager.getOpenForUserInRange(principalId,0,1);
			return openInvitations.getTotalNumberOfResults()>0L;
		} else {
			// the member to be added is someone other than me
			if (!amTeamAdmin) return false; // can't add somone unless I'm a Team administrator
			// can't add someone unless they are asking to be added
			QueryResults<MembershipRequest> openRequests = membershipRequestManager.getOpenByTeamAndRequestorInRange(principalId, teamId, 0, 1);
			return openRequests.getTotalNumberOfResults()>0L;
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
	public void addMember(UserInfo userInfo, String teamId, String principalId)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		if (!canAddTeamMember(userInfo, teamId, principalId)) throw new UnauthorizedException("Cannot add member to Team.");
		// check that user is not already in Team
		if (userGroupsHasPrincipalId(groupMembersDAO.getMembers(teamId), principalId))
			throw new IllegalArgumentException("Member is already in Team.");
		groupMembersDAO.addMembers(teamId, Arrays.asList(new String[]{principalId}));
		// also add member to ACL
		AccessControlList acl = aclDAO.get(teamId, ObjectType.TEAM);
		addToACL(acl, principalId,NON_ADMIN_TEAM_PERMISSIONS);
		aclDAO.update(acl);
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
		boolean principalIsSelf = userInfo.getIndividualGroup().getId().equals(principalId);
		if (principalIsSelf) return true;
		boolean amTeamAdmin = authorizationManager.canAccess(userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.MEMBERSHIP);
		if (amTeamAdmin) return true;
		return true;
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
		// TODO check that member is actually in Team
		groupMembersDAO.removeMembers(teamId, Arrays.asList(new String[]{principalId}));
		// TODO remove from ACL
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.TeamManager#getACL(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@Override
	public AccessControlList getACL(UserInfo userInfo, String teamId)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.READ)) throw new UnauthorizedException("Cannot read Team ACL.");
		AccessControlList acl = aclDAO.get(teamId, ObjectType.TEAM);
		return acl;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.TeamManager#updateACL(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.repo.model.AccessControlList)
	 */
	@Override
	public void updateACL(UserInfo userInfo, AccessControlList acl)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, acl.getId(), ObjectType.TEAM, ACCESS_TYPE.UPDATE)) throw new UnauthorizedException("Cannot read Team ACL.");
		aclDAO.update(acl);
	}

	@Override
	public URL getIconURL(String teamId) throws NotFoundException {
		Team team = teamDAO.get(teamId);
		String handleId = team.getIcon();
		return fileHandlerManager.getRedirectURLForFileHandle(handleId);
	}

	@Override
	public Map<TeamHeader, List<UserGroupHeader>> getAllTeamsAndMembers()
			throws DatastoreException {
		return teamDAO.getAllTeamsAndMembers();
	}

}
