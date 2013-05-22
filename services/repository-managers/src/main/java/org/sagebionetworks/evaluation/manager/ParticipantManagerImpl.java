package org.sagebionetworks.evaluation.manager;

import java.util.Date;
import java.util.List;

import org.sagebionetworks.evaluation.dao.ParticipantDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.util.EvaluationUtils;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class ParticipantManagerImpl implements ParticipantManager {
	
	@Autowired
	ParticipantDAO participantDAO;
	@Autowired
	UserManager userManager;
	@Autowired
	EvaluationManager evaluationManager;
	@Autowired
	AuthorizationManager authorizationManager;
	
	public ParticipantManagerImpl() {};
	
	// for testing purposes
	protected ParticipantManagerImpl(ParticipantDAO participantDAO, 
			UserManager userManager, EvaluationManager EvaluationManager, AuthorizationManager authorizationManager) {		
		this.participantDAO = participantDAO;
		this.userManager = userManager;
		this.evaluationManager = EvaluationManager;
		this.authorizationManager = authorizationManager;
	}
	
	@Override
	public Participant getParticipant(String userId, String evalId) throws DatastoreException, NotFoundException {
		EvaluationUtils.ensureNotNull(userId, evalId);
		return participantDAO.get(userId, evalId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Participant addParticipant(UserInfo userInfo, String evalId) throws NotFoundException {
		EvaluationUtils.ensureNotNull(evalId, "Evaluation ID");
		Evaluation eval = evaluationManager.getEvaluation(evalId);
		boolean canParticipate = authorizationManager.canAccess(userInfo, evalId, ObjectType.EVALUATION, ACCESS_TYPE.PARTICIPATE);
		if (!canParticipate) {
			throw new UnauthorizedException("Not allowed to participate in "+eval.getName());
		}
		try {
			EvaluationUtils.ensureEvaluationIsOpen(eval);
		} catch (IllegalStateException e) {
			throw new UnauthorizedException("Cannot join Evaluation ID " + evalId + " which is not currently OPEN.");
		}
		UserInfo.validateUserInfo(userInfo);
		String principalIdToAdd = userInfo.getIndividualGroup().getId();
		
		// create the new Participant
		Participant part = new Participant();
		part.setEvaluationId(evalId);
		part.setUserId(principalIdToAdd);
		part.setCreatedOn(new Date());
		participantDAO.create(part);
		
		// trigger etag update of the parent Evaluation
		// this is required for migration consistency
		evaluationManager.updateEvaluationEtag(evalId);
		
		return getParticipant(principalIdToAdd, evalId);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Participant addParticipantAsAdmin(UserInfo userInfo, String evalId, String principalIdToAdd) throws NotFoundException {
		EvaluationUtils.ensureNotNull(evalId, "Evaluation ID");
		EvaluationUtils.ensureNotNull(principalIdToAdd, "Principal ID");
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getIndividualGroup().getId();
		
		// verify permissions
		Evaluation eval = evaluationManager.getEvaluation(evalId);

		// add other user (requires admin rights)
		if (!evaluationManager.isEvalAdmin(userInfo, evalId)) {
			EvaluationUtils.ensureEvaluationIsOpen(eval);
			throw new UnauthorizedException("User Principal ID: " + principalId + " is not authorized to add other users to Evaluation ID: " + evalId);
		}
			
		// create the new Participant
		Participant part = new Participant();
		part.setEvaluationId(evalId);
		part.setUserId(principalIdToAdd);
		part.setCreatedOn(new Date());
		participantDAO.create(part);
		
		// trigger etag update of the parent Evaluation
		// this is required for migration consistency
		evaluationManager.updateEvaluationEtag(evalId);
		
		return getParticipant(principalIdToAdd, evalId);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeParticipant(UserInfo userInfo, String evalId, String idToRemove) throws DatastoreException, NotFoundException {
		EvaluationUtils.ensureNotNull(evalId, "Evaluation ID");
		EvaluationUtils.ensureNotNull(idToRemove, "Participant User ID");
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getIndividualGroup().getId();
		
		// verify permissions
		if (!evaluationManager.isEvalAdmin(userInfo, evalId)) {
			// user is not an admin; only authorized to cancel their own participation
			EvaluationUtils.ensureEvaluationIsOpen(evaluationManager.getEvaluation(evalId));
			if (!principalId.equals(idToRemove))
				throw new UnauthorizedException("User Principal ID: " + principalId + " is not authorized to remove other users from Evaluation ID: " + evalId);
		}
		
		// trigger etag update of the parent Evaluation
		// this is required for migration consistency
		evaluationManager.updateEvaluationEtag(evalId);
		
		participantDAO.delete(idToRemove, evalId);
	}
	
	@Override
	public QueryResults<Participant> getAllParticipants(String evalId, long limit, long offset) throws NumberFormatException, DatastoreException, NotFoundException {
		EvaluationUtils.ensureNotNull(evalId, "Evaluation ID");
		List<Participant> participants = participantDAO.getAllByEvaluation(evalId, limit, offset);
		long totalNumberOfResults = participantDAO.getCountByEvaluation(evalId);
		QueryResults<Participant> res = new QueryResults<Participant>(participants, totalNumberOfResults);
		return res;
	}
	
	@Override
	public long getNumberofParticipants(String evalId) throws DatastoreException, NotFoundException {
		EvaluationUtils.ensureNotNull(evalId, "Evaluation ID");
		return participantDAO.getCountByEvaluation(evalId);
	}
}
