package org.sagebionetworks.repo.model.ses;

import java.util.ArrayList;
import java.util.Objects;

/**
 * A batch of quarantined emails all with the same timeout. Using the {@link #add(String)} method it
 * is possible to set the same sesMessageId and reason for the entire or partial batch, the
 * {@link #withReason(QuarantineReason)} must be invoked before invoking {@link #add(String)}, to
 * set the sesMessageId for the entire batch invoke the {@link #withSesMessageId(String)} before any
 * {@link #add(String)}.
 * 
 * @author Marco
 *
 */
public class QuarantinedEmailBatch extends ArrayList<QuarantinedEmail> {

	public static QuarantinedEmailBatch emptyBatch() {
		return new QuarantinedEmailBatch();
	}

	private QuarantineReason reason;
	private String sesMessageId;
	
	// The expiration timeout is global to the batch
	private Long expirationTimeout;

	public QuarantinedEmailBatch withReason(QuarantineReason reason) {
		this.reason = reason;
		return this;
	}

	public QuarantinedEmailBatch withSesMessageId(String sesMessageId) {
		this.sesMessageId = sesMessageId;
		return this;
	}

	public QuarantinedEmailBatch withExpirationTimeout(Long expirationTimeout) {
		this.expirationTimeout = expirationTimeout;
		return this;
	}

	/**
	 * Adds the given email to the batch, the {@link #withReason(QuarantineReason)} should be invoked
	 * before this method. A {@link QuarantinedEmail} will be added to the batch with the current
	 * sesMessageId and {@link QuarantineReason}.
	 * 
	 * @param email
	 */
	public void add(String email) {
		if (this.reason == null) {
			throw new IllegalStateException("The reason for the batch must be set first");
		}
		add(new QuarantinedEmail(email, reason).withSesMessageId(sesMessageId));
	}

	public Long getExpirationTimeout() {
		return expirationTimeout;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(reason, sesMessageId, expirationTimeout);
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
		QuarantinedEmailBatch other = (QuarantinedEmailBatch) obj;
		return reason == other.reason && Objects.equals(sesMessageId, other.sesMessageId)
				&& Objects.equals(expirationTimeout, other.expirationTimeout);
	}

	@Override
	public String toString() {
		return "QuarantinedEmailBatch [reason=" + reason + ", sesMessageId=" + sesMessageId + ", timeout=" + expirationTimeout + "]";
	}

}
