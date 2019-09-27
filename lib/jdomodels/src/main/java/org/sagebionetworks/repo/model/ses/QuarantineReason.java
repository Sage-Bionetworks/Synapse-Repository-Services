package org.sagebionetworks.repo.model.ses;

/**
 * List of reasons for an email address to be in quarantine
 * 
 * @author Marco
 */
public enum QuarantineReason {
	/**
	 * An hard bounce always leads to a quarantine
	 */
	HARD_BOUNCE, 
	/**
	 * Too many messages were bounced for this address
	 */
	TOO_MANY_BOUNCES, 
	/**
	 * Some complaints leads to quarantine
	 */
	COMPLAINT,
	/**
	 * Other generic reason, or manual addition
	 */
	OTHER
}
