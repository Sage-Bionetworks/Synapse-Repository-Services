package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.time.Instant;
import java.util.Optional;

public interface DataAccessRequestNotificationDao {

	/**
	 * If a message was sent for the given request and principal, then get the
	 * date-time the message was sent.
	 * 
	 * @param dataAccessRequestId
	 * @param principalId
	 * @return {@link Optional#empty()} if a message does not exist, else the
	 *         date-time when the message was sent.
	 */
	Optional<Instant> getSentOn(String dataAccessRequestId, Long principalId);
	
	/**
	 * If a message was sent for the given request and principal, then get the
	 * message ID of the message.
	 * 
	 * @param dataAccessRequestId
	 * @param principalId
	 * @return {@link Optional#empty()} if a message does not exist, else the
	 *         message ID.
	 */
	Optional<String> getMessageId(String dataAccessRequestId, Long principalId);

	/**
	 * Save that a message was sent.
	 * @param reviewerPrincialId
	 * @param dataAccessRequestId
	 * @param messageId
	 * @param sentOn When the message was sent.
	 */
	void messageSentOn(Long reviewerPrincialId, String dataAccessRequestId, String messageId, Instant sentOn);

}
