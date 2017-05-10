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
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PostMessageContentAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.util.jrjc.JRJCHelper;
import org.sagebionetworks.repo.util.jrjc.JiraClient;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;

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

	public static void validateAccessRequirement(AccessRequirement ar) throws InvalidModelException {
		ValidateArgument.required(ar.getAccessType(), "AccessType");
		ValidateArgument.required(ar.getSubjectIds(), "AccessRequirement.subjectIds");
		ValidateArgument.requirement(!ar.getConcreteType().equals(PostMessageContentAccessRequirement.class.getName()),
				"No longer support PostMessageContentAccessRequirement.");
		for (RestrictableObjectDescriptor rod : ar.getSubjectIds()) {
			ValidateArgument.requirement(!rod.getType().equals(RestrictableObjectType.EVALUATION),
					"No longer support RestrictableObjectType.EVALUATION");
		}
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
	
	@WriteTransactionReadCommitted
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
	
	public static LockAccessRequirement newLockAccessRequirement(UserInfo userInfo, String entityId, String jiraKey) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(jiraKey, "jiraKey");

		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(entityId);
		subjectId.setType(RestrictableObjectType.ENTITY);
		LockAccessRequirement accessRequirement = new LockAccessRequirement();
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{subjectId}));
		accessRequirement.setJiraKey(jiraKey);
		populateCreationFields(userInfo, accessRequirement);
		return accessRequirement;
	}
	
	@WriteTransactionReadCommitted
	@Override
	public LockAccessRequirement createLockAccessRequirement(UserInfo userInfo, String entityId) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(entityId, "entityId");

		// check authority
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, entityId, ObjectType. ENTITY, ACCESS_TYPE.CREATE));
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, entityId, ObjectType. ENTITY, ACCESS_TYPE.UPDATE));

		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(entityId);
		subjectId.setType(RestrictableObjectType.ENTITY);

		// check whether there is already an access requirement in place
		List<String> subjectIds = AccessRequirementUtil.getNodeAncestorIds(nodeDao, entityId, true);
		AccessRequirementStats stats = accessRequirementDAO.getAccessRequirementStats(subjectIds, RestrictableObjectType.ENTITY);
		ValidateArgument.requirement(stats.getRequirementIdSet().isEmpty(), "Entity "+entityId+" is already restricted.");

		String emailString = notificationEmailDao.getNotificationEmailForPrincipal(userInfo.getId());
		String jiraKey = JRJCHelper.createRestrictIssue(jiraClient,
				userInfo.getId().toString(),
				emailString,
				entityId);

		LockAccessRequirement accessRequirement = newLockAccessRequirement(userInfo, entityId, jiraKey);
		return (LockAccessRequirement) accessRequirementDAO.create(setDefaultValues(accessRequirement));
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
	
	@WriteTransactionReadCommitted
	@Override
	public <T extends AccessRequirement> T updateAccessRequirement(UserInfo userInfo, String accessRequirementId, T accessRequirement) throws NotFoundException, UnauthorizedException, ConflictingUpdateException, InvalidModelException, DatastoreException {
		validateAccessRequirement(accessRequirement);
		if (!accessRequirementId.equals(accessRequirement.getId().toString()))
			throw new InvalidModelException("Update specified ID "+accessRequirementId+" but object contains id: "+
		accessRequirement.getId());
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, accessRequirement.getId().toString(),
						ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.UPDATE));
		populateModifiedFields(userInfo, accessRequirement);
		return (T) accessRequirementDAO.update(setDefaultValues(accessRequirement));
	}

	@WriteTransactionReadCommitted
	@Override
	public void deleteAccessRequirement(UserInfo userInfo,
			String accessRequirementId) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, accessRequirementId,
						ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.DELETE));
		accessRequirementDAO.delete(accessRequirementId);
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
	public RestrictionInformationResponse getRestrictionInformation(UserInfo userInfo, RestrictionInformationRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getObjectId(), "RestrictionInformationRequest.objectId");
		ValidateArgument.required(request.getRestrictableObjectType(), "RestrictionInformationRequest.restrictableObjectType");
		RestrictionInformationResponse info = new RestrictionInformationResponse();
		List<String> subjectIds;
		if (RestrictableObjectType.ENTITY == request.getRestrictableObjectType()) {
			subjectIds = AccessRequirementUtil.getNodeAncestorIds(nodeDao, request.getObjectId(), true);
		} else if (RestrictableObjectType.TEAM == request.getRestrictableObjectType()){
			subjectIds = Arrays.asList(request.getObjectId());
		} else {
			throw new IllegalArgumentException("Do not support retrieving restriction information for type: "+request.getRestrictableObjectType());
		}
		AccessRequirementStats stats = accessRequirementDAO.getAccessRequirementStats(subjectIds, request.getRestrictableObjectType());
		if (stats.getRequirementIdSet().isEmpty()) {
			info.setRestrictionLevel(RestrictionLevel.OPEN);
			info.setHasUnmetAccessRequirement(false);
		} else {
			if (stats.getHasACT() || stats.getHasLock()) {
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
