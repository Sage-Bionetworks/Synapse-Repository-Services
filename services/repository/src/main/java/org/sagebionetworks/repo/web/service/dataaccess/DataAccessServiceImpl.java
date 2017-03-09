package org.sagebionetworks.repo.web.service.dataaccess;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.DataAccessRequestManager;
import org.sagebionetworks.repo.manager.dataaccess.ResearchProjectManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.ChangeOwnershipRequest;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequest;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequestInterface;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.springframework.beans.factory.annotation.Autowired;

public class DataAccessServiceImpl implements DataAccessService{

	@Autowired
	private UserManager userManager;
	@Autowired
	private ResearchProjectManager researchProjectManager;
	@Autowired
	private DataAccessRequestManager dataAccessRequestManager;

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
	public ResearchProject getUserOwnResearchProject(Long userId, String accessRequirementId) {
		UserInfo user = userManager.getUserInfo(userId);
		return researchProjectManager.getUserOwnResearchProject(user, accessRequirementId);
	}

	@Override
	public ResearchProject changeOwnership(Long userId, ChangeOwnershipRequest request) {
		UserInfo user = userManager.getUserInfo(userId);
		return researchProjectManager.changeOwnership(user, request);
	}

	@Override
	public DataAccessRequest create(Long userId, DataAccessRequest toCreate) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessRequestManager.create(user, toCreate);
	}

	@Override
	public DataAccessRequestInterface update(Long userId, DataAccessRequestInterface toUpdate) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessRequestManager.update(user, toUpdate);
	}

	@Override
	public DataAccessRequestInterface getUserOwnCurrentRequest(Long userId, String requirementId) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessRequestManager.getUserOwnCurrentRequest(user, requirementId);
	}

	@Override
	public DataAccessRequestInterface getRequestForUpdate(Long userId, String requirementId) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessRequestManager.getDataAccessRequestForUpdate(user, requirementId);
	}

}
