package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AccessRequirementStats;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformation;
import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.util.jrjc.JRJCHelper;
import org.sagebionetworks.repo.util.jrjc.JiraClient;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import org.sagebionetworks.repo.transactions.WriteTransaction;

public class AccessRequirementManagerImpl implements AccessRequirementManager {
	public static final Long DEFAULT_LIMIT = 50L;
	public static final Long MAX_LIMIT = 50L;
	public static final Long DEFAULT_OFFSET = 0L;
	
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;

	@Autowired
	private AccessApprovalDAO accessApprovalDAO;

	@Autowired
	private AuthorizationManager authorizationManager;
	
	@Autowired
	private NodeDAO nodeDao;
	
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
	
	@WriteTransaction
	@Override
	public <T extends AccessRequirement> T createAccessRequirement(UserInfo userInfo, T accessRequirement) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException {
		validateAccessRequirement(accessRequirement);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canCreateAccessRequirement(userInfo, accessRequirement));
		populateCreationFields(userInfo, accessRequirement);
		if (accessRequirement.getAccessType()==ACCESS_TYPE.UPLOAD) {
			throw new IllegalArgumentException("Creating UPLOAD Access Requirement is not allowed.");
		}
		return (T) accessRequirementDAO.create(setDefaultValues(accessRequirement));
	}
	
	public static ACTAccessRequirement newLockAccessRequirement(UserInfo userInfo, String entityId) {
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(entityId);
		subjectId.setType(RestrictableObjectType.ENTITY);
		// create the 'lock down' access requirement'
		ACTAccessRequirement accessRequirement = new ACTAccessRequirement();
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		accessRequirement.setActContactInfo("Access restricted pending review by Synapse Access and Compliance Team.");
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{subjectId}));
		accessRequirement.setOpenJiraIssue(true);
		populateCreationFields(userInfo, accessRequirement);
		return accessRequirement;
	}
	
	@WriteTransaction
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
		List<AccessRequirement> ars = accessRequirementDAO.getAllAccessRequirementsForSubject(Collections.singletonList(subjectId.getId()), subjectId.getType());
		if (!ars.isEmpty()) throw new IllegalArgumentException("Entity "+entityId+" is already restricted.");
		
		ACTAccessRequirement accessRequirement = newLockAccessRequirement(userInfo, entityId);
		ACTAccessRequirement result  = (ACTAccessRequirement) accessRequirementDAO.create(setDefaultValues(accessRequirement));
		
		String emailString = notificationEmailDao.getNotificationEmailForPrincipal(userInfo.getId());
		
		// now create the Jira issue
		JRJCHelper.createRestrictIssue(jiraClient, 
				userInfo.getId().toString(), 
				emailString, 
				entityId);

		return result;
	}
	

	@Override
	public AccessRequirement getAccessRequirement(UserInfo userInfo, String requirementId) throws DatastoreException, NotFoundException {
		return accessRequirementDAO.get(requirementId);
	}
	
	@Override
	public List<AccessRequirement> getAllAccessRequirementsForSubject(UserInfo userInfo, RestrictableObjectDescriptor rod) throws DatastoreException, NotFoundException {
		List<String> subjectIds = new ArrayList<String>();
		if (RestrictableObjectType.ENTITY==rod.getType()) {
			subjectIds.addAll(AccessRequirementUtil.getNodeAncestorIds(nodeDao, rod.getId(), true));
		} else {
			subjectIds.add(rod.getId());
		}
		return accessRequirementDAO.getAllAccessRequirementsForSubject(subjectIds, rod.getType());
	}

	@Deprecated
	@Override
	public List<AccessRequirement> getAllUnmetAccessRequirements(UserInfo userInfo,
			RestrictableObjectDescriptor rod, ACCESS_TYPE accessType)
					throws DatastoreException, NotFoundException {
		// first check if there *are* any unmet requirements.  (If not, no further queries will be executed.)
		List<String> subjectIds = new ArrayList<String>();
		subjectIds.add(rod.getId());
		List<Long> unmetARIds = null;
		if (RestrictableObjectType.ENTITY==rod.getType()) {
			unmetARIds = new ArrayList<Long>();
			List<String> nodeAncestorIds = AccessRequirementUtil.getNodeAncestorIds(nodeDao, rod.getId(), false);
			if (accessType==null || accessType==ACCESS_TYPE.DOWNLOAD) {
				subjectIds.addAll(nodeAncestorIds);
				unmetARIds.addAll(AccessRequirementUtil.unmetDownloadAccessRequirementIdsForEntity(
						userInfo, rod.getId(), nodeAncestorIds, nodeDao, accessRequirementDAO));
			} else if (accessType==ACCESS_TYPE.UPLOAD) {
				List<String> entityAndAncestorIds = new ArrayList<String>(nodeAncestorIds);
				entityAndAncestorIds.add(rod.getId());
				unmetARIds.addAll(AccessRequirementUtil.
				unmetUploadAccessRequirementIdsForEntity(userInfo, 
							entityAndAncestorIds, nodeDao, accessRequirementDAO));
			} else {
				throw new IllegalArgumentException("Unexpected access type "+accessType);
			}
		} else {
			if (accessType==null) {
				if (rod.getType()==RestrictableObjectType.EVALUATION) {
					accessType = ACCESS_TYPE.SUBMIT;
				} else {
					throw new IllegalArgumentException("accessType is required.");	
				}
			}
			unmetARIds = accessRequirementDAO.getAllUnmetAccessRequirements(
					Collections.singletonList(rod.getId()), rod.getType(), userInfo.getGroups(), 
					Collections.singletonList(accessType));
		}
		
		List<AccessRequirement> unmetRequirements = new ArrayList<AccessRequirement>();
		// if there are any unmet requirements, retrieve the object(s)
		if (!unmetARIds.isEmpty()) {
			List<AccessRequirement> allRequirementsForSubject = accessRequirementDAO.getAllAccessRequirementsForSubject(subjectIds, rod.getType());
			for (Long unmetId : unmetARIds) { // typically there will be just one id here
				for (AccessRequirement ar : allRequirementsForSubject) { // typically there will be just one id here
					if (ar.getId().equals(unmetId)) unmetRequirements.add(ar);
				}
			}
		}
		return unmetRequirements;
	}

	@Override
	public List<AccessRequirement> getAccessRequirementsForSubject(UserInfo userInfo,
			RestrictableObjectDescriptor rod, Long limit, Long offset)
					throws DatastoreException, NotFoundException {
		if (limit == null) {
			limit = DEFAULT_LIMIT;
		}
		if (offset == null) {
			offset = DEFAULT_OFFSET;
		}
		ValidateArgument.requirement(limit >= 1L && limit <= MAX_LIMIT,
				"limit must be between 1 and "+MAX_LIMIT);
		ValidateArgument.requirement(offset >= 0L, "offset must be at least 0");
		List<String> subjectIds = new ArrayList<String>();
		if (RestrictableObjectType.ENTITY==rod.getType()) {
			subjectIds.addAll(AccessRequirementUtil.getNodeAncestorIds(nodeDao, rod.getId(), true));
		} else {
			subjectIds.add(rod.getId());
		}
		return accessRequirementDAO.getAccessRequirementsForSubject(subjectIds, rod.getType(), limit, offset);
	}
	
	@WriteTransaction
	@Override
	public <T extends AccessRequirement> T updateAccessRequirement(UserInfo userInfo, String accessRequirementId, T accessRequirement) throws NotFoundException, UnauthorizedException, ConflictingUpdateException, InvalidModelException, DatastoreException {
		validateAccessRequirement(accessRequirement);
		if (!accessRequirementId.equals(accessRequirement.getId().toString()))
			throw new InvalidModelException("Update specified ID "+accessRequirementId+" but object contains id: "+
		accessRequirement.getId());
		verifyCanAccess(userInfo, accessRequirement.getId().toString(), ACCESS_TYPE.UPDATE);
		populateModifiedFields(userInfo, accessRequirement);
		return (T) accessRequirementDAO.update(setDefaultValues(accessRequirement));
	}

	@WriteTransaction
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

	static AccessRequirement setDefaultValues(AccessRequirement ar) {
		if (!(ar instanceof ACTAccessRequirement)) {
			return ar;
		}
		ACTAccessRequirement actAR = (ACTAccessRequirement) ar;
		if (actAR.getIsCertifiedUserRequired() == null) {
			actAR.setIsCertifiedUserRequired(false);
		}
		if (actAR.getIsValidatedProfileRequired() == null) {
			actAR.setIsValidatedProfileRequired(false);
		}
		if (actAR.getIsDUCRequired() == null) {
			actAR.setIsDUCRequired(false);
		}
		if (actAR.getIsIRBApprovalRequired() == null) {
			actAR.setIsIRBApprovalRequired(false);
		}
		if (actAR.getAreOtherAttachmentsRequired() == null) {
			actAR.setAreOtherAttachmentsRequired(false);
		}
		if (actAR.getIsAnnualReviewRequired() == null) {
			actAR.setIsAnnualReviewRequired(false);
		}
		if (actAR.getIsIDUPublic() == null) {
			actAR.setIsIDUPublic(false);
		}
		return actAR;
	}

	@Override
	public RestrictionInformation getRestrictionInformation(UserInfo userInfo, String entityId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(entityId, "entityId");
		RestrictionInformation info = new RestrictionInformation();
		List<String> subjectIds = AccessRequirementUtil.getNodeAncestorIds(nodeDao, entityId, true);
		AccessRequirementStats stats = accessRequirementDAO.getAccessRequirementStats(subjectIds, RestrictableObjectType.ENTITY);
		if (stats.getRequirementIdSet().isEmpty()) {
			info.setRestrictionLevel(RestrictionLevel.OPEN);
			info.setHasUnmetAccessRequirement(false);
		} else {
			if (stats.getHasACT()) {
				info.setRestrictionLevel(RestrictionLevel.CONTROLLED_BY_ACT);
			} else if (stats.getHasToU()) {
				info.setRestrictionLevel(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE);
			} else {
				throw new IllegalStateException("Access Requirement does not contain either ACT or ToU: "+stats.getRequirementIdSet().toString());
			}
			info.setHasUnmetAccessRequirement(accessApprovalDAO.hasUnmetAccessRequirement(stats.getRequirementIdSet(), userInfo.getId().toString()));
		}
		return info;
	}
}
