/**
 * 
 */
package org.sagebionetworks.repo.web.service;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.ardverk.collection.Trie;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamHeader;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
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
	private volatile Trie<String, Collection<TeamHeader>> teamNamePrefixCache;


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

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.TeamService#get(java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<Team> get(String fragment, long limit, long offset)
			throws DatastoreException {
		if (fragment==null) return teamManager.get(limit, offset);

		if (teamNamePrefixCache == null || teamNamePrefixCache.size() == 0)
			refreshCache();
		
		int limitInt = 10;
		if(limit != null){
			limitInt = limit.intValue();
		}
		int offsetInt = 0;
		if(offset != null){
			offsetInt = offset.intValue();
		}
		// Get the results from the cache
		SortedMap<String, Collection<UserGroupHeader>> matched = teamNamePrefixCache.prefixMap(fragment.toLowerCase());
		List<UserGroupHeader> fullList = PrefixCacheHelper.flatten(matched);
		QueryResults<UserGroupHeader> eqr = new QueryResults<UserGroupHeader>(fullList, limitInt, offsetInt);
		UserGroupHeaderResponsePage results = new UserGroupHeaderResponsePage();
		results.setChildren(eqr.getResults());
		results.setPrefixFilter(fragment);
		results.setTotalNumberOfResults(new Long(eqr.getTotalNumberOfResults()));
		return results;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.TeamService#getMembers(java.lang.String, java.lang.String, long, long)
	 */
	@Override
	public PaginatedResults<TeamMember> getMembers(String teamId,
			String fragment, long limit, long offset)
			throws DatastoreException, NotFoundException {
		// TODO Auto-generated method stub
		return null;
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

}
