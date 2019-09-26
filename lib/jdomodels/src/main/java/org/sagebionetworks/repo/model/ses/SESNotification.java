package org.sagebionetworks.repo.model.ses;

import java.time.Instant;
import java.util.Objects;

/**
 * DTO object to save a SES notification to the DB
 * 
 * @author Marco
 *
 */
public class SESNotification {

	private Long id;
	private Instant createdOn;
	private SESNotificationType notificationType;
	private String notificationBody;
	private String sesEmailId;
	private String sesFeedbackId;
	private Instant messageTimestamp;
	private Instant ispTimestamp;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Instant getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Instant createdOn) {
		this.createdOn = createdOn;
	}

	public SESNotificationType getNotificationType() {
		return notificationType;
	}

	public void setNotificationType(SESNotificationType notificationType) {
		this.notificationType = notificationType;
	}

	public String getNotificationBody() {
		return notificationBody;
	}

	public void setNotificationBody(String notificationBody) {
		this.notificationBody = notificationBody;
	}

	public String getSesEmailId() {
		return sesEmailId;
	}

	public void setSesEmailId(String sesEmailId) {
		this.sesEmailId = sesEmailId;
	}

	public String getSesFeedbackId() {
		return sesFeedbackId;
	}

	public void setSesFeedbackId(String sesFeedbackId) {
		this.sesFeedbackId = sesFeedbackId;
	}

	public Instant getMessageTimestamp() {
		return messageTimestamp;
	}

	public void setMessageTimestamp(Instant messageTimestamp) {
		this.messageTimestamp = messageTimestamp;
	}

	public Instant getIspTimestamp() {
		return ispTimestamp;
	}

	public void setIspTimestamp(Instant ispTimestamp) {
		this.ispTimestamp = ispTimestamp;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdOn, id, ispTimestamp, messageTimestamp, notificationBody, notificationType, sesEmailId, sesFeedbackId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SESNotification other = (SESNotification) obj;
		return Objects.equals(createdOn, other.createdOn) && Objects.equals(id, other.id)
				&& Objects.equals(ispTimestamp, other.ispTimestamp) && Objects.equals(messageTimestamp, other.messageTimestamp)
				&& Objects.equals(notificationBody, other.notificationBody) && notificationType == other.notificationType
				&& Objects.equals(sesEmailId, other.sesEmailId) && Objects.equals(sesFeedbackId, other.sesFeedbackId);
	}

	@Override
	public String toString() {
		return "SESNotification [id=" + id + ", createdOn=" + createdOn + ", notificationType=" + notificationType + ", notificationBody="
				+ notificationBody + ", sesEmailId=" + sesEmailId + ", sesFeedbackId=" + sesFeedbackId + ", messageTimestamp="
				+ messageTimestamp + ", ispTimestamp=" + ispTimestamp + "]";
	}

}
