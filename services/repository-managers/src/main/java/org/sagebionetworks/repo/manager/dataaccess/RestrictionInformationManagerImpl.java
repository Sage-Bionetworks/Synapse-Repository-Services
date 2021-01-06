package org.sagebionetworks.repo.manager.dataaccess;

import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AccessRequirementStats;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.repo.model.SelfSignAccessRequirement;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.AccessRestrictionStatusDao;
import org.sagebionetworks.repo.model.ar.SubjectStatus;
import org.sagebionetworks.repo.model.ar.UsersRequirementStatus;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RestrictionInformationManagerImpl implements RestrictionInformationManager {
	
	public static final String TERMS_OF_USE = TermsOfUseAccessRequirement.class.getName();
	
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;

	@Autowired
	private AccessApprovalDAO accessApprovalDAO;

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private AccessRestrictionStatusDao accessRestrictionStatusDao;

	@Override
	public RestrictionInformationResponse getRestrictionInformation(UserInfo userInfo,
			RestrictionInformationRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getObjectId(), "RestrictionInformationRequest.objectId");
		ValidateArgument.required(request.getRestrictableObjectType(),
				"RestrictionInformationRequest.restrictableObjectType");
		RestrictionInformationResponse info = new RestrictionInformationResponse();

		List<SubjectStatus> results = accessRestrictionStatusDao.getSubjectStatus(
				Arrays.asList(KeyFactory.stringToKey(request.getObjectId())), request.getRestrictableObjectType(),
				userInfo.getId());
		SubjectStatus subjectStatus = results.get(0);

		boolean userIsFileCreator = false;
		if (RestrictableObjectType.ENTITY == request.getRestrictableObjectType()) {
			// if the user is the owner of the entity (and the entity is a File), then she
			// automatically
			// has access to the object and therefore has no unmet access requirements
			Long principalId = userInfo.getId();
			Node node = nodeDao.getNode(request.getObjectId());
			if (node.getCreatedByPrincipalId().equals(principalId) && EntityType.file.equals(node.getNodeType())) {
				userIsFileCreator = true;
			}
		}

		List<Long> subjectIds;
		if (RestrictableObjectType.ENTITY == request.getRestrictableObjectType()) {
			subjectIds = nodeDao.getEntityPathIds(request.getObjectId());
		} else if (RestrictableObjectType.TEAM == request.getRestrictableObjectType()) {
			subjectIds = Arrays.asList(KeyFactory.stringToKey(request.getObjectId()));
		} else {
			throw new IllegalArgumentException("Do not support retrieving restriction information for type: "
					+ request.getRestrictableObjectType());
		}
		AccessRequirementStats stats = accessRequirementDAO.getAccessRequirementStats(subjectIds,
				request.getRestrictableObjectType());
		if (stats.getRequirementIdSet().isEmpty()) {
			info.setRestrictionLevel(RestrictionLevel.OPEN);
			info.setHasUnmetAccessRequirement(false);
		} else {
			if (stats.getHasACT() || stats.getHasLock()) {
				info.setRestrictionLevel(RestrictionLevel.CONTROLLED_BY_ACT);
			} else if (stats.getHasToU()) {
				info.setRestrictionLevel(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE);
			} else {
				throw new IllegalStateException("Access Requirement does not contain either ACT or ToU: "
						+ stats.getRequirementIdSet().toString());
			}
			if (userIsFileCreator) {
				info.setHasUnmetAccessRequirement(false);
			} else {
				info.setHasUnmetAccessRequirement(accessApprovalDAO
						.hasUnmetAccessRequirement(stats.getRequirementIdSet(), userInfo.getId().toString()));
			}
		}
		return info;
	}

	public static RestrictionInformationResponse translate(SubjectStatus subjectStatus) {
		ValidateArgument.required(subjectStatus, "subjectStatus");
		RestrictionInformationResponse response = new RestrictionInformationResponse();
		response.setHasUnmetAccessRequirement(subjectStatus.hasUnmet());
		response.setRestrictionLevel(translateRestrictionLevel(subjectStatus.getAccessRestrictions()));
		return response;
	}

	public static RestrictionLevel translateRestrictionLevel(List<UsersRequirementStatus> accessRestrictions) {
		if (accessRestrictions == null || accessRestrictions.isEmpty()) {
			return RestrictionLevel.OPEN;
		}
		boolean hasTremsOfUse = false;
		for (UsersRequirementStatus status : accessRestrictions) {
			switch (status.getRequirementType()) {
			case "":
//			case SelfSignAccessRequirement.class.getName():
//				hasTremsOfUse = true;
//				break;
//			case ACTAccessRequirement.class.getName():
//			case ManagedACTAccessRequirement.class.getName():
//			case LockAccessRequirement.class.getName():
//				return RestrictionLevel.CONTROLLED_BY_ACT;
				
			}
//			if (TermsOfUseAccessRequirement.class.getName().equals(status.getRequirementType())
//					|| SelfSignAccessRequirement.class.getName().equals(status.getRequirementType())) {
//				stats.setHasToU(true);
//			} else if (type.equals(ACTAccessRequirement.class.getName())
//					|| type.equals(ManagedACTAccessRequirement.class.getName())) {
//				stats.setHasACT(true);
//			} else if (type.equals(LockAccessRequirement.class.getName())) {
//				stats.setHasLock(true);
//			}
		}
		return null;
	}

}
