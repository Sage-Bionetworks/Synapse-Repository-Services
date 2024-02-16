package org.sagebionetworks.repo.web.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.admin.ExpireQuarantinedEmailRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.NewIntegrationTestUser;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.model.feature.FeatureStatus;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.message.PublishResults;
import org.sagebionetworks.repo.model.migration.IdGeneratorExport;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.http.HttpHeaders;

public interface AdministrationService {

	/**
	 * Get the current status of the stack
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws ConflictingUpdateException
	 */
	StackStatus getStackStatus();

	/**
	 * Update the current status of the stack.
	 * 
	 * @param userId
	 * @param header
	 * @param request
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws ConflictingUpdateException
	 */
	StackStatus updateStatusStackStatus(Long userId,
			HttpHeaders header, HttpServletRequest request)
			throws DatastoreException, NotFoundException,
			UnauthorizedException, IOException;

	/**
	 * List change messages.
	 * @param userId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	ChangeMessages listChangeMessages(Long userId, Long startChangeNumber, ObjectType type, Long limit) throws DatastoreException, NotFoundException;


	/**
	 * Rebroadcast messages to a queue.
	 * @param userId
	 * @param queueName
	 * @param startChangeNumber
	 * @param type
	 * @param limit
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	PublishResults rebroadcastChangeMessagesToQueue(Long userId,	String queueName, Long startChangeNumber, ObjectType type,	Long limit) throws DatastoreException, NotFoundException;

	/**
	 * Rebroadcast messages
	 * @param userId
	 * @param startChangeNumber
	 * @param limit
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	FireMessagesResult reFireChangeMessages(Long userId, Long startChangeNumber, Long limit) throws DatastoreException, NotFoundException;
	
	/**
	 *	Return the last change message number
	 */
	FireMessagesResult getCurrentChangeNumber(Long userId) throws DatastoreException, NotFoundException;

	/**
	 * Create or update a list of ChangeMessage
	 */
	ChangeMessages createOrUpdateChangeMessages(Long userId, ChangeMessages batch) throws UnauthorizedException, NotFoundException ;

	/**
	 * Clears the Synapse DOI table.
	 */
	void clearDoi(Long userId) throws NotFoundException, UnauthorizedException, DatastoreException;

	/**
	 * Creates a test user
	 */
	LoginResponse createOrGetTestUser(Long userId, NewIntegrationTestUser userSpecs) throws NotFoundException;

	/**
	 * Deletes a user, iff all FK constraints are met
	 */
	void deleteUser(Long userId, String id) throws NotFoundException;

	/**
	 * Rebuild a table's index and caches
	 * 
	 * @param userId
	 * @param tableId
	 * @throws IOException
	 */
	void rebuildTable(Long userId, String tableId) throws NotFoundException, IOException;

	/**
	 * Clear all locks.
	 * 
	 * @param userId
	 * @throws NotFoundException
	 */
	void clearAllLocks(Long userId) throws NotFoundException;

	/**
	 * Create an ID generator export.
	 * @param userId
	 * @return
	 */
	IdGeneratorExport createIdGeneratorExport(Long userId);
	
	/**
	 * @param userId
	 * @param feature
	 * @return The status of the feature
	 */
	FeatureStatus getFeatureStatus(Long userId, Feature feature);
	
	/**
	 * Sets the status of the feature
	 * 
	 * @param userId
	 * @param feature
	 * @param status
	 * @return
	 */
	FeatureStatus setFeatureStatus(Long userId, Feature feature, FeatureStatus status);

	/**
	 * Obtain a login response containing an access token for the given target user
	 *
	 * @param userId
	 * @param targetUserId
	 * @return
	 */
	LoginResponse getUserAccessToken(Long userId, Long targetUserId);

	/**
	 * Update the expiration of a quarantined email address to the current time, effectively removing it from the quarantine list
	 * 
	 * @param userId
	 * @param request
	 */
	void expireQuarantinedEmail(Long userId, ExpireQuarantinedEmailRequest request);

}
