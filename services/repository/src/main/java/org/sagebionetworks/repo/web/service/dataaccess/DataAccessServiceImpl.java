package org.sagebionetworks.repo.web.service.dataaccess;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.ResearchProjectManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.ChangeOwnershipRequest;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.springframework.beans.factory.annotation.Autowired;

public class DataAccessServiceImpl implements DataAccessService{

	@Autowired
	private UserManager userManager;
	@Autowired
	private ResearchProjectManager researchProjectManager;

	@Override
	public ResearchProject create(Long userId, ResearchProject toCreate) {
		UserInfo user = userManager.getUserInfo(userId);
		return researchProjectManager.create(user, toCreate);
	}

	@Override
	public ResearchProject update(Long userId, ResearchProject toUpdate) {
		UserInfo user = userManager.getUserInfo(userId);
		return researchProjectManager.update(user, toUpdate);
	}

	@Override
	public ResearchProject get(Long userId, String accessRequirementId) {
		UserInfo user = userManager.getUserInfo(userId);
		return researchProjectManager.get(user, accessRequirementId);
	}

	@Override
	public ResearchProject changeOwnership(Long userId, ChangeOwnershipRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return researchProjectManager.changeOwnership(user, request);
	}

}
