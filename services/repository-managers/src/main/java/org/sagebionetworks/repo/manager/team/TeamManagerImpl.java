/**
 * 
 */
package org.sagebionetworks.repo.manager.team;

import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_DISPLAY_NAME;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_TEAM_ID;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_TEAM_NAME;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_TEAM_WEB_LINK;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_USER_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.http.entity.ContentType;
import org.sagebionetworks.manager.util.Validate;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.ProjectStatsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.principal.PrincipalManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.Count;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.MembershipInvitationDAO;
import org.sagebionetworks.repo.model.MembershipRequestDAO;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedTeamIds;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.TeamMembershipStatus;
import org.sagebionetworks.repo.model.TeamSortOrder;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroup;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.BootstrapTeam;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.model.util.ModelConstants;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;


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
	private PrincipalAliasDAO principalAliasDAO;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private MembershipInvitationDAO membershipInvitationDAO;
	@Autowired
	private MembershipRequestDAO membershipRequestDAO;
	@Autowired
	private UserManager userManager;
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;
	@Autowired
	private PrincipalManager principalManager;
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private UserProfileManager userProfileManager;
	@Autowired
	private ProjectStatsManager projectStatsManager;

	private static final String MSG_TEAM_MUST_HAVE_AT_LEAST_ONE_TEAM_MANAGER = "Team must have at least one team manager.";
	private List<BootstrapTeam> teamsToBootstrap;
	public static final String USER_HAS_JOINED_TEAM_TEMPLATE = "message/userHasJoinedTeamTemplate.html";
	public static final String ADMIN_HAS_ADDED_USER_TEMPLATE = "message/teamAdminHasAddedUserTemplate.html";
	private static final String JOIN_TEAM_CONFIRMATION_MESSAGE_SUBJECT = "New Member Has Joined the Team";
	
	public void setTeamsToBootstrap(List<BootstrapTeam> teamsToBootstrap) {
		this.teamsToBootstrap = teamsToBootstrap;
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
	
	public static ResourceAccess createResourceAccess(long principalId, Set<ACCESS_TYPE> accessSet) {
		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(accessSet);
		ra.setPrincipalId(principalId);
		return ra;
	}

	// This is the ACL used by the bootstrap process, when there is no user who is the creator
	public static AccessControlList createBootstrapTeamAcl( 
			final String teamId, 
			final Date creationDate) {
		AccessControlList acl = new AccessControlList();
		acl.setId(teamId);
		acl.setCreationDate(creationDate);
		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		
		ResourceAccess teamRa = createResourceAccess(
				Long.parseLong(teamId),
				ModelConstants.TEAM_MESSENGER_PERMISSIONS);
		raSet.add(teamRa);

		acl.setResourceAccess(raSet);

		return acl;
	}
	
	// This is the ACL used for Teams created by a user (the "creator")
	// we give the creator admin permissions and then give the team itself
	// default permissions, so that anyone added to the team has those 
	// permissions without having to explicitly add them to the ACL.
	public static AccessControlList createInitialAcl(
			final UserInfo creator, 
			final String teamId, 
			final Date creationDate) {
		AccessControlList acl = AccessControlListUtil.createACL(
				teamId, creator, ModelConstants.TEAM_ADMIN_PERMISSIONS, creationDate);
		// set up default permissions for additional members
		acl.getResourceAccess().add(createResourceAccess(
				Long.parseLong(teamId),
				ModelConstants.TEAM_MESSENGER_PERMISSIONS));
		// let public send messages to team by default
		acl.getResourceAccess().add(createResourceAccess(
				AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId(),
				ModelConstants.TEAM_MESSENGER_PERMISSIONS));

		return acl;
	}
	
	public static void addToACL(AccessControlList acl, String principalId, Set<ACCESS_TYPE> accessSet) {
		ResourceAccess ra = new ResourceAccess();
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
	@WriteTransaction
	public Team create(UserInfo userInfo, Team team) throws DatastoreException,
			InvalidModelException, UnauthorizedException, NotFoundException {
		if (AuthorizationUtils.isUserAnonymous(userInfo))
				throw new UnauthorizedException("Anonymous user cannot create Team.");
		validateForCreate(team);
		// create UserGroup (fail if UG with the given name already exists)
		UserGroup ug = new UserGroup();
		ug.setIsIndividual(false);
		ug.setCreationDate(new Date());
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
	
	/**
	 * This turns out to be very different from the normal create, bypassing checks for both 
	 * principal and team creation. Also, it's not transactional.
	 * @param team
	 * @param now
	 * @param teamIdTakesPriority
	 * @return
	 */
	private Team bootstrapCreate(Team team) {
		Long teamId = Long.parseLong(team.getId());
		Date now = new Date();
		
		team.setCreatedOn(now);
		team.setModifiedOn(now);
		
		// create UserGroup with the same ID as specified for the team.
		// Both share the keyspace of a PRINCIPAL, this should be okay
		DBOUserGroup dbo = new DBOUserGroup();
		dbo.setId(teamId);
		dbo.setEtag(UUID.randomUUID().toString());
		dbo.setIsIndividual(false);
		dbo.setCreationDate(now);
		basicDao.createOrUpdate(dbo);
		
		// bind the team name to this principal. 
		bindTeamName(team.getName(), teamId);
		
		return teamDAO.create(team);
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
		alias.setPrincipalId(teamId);
		alias.setType(AliasType.TEAM_NAME);
		// bind this alias
		principalAliasDAO.bindAliasToPrincipal(alias);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.TeamManager#get(long, long)
	 */
	@Override
	public PaginatedResults<Team> list(long limit, long offset)
			throws DatastoreException {
		List<Team> results = teamDAO.getInRange(limit, offset);
		return PaginatedResults.createWithLimitAndOffset(results, limit, offset);
	}

	@Override
	public ListWrapper<Team> list(List<Long> ids) throws DatastoreException, NotFoundException {
		return teamDAO.list(ids);
	}

	@Override
	public PaginatedResults<TeamMember> listMembers(String teamId, long limit,
			long offset) throws DatastoreException {
		List<TeamMember> results = teamDAO.getMembersInRange(teamId, limit, offset);
		return PaginatedResults.createWithLimitAndOffset(results, limit, offset);
	}
	
	@Override
	public Count countMembers(String teamId) throws DatastoreException {
		ValidateArgument.required(teamId, "teamId");
		Count result = new Count();
		result.setCount(teamDAO.getMembersCount(teamId));
		return result;
	}
	
	@Override
	public ListWrapper<TeamMember> listMembers(List<Long> teamIds, List<Long> memberIds)
			throws DatastoreException, NotFoundException {
		return teamDAO.listMembers(teamIds, memberIds);
	}
	

	@Override
	public TeamMember getMember(String teamId, String principalId) throws NotFoundException, DatastoreException {
		return teamDAO.getMember(teamId, principalId);
	}


	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.TeamManager#getByMember(java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<Team> listByMember(String principalId,
			long limit, long offset) throws DatastoreException {
		List<Team> results = teamDAO.getForMemberInRange(principalId, limit, offset);
		return PaginatedResults.createWithLimitAndOffset(results, limit, offset);
	}

	@Override
	public PaginatedTeamIds listIdsByMember(String teamMemberId, String nextPageToken, TeamSortOrder sort, Boolean ascending) {
		NextPageToken token = new NextPageToken(nextPageToken);
		List<String> teamIds = teamDAO.getIdsForMember(teamMemberId, token.getLimitForQuery(), token.getOffset(), sort, ascending);
		PaginatedTeamIds result = new PaginatedTeamIds();
		result.setTeamIds(teamIds);
		result.setNextPageToken(token.getNextPageTokenForCurrentResults(teamIds));
		return result;
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
	@WriteTransaction
	public Team put(UserInfo userInfo, Team team) throws InvalidModelException,
			DatastoreException, UnauthorizedException, NotFoundException {
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, team.getId(), ObjectType.TEAM, ACCESS_TYPE.UPDATE));
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
	@WriteTransaction
	public void delete(UserInfo userInfo, String id) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		try {
			teamDAO.get(id);
		} catch (NotFoundException e) {
			return;
		}
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, id, ObjectType.TEAM, ACCESS_TYPE.DELETE));
		// delete ACL
		aclDAO.delete(id, ObjectType.TEAM);
		// delete Team
		teamDAO.delete(id);
		// delete userGroup
		userGroupDAO.delete(id);
	}
	
	
	private boolean hasUnmetAccessRequirements(UserInfo memberUserInfo, String teamId) throws NotFoundException {
		List<Long> unmetRequirements = accessRequirementDAO.getAllUnmetAccessRequirements(
				Collections.singletonList(teamId), RestrictableObjectType.TEAM, memberUserInfo.getGroups(), 
				Collections.singletonList(ACCESS_TYPE.PARTICIPATE));
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
	public boolean canAddTeamMember(UserInfo userInfo, String teamId, UserInfo principalUserInfo, boolean alreadyInTeam) throws NotFoundException {
		if (userInfo.isAdmin()) return true;
		if (hasUnmetAccessRequirements(principalUserInfo, teamId)) return false;
		String principalId = principalUserInfo.getId().toString();
		boolean principalIsSelf = userInfo.getId().toString().equals(principalId);
		boolean amTeamAdmin = authorizationManager.canAccess(userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE).getAuthorized();
		long now = System.currentTimeMillis();
		if (principalIsSelf) {
			// trying to add myself to Team.  
			if (amTeamAdmin) return true;
			// if the team is open, I can join
			Team team = teamDAO.get(teamId);
			if (team.getCanPublicJoin()!=null && team.getCanPublicJoin()==true) return true;
			// if I'm not a team admin and the team is not open, then I need to have an open invitation
			if (alreadyInTeam) return true;
			long openInvitationCount = membershipInvitationDAO.getOpenByTeamAndUserCount(Long.parseLong(teamId), Long.parseLong(principalId), now);
			return openInvitationCount>0L;
		} else {
			// the member to be added is someone other than me
			if (!amTeamAdmin) return false; // can't add somone unless I'm a Team administrator
			// can't add someone unless they are asking to be added
			if (alreadyInTeam) return true;
			long openRequestCount = membershipRequestDAO.getOpenByTeamAndRequesterCount(Long.parseLong(teamId), Long.parseLong(principalId), now);
			return openRequestCount>0L;
		}
	}
	
	public static boolean userGroupsHasPrincipalId(Collection<UserGroup> userGroups, String principalId) {
		for (UserGroup ug : userGroups) if (ug.getId().equals(principalId)) return true;
		return false;
	}
	
	@Override
	public List<MessageToUserAndBody> createJoinedTeamNotifications(UserInfo joinerInfo, 
			UserInfo memberInfo, String teamId, String teamEndpoint,
			String notificationUnsubscribeEndpoint) throws NotFoundException {
		List<MessageToUserAndBody> result = new ArrayList<MessageToUserAndBody>();
		if (teamEndpoint==null || notificationUnsubscribeEndpoint==null) return result;
		boolean userJoiningTeamIsSelf = joinerInfo.getId().equals(memberInfo.getId());
		Map<String,String> fieldValues = new HashMap<String,String>();
		fieldValues.put(TEMPLATE_KEY_TEAM_NAME, teamDAO.get(teamId).getName());
		fieldValues.put(TEMPLATE_KEY_TEAM_ID, teamId);
		String teamUrl = teamEndpoint+teamId;
		EmailUtils.validateSynapsePortalHost(teamUrl);
		fieldValues.put(TEMPLATE_KEY_TEAM_WEB_LINK, teamUrl);
		if (userJoiningTeamIsSelf) {
			UserProfile memberUserProfile = userProfileManager.getUserProfile(memberInfo.getId().toString());
			String memberDisplayName = EmailUtils.getDisplayNameWithUsername(memberUserProfile);
			fieldValues.put(TEMPLATE_KEY_DISPLAY_NAME, memberDisplayName);
			fieldValues.put(TEMPLATE_KEY_USER_ID, memberInfo.getId().toString());
			for (String recipient : getInviters(Long.parseLong(teamId), memberInfo.getId())) {
				MessageToUser mtu = new MessageToUser();
				mtu.setSubject(JOIN_TEAM_CONFIRMATION_MESSAGE_SUBJECT);
				String messageContent = EmailUtils.readMailTemplate(USER_HAS_JOINED_TEAM_TEMPLATE, fieldValues);
				mtu.setRecipients(Collections.singleton(recipient));
				mtu.setNotificationUnsubscribeEndpoint(notificationUnsubscribeEndpoint);
				result.add(new MessageToUserAndBody(mtu, messageContent, ContentType.TEXT_HTML.getMimeType()));
			}
		} else {
			UserProfile joinerUserProfile = userProfileManager.getUserProfile(joinerInfo.getId().toString());
			String joinerDisplayName = EmailUtils.getDisplayNameWithUsername(joinerUserProfile);
			fieldValues.put(TEMPLATE_KEY_DISPLAY_NAME, joinerDisplayName);
			fieldValues.put(TEMPLATE_KEY_USER_ID, joinerInfo.getId().toString());
			String recipient = memberInfo.getId().toString();
			String messageContent = EmailUtils.readMailTemplate(ADMIN_HAS_ADDED_USER_TEMPLATE, fieldValues);
			MessageToUser mtu = new MessageToUser();
			mtu.setSubject(JOIN_TEAM_CONFIRMATION_MESSAGE_SUBJECT);
			mtu.setRecipients(Collections.singleton(recipient));
			mtu.setNotificationUnsubscribeEndpoint(notificationUnsubscribeEndpoint);
			result.add(new MessageToUserAndBody(mtu, messageContent, ContentType.TEXT_HTML.getMimeType()));
		}	
		return result;
	}

	private Set<String> getInviters(Long teamId, Long inviteeId) {
		return new LinkedHashSet<String>(membershipInvitationDAO.
			getInvitersByTeamAndUser(teamId, inviteeId, System.currentTimeMillis()));
	}	

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.TeamManager#addMember(org.sagebionetworks.repo.model.UserInfo, java.lang.String, java.lang.String)
	 * @return a Pair containing (1) the user ID of the one who joined, (2) a list of those to be notified
	 */
	@Override
	@WriteTransaction
	public boolean addMember(UserInfo userInfo, String teamId, UserInfo principalUserInfo)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		String principalId = principalUserInfo.getId().toString();
		boolean alreadyInTeam = userGroupsHasPrincipalId(groupMembersDAO.getMembers(teamId), principalId);
		if (!canAddTeamMember(userInfo, teamId, principalUserInfo, alreadyInTeam)) throw new UnauthorizedException("Cannot add member to Team.");

		if (!alreadyInTeam) {
			groupMembersDAO.addMembers(teamId, Collections.singletonList(principalId));
			// update the project stats for the new member.
			projectStatsManager.memberAddedToTeam(Long.parseLong(teamId), principalUserInfo.getId(), new Date());
		}
		
		// clean up any invitations
		membershipInvitationDAO.deleteByTeamAndUser(Long.parseLong(teamId), principalUserInfo.getId());
		// clean up and membership requests
		membershipRequestDAO.deleteByTeamAndRequester(Long.parseLong(teamId), principalUserInfo.getId());
		
		return !alreadyInTeam;
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
		boolean amTeamAdmin = authorizationManager.canAccess(userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE).getAuthorized();
		if (amTeamAdmin) return true;
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.TeamManager#removeMember(org.sagebionetworks.repo.model.UserInfo, java.lang.String, java.lang.String)
	 */
	@Override
	@WriteTransaction
	public void removeMember(UserInfo userInfo, String teamId,
			String principalId) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		if (!canRemoveTeamMember(userInfo, teamId, principalId)) throw new UnauthorizedException("Cannot remove member from Team.");
		// check that member is actually in Team
		List<UserGroup> currentMembers = groupMembersDAO.getMembers(teamId);
		if (userGroupsHasPrincipalId(currentMembers, principalId)) {
			if (currentMembers.size()==1) throw new UnauthorizedException("Cannot remove the last member of a Team.");
			// remove from ACL
			AccessControlList acl = aclDAO.get(teamId, ObjectType.TEAM);
			removeFromACL(acl, principalId);
			if (!userInfo.isAdmin() && !aclHasTeamAdmin(acl)) {
				throw new InvalidModelException(MSG_TEAM_MUST_HAVE_AT_LEAST_ONE_TEAM_MANAGER);
			}
			groupMembersDAO.removeMembers(teamId, Collections.singletonList(principalId));
			aclDAO.update(acl, ObjectType.TEAM);
		}
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.TeamManager#getACL(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@Override
	public AccessControlList getACL(UserInfo userInfo, String teamId)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.READ));
		return aclDAO.get(teamId, ObjectType.TEAM);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.team.TeamManager#updateACL(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.repo.model.AccessControlList)
	 */
	@Override
	public AccessControlList updateACL(UserInfo userInfo, AccessControlList acl)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, acl.getId(), ObjectType.TEAM, ACCESS_TYPE.UPDATE));
		aclDAO.update(acl, ObjectType.TEAM);
		return aclDAO.get(acl.getId(), ObjectType.TEAM);
	}

	@Override
	public String getIconURL(String teamId) throws NotFoundException {
		Team team = teamDAO.get(teamId);
		String handleId = team.getIcon();
		if (handleId==null) throw new NotFoundException("Team "+teamId+" has no icon file handle.");
		return fileHandleManager.getRedirectURLForFileHandle(handleId);
	}

	@Override
	public Map<Team, Collection<TeamMember>> listAllTeamsAndMembers()
			throws DatastoreException {
		return teamDAO.getAllTeamsAndMembers();
	}

	@Override
	@WriteTransaction
	public void setPermissions(UserInfo userInfo, String teamId,
			String principalId, boolean isAdmin) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.UPDATE));
		AccessControlList acl = aclDAO.get(teamId, ObjectType.TEAM);
		// first, remove the principal's entries from the ACL
		removeFromACL(acl, principalId);
		// now, if isAdmin is false, the team membership is enough to give the user basic permissions
		if (isAdmin) {
			// if isAdmin is true, then we add the specified admin permissions
			addToACL(acl, principalId, ModelConstants.TEAM_ADMIN_PERMISSIONS);
		}
		if (!userInfo.isAdmin() && !aclHasTeamAdmin(acl)) throw new InvalidModelException(MSG_TEAM_MUST_HAVE_AT_LEAST_ONE_TEAM_MANAGER);
		// finally, update the ACL
		aclDAO.update(acl, ObjectType.TEAM);
	}
	
	// answers the question about whether membership approval is required to add principal to team
	// the logic is !userIsSynapseAdmin && !userIsTeamAdmin && !publicCanJoinTeam
	public boolean isMembershipApprovalRequired(UserInfo principalUserInfo, String teamId) throws DatastoreException, NotFoundException {
		boolean userIsSynapseAdmin = principalUserInfo.isAdmin();
		if (userIsSynapseAdmin) return false;
		boolean userIsTeamAdmin = authorizationManager.canAccess(principalUserInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE).getAuthorized();
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
		boolean isMember = userGroupsHasPrincipalId(groupMembersDAO.getMembers(teamId), principalId);
		tms.setIsMember(isMember);
		long now = System.currentTimeMillis();
		long openInvitationCount = membershipInvitationDAO.getOpenByTeamAndUserCount(Long.parseLong(teamId), Long.parseLong(principalId), now);
		tms.setHasOpenInvitation(openInvitationCount>0L);
		long openRequestCount = membershipRequestDAO.getOpenByTeamAndRequesterCount(Long.parseLong(teamId), Long.parseLong(principalId), now);
		tms.setHasOpenRequest(openRequestCount>0L);
		tms.setCanJoin(canAddTeamMember(userInfo, teamId, principalUserInfo, isMember));
		tms.setHasUnmetAccessRequirement(hasUnmetAccessRequirements(principalUserInfo, teamId));
		tms.setMembershipApprovalRequired(isMembershipApprovalRequired(principalUserInfo, teamId));
		tms.setCanSendEmail(authorizationManager.canAccess(principalUserInfo, teamId, ObjectType.TEAM, ACCESS_TYPE.SEND_MESSAGE).getAuthorized());
		return tms;
	}

	@WriteTransaction
	public void bootstrapTeams() throws NotFoundException {
		if (this.teamsToBootstrap == null) {
			throw new IllegalArgumentException("bootstrapTeams cannot be null");
		}
		for (BootstrapTeam team: this.teamsToBootstrap) {
			if (team.getId() == null) {
				throw new IllegalArgumentException("Bootstrapped team must have an id");
			}
			if (!principalManager.isAliasValid(team.getName(), AliasType.TEAM_NAME)) {
				throw new IllegalArgumentException("Bootstrapped team name is either syntactially wrong, or not unique");
			}
			try {
				get(team.getId());
			} catch(NotFoundException e) {
				long teamIdLong = Long.parseLong(team.getId());
				if(!AuthorizationConstants.BOOTSTRAP_PRINCIPAL.isBootstrapPrincipalId(teamIdLong)) {
					throw new IllegalArgumentException("Not a bootstrap principal: "+teamIdLong);
				}
				Team newTeam = new Team();
				newTeam.setId(team.getId());
				newTeam.setName(team.getName());
				newTeam.setCanPublicJoin(team.getCanPublicJoin());
				newTeam.setDescription(team.getDescription());
				newTeam.setIcon(team.getIcon());
				newTeam = bootstrapCreate(newTeam);	
				if (null!=team.getInitialMembers()) {
					groupMembersDAO.addMembers(newTeam.getId(), team.getInitialMembers());
				}
				// create ACL
				AccessControlList acl = createBootstrapTeamAcl(newTeam.getId(), new Date());
				aclDAO.create(acl, ObjectType.TEAM);
			}
		}
	}

}
