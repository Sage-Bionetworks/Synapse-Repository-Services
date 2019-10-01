package org.sagebionetworks.repo.model.ses;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.sagebionetworks.repo.model.json.CatchAllJsonObject;

/**
 * Class used to parse the bounce property of the SES Notification from its json representation. See
 * <a href="https://docs.aws.amazon.com/ses/latest/DeveloperGuide/notification-contents.html#bounce-object">Mail
 * Object</a>
 * 
 * @author Marco
 *
 */
public class SESJsonBounce extends CatchAllJsonObject implements SESJsonNotificationDetails {

	private String bounceType;
	private String bounceSubType;
	private String feedbackId;
	private List<SESJsonRecipient> bouncedRecipients;

	public String getBounceType() {
		return bounceType;
	}

	public void setBounceType(String bounceType) {
		this.bounceType = bounceType;
	}

	public String getBounceSubType() {
		return bounceSubType;
	}

	public void setBounceSubType(String bounceSubType) {
		this.bounceSubType = bounceSubType;
	}

	@Override
	public String getFeedbackId() {
		return feedbackId;
	}

	public void setFeedbackId(String feedbackId) {
		this.feedbackId = feedbackId;
	}

	public List<SESJsonRecipient> getBouncedRecipients() {
		return bouncedRecipients;
	}

	public void setBouncedRecipients(List<SESJsonRecipient> bouncedRecipients) {
		this.bouncedRecipients = bouncedRecipients;
	}

	@Override
	public List<SESJsonRecipient> getRecipients() {
		return bouncedRecipients;
	}
	
	@Override
	public Optional<String> getSubType() {
		return Optional.ofNullable(bounceType);
	}
	
	@Override
	public Optional<String> getReason() {
		return Optional.ofNullable(bounceSubType);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(bounceSubType, bounceType, bouncedRecipients, feedbackId);
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
		SESJsonBounce other = (SESJsonBounce) obj;
		return Objects.equals(bounceSubType, other.bounceSubType) && Objects.equals(bounceType, other.bounceType)
				&& Objects.equals(bouncedRecipients, other.bouncedRecipients) && Objects.equals(feedbackId, other.feedbackId);
	}

	@Override
	public String toString() {
		return "SESJsonBounce [bounceType=" + bounceType + ", bounceSubType=" + bounceSubType + ", feedbackId=" + feedbackId
				+ ", bouncedRecipients=" + bouncedRecipients + "]";
	}

}
