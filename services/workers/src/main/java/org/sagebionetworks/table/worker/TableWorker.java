package org.sagebionetworks.table.worker;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.semaphore.SemaphoreDao;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.exception.LockUnavilableException;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import com.amazonaws.services.sqs.model.Message;

/**
 * This worker updates the index used to support the tables features. It will
 * listen to table changes and apply them to the RDS table that acts as the
 * index of the table feature.
 * 
 * @author John
 * 
 */
public class TableWorker implements Callable<List<Message>> {
	
	private enum State {
		SUCCESS,
		UNRECOVERABLE_FAILURE,
		RECOVERABLE_FAILURE,
	}

	static private Logger log = LogManager.getLogger(TableWorker.class);
	List<Message> messages;
	ConnectionFactory tableConnectionFactory;
	TableRowManager tableRowManager;
	TableIndexDAO tableIndexDAO;
	boolean featureEnabled;
	long timeoutMS;
	
	
	/**
	 * This worker has many dependencies.
	 * 
	 * @param messages
	 * @param tableConnectionFactory
	 * @param tableTruthDAO
	 * @param columnModelDAO
	 * @param tableIndexDAO
	 * @param semaphoreDao
	 * @param tableStatusDAO
	 * @param configuration
	 * @param timeoutMS
	 */
	public TableWorker(List<Message> messages,
			ConnectionFactory tableConnectionFactory,
			TableRowManager tableRowManager, SemaphoreDao semaphoreDao,
			TableIndexDAO tableIndexDAO, StackConfiguration configuration) {
		super();
		this.messages = messages;
		this.tableConnectionFactory = tableConnectionFactory;
		this.tableRowManager = tableRowManager;
		this.tableIndexDAO = tableIndexDAO;
		this.featureEnabled = configuration.getTableEnabled();
		this.timeoutMS = configuration.getTableWorkerTimeoutMS();
	}

	@Override
	public List<Message> call() throws Exception {
		List<Message> processedMessages = new LinkedList<Message>();
		// If the feature is disabled then we simply swallow all messages
		if(!featureEnabled){
			return messages;
		}
		// process each message
		for(Message message: messages){
			// Extract the ChangeMessage
			ChangeMessage change = MessageUtils.extractMessageBody(message);
			// We only care about entity messages here
			if(ObjectType.TABLE.equals((change.getObjectType()))){
				if(ChangeType.DELETE.equals(change.getChangeType())){
					// Delete the table in the index
					SimpleJdbcTemplate connection = tableConnectionFactory.getConnection(change.getObjectId());
					if(connection != null){
						tableIndexDAO.deleteTable(connection, change.getObjectId());
					}
					processedMessages.add(message);
				}else{
					// Create or update.
					String tableId = change.getObjectId();
					// this method does the real work.
					State state = createOrUpdateTable(tableId, change.getObjectEtag());
					if(!State.RECOVERABLE_FAILURE.equals(state)){
						// Only recoverable failures should remain in the queue.
						// All other must be removed.
						processedMessages.add(message);
					}
				}
			}else{
				// Non-table messages must be returned so they can be removed from the queue.
				processedMessages.add(message);
			}
		}
		return processedMessages;
	}
	
	/**
	 * This is where a single table index is created or updated.
	 * 
	 * @param tableId
	 * @return
	 */
	public State createOrUpdateTable(final String tableId, final String tableEtag){
		// Attempt to run with 
		try{
			// Run with the exclusive lock on the table if we can get it.
			return tableRowManager.runWithTableExclusiveLock(tableId, new Callable<State>() {
				@Override
				public State call() throws Exception {
					// This method does the real work.
					return createOrUpdateWhileHoldingLock(tableId, tableEtag);
				}
			});
		}catch(LockUnavilableException e){
			// We did not get the lock on this table.
			// This is a recoverable failure as we can try again later.
			return State.RECOVERABLE_FAILURE;
		}
	}
	
