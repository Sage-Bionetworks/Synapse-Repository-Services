package org.sagebionetworks.repo.model.ses;

import java.util.Objects;

import org.sagebionetworks.repo.model.json.CatchAllJsonObject;

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(bounce, complaint, mail, notificationType);
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
				&& Objects.equals(notificationType, other.notificationType);
	}

	@Override
	public String toString() {
		return "SESJsonNotification [notificationType=" + notificationType + ", mail=" + mail + ", bounce=" + bounce + ", complaint="
				+ complaint + "]";
	}

}
