package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.RestricableODUtil;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class AccessRequirementManagerImpl implements AccessRequirementManager {
	
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;		
	@Autowired
	private AccessApprovalDAO accessApprovalDAO;	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private AuthorizationManager authorizationManager;
	
	@Autowired
	NodeDAO nodeDAO;

	@Autowired
	EvaluationDAO evaluationDAO;

	public static void validateAccessRequirement(AccessRequirement a) throws InvalidModelException {
		if (a.getEntityType()==null ||
				a.getAccessType()==null ||
				a.getSubjectIds()==null) throw new InvalidModelException();
		
		if (!a.getEntityType().equals(a.getClass().getName())) throw new InvalidModelException("entity type differs from class");
	}
	
	public static void populateCreationFields(UserInfo userInfo, AccessRequirement a) {
		Date now = new Date();
		a.setCreatedBy(userInfo.getIndividualGroup().getId());
		a.setCreatedOn(now);
		a.setModifiedBy(userInfo.getIndividualGroup().getId());
		a.setModifiedOn(now);
	}

	public static void populateModifiedFields(UserInfo userInfo, AccessRequirement a) {
		Date now = new Date();
		a.setCreatedBy(null); // by setting to null we are telling the DAO to use the current values
		a.setCreatedOn(null);
		a.setModifiedBy(userInfo.getIndividualGroup().getId());
		a.setModifiedOn(now);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends AccessRequirement> T createAccessRequirement(UserInfo userInfo, T accessRequirement) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, ForbiddenException {
		validateAccessRequirement(accessRequirement);
		 Map<RestrictableObjectType, Collection<String>> sortedIds = 
			 RestricableODUtil.sortByType(accessRequirement.getSubjectIds());
		Collection<String> entityIds = sortedIds.get(RestrictableObjectType.ENTITY);
		if (entityIds!=null && entityIds.size()>0) {
			ACTUtils.verifyACTTeamMembershipOrCanCreateOrEdit(userInfo,  entityIds, userGroupDAO, authorizationManager);
		}
		Collection<String> evaluationIds = sortedIds.get(RestrictableObjectType.EVALUATION);
		if (evaluationIds!=null && evaluationIds.size()>0) {
			verifyCanAdministerEvaluation(userInfo, evaluationIds, evaluationDAO);
		}
		populateCreationFields(userInfo, accessRequirement);
		return accessRequirementDAO.create(accessRequirement);
	}
	
	public static void verifyCanAdministerEvaluation(
			UserInfo userInfo, 
			Collection<String> evaluationIds, 
			EvaluationDAO evaluationDAO) throws NotFoundException, ForbiddenException {
		if (userInfo.isAdmin()) return;
		for (String id : evaluationIds) {
			Evaluation evaluation = evaluationDAO.get(id);
			if (!EvaluationUtil.canAdminister(evaluation, userInfo)) {
				throw new ForbiddenException("You lack administrative access to Evaluation: "+evaluation.getName());
			}
		}
		
	}
	
	@Override
	public QueryResults<AccessRequirement> getAccessRequirementsForSubject(UserInfo userInfo, RestrictableObjectDescriptor subjectId) throws DatastoreException, NotFoundException, ForbiddenException {
		List<AccessRequirement> ars = accessRequirementDAO.getForSubject(subjectId);
		QueryResults<AccessRequirement> result = new QueryResults<AccessRequirement>(ars, ars.size());
		return result;
	}
	
	@Override
	public QueryResults<AccessRequirement> getUnmetAccessRequirements(UserInfo userInfo, RestrictableObjectDescriptor subjectId) throws DatastoreException, NotFoundException {
		// first check if there *are* any unmet requirements.  (If not, no further queries will be executed.)
		List<Long> unmetIds = AccessRequirementUtil.unmetAccessRequirementIds(
				userInfo, subjectId, ACCESS_TYPE.DOWNLOAD, nodeDAO, evaluationDAO, accessRequirementDAO); // TODO make access type a param
		
		List<AccessRequirement> unmetRequirements = new ArrayList<AccessRequirement>();
		// if there are any unmet requirements, retrieve the object(s)
		if (!unmetIds.isEmpty()) {
			List<AccessRequirement> allRequirementsForSubject = accessRequirementDAO.getForSubject(subjectId);
			for (Long unmetId : unmetIds) { // typically there will be just one id here
				for (AccessRequirement ar : allRequirementsForSubject) { // typically there will be just one id here
					if (ar.getId().equals(unmetId)) unmetRequirements.add(ar);
				}
			}
		}
		QueryResults<AccessRequirement> result = new QueryResults<AccessRequirement>(unmetRequirements, (int)unmetRequirements.size());
		return result;
	}	
	
	/**
	 * For Entities, check that user is an administrator or ACT member. 
	 * For Evaluations, check that user is an administrator or is the creator of the Evaluation
	 * @param userInfo
	 * @param accessRequirement
	 * @throws NotFoundException
	 */
	private void verifyCanAdmin(UserInfo userInfo, AccessRequirement accessRequirement) throws NotFoundException {
		Map<RestrictableObjectType, Collection<String>> sortedIds = 
			 RestricableODUtil.sortByType(accessRequirement.getSubjectIds());
		Collection<String> entityIds = sortedIds.get(RestrictableObjectType.ENTITY);
		if (entityIds!=null && !entityIds.isEmpty()) {
			ACTUtils.verifyACTTeamMembershipOrIsAdmin(userInfo, userGroupDAO);
		}
		Collection<String> evaluationIds = sortedIds.get(RestrictableObjectType.EVALUATION);
		if (evaluationIds!=null && !evaluationIds.isEmpty()) {
			verifyCanAdministerEvaluation(userInfo, evaluationIds, evaluationDAO);
		}		
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends AccessRequirement> T updateAccessRequirement(UserInfo userInfo, T accessRequirement) throws NotFoundException, UnauthorizedException, ConflictingUpdateException, InvalidModelException, ForbiddenException, DatastoreException {
		validateAccessRequirement(accessRequirement);
		verifyCanAdmin(userInfo, accessRequirement);
		populateModifiedFields(userInfo, accessRequirement);
		return accessRequirementDAO.update(accessRequirement);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteAccessRequirement(UserInfo userInfo,
			String accessRequirementId) throws NotFoundException,
			DatastoreException, UnauthorizedException, ForbiddenException {
		AccessRequirement accessRequirement = accessRequirementDAO.get(accessRequirementId);
		verifyCanAdmin(userInfo, accessRequirement);
		accessRequirementDAO.delete(accessRequirement.getId().toString());
	}
}
