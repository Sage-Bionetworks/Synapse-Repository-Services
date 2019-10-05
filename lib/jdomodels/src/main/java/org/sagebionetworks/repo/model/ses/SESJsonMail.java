package org.sagebionetworks.repo.model.ses;

import java.util.Objects;

import org.sagebionetworks.repo.model.json.CatchAllJsonObject;

/**
 * Class used to parse the email property of the SES Notification from its json representation. See
 * <a href="https://docs.aws.amazon.com/ses/latest/DeveloperGuide/notification-contents.html#mail-object">Mail
 * Object</a>
 * 
 * @author Marco
 */
public class SESJsonMail extends CatchAllJsonObject {

	private String messageId;

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(messageId);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SESJsonMail other = (SESJsonMail) obj;
		return Objects.equals(messageId, other.messageId);
	}

	@Override
	public String toString() {
		return "SESJsonMail [messageId=" + messageId + "]";
	}

}
