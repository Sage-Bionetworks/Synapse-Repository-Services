package org.sagebionetworks.repo.web.service;

import java.util.Comparator;
import java.util.List;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.NotificationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.UserProfileManagerUtils;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Count;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.JoinTeamSignedToken;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.PaginatedTeamIds;
import org.sagebionetworks.repo.model.ResponseMessage;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.TeamMemberTypeFilterOptions;
import org.sagebionetworks.repo.model.TeamMembershipStatus;
import org.sagebionetworks.repo.model.TeamSortOrder;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author brucehoff
 *
 */
public class TeamServiceImpl implements TeamService {
	public static final long MAX_LIMIT = 50L;

	@Autowired
	private TeamManager teamManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	PrincipalPrefixDAO principalPrefixDAO;
	@Autowired
	private NotificationManager notificationManager;
	@Autowired
	private UserProfileManager userProfileManager;
	@Autowired
	private TokenGenerator tokenGenerator;
	
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.TeamService#create(java.lang.String, org.sagebionetworks.repo.model.Team)
	 */
	@Override
	public Team create(Long userId, Team team) throws UnauthorizedException,
			InvalidModelException, DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return teamManager.create(userInfo, team);
	}
	
	@Override
	public TeamMember getMember(String teamId, String principalId) throws NotFoundException, DatastoreException {
		TeamMember teamMember = teamManager.getMember(teamId, principalId);
		UserProfileManagerUtils.clearPrivateFields(null, teamMember.getMember());
		return teamMember;
	}


	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.TeamService#get(long, long)
	 */
	@Override
	public PaginatedResults<Team> get(long limit, long offset)
			throws DatastoreException {
		return teamManager.list(limit, offset);
	}

	@Override
	public ListWrapper<Team> list(List<Long> ids) throws DatastoreException, NotFoundException {
		return teamManager.list(ids);
	}

	private static Comparator<Team> teamComparator = new Comparator<Team>() {
		@Override
		public int compare(Team o1, Team o2) {
			return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
		}
	};
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.TeamService#get(java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<Team> get(String fragment, long limit, long offset)
			throws DatastoreException, NotFoundException  {

		ValidateArgument.requirement(limit > 0 && limit <= MAX_LIMIT, "limit must be between 1 and "+MAX_LIMIT);
		ValidateArgument.requirement(offset >= 0, "'offset' may not be negative");

		if (fragment==null || fragment.trim().length()==0) {
			return teamManager.list(limit, offset);
		}
		List<Long> teamIds = principalPrefixDAO.listTeamsForPrefix(fragment, limit, offset);
		List<Team> teams = teamManager.list(teamIds).getList();
		return PaginatedResults.createWithLimitAndOffset(teams, limit, offset);
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.TeamService#getMembers(java.lang.String, java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<TeamMember> getMembers(String teamId,
			String fragment, TeamMemberTypeFilterOptions memberType, long limit, long offset)
			throws DatastoreException, NotFoundException {
		
		ValidateArgument.requirement(limit > 0 && limit <= MAX_LIMIT, "limit must be between 1 and "+MAX_LIMIT);
		ValidateArgument.requirement(offset >= 0, "'offset' may not be negative");

		if (memberType == null) memberType = TeamMemberTypeFilterOptions.ALL;
		PaginatedResults<TeamMember> results;
		if (fragment==null || fragment.trim().length()==0) {
			// if there is no prefix provided, we just do a regular paginated query against the database
			results = teamManager.listMembers(teamId, memberType, limit, offset);
		} else {
			results = teamManager.listMembersForPrefix(fragment, teamId, memberType, limit, offset);
		}
		// Clear out the private fields.
		for (TeamMember teamMember : results.getResults()) {
			UserProfileManagerUtils.clearPrivateFields(null, teamMember.getMember());
		}
		return results;
	}

	/**
	 *
	 * @param teamId
	 * @param fragment
	 * @return
	 */
	public Count getMemberCount(String teamId, String fragment) {
		// if there is no prefix provided, we just to a regular paginated query
		// against the database and return the result.  We also clear out the private fields.
		if (fragment==null || fragment.trim().length()==0) {
			return teamManager.countMembers(teamId);
		}
		Long teamIdLong = Long.parseLong(teamId);
		Count result = new Count();
		result.setCount(principalPrefixDAO.countTeamMembersForPrefix(fragment, teamIdLong));
		return result;
	}

	
	@Override
	public ListWrapper<TeamMember> listTeamMembers(List<Long> teamIds, List<Long> memberIds) throws DatastoreException, NotFoundException {
		return teamManager.listMembers(teamIds, memberIds);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.TeamService#getByMember(java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<Team> getByMember(String principalId, long limit,
			long offset) throws DatastoreException {
		return teamManager.listByMember(principalId, limit, offset);
	}

	@Override
	public PaginatedTeamIds getIdsByMember(String teamMemberId, String nextPageToken, TeamSortOrder sort, Boolean ascending) {
		return teamManager.listIdsByMember(teamMemberId, nextPageToken, sort, ascending);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.TeamService#get(java.lang.String)
	 */
	@Override
	public Team get(String teamId) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		return teamManager.get(teamId);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.TeamService#getIconURL(java.lang.String)
	 */
	@Override
	public String getIconURL(Long userId, String teamId) throws DatastoreException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);		
		return teamManager.getIconURL(userInfo, teamId);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.TeamService#update(java.lang.String, org.sagebionetworks.repo.model.Team)
	 */
	@Override
	public Team update(Long userId, Team team) throws DatastoreException,
			UnauthorizedException, NotFoundException, InvalidModelException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return teamManager.put(userInfo, team);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.TeamService#delete(java.lang.String, java.lang.String)
	 */
	@Override
	public void delete(Long userId, String teamId) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		teamManager.delete(userInfo, teamId);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.TeamService#addMember(java.lang.String, java.lang.String, java.lang.String, boolean)
	 */
	@Override
	public void addMember(Long userId, String teamId, String principalId, String teamEndpoint,
			String notificationUnsubscribeEndpoint) throws DatastoreException, UnauthorizedException,
			NotFoundException {
		 addMemberIntern(userId, teamId, principalId, teamEndpoint, notificationUnsubscribeEndpoint);
	}
	
	@Override
	public ResponseMessage addMember(JoinTeamSignedToken joinTeamToken, String teamEndpoint, String notificationUnsubscribeEndpoint) throws DatastoreException, UnauthorizedException, NotFoundException {
		tokenGenerator.validateToken(joinTeamToken);
		boolean memberAdded = addMemberIntern(Long.parseLong(joinTeamToken.getUserId()), joinTeamToken.getTeamId(), joinTeamToken.getMemberId(), teamEndpoint, notificationUnsubscribeEndpoint);
		ResponseMessage responseMessage = new ResponseMessage();
		UserProfile userProfile = userProfileManager.getUserProfile(joinTeamToken.getMemberId());
		Team team = teamManager.get(joinTeamToken.getTeamId());
		String responseMessageText;
		if (memberAdded) {
			responseMessageText = "User "+
					EmailUtils.getDisplayNameWithUsername(userProfile)+
					" has been added to team "+team.getName()+".";
		} else {
			responseMessageText = "User "+
					EmailUtils.getDisplayNameWithUsername(userProfile)+
					" is already in team "+team.getName()+".";
		}
		responseMessage.setMessage(responseMessageText);
		return responseMessage;
	}

	private boolean addMemberIntern(Long userId, String teamId, String principalId, 
			String teamEndpoint,
			String notificationUnsubscribeEndpoint) throws DatastoreException, UnauthorizedException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		UserInfo memberUserInfo = userManager.getUserInfo(Long.parseLong(principalId));
		// note:  this must be done _before_ adding the member, which cleans up the invitation information
		// needed to determine who to notify
		List<MessageToUserAndBody> messages = teamManager.createJoinedTeamNotifications(userInfo, memberUserInfo, teamId, teamEndpoint, notificationUnsubscribeEndpoint);
		
		// the method is idempotent.  If the member is already added, it returns false
		boolean memberAdded = teamManager.addMember(userInfo, teamId, memberUserInfo);
		
		if (memberAdded) notificationManager.sendNotifications(userInfo, messages);
		
		return memberAdded;
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.TeamService#removeMember(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void removeMember(Long userId, String teamId, String principalId)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		teamManager.removeMember(userInfo, teamId, principalId);
	}

	@Override
	public void setPermissions(Long userId, String teamId,
			String principalId, boolean isAdmin) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		teamManager.setPermissions(userInfo, teamId, principalId, isAdmin);
	}

	@Override
	public TeamMembershipStatus getTeamMembershipStatus(Long userId,
			String teamId, String principalId) throws DatastoreException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		UserInfo principalUserInfo = userManager.getUserInfo(Long.parseLong(principalId));
		return teamManager.getTeamMembershipStatus(userInfo, teamId, principalUserInfo);
	}

	@Override
	public AccessControlList getAccessControlList(Long userId, String teamId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return teamManager.getACL(userInfo, teamId);
	}

	@Override
	public AccessControlList updateAccessControlList(Long userId,
			AccessControlList acl) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return teamManager.updateACL(userInfo, acl);
	}
}
