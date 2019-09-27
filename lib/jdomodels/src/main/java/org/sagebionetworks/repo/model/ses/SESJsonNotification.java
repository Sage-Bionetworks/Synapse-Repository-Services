package org.sagebionetworks.repo.model.ses;

import java.util.Objects;

import org.sagebionetworks.repo.model.json.CatchAllJsonObject;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Class used to parse an SES Notification from its json representation. See
 * <a href="https://docs.aws.amazon.com/ses/latest/DeveloperGuide/notification-contents.html">Notification Content</a>
 * 
 * @author Marco
 */
public class SESJsonNotification extends CatchAllJsonObject {

	private String notificationType;
	private SESJsonMail mail;
	private SESJsonBounce bounce;
	private SESJsonComplaint complaint;

	// The original untouched notification body, added after parsing
	@JsonIgnore
	private String notificationBody;

	public String getNotificationType() {
		return notificationType;
	}

	public void setNotificationType(String notificationType) {
		this.notificationType = notificationType;
	}

	public SESJsonMail getMail() {
		return mail;
	}

	public void setMail(SESJsonMail mail) {
		this.mail = mail;
	}

	public SESJsonBounce getBounce() {
		return bounce;
	}

	public void setBounce(SESJsonBounce bounce) {
		this.bounce = bounce;
	}

	public SESJsonComplaint getComplaint() {
		return complaint;
	}

	public void setComplaint(SESJsonComplaint complaint) {
		this.complaint = complaint;
	}

	public String getNotificationBody() {
		return notificationBody;
	}

	public void setNotificationBody(String notificationBody) {
		this.notificationBody = notificationBody;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(bounce, complaint, mail, notificationBody, notificationType);
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
		SESJsonNotification other = (SESJsonNotification) obj;
		return Objects.equals(bounce, other.bounce) && Objects.equals(complaint, other.complaint) && Objects.equals(mail, other.mail)
				&& Objects.equals(notificationBody, other.notificationBody) && Objects.equals(notificationType, other.notificationType);
	}

	@Override
	public String toString() {
		return "SESJsonNotification [notificationType=" + notificationType + ", mail=" + mail + ", bounce=" + bounce + ", complaint="
				+ complaint + ", notificationBody=" + notificationBody + "]";
	}

}
