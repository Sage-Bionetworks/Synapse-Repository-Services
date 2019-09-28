package org.sagebionetworks.repo.model.ses;

import java.time.Instant;
import java.util.Objects;

import org.sagebionetworks.util.ValidateArgument;

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

	public SESNotification(SESNotificationType notificationType, String notificationBody) {
		ValidateArgument.required(notificationType, "The notification type");
		ValidateArgument.requiredNotBlank(notificationBody, "The notification body");
		this.notificationType = notificationType;
		this.notificationBody = notificationBody;
	}

	public SESNotificationType getNotificationType() {
		return notificationType;
	}

	public String getNotificationBody() {
		return notificationBody;
	}

	public Long getId() {
		return id;
	}

	public SESNotification withId(Long id) {
		this.id = id;
		return this;
	}

	public int getInstanceNumber() {
		return instanceNumber;
	}

	public SESNotification withInstanceNumber(int instanceNumber) {
		this.instanceNumber = instanceNumber;
		return this;
	}

	public Instant getCreatedOn() {
		return createdOn;
	}

	public SESNotification withCreatedOn(Instant createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	public String getNotificationSubType() {
		return notificationSubType;
	}

	public SESNotification withNotificationSubType(String notificationSubType) {
		this.notificationSubType = notificationSubType;
		return this;
	}

	public String getNotificationReason() {
		return notificationReason;
	}

	public SESNotification withNotificationReason(String notificationReason) {
		this.notificationReason = notificationReason;
		return this;
	}

	public String getSesMessageId() {
		return sesMessageId;
	}

	public SESNotification withSesMessageId(String sesMessageId) {
		this.sesMessageId = sesMessageId;
		return this;
	}

	public String getSesFeedbackId() {
		return sesFeedbackId;
	}

	public SESNotification withSesFeedbackId(String sesFeedbackId) {
		this.sesFeedbackId = sesFeedbackId;
		return this;
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
