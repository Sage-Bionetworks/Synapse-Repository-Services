package org.sagebionetworks.competition.manager;

import java.util.List;

import org.sagebionetworks.competition.dao.SubmissionDAO;
import org.sagebionetworks.competition.dao.SubmissionStatusDAO;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.Submission;
import org.sagebionetworks.competition.model.SubmissionStatus;
import org.sagebionetworks.competition.model.SubmissionStatusEnum;
import org.sagebionetworks.competition.util.CompetitionUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class SubmissionManagerImpl implements SubmissionManager {
	
	@Autowired
	SubmissionDAO submissionDAO;
	@Autowired
	SubmissionStatusDAO submissionStatusDAO;
	@Autowired
	CompetitionManager competitionManager;
	@Autowired
	ParticipantManager participantManager;
	
	public SubmissionManagerImpl() {};
	
	// for testing purposes
	protected SubmissionManagerImpl(SubmissionDAO submissionDAO, SubmissionStatusDAO submissionStatusDAO,
			CompetitionManager competitionManager, ParticipantManager participantManager) {		
		this.submissionDAO = submissionDAO;
		this.submissionStatusDAO = submissionStatusDAO;
		this.competitionManager = competitionManager;
		this.participantManager = participantManager;
	}

	@Override
	public Submission getSubmission(String submissionId) throws DatastoreException, NotFoundException {
		CompetitionUtils.ensureNotNull(submissionId, "Submission ID");
		return submissionDAO.get(submissionId);
	}

	@Override
	public SubmissionStatus getSubmissionStatus(String submissionId) throws DatastoreException, NotFoundException {
		CompetitionUtils.ensureNotNull(submissionId, "Submission ID");
		return submissionStatusDAO.get(submissionId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Submission createSubmission(UserInfo userInfo, Submission submission) throws NotFoundException {
		CompetitionUtils.ensureNotNull(submission, "Submission ID");
		String compId = submission.getCompetitionId();
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getIndividualGroup().getId();
		
		submission.setUserId(principalId);
		
		// ensure participant exists
		if (participantManager.getParticipant(principalId, compId) == null)
			throw new NotFoundException("User Princpal ID: " + principalId + 
					" has not joined Competition ID: " + compId);
		
		// ensure competition is open
		Competition comp = competitionManager.getCompetition(compId);
		CompetitionUtils.ensureCompetitionIsOpen(comp);
		
		// create the Submission and an accompanying SubmissionStatus object
		String id = submissionDAO.create(submission);
		
		// create an accompanying SubmissionStatus object
		SubmissionStatus status = new SubmissionStatus();
		status.setId(id);
		status.setStatus(SubmissionStatusEnum.OPEN);
		submissionStatusDAO.create(status);
		
		// return the Submission
		return submissionDAO.get(id);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public SubmissionStatus updateSubmissionStatus(UserInfo userInfo, SubmissionStatus submissionStatus) throws NotFoundException {
		CompetitionUtils.ensureNotNull(submissionStatus, "SubmissionStatus");
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getIndividualGroup().getId();
		
		// ensure Submission exists and validate admin rights
		SubmissionStatus old = getSubmissionStatus(submissionStatus.getId());
		String compId = getSubmission(submissionStatus.getId()).getCompetitionId();
		if (!competitionManager.isCompAdmin(principalId, compId))
			throw new UnauthorizedException("Not authorized");
		
		if (!old.getEtag().equals(submissionStatus.getEtag()))
			throw new IllegalArgumentException("Your copy of SubmissionStatus " + 
					submissionStatus.getId() + " is out of date. Please fetch it again before updating.");
		
		// update and return the new Submission
		submissionStatusDAO.update(submissionStatus);
		return submissionStatusDAO.get(submissionStatus.getId());
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteSubmission(UserInfo userInfo, String submissionId) throws DatastoreException, NotFoundException {
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getIndividualGroup().getId();
		
		Submission sub = submissionDAO.get(submissionId);		
		String compId = sub.getCompetitionId();
		
		// verify access permission
		if (!competitionManager.isCompAdmin(principalId, compId)) {
			throw new UnauthorizedException("User ID: " + principalId +
					" is not authorized to modify Submission ID: " + submissionId);
		}
		
		// the associated SubmissionStatus object will be deleted via cascade
		submissionDAO.delete(submissionId);
	}

	@Override
	public List<Submission> getAllSubmissions(UserInfo userInfo, String compId, SubmissionStatusEnum status) throws DatastoreException, UnauthorizedException, NotFoundException {
		CompetitionUtils.ensureNotNull(compId, "Competition ID");
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getIndividualGroup().getId();
		
		if (!competitionManager.isCompAdmin(principalId, compId))
			throw new UnauthorizedException("User Principal ID" + principalId + " is not authorized to adminster Competition " + compId);
		if (status == null)		
			return submissionDAO.getAllByCompetition(compId);
		else
			return submissionDAO.getAllByCompetitionAndStatus(compId, status);
	}
	
	@Override
	public List<Submission> getAllSubmissionsByUser(String userId) throws DatastoreException, NotFoundException {
		return submissionDAO.getAllByUser(userId);
	}
	
	@Override
	public long getSubmissionCount(String compId) throws DatastoreException, NotFoundException {
		CompetitionUtils.ensureNotNull(compId, "Competition ID");
		return submissionDAO.getCountByCompetition(compId);
	}

}
