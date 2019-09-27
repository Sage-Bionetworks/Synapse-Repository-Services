package org.sagebionetworks.repo.model.dbo.ses;

import java.util.Optional;

import org.sagebionetworks.repo.model.ses.QuarantineReason;

/**
 * DAO layer to work with quarantined email addresses
 * 
 * @author Marco
 */
public interface EmailQuarantineDao {

	/**
	 * Adds the given email address to the quarantine with the given reason and optional sesMessageId. If the email is
	 * already quarantined updates the reason and the sesMessageId
	 * 
	 * @param email        The email to be quarantined
	 * @param reason       The reason for the quarantine
	 * @param sesMessageId The optional id of the SES message that lead to the quarantine
	 */
	void addToQuarantine(String email, QuarantineReason reason, String sesMessageId);

	/**
	 * Removes the given email address from the quarantine
	 * 
	 * @param email The email address to be removed from the quarantine
	 * @return True if the address was in quarantine, false otherwise
	 */
	boolean removeFromQuarantine(String email);

	/**
	 * @param email The email address to check, the check is case insensitive
	 * @return True if the email address is quarantined, false otherwise
	 */
	boolean isQuarantined(String email);

	/**
	 * @param email The email address to get the reason for
	 * @return An optional containing the {@link QuarantineReason} for the given email address if present, empty otherwise
	 */
	Optional<QuarantineReason> getQuarantineReason(String email);

	/**
	 * Clear the quarantine
	 */
	void clearAll();

}
