package org.sagebionetworks.repo.manager.dataaccess;

import org.sagebionetworks.repo.manager.AuthorizationManagerImpl;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.FormTemplateDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class FormTemplateAuthorizationManagerImpl implements FormTemplateAuthorizationManager {

	private final FormTemplateDao formTemplateDao;

	public FormTemplateAuthorizationManagerImpl(FormTemplateDao formTemplateDao) {
		this.formTemplateDao = formTemplateDao;
	}

	@Override
	public AuthorizationStatus canAccess(UserInfo userInfo, String templateId, ACCESS_TYPE accessType)
			throws NotFoundException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.requiredNotBlank(templateId, "templateId");
		ValidateArgument.required(accessType, "accessType");
		// Checked first so that an unknown template is reported as such for every file.
		if (!formTemplateDao.exists(Long.parseLong(templateId))) {
			throw new NotFoundException("A form template with the id '" + templateId + "' does not exist.");
		}
		if (accessType != ACCESS_TYPE.DOWNLOAD) {
			return AuthorizationStatus.accessDenied("Unexpected access type " + accessType);
		}
		// The attachments of a template are open to every authenticated user.
		if (userInfo.isUserAnonymous()) {
			return AuthorizationStatus.accessDenied(AuthorizationManagerImpl.ANONYMOUS_ACCESS_DENIED_REASON);
		}
		return AuthorizationStatus.authorized();
	}

}
