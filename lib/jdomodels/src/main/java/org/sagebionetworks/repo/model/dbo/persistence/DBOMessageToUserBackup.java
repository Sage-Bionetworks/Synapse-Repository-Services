package org.sagebionetworks.repo.model.dbo.persistence;

import java.util.Arrays;

/**
 * This is the backup object for the old version of DBOMessageToUser
 */
public class DBOMessageToUserBackup {
	private Long messageId;
	private Long rootMessageId;
	private Long inReplyTo;
	// we use a byte array to allow non-latin-1 characters
	private byte[] subjectBytes;
	private Boolean sent;
	private String notificationsEndpoint;
	private String profileSettingEndpoint;
	private Boolean withUnsubscribeLink;
	private Boolean withProfileSettingLink;
	private Boolean isNotificationMessage;
	private String to;
	private String cc;
	private String bcc;

	public Long getMessageId() {
		return messageId;
	}

	public void setMessageId(Long messageId) {
		this.messageId = messageId;
	}

	public Long getRootMessageId() {
		return rootMessageId;
	}

	public void setRootMessageId(Long rootMessageId) {
		this.rootMessageId = rootMessageId;
	}

	public Long getInReplyTo() {
		return inReplyTo;
	}

	public void setInReplyTo(Long inReplyTo) {
		this.inReplyTo = inReplyTo;
	}

	public byte[] getSubjectBytes() {
		return subjectBytes;
	}

	public void setSubjectBytes(byte[] subject) {
		this.subjectBytes = subject;
	}

	public Boolean getSent() {
		return sent;
	}

	public void setSent(Boolean sent) {
		this.sent = sent;
	}

	public String getNotificationsEndpoint() {
		return notificationsEndpoint;
	}

	public void setNotificationsEndpoint(String notificationsEndpoint) {
		this.notificationsEndpoint = notificationsEndpoint;
	}

	public String getProfileSettingEndpoint() {
		return profileSettingEndpoint;
	}

	public void setProfileSettingEndpoint(String profileSettingEndpoint) {
		this.profileSettingEndpoint = profileSettingEndpoint;
	}

	public Boolean getWithUnsubscribeLink() {
		return withUnsubscribeLink;
	}

	public void setWithUnsubscribeLink(Boolean withUnsubscribeLink) {
		this.withUnsubscribeLink = withUnsubscribeLink;
	}

	public Boolean getWithProfileSettingLink() {
		return withProfileSettingLink;
	}

	public void setWithProfileSettingLink(Boolean withProfileSettingLink) {
		this.withProfileSettingLink = withProfileSettingLink;
	}

	public Boolean getIsNotificationMessage() {
		return isNotificationMessage;
	}

	public void setIsNotificationMessage(Boolean isNotificationMessage) {
		this.isNotificationMessage = isNotificationMessage;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getCc() {
		return cc;
	}

	public void setCc(String cc) {
		this.cc = cc;
	}

	public String getBcc() {
		return bcc;
	}

	public void setBcc(String bcc) {
		this.bcc = bcc;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bcc == null) ? 0 : bcc.hashCode());
		result = prime * result + ((cc == null) ? 0 : cc.hashCode());
		result = prime * result + ((inReplyTo == null) ? 0 : inReplyTo.hashCode());
		result = prime * result + ((isNotificationMessage == null) ? 0 : isNotificationMessage.hashCode());
		result = prime * result + ((messageId == null) ? 0 : messageId.hashCode());
		result = prime * result + ((notificationsEndpoint == null) ? 0 : notificationsEndpoint.hashCode());
		result = prime * result + ((profileSettingEndpoint == null) ? 0 : profileSettingEndpoint.hashCode());
		result = prime * result + ((rootMessageId == null) ? 0 : rootMessageId.hashCode());
		result = prime * result + ((sent == null) ? 0 : sent.hashCode());
		result = prime * result + Arrays.hashCode(subjectBytes);
		result = prime * result + ((to == null) ? 0 : to.hashCode());
		result = prime * result + ((withProfileSettingLink == null) ? 0 : withProfileSettingLink.hashCode());
		result = prime * result + ((withUnsubscribeLink == null) ? 0 : withUnsubscribeLink.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOMessageToUserBackup other = (DBOMessageToUserBackup) obj;
		if (bcc == null) {
			if (other.bcc != null)
				return false;
		} else if (!bcc.equals(other.bcc))
			return false;
		if (cc == null) {
			if (other.cc != null)
				return false;
		} else if (!cc.equals(other.cc))
			return false;
		if (inReplyTo == null) {
			if (other.inReplyTo != null)
				return false;
		} else if (!inReplyTo.equals(other.inReplyTo))
			return false;
		if (isNotificationMessage == null) {
			if (other.isNotificationMessage != null)
				return false;
		} else if (!isNotificationMessage.equals(other.isNotificationMessage))
			return false;
		if (messageId == null) {
			if (other.messageId != null)
				return false;
		} else if (!messageId.equals(other.messageId))
			return false;
		if (notificationsEndpoint == null) {
			if (other.notificationsEndpoint != null)
				return false;
		} else if (!notificationsEndpoint.equals(other.notificationsEndpoint))
			return false;
		if (profileSettingEndpoint == null) {
			if (other.profileSettingEndpoint != null)
				return false;
		} else if (!profileSettingEndpoint.equals(other.profileSettingEndpoint))
			return false;
		if (rootMessageId == null) {
			if (other.rootMessageId != null)
				return false;
		} else if (!rootMessageId.equals(other.rootMessageId))
			return false;
		if (sent == null) {
			if (other.sent != null)
				return false;
		} else if (!sent.equals(other.sent))
			return false;
		if (!Arrays.equals(subjectBytes, other.subjectBytes))
			return false;
		if (to == null) {
			if (other.to != null)
				return false;
		} else if (!to.equals(other.to))
			return false;
		if (withProfileSettingLink == null) {
			if (other.withProfileSettingLink != null)
				return false;
		} else if (!withProfileSettingLink.equals(other.withProfileSettingLink))
			return false;
		if (withUnsubscribeLink == null) {
			if (other.withUnsubscribeLink != null)
				return false;
		} else if (!withUnsubscribeLink.equals(other.withUnsubscribeLink))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOMessageToUserBackup [messageId=" + messageId + ", rootMessageId=" + rootMessageId + ", inReplyTo="
				+ inReplyTo + ", subjectBytes=" + Arrays.toString(subjectBytes) + ", sent=" + sent
				+ ", notificationsEndpoint=" + notificationsEndpoint + ", profileSettingEndpoint="
				+ profileSettingEndpoint + ", withUnsubscribeLink=" + withUnsubscribeLink + ", withProfileSettingLink="
				+ withProfileSettingLink + ", isNotificationMessage=" + isNotificationMessage + ", to=" + to + ", cc="
				+ cc + ", bcc=" + bcc + "]";
	}
}