package org.sagebionetworks.repo.model.ses;

import java.time.Instant;
import java.util.Objects;

/**
 * DTO object for a quarantined email address
 * 
 * @author Marco
 *
 */
public class QuarantinedEmail {

	private String email;
	private Instant createdOn;
	private Instant updatedOn;
	private Instant timeout;
	private QuarantineReason reason;
	private String sesMessageId;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Instant getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Instant createdOn) {
		this.createdOn = createdOn;
	}

	public Instant getUpdatedOn() {
		return updatedOn;
	}

	public void setUpdatedOn(Instant updatedOn) {
		this.updatedOn = updatedOn;
	}

	public QuarantineReason getReason() {
		return reason;
	}

	public void setReason(QuarantineReason reason) {
		this.reason = reason;
	}

	public String getSesMessageId() {
		return sesMessageId;
	}

	public void setSesMessageId(String sesMessageId) {
		this.sesMessageId = sesMessageId;
	}

	public Instant getTimeout() {
		return timeout;
	}

	public void setTimeout(Instant timeout) {
		this.timeout = timeout;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdOn, email, reason, sesMessageId, timeout, updatedOn);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QuarantinedEmail other = (QuarantinedEmail) obj;
		return Objects.equals(createdOn, other.createdOn) && Objects.equals(email, other.email) && reason == other.reason
				&& Objects.equals(sesMessageId, other.sesMessageId) && Objects.equals(timeout, other.timeout)
				&& Objects.equals(updatedOn, other.updatedOn);
	}

	@Override
	public String toString() {
		return "QuarantinedEmail [email=" + email + ", createdOn=" + createdOn + ", updatedOn=" + updatedOn + ", timeout=" + timeout
				+ ", reason=" + reason + ", sesMessageId=" + sesMessageId + "]";
	}

}
