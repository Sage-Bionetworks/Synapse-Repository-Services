package org.sagebionetworks.evaluation.manager;

import java.util.List;

import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.util.EvaluationUtils;
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

public class EvaluationManagerImpl implements EvaluationManager {
	
	@Autowired
	EvaluationDAO evaluationDAO;
	
	public EvaluationManagerImpl() {}
	
	// Used for testing purposes
	protected EvaluationManagerImpl(EvaluationDAO evaluationDAO) {
		this.evaluationDAO = evaluationDAO;
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Evaluation createEvaluation(UserInfo userInfo, Evaluation eval) 
			throws DatastoreException, InvalidModelException, NotFoundException {
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getIndividualGroup().getId();
		eval.setName(EntityNameValidation.valdiateName(eval.getName()));
		String id = evaluationDAO.create(eval, Long.parseLong(principalId));
		return evaluationDAO.get(id);
	}
	
	@Override
	public Evaluation getEvaluation(String id) throws DatastoreException, NotFoundException, UnauthorizedException {
		EvaluationUtils.ensureNotNull(id, "Evaluation ID");
		return evaluationDAO.get(id);
	}
	
	@Override
	public QueryResults<Evaluation> getInRange(long limit, long offset) throws DatastoreException, NotFoundException {
		List<Evaluation> Evaluations = evaluationDAO.getInRange(limit, offset);
		long totalNumberOfResults = evaluationDAO.getCount();
		QueryResults<Evaluation> res = new QueryResults<Evaluation>(Evaluations, totalNumberOfResults);
		return res;
	}
	
	@Override
	public long getCount() throws DatastoreException, NotFoundException {
		return evaluationDAO.getCount();
	}

	@Override
	public Evaluation findEvaluation(String name) throws DatastoreException, NotFoundException, UnauthorizedException {
		EvaluationUtils.ensureNotNull(name, "Name");
		String evalId = evaluationDAO.lookupByName(name);
		Evaluation comp = evaluationDAO.get(evalId);
		if (comp == null) throw new NotFoundException("No Evaluation found with name " + name);
		return comp;
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Evaluation updateEvaluation(UserInfo userInfo, Evaluation eval) throws DatastoreException, NotFoundException, UnauthorizedException, InvalidModelException, ConflictingUpdateException {
		EvaluationUtils.ensureNotNull(eval, "Evaluation");
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getIndividualGroup().getId();
		
		Evaluation old = evaluationDAO.get(eval.getId());
		if (old == null) 
			throw new NotFoundException("No Evaluation found with id " + eval.getId());
		if (!old.getEtag().equals(eval.getEtag()))
			throw new IllegalArgumentException("Your copy of Evaluation " + eval.getId() + " is out of date. Please fetch it again before updating.");

		validateAdminAccess(principalId, old);
		validateEvaluation(old, eval);		
		evaluationDAO.update(eval);
		return getEvaluation(eval.getId());
	}
	
	@Override
	public void updateEvaluationEtag(String evalId) throws NotFoundException {
		Evaluation comp = evaluationDAO.get(evalId);
		if (comp == null) throw new NotFoundException("No Evaluation found with id " + evalId);
		evaluationDAO.update(comp);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteEvaluation(UserInfo userInfo, String id) throws DatastoreException, NotFoundException, UnauthorizedException {
		EvaluationUtils.ensureNotNull(id, "Evaluation ID");
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getIndividualGroup().getId();
		Evaluation comp = evaluationDAO.get(id);
		if (comp == null) throw new NotFoundException("No Evaluation found with id " + id);
		validateAdminAccess(principalId, comp);
		evaluationDAO.delete(id);
	}
		
	@Override
	public boolean isEvalAdmin(String userId, String evalId) throws DatastoreException, UnauthorizedException, NotFoundException {
		EvaluationUtils.ensureNotNull(userId, "User ID");
		EvaluationUtils.ensureNotNull(evalId, "Evaluation ID");
		Evaluation comp = getEvaluation(evalId);
		return isCompAdmin(userId, comp);
	}
	
	private boolean isCompAdmin(String userId, Evaluation comp) {
		if (userId.equals(comp.getOwnerId())) return true;
		
		// TODO: check list of admins
		return false;
	}
	
	private void validateAdminAccess(String userId, Evaluation comp) {
		if (!isCompAdmin(userId, comp))
			throw new UnauthorizedException("User ID " + userId +
					" is not authorized to modify Evaluation ID " + comp.getId() +
					" (" + comp.getName() + ")");
	}
	
	private void validateEvaluation(Evaluation oldComp, Evaluation newComp) {
		if (!oldComp.getOwnerId().equals(newComp.getOwnerId()))
			throw new InvalidModelException("Cannot overwrite Evaluation Owner ID");
		if (!oldComp.getCreatedOn().equals(newComp.getCreatedOn()))
			throw new InvalidModelException("Cannot overwrite CreatedOn date");
		if (!oldComp.getEtag().equals(newComp.getEtag()))
			throw new InvalidModelException("Etag is invalid. Please fetch the Evaluation again.");
	}
}
