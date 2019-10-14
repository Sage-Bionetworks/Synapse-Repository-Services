package org.sagebionetworks.repo.model.dbo.ses;

import java.util.Optional;

import org.sagebionetworks.repo.model.ses.QuarantinedEmail;
import org.sagebionetworks.repo.model.ses.QuarantinedEmailBatch;

/**
 * DAO layer to work with quarantined email addresses
 * 
 * @author Marco
 */
public interface EmailQuarantineDao {

	/**
	 * Save the given batch to the database
	 * 
	 * @param batch The batch to be saved to the database
	 */
	void addToQuarantine(QuarantinedEmailBatch batch);

	/**
	 * Removes the given email address from the quarantine
	 * 
	 * @param email The email address to be removed from the quarantine
	 * @return True if the address was in quarantine, false otherwise
	 */
	boolean removeFromQuarantine(String email);

	/**
	 * Retrieves the details about the quarantined email if any iif the quarantine is not expired (See
	 * {@link #getQuarantinedEmail(String, boolean) getQuarantinedEmail(String, true)}).
	 * 
	 * @param email The email to lookup
	 * @return An optional containing the details about the quarantined email
	 */
	Optional<QuarantinedEmail> getQuarantinedEmail(String email);

	/**
	 * Retrieves the details about the quarantined email if any
	 * 
	 * @param email           The email to lookup
	 * @param expirationCheck True if a value should be returned only for not expired quarantine, false otherwise
	 * @return An optional containing the details about the quarantined email
	 */
	Optional<QuarantinedEmail> getQuarantinedEmail(String email, boolean expirationCheck);

	/**
	 * Checks whether the given email address is currently quarantined, if the email is in quarantine but the quarantine is
	 * expired returns false
	 * 
	 * @param email The email to lookup
	 * @return True if the email is currently quarantined and the quarantine is not expired
	 */
	boolean isQuarantined(String email);

	/**
	 * Clear the quarantine
	 */
	void clearAll();

}
