package org.sagebionetworks.repo.service;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.FormTemplateManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplate;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateSearchRequest;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateSearchResponse;
import org.springframework.stereotype.Service;

@Service
public class FormTemplateService {

	private final UserManager userManager;
	private final FormTemplateManager formTemplateManager;

	public FormTemplateService(UserManager userManager, FormTemplateManager formTemplateManager) {
		this.userManager = userManager;
		this.formTemplateManager = formTemplateManager;
	}

	public FormTemplate create(Long userId, FormTemplate template) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return formTemplateManager.create(userInfo, template);
	}

	public FormTemplate createNewVersion(Long userId, FormTemplate template) {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return formTemplateManager.createNewVersion(userInfo, template);
	}

	public FormTemplate getLatestVersion(String id) {
		return formTemplateManager.getLatestVersion(id);
	}

	public FormTemplate getVersion(String id, Long versionNumber) {
		return formTemplateManager.getVersion(id, versionNumber);
	}

	public FormTemplateSearchResponse search(FormTemplateSearchRequest request) {
		return formTemplateManager.search(request);
	}
}
