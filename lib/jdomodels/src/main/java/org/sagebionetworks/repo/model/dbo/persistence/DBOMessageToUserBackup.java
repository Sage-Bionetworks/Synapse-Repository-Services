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
	private byte[] bytesTo;
	private byte[] bytesCc;
	private byte[] bytesBcc;

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

	public byte[] getBytesTo() {
		return bytesTo;
	}

	public void setBytesTo(byte[] bytesTo) {
		this.bytesTo = bytesTo;
	}

	public byte[] getBytesCc() {
		return bytesCc;
	}

	public void setBytesCc(byte[] bytesCc) {
		this.bytesCc = bytesCc;
	}

	public byte[] getBytesBcc() {
		return bytesBcc;
	}

	public void setBytesBcc(byte[] bytesBcc) {
		this.bytesBcc = bytesBcc;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		DBOMessageToUserBackup that = (DBOMessageToUserBackup) o;

		if (messageId != null ? !messageId.equals(that.messageId) : that.messageId != null) return false;
		if (rootMessageId != null ? !rootMessageId.equals(that.rootMessageId) : that.rootMessageId != null)
			return false;
		if (inReplyTo != null ? !inReplyTo.equals(that.inReplyTo) : that.inReplyTo != null) return false;
		if (!Arrays.equals(subjectBytes, that.subjectBytes)) return false;
		if (sent != null ? !sent.equals(that.sent) : that.sent != null) return false;
		if (notificationsEndpoint != null ? !notificationsEndpoint.equals(that.notificationsEndpoint) : that.notificationsEndpoint != null)
			return false;
		if (profileSettingEndpoint != null ? !profileSettingEndpoint.equals(that.profileSettingEndpoint) : that.profileSettingEndpoint != null)
			return false;
		if (withUnsubscribeLink != null ? !withUnsubscribeLink.equals(that.withUnsubscribeLink) : that.withUnsubscribeLink != null)
			return false;
		if (withProfileSettingLink != null ? !withProfileSettingLink.equals(that.withProfileSettingLink) : that.withProfileSettingLink != null)
			return false;
		if (isNotificationMessage != null ? !isNotificationMessage.equals(that.isNotificationMessage) : that.isNotificationMessage != null)
			return false;
		if (to != null ? !to.equals(that.to) : that.to != null) return false;
		if (cc != null ? !cc.equals(that.cc) : that.cc != null) return false;
		if (bcc != null ? !bcc.equals(that.bcc) : that.bcc != null) return false;
		if (!Arrays.equals(bytesTo, that.bytesTo)) return false;
		if (!Arrays.equals(bytesCc, that.bytesCc)) return false;
		return Arrays.equals(bytesBcc, that.bytesBcc);
	}

	@Override
	public int hashCode() {
		int result = messageId != null ? messageId.hashCode() : 0;
		result = 31 * result + (rootMessageId != null ? rootMessageId.hashCode() : 0);
		result = 31 * result + (inReplyTo != null ? inReplyTo.hashCode() : 0);
		result = 31 * result + Arrays.hashCode(subjectBytes);
		result = 31 * result + (sent != null ? sent.hashCode() : 0);
		result = 31 * result + (notificationsEndpoint != null ? notificationsEndpoint.hashCode() : 0);
		result = 31 * result + (profileSettingEndpoint != null ? profileSettingEndpoint.hashCode() : 0);
		result = 31 * result + (withUnsubscribeLink != null ? withUnsubscribeLink.hashCode() : 0);
		result = 31 * result + (withProfileSettingLink != null ? withProfileSettingLink.hashCode() : 0);
		result = 31 * result + (isNotificationMessage != null ? isNotificationMessage.hashCode() : 0);
		result = 31 * result + (to != null ? to.hashCode() : 0);
		result = 31 * result + (cc != null ? cc.hashCode() : 0);
		result = 31 * result + (bcc != null ? bcc.hashCode() : 0);
		result = 31 * result + Arrays.hashCode(bytesTo);
		result = 31 * result + Arrays.hashCode(bytesCc);
		result = 31 * result + Arrays.hashCode(bytesBcc);
		return result;
	}

	@Override
	public String toString() {
		return "DBOMessageToUserBackup{" +
				"messageId=" + messageId +
				", rootMessageId=" + rootMessageId +
				", inReplyTo=" + inReplyTo +
				", subjectBytes=" + Arrays.toString(subjectBytes) +
				", sent=" + sent +
				", notificationsEndpoint='" + notificationsEndpoint + '\'' +
				", profileSettingEndpoint='" + profileSettingEndpoint + '\'' +
				", withUnsubscribeLink=" + withUnsubscribeLink +
				", withProfileSettingLink=" + withProfileSettingLink +
				", isNotificationMessage=" + isNotificationMessage +
				", to='" + to + '\'' +
				", cc='" + cc + '\'' +
				", bcc='" + bcc + '\'' +
				", bytesTo=" + Arrays.toString(bytesTo) +
				", bytesCc=" + Arrays.toString(bytesCc) +
				", bytesBcc=" + Arrays.toString(bytesBcc) +
				'}';
	}
}
