package org.sagebionetworks.repo.model.ses;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.sagebionetworks.repo.model.json.CatchAllJsonObject;

/**
 * Class used to parse the complaint property of the SES Notification from its json representation. See
 * <a href="https://docs.aws.amazon.com/ses/latest/DeveloperGuide/notification-contents.html#complaint-object">Mail
 * Object</a>
 * 
 * @author Marco
 *
 */
public class SESJsonComplaint extends CatchAllJsonObject implements SESJsonNotificationDetails {

	private String userAgent;
	private String feedbackId;
	private String complaintFeedbackType;
	private List<SESJsonRecipient> complainedRecipients;

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	@Override
	public String getFeedbackId() {
		return feedbackId;
	}

	public void setFeedbackId(String feedbackId) {
		this.feedbackId = feedbackId;
	}

	public String getComplaintFeedbackType() {
		return complaintFeedbackType;
	}

	public void setComplaintFeedbackType(String complaintFeedbackType) {
		this.complaintFeedbackType = complaintFeedbackType;
	}

	public List<SESJsonRecipient> getComplainedRecipients() {
		return complainedRecipients;
	}

	public void setComplainedRecipients(List<SESJsonRecipient> complainedRecipients) {
		this.complainedRecipients = complainedRecipients;
	}

	@Override
	public List<SESJsonRecipient> getRecipients() {
		return complainedRecipients;
	}
	
	@Override
	public Optional<String> getSubType() {
		return Optional.empty();
	}
	
	@Override
	public Optional<String> getReason() {
		return Optional.ofNullable(complaintFeedbackType);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(complainedRecipients, complaintFeedbackType, feedbackId, userAgent);
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
		SESJsonComplaint other = (SESJsonComplaint) obj;
		return Objects.equals(complainedRecipients, other.complainedRecipients)
				&& Objects.equals(complaintFeedbackType, other.complaintFeedbackType) && Objects.equals(feedbackId, other.feedbackId)
				&& Objects.equals(userAgent, other.userAgent);
	}

	@Override
	public String toString() {
		return "SESJsonComplaint [userAgent=" + userAgent + ", feedbackId=" + feedbackId + ", complaintFeedbackType="
				+ complaintFeedbackType + ", complainedRecipients=" + complainedRecipients + "]";
	}

}
