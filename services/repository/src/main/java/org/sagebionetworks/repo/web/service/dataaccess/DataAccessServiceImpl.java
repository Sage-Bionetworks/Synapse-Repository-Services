package org.sagebionetworks.repo.web.service.dataaccess;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.DataAccessRequestManager;
import org.sagebionetworks.repo.manager.dataaccess.ResearchProjectManager;
import org.sagebionetworks.repo.model.UserInfo;
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
	public ResearchProject createOrUpdate(Long userId, ResearchProject toCreateOrUpdate) {
		UserInfo user = userManager.getUserInfo(userId);
		return researchProjectManager.createOrUpdate(user, toCreateOrUpdate);
	}

	@Override
	public ResearchProject getUserOwnResearchProjectForUpdate(Long userId, String accessRequirementId) {
		UserInfo user = userManager.getUserInfo(userId);
		return researchProjectManager.getUserOwnResearchProjectForUpdate(user, accessRequirementId);
	}

	@Override
	public DataAccessRequestInterface createOrUpdate(Long userId, DataAccessRequestInterface toCreateOrUpdate) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessRequestManager.createOrUpdate(user, toCreateOrUpdate);
	}

	@Override
	public DataAccessRequestInterface getRequestForUpdate(Long userId, String requirementId) {
		UserInfo user = userManager.getUserInfo(userId);
		return dataAccessRequestManager.getDataAccessRequestForUpdate(user, requirementId);
	}

}
