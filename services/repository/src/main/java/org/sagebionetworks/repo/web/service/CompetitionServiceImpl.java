package org.sagebionetworks.repo.web.service;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.competition.manager.CompetitionManager;
import org.sagebionetworks.competition.manager.ParticipantManager;
import org.sagebionetworks.competition.manager.SubmissionManager;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.competition.model.Submission;
import org.sagebionetworks.competition.model.SubmissionStatus;
import org.sagebionetworks.competition.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.QueryResults;
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
	@Autowired
	SubmissionManager submissionManager;
	@Autowired
	UserManager userManager;
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Competition createCompetition(String userId, Competition comp) 
			throws DatastoreException, InvalidModelException, NotFoundException {
		return competitionManager.createCompetition(userId, comp);
	}
	
	@Override
	public QueryResults<Competition> getCompetitionsInRange(long limit, long offset) 
			throws DatastoreException, NotFoundException {
		return competitionManager.getInRange(limit, offset);
	}

	@Override
	public long getCompetitionCount() throws DatastoreException, NotFoundException {
		return competitionManager.getCount();
	}

	@Override
	public Competition findCompetition(String name)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		return competitionManager.findCompetition(name);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Competition updateCompetition(String userId, Competition comp)
			throws DatastoreException, NotFoundException, UnauthorizedException,
			InvalidModelException, ConflictingUpdateException {
		return competitionManager.updateCompetition(userId, comp);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteCompetition(String userId, String id)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		competitionManager.deleteCompetition(userId, id);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Participant addParticipant(String userId, String compId,
			String idToAdd) throws NotFoundException {
		return participantManager.addParticipant(userId, compId, idToAdd);
	}

	@Override
	public Participant getParticipant(String userId, String compId)
			throws DatastoreException, NotFoundException {
		return participantManager.getParticipant(userId, compId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeParticipant(String userId, String compId,
			String idToRemove) throws DatastoreException, NotFoundException {
		participantManager.removeParticipant(userId, compId, idToRemove);
	}

	@Override
	public Set<Participant> getAllParticipants(String compId)
			throws NumberFormatException, DatastoreException, NotFoundException {
		return participantManager.getAllParticipants(compId);
	}

	@Override
	public long getParticipantCount(String compId)
			throws DatastoreException, NotFoundException {
		return participantManager.getNumberofParticipants(compId);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Submission createSubmission(String userId, Submission submission)
			throws NotFoundException {
		return submissionManager.createSubmission(userId, submission);
	}

	@Override
	public Submission getSubmission(String submissionId)
			throws DatastoreException, NotFoundException {
		return submissionManager.getSubmission(submissionId);
	}

	@Override
	public SubmissionStatus getSubmissionStatus(String submissionId)
			throws DatastoreException, NotFoundException {
		return submissionManager.getSubmissionStatus(submissionId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public SubmissionStatus updateSubmissionStatus(String userId,
			SubmissionStatus submissionStatus) throws NotFoundException {
		return submissionManager.updateSubmissionStatus(userId, submissionStatus);
	}

	@Override
	@Deprecated
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteSubmission(String userId, String submissionId)
			throws DatastoreException, NotFoundException {
		submissionManager.deleteSubmission(userId, submissionId);
	}

	@Override
	public List<Submission> getAllSubmissions(String userId, String compId,
			SubmissionStatusEnum status) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		return submissionManager.getAllSubmissions(userId, compId, status);
	}

	@Override
	public List<Submission> getAllSubmissionsByUser(String userId)
			throws DatastoreException, NotFoundException {
		return submissionManager.getAllSubmissionsByUser(userId);
	}

	@Override
	public long getSubmissionCount(String compId) throws DatastoreException,
			NotFoundException {
		return submissionManager.getSubmissionCount(compId);
	}

}
