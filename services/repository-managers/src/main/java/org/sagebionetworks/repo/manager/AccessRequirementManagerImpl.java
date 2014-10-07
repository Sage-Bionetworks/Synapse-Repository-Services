package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.evaluation.EvaluationDAO;
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
	private NodeDAO nodeDao;

	@Autowired
	private EvaluationDAO evaluationDAO;
	
	@Autowired
	private NotificationEmailDAO notificationEmailDao;

	@Autowired
	private JiraClient jiraClient;
	
	public AccessRequirementManagerImpl() {}
	
	// for testing 
	public AccessRequirementManagerImpl(
			AccessRequirementDAO accessRequirementDAO,
			NodeDAO nodeDao,
			AuthorizationManager authorizationManager,
			JiraClient jiraClient,
			NotificationEmailDAO notificationEmailDao
	) {
		this.accessRequirementDAO=accessRequirementDAO;
		this.nodeDao=nodeDao;
		this.authorizationManager=authorizationManager;
		this.jiraClient=jiraClient;
		this.notificationEmailDao=notificationEmailDao;
	}
	
	public static void validateAccessRequirement(AccessRequirement a) throws InvalidModelException {
		if (a.getAccessType()==null ||
				a.getSubjectIds()==null) throw new InvalidModelException();
	}
	
	public static void populateCreationFields(UserInfo userInfo, AccessRequirement a) {
		Date now = new Date();
		a.setCreatedBy(userInfo.getId().toString());
		a.setCreatedOn(now);
		a.setModifiedBy(userInfo.getId().toString());
		a.setModifiedOn(now);
	}

	public static void populateModifiedFields(UserInfo userInfo, AccessRequirement a) {
		Date now = new Date();
		a.setCreatedBy(null); // by setting to null we are telling the DAO to use the current values
		a.setCreatedOn(null);
		a.setModifiedBy(userInfo.getId().toString());
		a.setModifiedOn(now);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends AccessRequirement> T createAccessRequirement(UserInfo userInfo, T accessRequirement) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException {
		validateAccessRequirement(accessRequirement);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canCreateAccessRequirement(userInfo, accessRequirement));
		populateCreationFields(userInfo, accessRequirement);
		return accessRequirementDAO.create(accessRequirement);
	}
	
	public static ACTAccessRequirement newLockAccessRequirement(UserInfo userInfo, String entityId) {
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(entityId);
		subjectId.setType(RestrictableObjectType.ENTITY);
		// create the 'lock down' access requirement'
		ACTAccessRequirement accessRequirement = new ACTAccessRequirement();
		accessRequirement.setConcreteType("org.sagebionetworks.repo.model.ACTAccessRequirement");
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
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, entityId, ObjectType. ENTITY, ACCESS_TYPE.CREATE));
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, entityId, ObjectType. ENTITY, ACCESS_TYPE.UPDATE));
		
		
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(entityId);
		subjectId.setType(RestrictableObjectType.ENTITY);

		// check whether there is already an access requirement in place
		List<AccessRequirement> ars = accessRequirementDAO.getForSubject(Collections.singletonList(subjectId.getId()), subjectId.getType());
		if (!ars.isEmpty()) throw new IllegalArgumentException("Entity "+entityId+" is already restricted.");
		
		ACTAccessRequirement accessRequirement = newLockAccessRequirement(userInfo, entityId);
		ACTAccessRequirement result  = accessRequirementDAO.create(accessRequirement);
		
		String emailString = notificationEmailDao.getNotificationEmailForPrincipal(userInfo.getId());
		
		// now create the Jira issue
		JRJCHelper.createRestrictIssue(jiraClient, 
				userInfo.getId().toString(), 
				emailString, 
				entityId);

		return result;
	}
	
	@Override
	public QueryResults<AccessRequirement> getAccessRequirementsForSubject(UserInfo userInfo, RestrictableObjectDescriptor subjectId) throws DatastoreException, NotFoundException {
		List<String> subjectIds = new ArrayList<String>();
		if (RestrictableObjectType.ENTITY==subjectId.getType()) {
			subjectIds.addAll(AccessRequirementUtil.getNodeAncestorIds(nodeDao, subjectId.getId(), true));
		} else {
			subjectIds.add(subjectId.getId());			
		}
		List<AccessRequirement> ars = accessRequirementDAO.getForSubject(subjectIds, subjectId.getType());
		QueryResults<AccessRequirement> result = new QueryResults<AccessRequirement>(ars, ars.size());
		return result;
	}
	
	@Override
	public QueryResults<AccessRequirement> getUnmetAccessRequirements(UserInfo userInfo, RestrictableObjectDescriptor subjectId) throws DatastoreException, NotFoundException {
		// first check if there *are* any unmet requirements.  (If not, no further queries will be executed.)
		List<String> subjectIds = new ArrayList<String>();
		subjectIds.add(subjectId.getId());
		List<Long> unmetARIds = null;
		if (RestrictableObjectType.ENTITY==subjectId.getType()) {
			List<String> nodeAncestorIds = AccessRequirementUtil.getNodeAncestorIds(nodeDao, subjectId.getId(), false);
			subjectIds.addAll(nodeAncestorIds);
			unmetARIds = new ArrayList<Long>();
			unmetARIds.addAll(AccessRequirementUtil.unmetDownloadAccessRequirementIdsForEntity(
						userInfo, subjectId.getId(), nodeAncestorIds, nodeDao, accessRequirementDAO));
			List<String> entityAndAncestorIds = new ArrayList<String>(nodeAncestorIds);
			entityAndAncestorIds.add(subjectId.getId());
			unmetARIds.addAll(AccessRequirementUtil.
					unmetUploadAccessRequirementIdsForEntity(userInfo, 
							entityAndAncestorIds, nodeDao, accessRequirementDAO));
		} else if (RestrictableObjectType.EVALUATION==subjectId.getType()) {
			unmetARIds = AccessRequirementUtil.unmetAccessRequirementIdsForEvaluation(
					userInfo, subjectId.getId(), accessRequirementDAO);
		} else if (RestrictableObjectType.TEAM==subjectId.getType()) {
			unmetARIds = AccessRequirementUtil.unmetAccessRequirementIdsForTeam(
					userInfo, subjectId.getId(), accessRequirementDAO);
		} else {
			throw new InvalidModelException("Unexpected object type "+subjectId.getType());
		}
		
		List<AccessRequirement> unmetRequirements = new ArrayList<AccessRequirement>();
		// if there are any unmet requirements, retrieve the object(s)
		if (!unmetARIds.isEmpty()) {
			List<AccessRequirement> allRequirementsForSubject = accessRequirementDAO.getForSubject(subjectIds, subjectId.getType());
			for (Long unmetId : unmetARIds) { // typically there will be just one id here
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
	public <T extends AccessRequirement> T updateAccessRequirement(UserInfo userInfo, String accessRequirementId, T accessRequirement) throws NotFoundException, UnauthorizedException, ConflictingUpdateException, InvalidModelException, DatastoreException {
		validateAccessRequirement(accessRequirement);
		if (!accessRequirementId.equals(accessRequirement.getId().toString()))
			throw new InvalidModelException("Update specified ID "+accessRequirementId+" but object contains id: "+
		accessRequirement.getId());
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
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, accessRequirementId, ObjectType.ACCESS_REQUIREMENT, accessType));
	}
}
