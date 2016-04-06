package org.sagebionetworks.repo.web.service;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.auth.NewIntegrationTestUser;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.message.ChangeMessage;
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
	public BackupRestoreStatus getStatus(String daemonId, Long userId,
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
	public void terminateDaemon(String daemonId, Long userId,
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
	public StackStatus getStackStatus();

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
	public StackStatus updateStatusStackStatus(Long userId,
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
	public ChangeMessages listChangeMessages(Long userId, Long startChangeNumber, ObjectType type, Long limit) throws DatastoreException, NotFoundException;


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
	public EntityId createTestUser(Long userId, NewIntegrationTestUser userSpecs) throws NotFoundException;

	/**
	 * Deletes a user, iff all FK constraints are met
	 */
	public void deleteUser(Long userId, String id) throws NotFoundException;

	/**
	 * Rebuild a table's index and caches
	 * 
	 * @param userId
	 * @param tableId
	 * @throws IOException
	 */
	public void rebuildTable(Long userId, String tableId) throws NotFoundException, IOException;

	/**
	 * Clear all locks.
	 * 
	 * @param userId
	 * @throws NotFoundException
	 */
	public void clearAllLocks(Long userId) throws NotFoundException;

	/**
	 * Wait for a long time or release the waiters
	 * 
	 * @param userId
	 * @param release
	 * @throws Exception
	 */
	public void waitForTesting(Long userId, boolean release) throws Exception;

	public void throwExceptionTransactional(String exception) throws Throwable;
	public void doNothing() throws Throwable;
	public void throwException(String exception) throws Throwable;
	public void throwExceptionTransactionalBeforeCommit(String exception);
}
