package org.sagebionetworks.competition.manager;

import org.sagebionetworks.competition.dao.SubmissionDAO;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.competition.model.Submission;
import org.sagebionetworks.competition.util.CompetitionUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class SubmissionManagerImpl {
	
	@Autowired
	SubmissionDAO submissionDAO;
	@Autowired
	CompetitionManager competitionManager;
	@Autowired
	ParticipantManager participantManager;
	
	// for testing purposes
	protected SubmissionManagerImpl(SubmissionDAO submissionDAO, 
			CompetitionManager competitionManager, ParticipantManager participantManager) {		
		this.submissionDAO = submissionDAO;
		this.competitionManager = competitionManager;
		this.participantManager = participantManager;
	}
	
	public Submission getSubmission(String submissionId) throws DatastoreException, NotFoundException {
		CompetitionUtils.ensureNotNull(submissionId, "Submission ID");
		return submissionDAO.get(submissionId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Submission createSubmission(String userId, Submission submission) throws NotFoundException {
		CompetitionUtils.ensureNotNull(userId, "User ID");
		CompetitionUtils.ensureNotNull(submission, "Submission ID");
		String compId = submission.getCompetitionId();
		submission.setId(userId);
		
		// ensure participant exists
		if (participantManager.getParticipant(userId, compId) == null)
			throw new NotFoundException("User ID: " + userId + 
					" has not joined Competition ID: " + compId);
		
		// ensure competition is open
		Competition comp = competitionManager.getCompetition(compId);
		CompetitionUtils.ensureCompetitionIsOpen(comp);
		
		// create and return the new Submission
		return submissionDAO.create(submission);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Submission updateSubmission(String userId, Submission submission) throws NotFoundException {
		CompetitionUtils.ensureNotNull(userId, "User ID");
		CompetitionUtils.ensureNotNull(submission, "Submission ID");
		String compId = submission.getCompetitionId();
		submission.setId(userId);
		
		// ensure Submission exists is owned by this user
		Submission old = getSubmission(submission.getId());
		if (!old.getUserId().equals(userId))
			throw new UnauthorizedException("Not authorized");
		
		// TODO: check eTag
		
		// ensure competition is open
		if (competitionManager.getCompetition(compId).getStatus() != CompetitionStatus.OPEN)
			throw new IllegalStateException("Competition ID: " + compId + " is not currently open");
			
		// create and return the new Submission
		return submissionDAO.create(submission);
	}
	

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteSubmission(String userId, String submissionId) throws DatastoreException, NotFoundException {
		CompetitionUtils.ensureNotNull(userId, submissionId);
		
		Submission sub = submissionDAO.get(submissionId);		
		Competition comp = competitionManager.getCompetition(sub.getCompetitionId());
		
		// verify access permission
		if ((!sub.getUserId().equals(userId)) && (!competitionManager.isCompAdmin(userId, comp))) {
			throw new UnauthorizedException("User ID: " + userId +
					" is not authorized to modify Submission ID: " + submissionId);
		}
		
		submissionDAO.delete(submissionId);
	}

}
