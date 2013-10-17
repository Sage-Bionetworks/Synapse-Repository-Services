/**
 * 
 */
package org.sagebionetworks.repo.web.service;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ardverk.collection.PatriciaTrie;
import org.ardverk.collection.StringKeyAnalyzer;
import org.ardverk.collection.Trie;
import org.ardverk.collection.Tries;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManagerUtils;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.TeamMembershipStatus;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
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
	
	private final Logger logger = LogManager.getLogger(TeamServiceImpl.class);

/**
	 * 
	 * The following is taken from the cached prefix tree in UserProfileServiceImpl
	 * 
	 * These member variables are declared volatile to enforce thread-safe
	 * cache access. Clients should fetch the latest cache objects for every
	 * request.
	 * 
	 * The cache objects may be *replaced* by new cache objects created in the
	 * refreshCache() method, but existing cache objects can NOT be modified.
	 * This is to avoid corruption of cache state during multithreaded read
	 * operations.
	 */
	private volatile Long cachesLastUpdated = 0L;
	private volatile Trie<String, Collection<Team>> teamNamePrefixCache;
	// key is team id, value is a prefix cache for the team's members
	private volatile Map<String, Trie<String, Collection<TeamMember>>> teamMemberPrefixCache;
	
	// for testing (e.g. setting a mocked manager
	public void setTeamManager(TeamManager teamManager) {this.teamManager=teamManager;}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.TeamService#create(java.lang.String, org.sagebionetworks.repo.model.Team)
	 */
	@Override
	public Team create(String userId, Team team) throws UnauthorizedException,
			InvalidModelException, DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return teamManager.create(userInfo, team);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.TeamService#get(long, long)
	 */
	@Override
	public PaginatedResults<Team> get(long limit, long offset)
			throws DatastoreException {
		return teamManager.get(limit, offset);
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
		if (fragment==null) return teamManager.get(limit, offset);

		if (teamNamePrefixCache == null || teamNamePrefixCache.size() == 0 )
			refreshCache();
		
		// Get the results from the cache
		SortedMap<String, Collection<Team>> matched = teamNamePrefixCache.prefixMap(fragment.toLowerCase());
		List<Team> fullList = PrefixCacheHelper.flatten(matched, teamComparator);
		PaginatedResults<Team> results = new PaginatedResults<Team>();
		results.setTotalNumberOfResults(fullList.size());
		List<Team> page = new ArrayList<Team>((int)limit);
		for (int i=(int)offset; i<offset+limit && i<fullList.size(); i++) page.add(fullList.get(i));
		results.setResults(page);
		return results;
	}

	@Override
	public void refreshCache(String userId) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!userInfo.isAdmin()) throw new UnauthorizedException("Must be a Synapse administrator.");
		refreshCache();
	}
	
	@Override
	public void refreshCache() throws DatastoreException, NotFoundException {
		this.logger.info("refreshCache() started at time " + System.currentTimeMillis());

		// Create and populate local caches. Upon completion, swap them for the
		// singleton member variable caches.
		Trie<String, Collection<Team>> tempPrefixCache = new PatriciaTrie<String, Collection<Team>>(StringKeyAnalyzer.CHAR);
		Map<String, Trie<String, Collection<TeamMember>>> tempTeamMemberPrefixCacheSet = new HashMap<String, Trie<String, Collection<TeamMember>>>();
		Map<Team, Collection<TeamMember>> allTeams = teamManager.getAllTeamsAndMembers();
		for (Team team : allTeams.keySet()) {
			addToTeamPrefixCache(tempPrefixCache, team);
			Trie<String, Collection<TeamMember>>tempTeamMemberPrefixCache = tempTeamMemberPrefixCacheSet.get(team.getId());
			if (tempTeamMemberPrefixCache==null) {
				tempTeamMemberPrefixCache = new PatriciaTrie<String, Collection<TeamMember>>(StringKeyAnalyzer.CHAR);
				tempTeamMemberPrefixCacheSet.put(team.getId(), tempTeamMemberPrefixCache);
			}
			for (TeamMember member : allTeams.get(team)) {
				addToMemberPrefixCache(tempTeamMemberPrefixCache, member);
			}
		}
		Map<String, Trie<String, Collection<TeamMember>>> tempUnModifiableTeamMemberPrefixCacheSet = new HashMap<String, Trie<String, Collection<TeamMember>>>();
		for (String teamId : tempTeamMemberPrefixCacheSet.keySet()) {
			tempUnModifiableTeamMemberPrefixCacheSet.put(teamId, 
					Tries.unmodifiableTrie(tempTeamMemberPrefixCacheSet.get(teamId)));
		}
		teamNamePrefixCache = Tries.unmodifiableTrie(tempPrefixCache);
		teamMemberPrefixCache = tempUnModifiableTeamMemberPrefixCacheSet;
		cachesLastUpdated = System.currentTimeMillis();

		this.logger.info("refreshCache() completed at time " + System.currentTimeMillis());
	}
	
	private void addToTeamPrefixCache(Trie<String, Collection<Team>> prefixCache, Team team) {
		//get the collection of prefixes that we want to associate to this Team
		List<String> prefixes = PrefixCacheHelper.getPrefixes(team.getName());
		for (String prefix : prefixes) {
			Collection<Team> coll = prefixCache.get(prefix);
			if (coll==null) {
				coll = new HashSet<Team>();
				prefixCache.put(prefix, coll);
			}
			coll.add(team);
		}
	}

	// NOTE:  A side effect is clearing the private fields of the UserGroupHeader in 'member',
	// as well as obfuscating the email address.
	private void addToMemberPrefixCache(Trie<String, Collection<TeamMember>> prefixCache, TeamMember member) {
		//get the collection of prefixes that we want to associate to this UserGroupHeader
		List<String> prefixes = PrefixCacheHelper.getPrefixes(member.getMember().getDisplayName());
		
		String unobfuscatedEmailAddress = member.getMember().getEmail();
		if (unobfuscatedEmailAddress != null && unobfuscatedEmailAddress.length() > 0)
			prefixes.add(unobfuscatedEmailAddress.toLowerCase());
		
		UserProfileManagerUtils.clearPrivateFields(null, member.getMember());
		
		for (String prefix : prefixes) {
			Collection<TeamMember> coll = prefixCache.get(prefix);
			if (coll==null) {
				coll = new HashSet<TeamMember>();
				prefixCache.put(prefix, coll);
			}
			coll.add(member);
		}
	}

	@Override
	public Long millisSinceLastCacheUpdate() {
		if (teamNamePrefixCache == null) {
			return null;
		}
		return System.currentTimeMillis() - cachesLastUpdated;
	}

	private static Comparator<TeamMember> teamMemberComparator = new Comparator<TeamMember>() {
		@Override
		public int compare(TeamMember o1, TeamMember o2) {
			return o1.getMember().getDisplayName().compareTo(o2.getMember().getDisplayName());
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
		if (fragment==null) {
			PaginatedResults<TeamMember>results = teamManager.getMembers(teamId, limit, offset);
			for (TeamMember teamMember : results.getResults()) {
				UserProfileManagerUtils.clearPrivateFields(null, teamMember.getMember());
			}
			return results;
		}
		
		if (teamMemberPrefixCache == null || teamMemberPrefixCache.size() == 0 )
			refreshCache();
		
		Trie<String, Collection<TeamMember>> teamSpecificMemberPrefixCache = teamMemberPrefixCache.get(teamId);
		if (teamSpecificMemberPrefixCache==null) throw new NotFoundException("Unrecognized teamId: "+teamId);
		
		// Get the results from the cache
		SortedMap<String, Collection<TeamMember>> matched = teamSpecificMemberPrefixCache.prefixMap(fragment.toLowerCase());
		List<TeamMember> fullList = PrefixCacheHelper.flatten(matched, teamMemberComparator);
		PaginatedResults<TeamMember> results = new PaginatedResults<TeamMember>();
		results.setTotalNumberOfResults(fullList.size());
		List<TeamMember> page = new ArrayList<TeamMember>((int)limit);
		for (int i=(int)offset; i<offset+limit && i<fullList.size(); i++) page.add(fullList.get(i));
		results.setResults(page);
		return results;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.TeamService#getByMember(java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<Team> getByMember(String principalId, long limit,
			long offset) throws DatastoreException {
		return teamManager.getByMember(principalId, limit, offset);
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
	public URL getIconURL(String teamId) throws DatastoreException,
			NotFoundException {
		return teamManager.getIconURL(teamId);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.TeamService#update(java.lang.String, org.sagebionetworks.repo.model.Team)
	 */
	@Override
	public Team update(String userId, Team team) throws DatastoreException,
			UnauthorizedException, NotFoundException, InvalidModelException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return teamManager.put(userInfo, team);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.TeamService#delete(java.lang.String, java.lang.String)
	 */
	@Override
	public void delete(String userId, String teamId) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		teamManager.delete(userInfo, teamId);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.TeamService#addMember(java.lang.String, java.lang.String, java.lang.String, boolean)
	 */
	@Override
	public void addMember(String userId, String teamId, String principalId) throws DatastoreException, UnauthorizedException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		teamManager.addMember(userInfo, teamId, principalId);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.TeamService#removeMember(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void removeMember(String userId, String teamId, String principalId)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		teamManager.removeMember(userInfo, teamId, principalId);
	}

	@Override
	public void setPermissions(String userId, String teamId,
			String principalId, boolean isAdmin) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		teamManager.setPermissions(userInfo, teamId, principalId, isAdmin);
	}

	@Override
	public TeamMembershipStatus getTeamMembershipStatus(String userId,
			String teamId, String principalId) throws DatastoreException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return teamManager.getTeamMembershipStatus(userInfo, teamId, principalId);
	}

}
