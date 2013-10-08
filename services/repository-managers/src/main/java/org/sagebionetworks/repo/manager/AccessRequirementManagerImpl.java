package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.util.jrjc.JRJCHelper;
import org.sagebionetworks.repo.util.jrjc.JiraClient;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class AccessRequirementManagerImpl implements AccessRequirementManager {
	
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;		

	@Autowired
	private AuthorizationManager authorizationManager;
	
	@Autowired
	NodeDAO nodeDAO;

	@Autowired
	EvaluationDAO evaluationDAO;

	@Autowired
	private JiraClient jiraClient;
	
	public AccessRequirementManagerImpl() {}
	
	// for testing 
	public AccessRequirementManagerImpl(
			AccessRequirementDAO accessRequirementDAO,
			AuthorizationManager authorizationManager,
			JiraClient jiraClient
	) {
		this.accessRequirementDAO=accessRequirementDAO;
		this.authorizationManager=authorizationManager;
		this.jiraClient=jiraClient;
	}
	
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
	
	// TODO Once the web client switches over to use 'createLockAccessRequirement', restrict access here to ACT
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends AccessRequirement> T createAccessRequirement(UserInfo userInfo, T accessRequirement) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException {
		validateAccessRequirement(accessRequirement);
		if (!authorizationManager.canCreateAccessRequirement(userInfo, accessRequirement)) {
			throw new UnauthorizedException();
		}
		populateCreationFields(userInfo, accessRequirement);
		return accessRequirementDAO.create(accessRequirement);
	}
	
	public static ACTAccessRequirement newLockAccessRequirement(UserInfo userInfo, String entityId) {
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(entityId);
		subjectId.setType(RestrictableObjectType.ENTITY);
		// create the 'lock down' access requirement'
		ACTAccessRequirement accessRequirement = new ACTAccessRequirement();
		accessRequirement.setEntityType("org.sagebionetworks.repo.model.ACTAccessRequirement");
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		accessRequirement.setActContactInfo("Access restricted pending review by Synapse Access and Compliance Team.");
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{subjectId}));
		populateCreationFields(userInfo, accessRequirement);
		return accessRequirement;
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ACTAccessRequirement createLockAccessRequirement(UserInfo userInfo, String entityId) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException {
		// check authority
		if (!(authorizationManager.canAccess(userInfo, entityId, ObjectType. ENTITY, ACCESS_TYPE.CREATE) &&
				authorizationManager.canAccess(userInfo, entityId, ObjectType. ENTITY, ACCESS_TYPE.UPDATE))) 
			throw new UnauthorizedException();
		
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(entityId);
		subjectId.setType(RestrictableObjectType.ENTITY);

		// check whether there is already an access requirement in place
		List<AccessRequirement> ars = accessRequirementDAO.getForSubject(subjectId);
		if (!ars.isEmpty()) throw new IllegalArgumentException("Entity "+entityId+" is already restricted.");
		
		ACTAccessRequirement accessRequirement = newLockAccessRequirement(userInfo, entityId);
		ACTAccessRequirement result  = accessRequirementDAO.create(accessRequirement);
		
		// now create the Jira issue
		JRJCHelper.createRestrictIssue(jiraClient, 
				userInfo.getIndividualGroup().getId(), 
				userInfo.getIndividualGroup().getName(), 
				entityId);

		return result;
	}
	
	@Override
	public QueryResults<AccessRequirement> getAccessRequirementsForSubject(UserInfo userInfo, RestrictableObjectDescriptor subjectId) throws DatastoreException, NotFoundException {
		List<AccessRequirement> ars = accessRequirementDAO.getForSubject(subjectId);
		QueryResults<AccessRequirement> result = new QueryResults<AccessRequirement>(ars, ars.size());
		return result;
	}
	
	@Override
	public QueryResults<AccessRequirement> getUnmetAccessRequirements(UserInfo userInfo, RestrictableObjectDescriptor subjectId) throws DatastoreException, NotFoundException {
		// first check if there *are* any unmet requirements.  (If not, no further queries will be executed.)
		List<Long> unmetIds = AccessRequirementUtil.unmetAccessRequirementIds(
				userInfo, subjectId, nodeDAO, accessRequirementDAO);
		
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
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends AccessRequirement> T updateAccessRequirement(UserInfo userInfo, T accessRequirement) throws NotFoundException, UnauthorizedException, ConflictingUpdateException, InvalidModelException, DatastoreException {
		validateAccessRequirement(accessRequirement);
		verifyCanAccess(userInfo, accessRequirement.getId().toString(), ACCESS_TYPE.UPDATE);
		populateModifiedFields(userInfo, accessRequirement);
		return accessRequirementDAO.update(accessRequirement);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteAccessRequirement(UserInfo userInfo,
			String accessRequirementId) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		verifyCanAccess(userInfo, accessRequirementId, ACCESS_TYPE.DELETE);
		accessRequirementDAO.delete(accessRequirementId);
	}
	
	private void verifyCanAccess(UserInfo userInfo, String accessRequirementId, ACCESS_TYPE accessType) throws UnauthorizedException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, accessRequirementId, ObjectType.ACCESS_REQUIREMENT, accessType)) {
			throw new UnauthorizedException("Unauthorized for "+accessType+" access to AccessRequirement"+accessRequirementId);
		}
	}
}
