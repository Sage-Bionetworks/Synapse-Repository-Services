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
	
	@Override
	public String createCompetition(String userId, Competition comp) throws DatastoreException, InvalidModelException {
		Utility.ensureNotNull(userId);
		comp.setOwnerId(userId);
		return competitionDAO.create(comp);
	}
	
	@Override
	public Competition getCompetition(String id) throws DatastoreException, NotFoundException, UnauthorizedException {
		Utility.ensureNotNull(id);
		return competitionDAO.get(id);
	}
	
	@Override
	public QueryResults<Competition> getInRange(long startIncl, long endExcl) throws DatastoreException, NotFoundException {
		List<Competition> competitions = competitionDAO.getInRange(startIncl, endExcl);
		long totalNumberOfResults = competitionDAO.getCount();
		QueryResults<Competition> result = new QueryResults<Competition>(competitions, (int) totalNumberOfResults);
		return result;
	}
	
	@Override
	public long getCount() throws DatastoreException, NotFoundException {
		return competitionDAO.getCount();
	}

	@Override
	public Competition findCompetition(String name) throws DatastoreException, NotFoundException, UnauthorizedException {
		Utility.ensureNotNull(name);
		Competition comp = competitionDAO.find(name);
		if (comp == null) throw new NotFoundException("No Competition found with name " + name);
		return comp;
	}
	
	@Override
	public Competition updateCompetition(String userId, Competition comp) throws DatastoreException, NotFoundException, UnauthorizedException, InvalidModelException, ConflictingUpdateException {
		Utility.ensureNotNull(userId, comp);
		Competition old = competitionDAO.get(comp.getId());
		if (old == null) throw new NotFoundException("No Competition found with id " + comp.getId());
		validateAdminAccess(userId, old);
		validateCompetition(old, comp);
		competitionDAO.update(comp);
		return getCompetition(comp.getId());
	}
	
	@Override
	public void deleteCompetition(String userId, String id) throws DatastoreException, NotFoundException, UnauthorizedException {
		Utility.ensureNotNull(userId, id);
		Competition comp = competitionDAO.get(id);
		if (comp == null) throw new NotFoundException("No Competition found with id " + id);
		validateAdminAccess(userId, comp);
		competitionDAO.delete(id);			
	}
	
	@Override
	public boolean isCompAdmin(String userId, String compId) throws DatastoreException, UnauthorizedException, NotFoundException {
		return isCompAdmin(userId, getCompetition(compId));
	}
	
	@Override
	public boolean isCompAdmin(String userId, Competition comp) {
		if (userId.equals(comp.getOwnerId())) return true;
		
		// TODO: check list of admins
		return false;
	}
	
	private void validateAdminAccess(String userId, Competition comp) {
		if (!isCompAdmin(userId, comp))
			throw new UnauthorizedException("User ID " + userId +
					" is not authorized to modify Competition ID " + comp.getId() +
					" (" + comp.getName() + ")");
	}
	
	private void validateCompetition(Competition oldComp, Competition newComp) {
		if (!oldComp.getOwnerId().equals(newComp.getOwnerId()))
			throw new InvalidModelException("Cannot overwrite Competition Owner ID");
		if (!oldComp.getCreatedOn().equals(newComp.getCreatedOn()))
			throw new InvalidModelException("Cannot overwrite CreatedOn date");
		if (!oldComp.getEtag().equals(newComp.getEtag()))
			throw new InvalidModelException("Etag is invalid. Please fetch the Competition again.");
	}
}
