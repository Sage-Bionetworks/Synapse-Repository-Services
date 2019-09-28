package org.sagebionetworks.repo.model.ses;

import java.time.Instant;
import java.util.Objects;

import org.sagebionetworks.util.ValidateArgument;

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
	private Instant expiresOn;
	private QuarantineReason reason;
	private String sesMessageId;

	public QuarantinedEmail(String email, QuarantineReason reason) {
		ValidateArgument.requiredNotBlank(email, "The email");
		ValidateArgument.required(reason, "The reason");
		this.email = email;
		this.reason = reason;
	}

	public String getEmail() {
		return email;
	}

	public Instant getCreatedOn() {
		return createdOn;
	}

	public QuarantinedEmail withCreatedOn(Instant createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	public Instant getUpdatedOn() {
		return updatedOn;
	}

	public QuarantinedEmail withUpdatedOn(Instant updatedOn) {
		this.updatedOn = updatedOn;
		return this;
	}

	public QuarantineReason getReason() {
		return reason;
	}

	public String getSesMessageId() {
		return sesMessageId;
	}

	public QuarantinedEmail withSesMessageId(String sesMessageId) {
		this.sesMessageId = sesMessageId;
		return this;
	}

	public Instant getExpiresOn() {
		return expiresOn;
	}

	public QuarantinedEmail withExpiresOn(Instant expiresOn) {
		this.expiresOn = expiresOn;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdOn, email, reason, sesMessageId, expiresOn, updatedOn);
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
				&& Objects.equals(sesMessageId, other.sesMessageId) && Objects.equals(expiresOn, other.expiresOn)
				&& Objects.equals(updatedOn, other.updatedOn);
	}

	@Override
	public String toString() {
		return "QuarantinedEmail [email=" + email + ", createdOn=" + createdOn + ", updatedOn=" + updatedOn + ", expiresOn=" + expiresOn
				+ ", reason=" + reason + ", sesMessageId=" + sesMessageId + "]";
	}

}
