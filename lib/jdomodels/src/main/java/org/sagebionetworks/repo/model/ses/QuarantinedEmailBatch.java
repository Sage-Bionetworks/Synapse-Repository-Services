package org.sagebionetworks.repo.model.ses;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.sagebionetworks.util.ValidateArgument;

/**
 * Containter for a batch of {@link QuarantinedEmail}s that all share the same expiration timeout
 * 
 * @author Marco
 *
 */
public class QuarantinedEmailBatch {

	public static final QuarantinedEmailBatch EMPTY_BATCH = new QuarantinedEmailBatch(Collections.emptyList());

	private List<QuarantinedEmail> batch;
	private Long expirationTimeout;

	public QuarantinedEmailBatch() {
		this(new ArrayList<>());
	}

	private QuarantinedEmailBatch(List<QuarantinedEmail> batch) {
		this.batch = batch;
	}

	public List<QuarantinedEmail> getBatch() {
		return batch;
	}

	public Long getExpirationTimeout() {
		return expirationTimeout;
	}
	
	public Optional<Instant> getExpiration() {
		if (expirationTimeout == null) {
			return Optional.empty();
		}
		return Optional.of(Instant.now().plusMillis(expirationTimeout));
	}

	public QuarantinedEmailBatch withExpirationTimeout(Long expirationTimeout) {
		ValidateArgument.requirement(expirationTimeout == null || expirationTimeout > 0, "The expiration timeout must be greater than zero");
		this.expirationTimeout = expirationTimeout;
		return this;
	}

	public QuarantinedEmailBatch add(QuarantinedEmail quarantinedEmail) {
		batch.add(quarantinedEmail);
		return this;
	}
	
	public QuarantinedEmail get(int index) {
		return batch.get(index);
	}

	public boolean isEmpty() {
		return batch.isEmpty();
	}

	public int size() {
		return batch.size();
	}

	@Override
	public int hashCode() {
		return Objects.hash(batch, expirationTimeout);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QuarantinedEmailBatch other = (QuarantinedEmailBatch) obj;
		return Objects.equals(batch, other.batch) && Objects.equals(expirationTimeout, other.expirationTimeout);
	}

	@Override
	public String toString() {
		return "QuarantinedEmailBatch [batch=" + batch + ", expirationTimeout=" + expirationTimeout + "]";
	}

}
