package org.sagebionetworks.competition.manager;

import java.util.List;

import org.sagebionetworks.competition.dao.CompetitionDAO;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.util.Utility;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class CompetitionManagerImpl implements CompetitionManager {
	
	@Autowired
	CompetitionDAO competitionDAO;
	
	public CompetitionManagerImpl() {}
	
	// Used for testing purposes
	protected CompetitionManagerImpl(CompetitionDAO competitionDAO) {
		this.competitionDAO = competitionDAO;
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.competition.manager.CompetitionManager#createCompetition(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.competition.model.Competition)
	 */
	@Override
	public String createCompetition(UserInfo userInfo, Competition comp) 
			throws DatastoreException, InvalidModelException {
		Utility.ensureNotNull(userInfo, "User");
		comp.setOwnerId(userInfo.getIndividualGroup().getId());
		return competitionDAO.create(comp);
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.competition.manager.CompetitionManager#getCompetition(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@Override
	public Competition getCompetition(String id) throws DatastoreException, NotFoundException, UnauthorizedException {
		Utility.ensureNotNull(id, "Competition ID");
		return competitionDAO.get(id);
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.competition.manager.CompetitionManager#getInRange(org.sagebionetworks.repo.model.UserInfo, long, long)
	 */
	@Override
	public QueryResults<Competition> getInRange(long startIncl, long endExcl) throws DatastoreException, NotFoundException {
		List<Competition> competitions = competitionDAO.getInRange(startIncl, endExcl);
		long totalNumberOfResults = competitionDAO.getCount();
		QueryResults<Competition> result = new QueryResults<Competition>(competitions, (int) totalNumberOfResults);
		return result;
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.competition.manager.CompetitionManager#getCount()
	 */
	@Override
	public long getCount() throws DatastoreException, NotFoundException {
		return competitionDAO.getCount();
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.competition.manager.CompetitionManager#findCompetition(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@Override
	public Competition findCompetition(String name) throws DatastoreException, NotFoundException, UnauthorizedException {
		Competition comp = competitionDAO.find(name);
		if (comp == null) throw new NotFoundException("No Competition found with name " + name);
		return comp;
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.competition.manager.CompetitionManager#updateCompetition(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.competition.model.Competition)
	 */
	@Override
	public Competition updateCompetition(UserInfo userInfo, Competition comp) throws DatastoreException, NotFoundException, UnauthorizedException, InvalidModelException, ConflictingUpdateException {
		Competition old = competitionDAO.get(comp.getId());
		if (isAdmin(userInfo, comp) && isAdmin(userInfo, old)) {
			competitionDAO.update(comp);		
			return getCompetition(comp.getId());
		} else {
			throw new UnauthorizedException("User ID '" + userInfo.getIndividualGroup().getId() +
					"' is not authorized to modify Competition ID '" + comp.getId() +
					"' (" + comp.getName() + ")");
		}
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.competition.manager.CompetitionManager#deleteCompetition(org.sagebionetworks.repo.model.UserInfo, java.lang.String)
	 */
	@Override
	public void deleteCompetition(UserInfo userInfo, String id) throws DatastoreException, NotFoundException, UnauthorizedException {
		Competition comp = competitionDAO.get(id);
		if (isAdmin(userInfo, comp))
			competitionDAO.delete(id);
		else
			throw new UnauthorizedException("User ID " + userInfo.getIndividualGroup().getId() +
					" is not authorized to modify Competition ID " + comp.getId() +
					" (" + comp.getName() + ")");
	}
	
	private boolean isAdmin(UserInfo userInfo, Competition comp) {
		return userInfo.getIndividualGroup().getId().equals(comp.getOwnerId());
	}
}
