package org.sagebionetworks.repo.service.docusign;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.docusign.EDucManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.educ.EDucFileHandleId;
import org.sagebionetworks.repo.model.educ.EDucSignatureStatus;
import org.sagebionetworks.repo.model.educ.EDucTemplateListRequest;
import org.sagebionetworks.repo.model.educ.EDucTemplatePage;
import org.sagebionetworks.repo.model.educ.EDucTemplateValidationResult;
import org.sagebionetworks.repo.model.educ.EDucSignatureQuota;
import org.springframework.stereotype.Service;

@Service
public class EDucService {

	private final UserManager userManager;
	private final EDucManager eDucManager;

	public EDucService(UserManager userManager, EDucManager eDucManager) {
		this.userManager = userManager;
		this.eDucManager = eDucManager;
	}

	public EDucTemplatePage listTemplates(Long userId, EDucTemplateListRequest request) throws Exception {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return eDucManager.listTemplates(userInfo, request);
	}

	public EDucTemplateValidationResult validateTemplate(Long userId, String templateId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return eDucManager.validateTemplate(userInfo, templateId);
	}

	public EDucSignatureQuota routeForSignature(Long userId, String requestId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return eDucManager.routeForSignature(userInfo, requestId);
	}

	public EDucSignatureQuota getSignatureQuota(Long userId, String requestId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return eDucManager.getSignatureQuota(userInfo, requestId);
	}

	public EDucSignatureQuota resetQuota(Long userId, String accessRequirementId, Long targetUserId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		UserInfo targetUserInfo = userManager.getUserInfo(targetUserId);
		return eDucManager.resetQuota(userInfo, accessRequirementId, targetUserInfo.getId());
	}

	public EDucFileHandleId previewEDuc(Long userId, String requestId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return eDucManager.previewEDuc(userInfo, requestId);
	}

	public EDucSignatureStatus getSignatureStatus(Long userId, String requestId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return eDucManager.getSignatureStatus(userInfo, requestId);
	}

	public void cancelSignature(Long userId, String requestId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		eDucManager.cancelSignature(userInfo, requestId);
	}

	public EDucFileHandleId getSignedDocumentFileHandle(Long userId, String requestId) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return eDucManager.getSignedDocumentFileHandle(userInfo, requestId);
	}
}
