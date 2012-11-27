package org.sagebionetworks.competition.service;

import java.util.Set;

import org.sagebionetworks.competition.manager.CompetitionManager;
import org.sagebionetworks.competition.manager.ParticipantManager;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.competition.model.Submission;
import org.sagebionetworks.competition.util.CompetitionUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class CompetitionServiceImpl implements CompetitionService {
	
	@Autowired
	CompetitionManager competitionManager;
	
	@Autowired
	ParticipantManager participantManager;
	
	
	/* ***************************
	 * core Competition CRUD 
	 */
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Competition createCompetition(String userId, Competition comp) throws DatastoreException, NotFoundException {
		CompetitionUtils.ensureNotNull(userId, "User ID");
		String compId = competitionManager.createCompetition(userId, comp);
		return competitionManager.getCompetition(compId);
	}
	
	@Override
	public Competition getCompetition(String userId, String compId) throws DatastoreException, NotFoundException {
		CompetitionUtils.ensureNotNull(userId, compId);
		return competitionManager.getCompetition(compId);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Competition updateCompetition(String userId, Competition comp) throws DatastoreException, UnauthorizedException, NotFoundException {
		CompetitionUtils.ensureNotNull(userId, "User ID");
		CompetitionUtils.ensureNotNull(comp, "Competition");
		return competitionManager.updateCompetition(userId, comp);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteCompetition(String userId, String compId) throws DatastoreException, UnauthorizedException, NotFoundException {
		CompetitionUtils.ensureNotNull(userId, compId);
		competitionManager.deleteCompetition(userId, compId);
	}
	
	
	/* ***************************
	 * Administrator management
	 */

	@Override
	public void getAdministrators(String userId, String compId) {
		
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void setAdministrators(String userId, String compId, Set<String> adminIds) {
		
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void addAdministrator(String userId, String compId, String idToAdd) {
		
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeAdministrator(String userId, String compId, String idToRemove) {
		
	}
	
	/* ***************************
	 * Participant management
	 */

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Participant addParticipant(String userId, String compId, String idToAdd) {
		return null;
		
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeParticipant(String userId, String compId, String idToRemove) {
		
	}

	@Override
	public Set<Participant> getAllParticipants(String userId, String compId) {
		return null;		
	}
	
	/* ***************************
	 * Submission management
	 */ 

	@Override
	public Set<Submission> getAllSubmissions(String userId, String compId) {
		return null;
	}
	
	@Override
	public Set<Submission> getAllUnscoredSubmissions(String userId, String compId) {
		return null;
	}
	
	@Override
	public Set<Submission> getAllSubmissionsByUser(String userId, String participantId) {
		return null;
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Submission addSubmission(String userId, String compId, String entityId) {
		// check permissions on the attached entity
		return null;
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Submission updateSubmission(String userId, Submission submission) {
		return null;
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Submission deleteSubmission(String userId, String submissionId) {
		return null;
	}
}
