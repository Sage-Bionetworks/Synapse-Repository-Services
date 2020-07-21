package org.sagebionetworks.repo.manager.dataaccess.notifications;

import java.util.Collections;
import java.util.List;

import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.DataAccessNotificationType;
import org.springframework.stereotype.Service;

@Service
public class AccessRevokedNotificationBuilder implements DataAccessNotificationBuilder {

	@Override
	public List<DataAccessNotificationType> supportedTypes() {
		return Collections.singletonList(DataAccessNotificationType.REVOCATION);
	}

	@Override
	public String buildSubject(ManagedACTAccessRequirement accessRequirement, AccessApproval approval, UserInfo recipient) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String buildMessageBody(ManagedACTAccessRequirement accessRequirement, AccessApproval approval, UserInfo recipient) {
		// TODO Auto-generated method stub
		return null;
	}

}
