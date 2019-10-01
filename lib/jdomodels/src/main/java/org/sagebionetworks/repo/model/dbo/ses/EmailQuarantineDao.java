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
	 * Adds the email address in the given DTO to the quarantine. If the email is already quarantined updates the reason,
	 * the sesMessageId and the expiration
	 * 
	 * @param quarantinedEmail  The quarantined email to save to the database
	 * @param expirationTimeout The optional expiration timeout (ms) for the quarantine, if null and the quarantine exists
	 *                          removes the expiration
	 * @return The created or updated quarantined record
	 */
	QuarantinedEmail addToQuarantine(QuarantinedEmail quarantinedEmail, Long expirationTimeout);

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
	 * Retrieves the details about the quarantined email if any
	 * 
	 * @param email The email to lookup
	 * @return An optional containing the details about the quarantined email
	 */
	Optional<QuarantinedEmail> getQuarantinedEmail(String email);

	/**
	 * Clear the quarantine
	 */
	void clearAll();

}
