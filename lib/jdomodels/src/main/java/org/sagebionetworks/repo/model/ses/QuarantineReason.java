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
	PERMANENT_BOUNCE,
	/**
	 * Some bounce that might be temporary
	 */
	TRANSIENT_BOUNCE, 
	/**
	 * Some complaints leads to quarantine
	 */
	COMPLAINT,
	/**
	 * Other generic reason, or manual addition
	 */
	OTHER
}
