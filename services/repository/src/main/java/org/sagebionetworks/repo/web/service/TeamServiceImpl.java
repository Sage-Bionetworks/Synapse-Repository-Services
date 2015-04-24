/**
 * 
 */
package org.sagebionetworks.repo.web.service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManagerUtils;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.TeamMembershipStatus;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author brucehoff
 *
 */
public class TeamServiceImpl implements TeamService {

	@Autowired
	private TeamManager teamManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	PrincipalPrefixDAO principalPrefixDAO;
	
	private final Logger logger = LogManager.getLogger(TeamServiceImpl.class);
	
	// for testing (e.g. setting a mocked manager
	public void setTeamManager(TeamManager teamManager) {this.teamManager=teamManager;}
	
	public void setPrincipalPrefixDAO(PrincipalPrefixDAO principalPrefixDAO) {this.principalPrefixDAO=principalPrefixDAO;}

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
		if (limit<1) throw new IllegalArgumentException("'limit' must be at least 1");
		if (offset<0) throw new IllegalArgumentException("'offset' may not be negative");
		if (fragment==null || fragment.trim().length()==0) return teamManager.list(limit, offset);
		
		List<Long> teamIds = principalPrefixDAO.listTeamsForPrefix(fragment, limit, offset);
		List<Team> teams = teamManager.list(teamIds).getList();
		PaginatedResults<Team> results = new PaginatedResults<Team>(teams, 0);
		results.setTotalNumberOfResults(principalPrefixDAO.countTeamsForPrefix(fragment));
		return results;
	}



	private static Comparator<TeamMember> teamMemberComparator = new Comparator<TeamMember>() {
		@Override
		public int compare(TeamMember o1, TeamMember o2) {
			return o1.getMember().getUserName().compareTo(o2.getMember().getUserName());
		}
	};
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.TeamService#getMembers(java.lang.String, java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<TeamMember> getMembers(String teamId,
			String fragment, long limit, long offset)
			throws DatastoreException, NotFoundException {
		
		if (limit<1) throw new IllegalArgumentException("'limit' must be at least 1");
		if (offset<0) throw new IllegalArgumentException("'offset' may not be negative");

		// if there is no prefix provided, we just to a regular paginated query
		// against the database and return the result.  We also clear out the private fields.
		if (fragment==null || fragment.trim().length()==0) {
			PaginatedResults<TeamMember>results = teamManager.listMembers(teamId, limit, offset);
			for (TeamMember teamMember : results.getResults()) {
				UserProfileManagerUtils.clearPrivateFields(null, teamMember.getMember());
			}
			return results;
		}
		Long teamIdLong = Long.parseLong(teamId);
		List<Long> memberIds = principalPrefixDAO.listTeamMembersForPrefix(fragment, teamIdLong, limit, offset);
		List<TeamMember> members = listTeamMembers(Arrays.asList(teamIdLong), memberIds).getList();
		PaginatedResults<TeamMember> results = new PaginatedResults<TeamMember>(members, 0);
		results.setTotalNumberOfResults(principalPrefixDAO.countTeamMembersForPrefix(fragment, teamIdLong));
		return results;
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
	public String getIconURL(String teamId) throws DatastoreException,
			NotFoundException {
		return teamManager.getIconURL(teamId);
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
	public void addMember(Long userId, String teamId, String principalId) throws DatastoreException, UnauthorizedException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		UserInfo memberUserInfo = userManager.getUserInfo(Long.parseLong(principalId));
		teamManager.addMember(userInfo, teamId, memberUserInfo);
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

}
