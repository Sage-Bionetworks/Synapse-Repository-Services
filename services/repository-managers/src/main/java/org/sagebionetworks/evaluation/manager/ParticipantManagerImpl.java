package org.sagebionetworks.evaluation.manager;

import static org.sagebionetworks.repo.model.ACCESS_TYPE.DELETE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.PARTICIPATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE;

import java.util.Date;
import java.util.List;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.util.EvaluationUtils;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.evaluation.EvaluationDAO;
import org.sagebionetworks.repo.model.evaluation.ParticipantDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import org.sagebionetworks.repo.transactions.WriteTransaction;

public class ParticipantManagerImpl implements ParticipantManager {

	@Autowired
	private EvaluationDAO evaluationDAO;
	@Autowired
	private ParticipantDAO participantDAO;
	@Autowired
	private UserManager userManager;
	@Autowired
	private EvaluationManager evaluationManager;
	@Autowired
	private EvaluationPermissionsManager evaluationPermissionsManager;

	@Override
	public Participant getParticipant(UserInfo userInfo, final String participantId, final String evalId)
			throws DatastoreException, NotFoundException {

		if (userInfo == null) {
			throw new IllegalArgumentException("User info cannot be null or empty.");
		}
		if (participantId == null || participantId.isEmpty()) {
			throw new IllegalArgumentException("Participant user ID cannot be null or empty.");
		}
		if (evalId == null || evalId.isEmpty()) {
			throw new IllegalArgumentException("Evaluation ID cannot be null or empty.");
		}

		String userId = userInfo.getId().toString();
		if (!userId.equals(participantId)) {
			if (!evaluationPermissionsManager.hasAccess(userInfo, evalId, UPDATE).getAuthorized()) {
				throw new UnauthorizedException("User " + userId +
						" not allowed to read participant " + participantId);
			}
		}

		return participantDAO.get(participantId, evalId);
	}

	@Override
	@WriteTransaction
	public Participant addParticipant(UserInfo userInfo, String evalId) throws NotFoundException {
		EvaluationUtils.ensureNotNull(evalId, "Evaluation ID");

		if (!evaluationPermissionsManager.hasAccess(userInfo, evalId, PARTICIPATE).getAuthorized()) {
			throw new UnauthorizedException("User " + userInfo.getId().toString() +
					" is not allowed to join evaluation " + evalId);
		}
		Evaluation eval = evaluationDAO.get(evalId);
		try {
			EvaluationUtils.ensureEvaluationIsOpen(eval);
		} catch (IllegalStateException e) {
			throw new UnauthorizedException("Cannot join Evaluation ID " + evalId + " which is not currently OPEN.");
		}
		UserInfo.validateUserInfo(userInfo);
		String principalIdToAdd = userInfo.getId().toString();
		
		// create the new Participant
		Participant part = new Participant();
		part.setEvaluationId(evalId);
		part.setUserId(principalIdToAdd);
		part.setCreatedOn(new Date());
		participantDAO.create(part);
		
		// trigger etag update of the parent Evaluation
		// this is required for migration consistency
		evaluationManager.updateEvaluationEtag(evalId);
		
		return participantDAO.get(principalIdToAdd, evalId);
	}

	@Override
	@WriteTransaction
	public void removeParticipant(UserInfo userInfo, String evalId, String idToRemove)
			throws DatastoreException, NotFoundException {

		EvaluationUtils.ensureNotNull(evalId, "Evaluation ID");
		EvaluationUtils.ensureNotNull(idToRemove, "Participant User ID");
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getId().toString();

		// verify permissions
		if (!evaluationPermissionsManager.hasAccess(userInfo, evalId, DELETE).getAuthorized()) {
			EvaluationUtils.ensureEvaluationIsOpen(evaluationDAO.get(evalId));
			throw new UnauthorizedException("User Principal ID: " + principalId +
					" is not authorized to remove other users from Evaluation ID: " + evalId);
		}

		// trigger etag update of the parent Evaluation
		// this is required for migration consistency
		evaluationManager.updateEvaluationEtag(evalId);
		
		participantDAO.delete(idToRemove, evalId);
	}
	
	@Override
	public QueryResults<Participant> getAllParticipants(
			UserInfo userInfo, final String evalId, final long limit, final long offset)
			throws NumberFormatException, DatastoreException, NotFoundException {
		validateUpdateAccess(userInfo, evalId);
		List<Participant> participants = participantDAO.getAllByEvaluation(evalId, limit, offset);
		long totalNumberOfResults = participantDAO.getCountByEvaluation(evalId);
		QueryResults<Participant> res = new QueryResults<Participant>(participants, totalNumberOfResults);
		return res;
	}

	@Override
	public long getNumberofParticipants(UserInfo userInfo, final String evalId)
			throws DatastoreException, NotFoundException {
		validateUpdateAccess(userInfo, evalId);
		return participantDAO.getCountByEvaluation(evalId);
	}

	private void validateUpdateAccess(final UserInfo userInfo, final String evalId)
			throws DatastoreException, NotFoundException {

		if (userInfo == null) {
			throw new IllegalArgumentException("User info cannot be null or empty.");
		}
		if (evalId == null || evalId.isEmpty()) {
			throw new IllegalArgumentException("Evaluation ID cannot be null or empty.");
		}

		if (!evaluationPermissionsManager.hasAccess(userInfo, evalId, UPDATE).getAuthorized()) {
			throw new UnauthorizedException("User " + userInfo.getId().toString() +
						" not allowed to get participants for evaluation " + evalId);
		}
	}
}
