package org.sagebionetworks.repo.manager.dataaccess.notifications;

import java.util.List;

import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.dataaccess.DataAccessNotificationType;

public interface DataAccessNotificationBuilder {

	List<DataAccessNotificationType> supportedTypes();
	
	String getMimeType();
	
	String buildSubject(ManagedACTAccessRequirement accessRequirement, AccessApproval approval);
	
	String buildMessageBody(ManagedACTAccessRequirement accessRequirement, AccessApproval approval);
	
}
