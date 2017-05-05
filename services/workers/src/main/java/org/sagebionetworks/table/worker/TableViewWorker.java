package org.sagebionetworks.table.worker;

import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionUnavailableException;
import org.sagebionetworks.repo.manager.table.TableIndexManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This worker will completely re-build a table view
 * on any change to a the view schema or scope.
 *
 */
public class TableViewWorker implements ChangeMessageDrivenRunner {
	
	public static final String ALREADY_SYNCHRONIZED = "Already Synchronized";

	public static final String DEFAULT_ETAG = "DEFAULT";

	static private Logger log = LogManager.getLogger(TableViewWorker.class);
	
	public static int TIMEOUT_MS = 1000*60*10;
	public static int BATCH_SIZE_BYTES = 1024*1024*5; // 5 MBs

	@Autowired
	TableViewManager tableViewManager;
	@Autowired
	TableManagerSupport tableManagerSupport;
	@Autowired
	TableIndexConnectionFactory connectionFactory;
	

	@Override
	public void run(ProgressCallback<Void> progressCallback,
			ChangeMessage message) throws RecoverableMessageException,
			Exception {
		// This worker is only works on FileView messages
		if(ObjectType.ENTITY_VIEW.equals(message.getObjectType())){
			final String tableId = message.getObjectId();
			final TableIndexManager indexManager;
			try {
				indexManager = connectionFactory.connectToTableIndex(tableId);
				if(ChangeType.DELETE.equals(message.getChangeType())){
					log.info("Deleting index for view: "+tableId);
					// just delete the index
					indexManager.deleteTableIndex(tableId);
					return;
				}else{
					// create or update the index
					createOrUpdateIndex(tableId, indexManager, progressCallback, message);
				}
			} catch (TableIndexConnectionUnavailableException e) {
				log.info("Connection unavailable for view: "+tableId+", message will be returned to the queue.");
				// try again later.
				throw new RecoverableMessageException();
			}
		}
	}
	
	/**
	 * Create or update the index for the given table.
	 * 
	 * @param tableId
	 * @param indexManager
	 * @throws RecoverableMessageException 
	 */
	public void createOrUpdateIndex(final String tableId, final TableIndexManager indexManager, ProgressCallback<Void> outerCallback, final ChangeMessage message) throws RecoverableMessageException{
		// get the exclusive lock to update the table
		try {
			log.info("Attempting to acquire exclusive lock for view: "+tableId+" ...");
			tableManagerSupport.tryRunWithTableExclusiveLock(outerCallback, tableId, TIMEOUT_MS, new ProgressingCallable<Void, Void>() {

				@Override
				public Void call(ProgressCallback<Void> innerCallback)
						throws Exception {
					// next level.
					createOrUpdateIndexHoldingLock(tableId, indexManager, innerCallback, message);
					return null;
				}

			} );
		} catch (TableUnavailableException e) {
			log.info("TableUnavailableException for view: "+tableId+", message will be returned to the queue.");
			// try again later.
			throw new RecoverableMessageException();
		} catch (LockUnavilableException e) {
			log.info("LockUnavilableException for view: "+tableId+", message will be returned to the queue.");
			// try again later.
			throw new RecoverableMessageException();
		} catch (RecoverableMessageException e) {
			throw e;
		}  catch (Exception e) {
			log.error("Failed to build view: "+tableId, e);
		}
	}
	
	/**
	 * Create or update the index for the given table while holding the lock.
	 * 
	 * @param tableId
	 * @param indexManager
	 * @param callback
	 */
	public void createOrUpdateIndexHoldingLock(final String tableId, final TableIndexManager indexManager, final ProgressCallback<Void> callback, final ChangeMessage message){
		// Start the worker
		final String token = tableManagerSupport.startTableProcessing(tableId);
		// Is the index out-of-synch?
		if(tableManagerSupport.isIndexSynchronizedWithTruth(tableId)){
			/*
			 * See PLFM-4366. When the view is already synchronized, it must be
			 * unconditionally set to available.
			 */
			log.info("Index for view: "+tableId+" is already synchronized. Setting the view to available.");
			// Attempt to set the table to complete.
			tableManagerSupport.attemptToSetTableStatusToAvailable(tableId, token, ALREADY_SYNCHRONIZED);
			return;
		}
		log.info("Processing index for view: "+tableId+" with token: "+token);
		try{
			// Look-up the type for this table.
			ViewType viewType = tableManagerSupport.getViewType(tableId);

			// Since this worker re-builds the index, start by deleting it.
			indexManager.deleteTableIndex(tableId);
			callback.progressMade(null);
			// Need the MD5 for the original schema.
			List<ColumnModel> originalSchema = tableManagerSupport.getColumnModelsForTable(tableId);
			String originalSchemaMD5Hex = TableModelUtils.createSchemaMD5HexCM(originalSchema);
			// The expanded schema includes etag and benefactorId even if they are not included in the original schema.
			List<ColumnModel> expandedSchema = tableViewManager.getViewSchemaWithRequiredColumns(tableId);
			
			// Get the containers for this view.
			Set<Long> allContainersInScope  = tableManagerSupport.getAllContainerIdsForViewScope(tableId);

			// create the table in the index.
			indexManager.setIndexSchema(tableId, callback, expandedSchema);
			callback.progressMade(null);
			tableManagerSupport.attemptToUpdateTableProgress(tableId, token, "Copying data to view...", 0L, 1L);
			// populate the view by coping data from the entity replication tables.
			Long viewCRC = indexManager.populateViewFromEntityReplication(tableId, callback, viewType, allContainersInScope, expandedSchema);
			callback.progressMade(null);
			// now that table is created and populated the indices on the table can be optimized.
			indexManager.optimizeTableIndices(tableId);
			callback.progressMade(null);
			// both the CRC and schema MD5 are used to determine if the view is up-to-date.
			indexManager.setIndexVersionAndSchemaMD5Hex(tableId, viewCRC, originalSchemaMD5Hex);
			// Attempt to set the table to complete.
			tableManagerSupport.attemptToSetTableStatusToAvailable(tableId, token, DEFAULT_ETAG);
		}catch (Exception e){
			// failed.
			tableManagerSupport.attemptToSetTableStatusToFailed(tableId, token, e);
			throw e;
		}

	}	
	
}
