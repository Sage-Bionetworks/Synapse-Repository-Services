package org.sagebionetworks.competition.service;

import java.util.Set;

import org.sagebionetworks.competition.manager.CompetitionManager;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.competition.util.Utility;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class CompetitionServiceImpl {
	
	@Autowired
	CompetitionManager competitionManager;
	
	/* ***************************
	 * core Competition CRUD 
	 */
	
	public Competition getCompetition(String userId, String compId) throws DatastoreException, NotFoundException {
		Utility.ensureNotNull(userId, compId);
		return competitionManager.getCompetition(compId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Competition createCompetition(String userId, Competition comp) throws DatastoreException, NotFoundException {
		Utility.ensureNotNull(userId);
		String compId = competitionManager.createCompetition(userId, comp);
		return competitionManager.getCompetition(compId);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Competition updateCompetition(String userId, Competition comp) throws DatastoreException, UnauthorizedException, NotFoundException {
		Utility.ensureNotNull(userId, comp);
		return competitionManager.updateCompetition(userId, comp);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteCompetition(String userId, String compId) throws DatastoreException, UnauthorizedException, NotFoundException {
		Utility.ensureNotNull(userId, compId);
		competitionManager.deleteCompetition(userId, compId);
	}
	
	
	/* ***************************
	 * Administrator management
	 */
	public void getAdministrators(String userId, String compId) {
		
	}
	
	public void setAdministrators(String userId, String compId, Set<String> adminIds) {
		
	}
	
	public void addAdministrator(String userId, String compId, String idToAdd) {
		
	}
	
	public void removeAdministrator(String userId, String compId, String idToRemove) {
		
	}
	
	/* ***************************
	 * Participant management
	 */
	public Participant addParticipant(String userId, String compId, String idToAdd) {
		return null;
		
	}
	
	public void removeParticipant(String userId, String compId, String idToRemove) {
		
	}
	
	public Set<Participant> getAllParticipants(String userId, String compId) {
		return null;
		
	}
	
	/* ***************************
	 * Submission management
	 */ 
	public Submission addSubmission(String userId, String compId, String entityId) {
		
	}
	
}
