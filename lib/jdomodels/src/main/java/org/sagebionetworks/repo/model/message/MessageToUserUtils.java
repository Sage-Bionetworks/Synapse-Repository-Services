package org.sagebionetworks.repo.model.message;

import org.sagebionetworks.util.ValidateArgument;

public class MessageToUserUtils {

	public static MessageToUser setUserGeneratedMessageFooter(MessageToUser dto) {
		ValidateArgument.required(dto, "dto");
		dto.setWithUnsubscribeLink(false);
		dto.setIsNotificationMessage(false);
		dto.setWithProfileSettingLink(true);
		return dto;
	}
}
