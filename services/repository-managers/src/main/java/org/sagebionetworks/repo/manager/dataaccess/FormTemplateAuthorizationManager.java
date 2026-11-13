package org.sagebionetworks.repo.manager.dataaccess;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Authorization checks on form templates.
 */
public interface FormTemplateAuthorizationManager {

	/**
	 * Only {@link ACCESS_TYPE#DOWNLOAD} of a template's attachments is granted, and only to
	 * authenticated users. Reading the template itself is not checked here.
	 *
	 * @throws NotFoundException If the template does not exist, whatever the access type.
	 */
	AuthorizationStatus canAccess(UserInfo userInfo, String templateId, ACCESS_TYPE accessType)
			throws NotFoundException;
}
