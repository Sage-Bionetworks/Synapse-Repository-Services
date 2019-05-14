package org.sagebionetworks.table.worker;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.asynchronous.workers.changes.LockTimeoutAware;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionUnavailableException;
import org.sagebionetworks.repo.manager.table.TableIndexManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.TableChangeType;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ColumnChangeDetails;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This worker updates the index used to support the tables features. It will
 * listen to table changes and apply them to the RDS table that acts as the
 * index of the table feature.
 * 
 * @author John
 * 
 */
public class TableWorker implements ChangeMessageDrivenRunner, LockTimeoutAware {

	enum State {
		SUCCESS, UNRECOVERABLE_FAILURE, RECOVERABLE_FAILURE,
	}
	
	static private Logger log = LogManager.getLogger(TableWorker.class);
	

	@Autowired
	TableEntityManager tableEntityManager;
	@Autowired
	TableManagerSupport tableManagerSupport;
	@Autowired
	StackConfiguration configuration;
	@Autowired
	TableIndexConnectionFactory connectionFactory;
	
	Integer lockTimeoutSec;

	@Override
	public void run(ProgressCallback progressCallback, ChangeMessage change)
			throws RecoverableMessageException, Exception {
		if(lockTimeoutSec == null){
			throw new IllegalStateException("lockTimeoutSec must be set before the worker can be run.");
		}
		// We only care about entity messages here
		if (ObjectType.TABLE.equals((change.getObjectType()))) {
			final String tableId = change.getObjectId();
			final IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
			final TableIndexManager indexManager;
			try {
				indexManager = connectionFactory.connectToTableIndex(idAndVersion);
			} catch (TableIndexConnectionUnavailableException e) {
				// try again later.
				throw new RecoverableMessageException();
			}
			if (ChangeType.DELETE.equals(change.getChangeType())) {
				// Delete the table in the index
				tableEntityManager.deleteTableIfDoesNotExist(tableId);
				indexManager.deleteTableIndex(idAndVersion);
				return;
			} else {
				// this method does the real work.
				State state = createOrUpdateTable(progressCallback, idAndVersion, indexManager, change);
				if (State.RECOVERABLE_FAILURE.equals(state)) {
					throw new RecoverableMessageException();
				}
			}
		}
	}
	

	/**
	 * This is where a single table index is created or updated.
	 * 
	 * @param tableId
	 * @return
	 */
	public State createOrUpdateTable(
			ProgressCallback progressCallback,
			final IdAndVersion idAndVersion, final TableIndexManager tableIndexManger,
			final ChangeMessage change) {
		// Attempt to run with
		try {
			// Only proceed if work is needed.
			if(!tableManagerSupport.isIndexWorkRequired(idAndVersion)){
				log.info("No work needed for table "+idAndVersion);
				return State.SUCCESS;
			}
			
			/*
			 * Before we start working on the table make sure it is in the processing mode.
			 * This will generate a new reset token and will not broadcast the change.
			 */
			final String tableResetToken = tableManagerSupport.startTableProcessing(idAndVersion);

			// Run with the exclusive lock on the table if we can get it.
			return tableManagerSupport.tryRunWithTableExclusiveLock(progressCallback, idAndVersion, lockTimeoutSec,
					(ProgressCallback progress) -> {
						// This method does the real work.
						return createOrUpdateWhileHoldingLock(idAndVersion, tableIndexManger, tableResetToken, change);
					});
		} catch (LockUnavilableException e) {
			// We did not get the lock on this table.
			// This is a recoverable failure as we can try again later.
			return State.RECOVERABLE_FAILURE;
		} catch (ConflictingUpdateException e) {
			// This is thrown when the table gets updated while it is being
			// processed.
			// We cannot continue with this attempt.
			return State.UNRECOVERABLE_FAILURE;
		} catch (NotFoundException e) {
			// This is thrown if the table no longer exits
			return State.UNRECOVERABLE_FAILURE;
		} catch (TableUnavailableException e) {
			// This is a recoverable failure as we can try again later.
			return State.RECOVERABLE_FAILURE;
		} catch (InterruptedException e) {
			// This is a recoverable failure as we can try again later.
			return State.RECOVERABLE_FAILURE;
		} catch (Exception e) {
			log.error("Failed with unknown error", e);
			// Cannot recover from this.
			return State.UNRECOVERABLE_FAILURE;
		}
	}

	/**
	 * This is where the table status gets stated and error handling is
	 * performed.
	 * 
	 * @param tableId
	 * @param token
	 * @return
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 */
	private State createOrUpdateWhileHoldingLock(IdAndVersion idAndVersion, TableIndexManager tableIndexManager,
			String tableResetToken, ChangeMessage change)
			throws ConflictingUpdateException, NotFoundException {
		// Start the real work
		log.info("Create index " + idAndVersion);
		try {
			// Save the status before we start
			// This method will do the rest of the work.
			String lastEtag = synchIndexWithTable(tableIndexManager,
					idAndVersion, tableResetToken, change);
			// We are finished set the status
			log.info("Create index " + idAndVersion + " done");
			tableManagerSupport.attemptToSetTableStatusToAvailable(idAndVersion,
					tableResetToken, lastEtag);
			return State.SUCCESS;
		} catch (TableUnavailableException e) {
			// recoverable
			log.info("Create index " + idAndVersion + " aborted, unavailable");
			return State.RECOVERABLE_FAILURE;
		} catch (Exception e) {
			// Failed.
			// Attempt to set the status to failed.
			tableManagerSupport.attemptToSetTableStatusToFailed(idAndVersion, tableResetToken, e);
			// This is not an error we can recover from.
			log.info("Create index " + idAndVersion + " aborted, unrecoverable");
			return State.UNRECOVERABLE_FAILURE;
		}
	}

