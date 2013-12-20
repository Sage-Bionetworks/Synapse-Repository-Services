package org.sagebionetworks.repo.web.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.auth.NewIntegrationTestUser;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.message.PublishResults;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.http.HttpHeaders;

public interface AdministrationService {


	/**
	 * Get the status of a running daemon (either a backup or restore)
	 * @param daemonId
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
	public BackupRestoreStatus getStatus(String daemonId, String userId,
			HttpHeaders header, HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException,
			ConflictingUpdateException;

	/**
	 * Terminate a running daemon.  This has no effect if the daemon is already terminated.
	 * @param daemonId
	 * @param userId
	 * @param header
	 * @param request
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws ConflictingUpdateException
	 */
	public void terminateDaemon(String daemonId, String userId,
			HttpHeaders header, HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException, IOException,
			ConflictingUpdateException;

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
	public StackStatus getStackStatus(String userId, HttpHeaders header,
			HttpServletRequest request);

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
	public StackStatus updateStatusStackStatus(String userId,
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
	public ChangeMessages listChangeMessages(String userId, Long startChangeNumber, ObjectType type, Long limit) throws DatastoreException, NotFoundException;


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
	PublishResults rebroadcastChangeMessagesToQueue(String userId,	String queueName, Long startChangeNumber, ObjectType type,	Long limit) throws DatastoreException, NotFoundException;

	/**
	 * Rebroadcast messages
	 * @param userId
	 * @param startChangeNumber
	 * @param limit
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	FireMessagesResult reFireChangeMessages(String userId, Long startChangeNumber, Long limit) throws DatastoreException, NotFoundException;
	
	/**
	 *	Return the last change message number
	 */
	FireMessagesResult getCurrentChangeNumber(String userId) throws DatastoreException, NotFoundException;

	/**
	 * Clears the Synapse DOI table.
	 */
	void clearDoi(String userId) throws NotFoundException, UnauthorizedException, DatastoreException;

	/**
	 * Clears the specified dynamo table.
	 */
	void clearDynamoTable(String userId, String tableName, String hashKeyName, String rangeKeyName) throws NotFoundException, UnauthorizedException, DatastoreException;

	/**
	 * Creates a test user
	 */
	public EntityId createTestUser(String userId, NewIntegrationTestUser userSpecs) throws NotFoundException;

	/**
	 * Deletes a user, iff all FK constraints are met
	 */
	public void deleteUser(String userId, String id) throws NotFoundException;
}