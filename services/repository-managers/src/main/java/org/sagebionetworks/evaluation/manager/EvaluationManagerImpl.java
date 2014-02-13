package org.sagebionetworks.evaluation.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.util.EvaluationUtils;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
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
		if (!authorizationManager.canAccess(userInfo, nodeId, ObjectType. ENTITY, ACCESS_TYPE.CREATE)) {
			throw new UnauthorizedException("User " + userInfo.getId().toString() +
					" must have " + ACCESS_TYPE.CREATE.name() + " right on the entity " +
					nodeId + " in order to create a evaluation based on it.");
		}

		// Create the evaluation
		eval.setName(EntityNameValidation.valdiateName(eval.getName()));
		eval.setId(idGenerator.generateNewId(TYPE.DOMAIN_IDS).toString());
		eval.setCreatedOn(new Date());
		String principalId = userInfo.getId().toString();
		String id = evaluationDAO.create(eval, Long.parseLong(principalId));

		// Create the default ACL
		AccessControlList acl = createDefaultAcl(userInfo, eval.getId());
		evaluationPermissionsManager.createAcl(userInfo, acl);

		return evaluationDAO.get(id);
	}

	@Override
	public Evaluation getEvaluation(UserInfo userInfo, String id)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		EvaluationUtils.ensureNotNull(id, "Evaluation ID");
		if (!evaluationPermissionsManager.hasAccess(userInfo, id, ACCESS_TYPE.READ)) {
			throw new UnauthorizedException("User " + userInfo.getId().toString() +
					" not allowed to read evaluation " + id);
		}
		return evaluationDAO.get(id);
	}
	
	@Override
	public QueryResults<Evaluation> getEvaluationByContentSource(UserInfo userInfo, String id, long limit, long offset)
			throws DatastoreException, NotFoundException {
		EvaluationUtils.ensureNotNull(id, "Entity ID");
		if (userInfo == null) {
			throw new IllegalArgumentException("User info cannot be null.");
		}
		
		List<Evaluation> evalList = evaluationDAO.getByContentSource(id, limit, offset);
		List<Evaluation> evaluations = new ArrayList<Evaluation>();
		for (Evaluation eval : evalList) {
			if (evaluationPermissionsManager.hasAccess(userInfo, eval.getId(), ACCESS_TYPE.READ)) {
				evaluations.add(eval);
			}
		}
		long totalNumberOfResults = evaluationDAO.getCountByContentSource(id);
		return new QueryResults<Evaluation>(evaluations, totalNumberOfResults);
	}

	@Deprecated
	@Override
	public QueryResults<Evaluation> getInRange(UserInfo userInfo, long limit, long offset)
			throws DatastoreException, NotFoundException {
		List<Evaluation> evalList = evaluationDAO.getInRange(limit, offset);
		List<Evaluation> evaluations = new ArrayList<Evaluation>();
		for (Evaluation eval : evalList) {
			if (evaluationPermissionsManager.hasAccess(userInfo, eval.getId(), ACCESS_TYPE.READ)) {
				evaluations.add(eval);
			}
		}
		long totalNumberOfResults = evaluationDAO.getCount();
		QueryResults<Evaluation> res = new QueryResults<Evaluation>(evaluations, totalNumberOfResults);
		return res;
	}

	@Override
	public QueryResults<Evaluation> getAvailableInRange(UserInfo userInfo, long limit, long offset)
			throws DatastoreException, NotFoundException {
		List<Long> principalIds = new ArrayList<Long>(userInfo.getGroups().size());
		for (Long g : userInfo.getGroups()) {
			principalIds.add(g);
		}
		List<Evaluation> evalList = evaluationDAO.getAvailableInRange(principalIds, limit, offset);
		long totalNumberOfResults = evaluationDAO.getAvailableCount(principalIds);
		QueryResults<Evaluation> res = new QueryResults<Evaluation>(evalList, totalNumberOfResults);
		return res;
	}

	@Deprecated
	@Override
	public long getCount(UserInfo userInfo) throws DatastoreException, NotFoundException {
		if (!userInfo.isAdmin()) {
			throw new UnauthorizedException("Must be an administrator.");
		}
		return evaluationDAO.getCount();
	}

	@Override
	public Evaluation findEvaluation(UserInfo userInfo, String name)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		EvaluationUtils.ensureNotNull(name, "Name");
		String evalId = evaluationDAO.lookupByName(name);
		Evaluation eval = evaluationDAO.get(evalId);
		if (!evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.READ)) {
			eval = null;
		}
		if (eval == null) {
			throw new NotFoundException("No Evaluation found with name " + name);
		}
		return eval;
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Evaluation updateEvaluation(UserInfo userInfo, Evaluation eval)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		// validate arguments
		EvaluationUtils.ensureNotNull(eval, "Evaluation");
		UserInfo.validateUserInfo(userInfo);
		final String evalId = eval.getId();
		
		// validate permissions
		if (!evaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.UPDATE)) {
			throw new UnauthorizedException("User " + userInfo.getId().toString() +
					" is not authorized to update evaluation " + evalId +
					" (" + eval.getName() + ")");
		}

		// fetch the existing Evaluation and validate changes		
		Evaluation old = evaluationDAO.get(evalId);
		if (old == null) {
			throw new NotFoundException("No Evaluation found with id " + eval.getId());
		}
		validateEvaluation(old, eval);

		// perform the update
		evaluationDAO.update(eval);
		return evaluationDAO.get(evalId);
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
			throw new UnauthorizedException("User " + userInfo.getId().toString() +
					" is not authorized to update evaluation " + id +
					" (" + eval.getName() + ")");
		}
		evaluationPermissionsManager.deleteAcl(userInfo, id);
		evaluationDAO.delete(id);
	}

	private static void validateEvaluation(Evaluation oldEval, Evaluation newEval) {
		if (!oldEval.getOwnerId().equals(newEval.getOwnerId())) {
			throw new InvalidModelException("Cannot overwrite Evaluation Owner ID");
		}
		if (!oldEval.getCreatedOn().equals(newEval.getCreatedOn())) {
			throw new InvalidModelException("Cannot overwrite CreatedOn date");
		}
	}

	private static AccessControlList createDefaultAcl(final UserInfo creator, final String evalId) {

		Set<ACCESS_TYPE> accessSet = new HashSet<ACCESS_TYPE>(12);
		accessSet.add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		accessSet.add(ACCESS_TYPE.CREATE);
		accessSet.add(ACCESS_TYPE.READ);
		accessSet.add(ACCESS_TYPE.UPDATE);
		accessSet.add(ACCESS_TYPE.DELETE);
		accessSet.add(ACCESS_TYPE.PARTICIPATE);
		accessSet.add(ACCESS_TYPE.SUBMIT);
		accessSet.add(ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
		accessSet.add(ACCESS_TYPE.UPDATE_SUBMISSION);
		accessSet.add(ACCESS_TYPE.DELETE_SUBMISSION);

		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(accessSet);
		String userId = creator.getId().toString();
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
