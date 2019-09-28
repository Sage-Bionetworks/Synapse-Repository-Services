package org.sagebionetworks.repo.model.dbo.ses;

import java.util.Optional;

import org.sagebionetworks.repo.model.ses.QuarantinedEmail;

/**
 * DAO layer to work with quarantined email addresses
 * 
 * @author Marco
 */
public interface EmailQuarantineDao {

	/**
	 * Adds the email address in the given DTO to the quarantine. If the email is already quarantined
	 * updates the reason, the sesMessageId and the timeout
	 * 
	 * @param email        The email to be quarantined
	 * @param reason       The reason for the quarantine
	 * @param sesMessageId The optional id of the SES message that lead to the quarantine
	 */
	QuarantinedEmail addToQuarantine(QuarantinedEmail quarantinedEmail);

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
