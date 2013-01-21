package org.sagebionetworks.competition.manager;

import java.util.List;

import org.sagebionetworks.competition.dao.CompetitionDAO;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.util.CompetitionUtils;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.EntityNameValidation;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class CompetitionManagerImpl implements CompetitionManager {
	
	@Autowired
	CompetitionDAO competitionDAO;
	
	public CompetitionManagerImpl() {}
	
	// Used for testing purposes
	protected CompetitionManagerImpl(CompetitionDAO competitionDAO) {
		this.competitionDAO = competitionDAO;
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Competition createCompetition(UserInfo userInfo, Competition comp) 
			throws DatastoreException, InvalidModelException, NotFoundException {
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getIndividualGroup().getId();
		comp.setName(EntityNameValidation.valdiateName(comp.getName()));
		String id = competitionDAO.create(comp, Long.parseLong(principalId));
		return competitionDAO.get(id);
	}
	
	@Override
	public Competition getCompetition(String id) throws DatastoreException, NotFoundException, UnauthorizedException {
		CompetitionUtils.ensureNotNull(id, "Competition ID");
		return competitionDAO.get(id);
	}
	
	@Override
	public QueryResults<Competition> getInRange(long limit, long offset) throws DatastoreException, NotFoundException {
		List<Competition> competitions = competitionDAO.getInRange(limit, offset);
		long totalNumberOfResults = competitionDAO.getCount();
		QueryResults<Competition> res = new QueryResults<Competition>(competitions, totalNumberOfResults);
		return res;
	}
	
	@Override
	public long getCount() throws DatastoreException, NotFoundException {
		return competitionDAO.getCount();
	}

	@Override
	public Competition findCompetition(String name) throws DatastoreException, NotFoundException, UnauthorizedException {
		CompetitionUtils.ensureNotNull(name, "Name");
		String compId = competitionDAO.lookupByName(name);
		Competition comp = competitionDAO.get(compId);
		if (comp == null) throw new NotFoundException("No Competition found with name " + name);
		return comp;
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Competition updateCompetition(UserInfo userInfo, Competition comp) throws DatastoreException, NotFoundException, UnauthorizedException, InvalidModelException, ConflictingUpdateException {
		CompetitionUtils.ensureNotNull(comp, "Competition");
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getIndividualGroup().getId();
		
		Competition old = competitionDAO.get(comp.getId());
		if (old == null) 
			throw new NotFoundException("No Competition found with id " + comp.getId());
		if (!old.getEtag().equals(comp.getEtag()))
			throw new IllegalArgumentException("Your copy of Competition " + comp.getId() + " is out of date. Please fetch it again before updating.");

		validateAdminAccess(principalId, old);
		validateCompetition(old, comp);		
		competitionDAO.update(comp);
		return getCompetition(comp.getId());
	}
	
	@Override
	public void updateCompetitionEtag(String compId) throws NotFoundException {
		Competition comp = competitionDAO.get(compId);
		if (comp == null) throw new NotFoundException("No Competition found with id " + compId);
		competitionDAO.update(comp);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteCompetition(UserInfo userInfo, String id) throws DatastoreException, NotFoundException, UnauthorizedException {
		CompetitionUtils.ensureNotNull(id, "Competition ID");
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getIndividualGroup().getId();
		Competition comp = competitionDAO.get(id);
		if (comp == null) throw new NotFoundException("No Competition found with id " + id);
		validateAdminAccess(principalId, comp);
		competitionDAO.delete(id);
	}
		
	@Override
	public boolean isCompAdmin(String userId, String compId) throws DatastoreException, UnauthorizedException, NotFoundException {
		CompetitionUtils.ensureNotNull(userId, "User ID");
		CompetitionUtils.ensureNotNull(compId, "Competition ID");
		Competition comp = getCompetition(compId);
		return isCompAdmin(userId, comp);
	}
	
	private boolean isCompAdmin(String userId, Competition comp) {
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