	/**
	 * This is where the table status gets stated and error handling is performed.
	 * 
	 * @param tableId
	 * @param token
	 * @return
	 */
	private State createOrUpdateWhileHoldingLock(String tableId, String tableEtag){
		long startTime = System.currentTimeMillis();
		// First set the status
		TableStatus status = new TableStatus();
		status.setChangedOn(new Date(System.currentTimeMillis()));
		status.setProgresssCurrent(0L);
		status.setProgresssTotal(100L);
		status.setProgresssMessage("Starting...");
		status.setTableId(tableId);
		status.setTotalTimeMS(0L);
		status.setState(TableState.PROCESSING);
		// Start the real work
		try{
			// Save the status before we start
			status = tableRowManager.updateTableStatus(tableEtag, status);
			// Try to get a connection.
			SimpleJdbcTemplate connection = tableConnectionFactory.getConnection(tableId);
			// If we do not have  connection we can try again later
			if(connection == null) {
				// Change the state
				status.setProgresssMessage("Waiting for a connection to the cluster...");
				status.setTotalTimeMS(System.currentTimeMillis() - startTime);
				status.setState(TableState.PROCESSING);
				// Save the status
				status = tableRowManager.updateTableStatus(tableEtag, status);
				return State.RECOVERABLE_FAILURE;
			}
			// This method will do the rest of the work.
			synchIndexWithTable(connection, tableId, status);
			// We are finished
			status.setProgresssCurrent(100L);
			status.setErrorDetails(null);
			status.setErrorMessage(null);
			status.setProgresssMessage("Finished processing table index.");
			status.setState(TableState.AVAILABLE_FOR_QUERY);
			status.setTotalTimeMS(System.currentTimeMillis() - startTime);
			// save the state
			status = tableRowManager.updateTableStatus(tableEtag, status);
			return State.SUCCESS;
		}catch (Exception e){
			// Failed.
			status.setErrorDetails(e.getMessage());
			// Get the stack trace.
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			status.setErrorDetails(writer.toString());
			status.setState(TableState.PROCESSING_FAILED);
			status.setTotalTimeMS(System.currentTimeMillis() - startTime);
			try {
				status = tableRowManager.updateTableStatus(tableEtag, status);
			} catch (Exception e1) {
				log.error("Failed to update the table and failed to update the table status.", e);
			}
			// This is not an error we can recover from.
			return State.UNRECOVERABLE_FAILURE;
		}
	}

	/**
	 * Synchronizes the table index with the table truth data.
	 * After the successful completion of this method the table index schema will match the schema of the truth
	 * and all row changes that have not already been applied will be applied.
	 * Note: This method will do no work if the index and truth are already synchronized.
	 * @param connection
	 * @param tableId
	 * @param status
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	private void synchIndexWithTable(SimpleJdbcTemplate connection, String tableId, TableStatus status) throws DatastoreException, NotFoundException, IOException{
		// The first task is to get the table schema in-synch.
		// Get the current schema of the table.
		List<ColumnModel> currentSchema = tableRowManager.getColumnModelsForTable(tableId);
		// Create or update the table with this schema.
		tableIndexDAO.createOrUpdateTable(connection, currentSchema, tableId);
		// Now determine which changes need to be applied to the table
		Long maxVersion = tableIndexDAO.getMaxVersionForTable(connection, tableId);
		// List all of the changes
		List<TableRowChange> changes = tableRowManager.listRowSetsKeysForTable(tableId);
		// Apply any change that has a version number greater than the max version already in the index
		if(changes != null){
			for(TableRowChange change: changes){
				if(change.getRowVersion() > maxVersion){
					// This is a change that we must apply.
					RowSet rowSet = tableRowManager.getRowSet(tableId, change.getRowVersion());
					// apply the change to the table
					tableIndexDAO.createOrUpdateRows(connection, rowSet, currentSchema);
				}
			}
		}
	}
}
