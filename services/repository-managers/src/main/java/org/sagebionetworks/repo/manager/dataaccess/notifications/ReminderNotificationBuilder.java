package org.sagebionetworks.repo.manager.dataaccess.notifications;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.DataAccessNotificationType;
import org.springframework.stereotype.Service;

@Service
public class ReminderNotificationBuilder implements DataAccessNotificationBuilder {
	
	// All the reminders
	private static final List<DataAccessNotificationType> SUPPORTED_TYPES = Stream.of(DataAccessNotificationType.values())
			.filter(DataAccessNotificationType::isReminder)
			.collect(Collectors.toList());
	
	@Override
	public List<DataAccessNotificationType> supportedTypes() {
		return SUPPORTED_TYPES;
	}

	@Override
	public String buildSubject(ManagedACTAccessRequirement accessRequirement, AccessApproval approval,
			UserInfo recipient) {
		// TODO Auto-generated method stub
		return "Some subject";
	}

	@Override
	public String buildMessageBody(ManagedACTAccessRequirement accessRequirement, AccessApproval approval,
			UserInfo recipient) {
		// TODO Auto-generated method stub
		return "Some email body";
	}

}
