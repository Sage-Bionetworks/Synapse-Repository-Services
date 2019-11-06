package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AccessRequirementInfoForUpdate;
import org.sagebionetworks.repo.model.AccessRequirementStats;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PostMessageContentAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptorResponse;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.repo.model.SelfSignAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementConversionRequest;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.util.jrjc.JRJCHelper;
import org.sagebionetworks.repo.util.jrjc.JiraClient;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class AccessRequirementManagerImpl implements AccessRequirementManager {
	public static final Long DEFAULT_LIMIT = 50L;
	public static final Long MAX_LIMIT = 50L;
	public static final Long DEFAULT_OFFSET = 0L;
	public static final Long DEFAULT_EXPIRATION_PERIOD = 0L;
	
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
		RestrictableObjectType expecitingObjectType = determineObjectType(ar.getAccessType());
		for (RestrictableObjectDescriptor rod : ar.getSubjectIds()) {
			ValidateArgument.requirement(rod.getType().equals(expecitingObjectType),
					"Cannot apply AccessRequirement with AccessType "+ar.getAccessType().name()+" to an object of type "+rod.getType().name());
		}
	}

	public static RestrictableObjectType determineObjectType(ACCESS_TYPE accessType) {
		switch(accessType) {
			case DOWNLOAD:
				return RestrictableObjectType.ENTITY;
			case PARTICIPATE:
				return RestrictableObjectType.TEAM;
			default:
				throw new IllegalArgumentException("Not support creating AccessRequirement with AccessType: "+accessType.name());
		}
	}

	public static void populateCreationFields(UserInfo userInfo, AccessRequirement a) {
		Date now = new Date();
		a.setCreatedBy(userInfo.getId().toString());
		a.setCreatedOn(now);
		populateModifiedFields(userInfo, a);
	}

	public static void populateModifiedFields(UserInfo userInfo, AccessRequirement a) {
		Date now = new Date();
		a.setModifiedBy(userInfo.getId().toString());
		a.setModifiedOn(now);
	}

	@WriteTransaction
	@Override
	public <T extends AccessRequirement> T createAccessRequirement(UserInfo userInfo, T accessRequirement) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException {
		validateAccessRequirement(accessRequirement);
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can create an AccessRequirement.");
		}
		populateCreationFields(userInfo, accessRequirement);
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

	@WriteTransaction
	@Override
	public LockAccessRequirement createLockAccessRequirement(UserInfo userInfo, String entityId) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(entityId, "entityId");

		// check authority
		authorizationManager.canAccess(userInfo, entityId, ObjectType. ENTITY, ACCESS_TYPE.CREATE).checkAuthorizationOrElseThrow();
		authorizationManager.canAccess(userInfo, entityId, ObjectType. ENTITY, ACCESS_TYPE.UPDATE).checkAuthorizationOrElseThrow();

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
	public AccessRequirement getAccessRequirement(String requirementId) throws DatastoreException, NotFoundException {
		return accessRequirementDAO.get(requirementId);
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

	@Deprecated
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
	public <T extends AccessRequirement> T updateAccessRequirement(UserInfo userInfo, String accessRequirementId, T toUpdate) throws NotFoundException, UnauthorizedException, ConflictingUpdateException, InvalidModelException, DatastoreException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		ValidateArgument.required(toUpdate, "toUpdate");
		ValidateArgument.requirement(accessRequirementId.equals(toUpdate.getId().toString()),
			"Update specified ID "+accessRequirementId+" but object contains id: "+toUpdate.getId());
		validateAccessRequirement(toUpdate);

		authorizationManager.canAccess(userInfo, toUpdate.getId().toString(), ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.UPDATE)
				.checkAuthorizationOrElseThrow();

		AccessRequirementInfoForUpdate current = accessRequirementDAO.getForUpdate(accessRequirementId);
		if(!current.getEtag().equals(toUpdate.getEtag())
				|| !current.getCurrentVersion().equals(toUpdate.getVersionNumber())){
			throw new ConflictingUpdateException("Access Requirement was updated since you last fetched it, retrieve it again and reapply the update.");
		}
		ValidateArgument.requirement(current.getAccessType().equals(toUpdate.getAccessType()), "Cannot modify AccessType");
		ValidateArgument.requirement(current.getConcreteType().equals(toUpdate.getConcreteType()), "Cannot change "+current.getConcreteType()+" to "+toUpdate.getConcreteType());

		toUpdate.setVersionNumber(current.getCurrentVersion()+1);
		populateModifiedFields(userInfo, toUpdate);
		return (T) accessRequirementDAO.update(setDefaultValues(toUpdate));
	}


	@WriteTransaction
	@Override
	public void deleteAccessRequirement(UserInfo userInfo,
			String accessRequirementId) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can delete an AccessRequirement.");
		}
		accessRequirementDAO.delete(accessRequirementId);
	}

	static AccessRequirement setDefaultValues(AccessRequirement ar) {
		if (ar instanceof ManagedACTAccessRequirement) {
			return setDefaultValuesForManagedACTAccessRequirement(ar);
		} else if (ar instanceof SelfSignAccessRequirement) {
			return setDefaultValuesForSelfSignAccessRequirement(ar);
		}
		return ar;
	}

	/**
	 * @param ar
	 * @return
	 */
	public static AccessRequirement setDefaultValuesForSelfSignAccessRequirement(AccessRequirement ar) {
		SelfSignAccessRequirement req = (SelfSignAccessRequirement) ar;
		if (req.getIsCertifiedUserRequired() == null) {
			req.setIsCertifiedUserRequired(false);
		}
		if (req.getIsValidatedProfileRequired() == null) {
			req.setIsValidatedProfileRequired(false);
		}
		return req;
	}

	/**
	 * @param ar
	 * @return
	 */
	public static AccessRequirement setDefaultValuesForManagedACTAccessRequirement(AccessRequirement ar) {
		ManagedACTAccessRequirement actAR = (ManagedACTAccessRequirement) ar;
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
		if (actAR.getIsIDUPublic() == null) {
			actAR.setIsIDUPublic(false);
		}
		if (actAR.getExpirationPeriod() == null) {
			actAR.setExpirationPeriod(DEFAULT_EXPIRATION_PERIOD);
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

	@WriteTransaction
	@Override
	public AccessRequirement convertAccessRequirement(UserInfo userInfo, AccessRequirementConversionRequest request) throws NotFoundException, UnauthorizedException, ConflictingUpdateException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getAccessRequirementId(), "requirementId");
		ValidateArgument.required(request.getEtag(), "etag");
		ValidateArgument.required(request.getCurrentVersion(), "currentVersion");
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT member can perform this action.");
		}

		AccessRequirement current = accessRequirementDAO.getAccessRequirementForUpdate(request.getAccessRequirementId());
		ValidateArgument.requirement(current.getConcreteType().equals(ACTAccessRequirement.class.getName()),
				"Do not support converting AccessRequirement type "+current.getConcreteType());
		if(!current.getEtag().equals(request.getEtag())
				|| !current.getVersionNumber().equals(request.getCurrentVersion())){
			throw new ConflictingUpdateException("Access Requirement was updated since you last fetched it, retrieve it again and reapply the update.");
		}

		ManagedACTAccessRequirement toUpdate = convert((ACTAccessRequirement) current, userInfo.getId().toString());
		return accessRequirementDAO.update(setDefaultValues(toUpdate));
	}

	public static ManagedACTAccessRequirement convert(ACTAccessRequirement current, String modifiedBy) {
		ValidateArgument.required(current, "current");
		ValidateArgument.required(modifiedBy, "modifiedBy");
		ManagedACTAccessRequirement toUpdate = new ManagedACTAccessRequirement();
		toUpdate.setId(current.getId());
		toUpdate.setAccessType(current.getAccessType());
		toUpdate.setCreatedBy(current.getCreatedBy());
		toUpdate.setCreatedOn(current.getCreatedOn());
		toUpdate.setEtag(UUID.randomUUID().toString());
		toUpdate.setModifiedBy(modifiedBy);
		toUpdate.setModifiedOn(new Date());
		toUpdate.setSubjectIds(current.getSubjectIds());
		toUpdate.setVersionNumber(current.getVersionNumber()+1);
		return toUpdate;
	}

	@Override
	public RestrictableObjectDescriptorResponse getSubjects(String accessRequirementId, String nextPageToken){
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		NextPageToken token = new NextPageToken(nextPageToken);
		RestrictableObjectDescriptorResponse response = new RestrictableObjectDescriptorResponse();
		List<RestrictableObjectDescriptor> subjects = accessRequirementDAO.getSubjects(Long.parseLong(accessRequirementId), token.getLimitForQuery(), token.getOffset());
		response.setSubjects(subjects);
		response.setNextPageToken(token.getNextPageTokenForCurrentResults(subjects));
		return response;
	}
}