	/**
	 * Synchronizes the table index with the table truth data. After the
	 * successful completion of this method the table index schema will match
	 * the schema of the truth and all row changes that have not already been
	 * applied will be applied. Note: This method will do no work if the index
	 * and truth are already synchronized.
	 * 
	 * @param connection
	 * @param tableId
	 * @param status
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws TableUnavailableException
	 */
	String synchIndexWithTable(final TableIndexManager indexManager, IdAndVersion idAndVersion, String resetToken,
			ChangeMessage change) throws DatastoreException, NotFoundException,
			IOException, TableUnavailableException {
		// Create or update the table with this schema.
		tableManagerSupport.attemptToUpdateTableProgress(idAndVersion, resetToken,
				"Creating table ", 0L, 100L);

		// List all change sets applied to this table.
		List<TableRowChange> changes = tableEntityManager.listRowSetsKeysForTable(idAndVersion.toString());
		
		// Calculate the total work to perform
		long totalProgress = 1;
		for (TableRowChange changSet : changes) {
				totalProgress += changSet.getRowCount();
		}
		// Apply each change set not already indexed
		long currentProgress = 0;
		String lastEtag = null;
		if(changes != null){
			for(TableRowChange changeSet: changes){
				currentProgress += changeSet.getRowCount();
				lastEtag = changeSet.getEtag();
				// Only apply changes sets not already applied to the index.
				if(!indexManager.isVersionAppliedToIndex(idAndVersion, changeSet.getRowVersion())){
					// update the progress between actual change.
					tableManagerSupport.attemptToUpdateTableProgress(idAndVersion,
							resetToken, "Applying version: " + changeSet.getRowVersion(), currentProgress,
									totalProgress);
					// Each type of change is applied 
					switch(changeSet.getChangeType()){
					case ROW:
						applyRowChange(indexManager, idAndVersion, changeSet);
						break;
					case COLUMN:
						applyColumnChange(indexManager, idAndVersion,
								changeSet);
						break;
					default:
						throw new IllegalArgumentException("Unknown change type: "+changeSet.getChangeType());
					}
				}
			}
		}
		// After all changes are applied to the index ensure the final schema is set
		List<ColumnModel> currentSchema = tableManagerSupport.getColumnModelsForTable(idAndVersion);
		boolean isTableView = false;
		indexManager.setIndexSchema(idAndVersion, isTableView, currentSchema);
		
		// now that table is created and populated the indices on the table can be optimized.
		indexManager.optimizeTableIndices(idAndVersion);
		
		return lastEtag;
	}

	/**
	 * Apply a column (a schema) change to a table.
	 * 
	 * @param progressCallback
	 * @param indexManager
	 * @param tableId
	 * @param changeSet
	 * @throws IOException
	 */
	void applyColumnChange(final TableIndexManager indexManager, IdAndVersion idAndVersion,
			TableRowChange changeSet) throws IOException {
		ValidateArgument.required(changeSet, "changeSet");
		if(!TableChangeType.COLUMN.equals(changeSet.getChangeType())){
			throw new IllegalArgumentException("Expected: "+TableChangeType.COLUMN);
		}
		// apply the schema change
		List<ColumnChangeDetails> schemaChange = tableEntityManager.getSchemaChangeForVersion(idAndVersion.toString(), changeSet.getRowVersion());
		boolean isTableView = false;
		indexManager.updateTableSchema(idAndVersion, isTableView, schemaChange);
		indexManager.setIndexVersion(idAndVersion, changeSet.getRowVersion());
	}


	/**
	 * Apply a row change to a table.
	 * 
	 * @param progressCallback
	 * @param indexManager
	 * @param tableId
	 * @param changeSet
	 * @throws IOException
	 */
	void applyRowChange(final TableIndexManager indexManager, IdAndVersion idAndVersion,
			TableRowChange change) throws IOException {
		ValidateArgument.required(change, "changeSet");
		if(!TableChangeType.ROW.equals(change.getChangeType())){
			throw new IllegalArgumentException("Expected: "+TableChangeType.ROW);
		}
		// Get the change set.
		SparseChangeSet sparseChangeSet = tableEntityManager.getSparseChangeSet(change);
		// match the schema to the change set.
		boolean isTableView = false;
		indexManager.setIndexSchema(idAndVersion, isTableView, sparseChangeSet.getSchema());
		// attempt to apply this change set to the table.
		indexManager.applyChangeSetToIndex(idAndVersion, sparseChangeSet, change.getRowVersion());
	}


	@Override
	public void setTimeoutSeconds(Long lockTimeoutSec) {
		ValidateArgument.required(lockTimeoutSec, "lockTimeoutSec");
		this.lockTimeoutSec = lockTimeoutSec.intValue();
	}

}
