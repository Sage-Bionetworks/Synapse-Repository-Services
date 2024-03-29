package org.sagebionetworks.repo.manager.dataaccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.verification.VerificationHelper;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.dbo.verification.VerificationDAO;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataAccessAuthorizationManagerImpl implements DataAccessAuthorizationManager {
	
	private VerificationDAO verificationDao;
	
	private AccessControlListDAO aclDao;
	
	private RequestDAO requestDao;
	
	private SubmissionDAO submissionDao;

	@Autowired
	public DataAccessAuthorizationManagerImpl(VerificationDAO verificationDao, AccessControlListDAO aclDao, RequestDAO requestDao, SubmissionDAO submissionDao) {
		this.verificationDao = verificationDao;
		this.aclDao = aclDao;
		this.requestDao = requestDao;
		this.submissionDao = submissionDao;
	}
	
	@Override
	public AuthorizationStatus canDownloadRequestFiles(UserInfo userInfo, String requestId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(requestId, "requestId");
		
		String accessRequirementId = requestDao.getAccessRequirementId(requestId).toString();
		
		return checkDownloadAccessForAccessRequirement(userInfo, accessRequirementId);
	}
	

	@Override
	public AuthorizationStatus canDownloadSubmissionFiles(UserInfo userInfo, String submissionId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(submissionId, "submissionId");
		
		String accessRequirementId = submissionDao.getAccessRequirementId(submissionId);
		
		return checkDownloadAccessForAccessRequirement(userInfo, accessRequirementId);
	}
	
	AuthorizationStatus checkDownloadAccessForAccessRequirement(UserInfo userInfo, String accessRequirementId) {
		return canReviewAccessRequirementSubmissions(userInfo, accessRequirementId)
				.isAuthorizedOrElseGet( () -> "The user does not have download access.");
	}
	
	@Override
	public AuthorizationStatus canReviewAccessRequirementSubmissions(UserInfo userInfo, String accessRequirementId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(accessRequirementId, "accessRequirementId");
		
		if (AuthorizationUtils.isACTTeamMemberOrAdmin(userInfo)) {
			return AuthorizationStatus.authorized();
		}
		
		// Only validated users can review submissions
		VerificationSubmission currentVerification = verificationDao.getCurrentVerificationSubmissionForUser(userInfo.getId());
		
		if (!VerificationHelper.isVerified(currentVerification)) {
			return AuthorizationStatus.accessDenied("The user must be validated in order to review data access submissions.");
		}
		
		if (!aclDao.canAccess(userInfo, accessRequirementId, ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.REVIEW_SUBMISSIONS).isAuthorized()) {
			return AuthorizationStatus.accessDenied(String.format("The user does not have permissions to review data access submissions for access requirement %s.", accessRequirementId));
		}
		
		return AuthorizationStatus.authorized();
	}
	
	@Override
	public boolean isAccessRequirementReviewer(UserInfo userInfo) {
		ValidateArgument.required(userInfo, "userInfo");
		
		if (AuthorizationUtils.isACTTeamMemberOrAdmin(userInfo)) {
			return true;
		}
		
		return aclDao.hasAccessToResourceOfType(userInfo, ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.REVIEW_SUBMISSIONS);
	}
	
	@Override
	public Map<Long, List<String>> getAccessRequirementReviewers(Set<Long> accessRequirementIds) {
		ValidateArgument.required(accessRequirementIds, "accessRequirementIds");
		// The ACL dao works with string object ids, we need to convert to a more convinient Long map
		return aclDao.getPrincipalIdsMap(accessRequirementIds.stream().map(String::valueOf).collect(Collectors.toSet()), ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.REVIEW_SUBMISSIONS)
			.entrySet().stream()
			.collect(Collectors.toMap(entry -> Long.valueOf(entry.getKey()), entry -> new ArrayList<>(entry.getValue())));
	}
	

}
