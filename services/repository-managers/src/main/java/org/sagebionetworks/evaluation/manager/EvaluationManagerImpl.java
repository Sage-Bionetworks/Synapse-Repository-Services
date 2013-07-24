package org.sagebionetworks.evaluation.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.util.EvaluationUtils;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.EntityNameValidation;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class EvaluationManagerImpl implements EvaluationManager {

	@Autowired
	private EvaluationDAO evaluationDAO;

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private AuthorizationManager authorizationManager;

	@Autowired
	private EvaluationPermissionsManager evaluationPermissionsManager;

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Evaluation createEvaluation(UserInfo userInfo, Evaluation eval) 
			throws DatastoreException, InvalidModelException, NotFoundException {

		UserInfo.validateUserInfo(userInfo);

		final String nodeId = eval.getContentSource();
		if (nodeId == null || nodeId.isEmpty()) {
			throw new IllegalArgumentException("Evaluation " + eval.getId() +
					" is missing content source (are you sure there is Synapse entity for it?).");
		}
		if (!authorizationManager.canAccess(userInfo, nodeId, ACCESS_TYPE.CREATE)) {
			throw new UnauthorizedException("User " + userInfo.getIndividualGroup().getId() +
					" must have " + ACCESS_TYPE.CREATE.name() + " right on the entity " +
					nodeId + " in order to create a evaluation based on it.");
		}

		// Create the evaluation
		eval.setName(EntityNameValidation.valdiateName(eval.getName()));
		eval.setId(idGenerator.generateNewId(TYPE.DOMAIN_IDS).toString());
		eval.setCreatedOn(new Date());
		String principalId = userInfo.getIndividualGroup().getId();
		String id = evaluationDAO.create(eval, Long.parseLong(principalId));

		// Create the default ACL
		AccessControlList acl = createDefaultAcl(userInfo, eval.getId());
		evaluationPermissionsManager.createAcl(userInfo, acl);

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
	public Evaluation updateEvaluation(UserInfo userInfo, Evaluation eval)
			throws DatastoreException, NotFoundException, UnauthorizedException {

		EvaluationUtils.ensureNotNull(eval, "Evaluation");
		UserInfo.validateUserInfo(userInfo);

		final String evalId = eval.getId();
		Evaluation old = evaluationDAO.get(evalId);
		if (old == null) {
			throw new NotFoundException("No Evaluation found with id " + eval.getId());
		}
		if (!old.getEtag().equals(eval.getEtag())) {
			// NOTE: we have not yet locked the DB row for update; this will occur at the DAO layer.			
			// This check is a performance optimization (in the interest of fail-fast behavior).
			throw new ConflictingUpdateException("Your copy of Evaluation " + eval.getId() +
					" is out of date. Please fetch it again before updating.");
		}

		if (!evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.UPDATE)) {
			throw new UnauthorizedException("User " + userInfo.getIndividualGroup().getId() +
					" is not authorized to update evaluation " + evalId +
					" (" + eval.getName() + ")");
		}

		validateEvaluation(old, eval);

		evaluationDAO.update(eval);
		return getEvaluation(evalId);
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
		Evaluation eval = evaluationDAO.get(id);
		if (eval == null) throw new NotFoundException("No Evaluation found with id " + id);
		if (!evaluationPermissionsManager.hasAccess(userInfo, id, ACCESS_TYPE.DELETE)) {
			throw new UnauthorizedException("User " + userInfo.getIndividualGroup().getId() +
					" is not authorized to update evaluation " + id +
					" (" + eval.getName() + ")");
		}
		evaluationPermissionsManager.deleteAcl(userInfo, id);
		evaluationDAO.delete(id);
	}

	private void validateEvaluation(Evaluation oldComp, Evaluation newComp) {
		if (!oldComp.getOwnerId().equals(newComp.getOwnerId()))
			throw new InvalidModelException("Cannot overwrite Evaluation Owner ID");
		if (!oldComp.getCreatedOn().equals(newComp.getCreatedOn()))
			throw new InvalidModelException("Cannot overwrite CreatedOn date");
		if (!oldComp.getEtag().equals(newComp.getEtag()))
			throw new InvalidModelException("Etag is invalid. Please fetch the Evaluation again.");
	}

	private AccessControlList createDefaultAcl(final UserInfo creator, final String evalId) {

		Set<ACCESS_TYPE> accessSet = new HashSet<ACCESS_TYPE>(12);
		accessSet.add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		accessSet.add(ACCESS_TYPE.CREATE);
		accessSet.add(ACCESS_TYPE.DELETE);
		accessSet.add(ACCESS_TYPE.READ);
		accessSet.add(ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
		accessSet.add(ACCESS_TYPE.UPDATE);

		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(accessSet);
		String userId = creator.getIndividualGroup().getId();
		ra.setPrincipalId(Long.parseLong(userId));

		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		raSet.add(ra);

		AccessControlList acl = new AccessControlList();
		acl.setId(evalId);
		acl.setCreationDate(new Date());
		acl.setResourceAccess(raSet);

		return acl;
	}
}
