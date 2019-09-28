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
	private int instanceNumber;
	private Instant createdOn;
	private SESNotificationType notificationType;
	private String notificationSubType;
	private String notificationReason;
	private String notificationBody;
	private String sesMessageId;
	private String sesFeedbackId;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getInstanceNumber() {
		return instanceNumber;
	}

	public void setInstanceNumber(int instanceNumber) {
		this.instanceNumber = instanceNumber;
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

	public String getNotificationSubType() {
		return notificationSubType;
	}

	public void setNotificationSubType(String notificationSubType) {
		this.notificationSubType = notificationSubType;
	}

	public String getNotificationReason() {
		return notificationReason;
	}

	public void setNotificationReason(String notificationReason) {
		this.notificationReason = notificationReason;
	}

	public String getNotificationBody() {
		return notificationBody;
	}

	public void setNotificationBody(String notificationBody) {
		this.notificationBody = notificationBody;
	}

	public String getSesMessageId() {
		return sesMessageId;
	}

	public void setSesMessageId(String sesMessageId) {
		this.sesMessageId = sesMessageId;
	}

	public String getSesFeedbackId() {
		return sesFeedbackId;
	}

	public void setSesFeedbackId(String sesFeedbackId) {
		this.sesFeedbackId = sesFeedbackId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdOn, id, instanceNumber, notificationBody, notificationReason, notificationSubType, notificationType,
				sesFeedbackId, sesMessageId);
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
		return Objects.equals(createdOn, other.createdOn) && Objects.equals(id, other.id) && instanceNumber == other.instanceNumber
				&& Objects.equals(notificationBody, other.notificationBody) && Objects.equals(notificationReason, other.notificationReason)
				&& Objects.equals(notificationSubType, other.notificationSubType) && notificationType == other.notificationType
				&& Objects.equals(sesFeedbackId, other.sesFeedbackId) && Objects.equals(sesMessageId, other.sesMessageId);
	}

	@Override
	public String toString() {
		return "SESNotification [id=" + id + ", instanceNumber=" + instanceNumber + ", createdOn=" + createdOn + ", notificationType="
				+ notificationType + ", notificationSubType=" + notificationSubType + ", notificationReason=" + notificationReason
				+ ", notificationBody=" + notificationBody + ", sesMessageId=" + sesMessageId + ", sesFeedbackId=" + sesFeedbackId + "]";
	}

}
