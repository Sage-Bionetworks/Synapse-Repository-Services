package org.sagebionetworks.evaluation.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.util.EvaluationUtils;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.EntityNameValidation;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class EvaluationManagerImpl implements EvaluationManager {
	
	@Autowired
	EvaluationDAO evaluationDAO;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private AuthorizationManager authorizationManager;
	
	public EvaluationManagerImpl() {}
	
	// Used for testing purposes
	protected EvaluationManagerImpl(AuthorizationManager authorizationManager, EvaluationDAO evaluationDAO, IdGenerator idGenerator) {
		this.authorizationManager = authorizationManager;
		this.evaluationDAO = evaluationDAO;
		this.idGenerator = idGenerator;
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Evaluation createEvaluation(UserInfo userInfo, Evaluation eval) 
			throws DatastoreException, InvalidModelException, NotFoundException {
		UserInfo.validateUserInfo(userInfo);
		String principalId = userInfo.getIndividualGroup().getId();
		eval.setName(EntityNameValidation.valdiateName(eval.getName()));
		
		// always generate a unique ID
		eval.setId(idGenerator.generateNewId().toString());
		
		// set creation date
		eval.setCreatedOn(new Date());
		
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
		List<Evaluation> evaluations = evaluationDAO.getInRange(limit, offset);
		long totalNumberOfResults = evaluationDAO.getCount();
		QueryResults<Evaluation> res = new QueryResults<Evaluation>(evaluations, totalNumberOfResults);
		return res;
	}
	
	@Override
	public QueryResults<Evaluation> getAvailableInRange(UserInfo userInfo, EvaluationStatus status, long limit, long offset) throws DatastoreException{
		List<Long> principalIds = new ArrayList<Long>(userInfo.getGroups().size());
		for (UserGroup g : userInfo.getGroups()) principalIds.add(Long.parseLong(g.getId()));
		List<Evaluation> evaluations = evaluationDAO.getAvailableInRange(principalIds, status, limit, offset);
		long totalNumberOfResults = evaluationDAO.getAvailableCount(principalIds, status);
		QueryResults<Evaluation> res = new QueryResults<Evaluation>(evaluations, totalNumberOfResults);
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
		
		Evaluation old = evaluationDAO.get(eval.getId());
		if (old == null) 
			throw new NotFoundException("No Evaluation found with id " + eval.getId());
		if (!old.getEtag().equals(eval.getEtag()))
			throw new IllegalArgumentException("Your copy of Evaluation " + eval.getId() + " is out of date. Please fetch it again before updating.");

		validateAccessPermission(userInfo, old, ACCESS_TYPE.UPDATE);
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
		Evaluation comp = evaluationDAO.get(id);
		if (comp == null) throw new NotFoundException("No Evaluation found with id " + id);
		validateAccessPermission(userInfo, comp, ACCESS_TYPE.DELETE);
		evaluationDAO.delete(id);
	}
		
	private void validateAccessPermission(UserInfo userInfo, Evaluation evaluation, ACCESS_TYPE accessType) throws NotFoundException {
		if (!authorizationManager.canAccess(userInfo, evaluation.getId(), ObjectType.EVALUATION, accessType))
			throw new UnauthorizedException("User ID " + userInfo.getIndividualGroup().getId() +
					" is not authorized to access Evaluation ID " + evaluation.getId() +
					" (" + evaluation.getName() + ")");
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
