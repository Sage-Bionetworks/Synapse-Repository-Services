package org.sagebionetworks.table.worker;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.manager.table.FileViewManager;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionUnavailableException;
import org.sagebionetworks.repo.manager.table.TableIndexManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This file view worker will completely re-build a file view
 * on any FileView change.
 *
 */
public class FileViewWorker implements ChangeMessageDrivenRunner {
	
	static private Logger log = LogManager.getLogger(FileViewWorker.class);
	
	public static int TIMEOUT_MS = 1000*60*10;

	@Autowired
	FileViewManager tableViewManager;
	@Autowired
	TableManagerSupport tableManagerSupport;
	@Autowired
	TableIndexConnectionFactory connectionFactory;
	

	@Override
	public void run(ProgressCallback<ChangeMessage> progressCallback,
			ChangeMessage message) throws RecoverableMessageException,
			Exception {
		// This worker is only works on FileView messages
		if(ObjectType.FILE_VIEW.equals(message.getObjectType())){
			final String tableId = message.getObjectId();
			final TableIndexManager indexManager;
			try {
				indexManager = connectionFactory.connectToTableIndex(tableId);
				if(ChangeType.DELETE.equals(message.getChangeType())){
					// just delete the index
					indexManager.deleteTableIndex();
					return;
				}else{
					// create or update the index
					createOrUpdateIndex(tableId, indexManager, progressCallback);
				}
			} catch (TableIndexConnectionUnavailableException e) {
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
	public void createOrUpdateIndex(final String tableId, final TableIndexManager indexManager, ProgressCallback<ChangeMessage> outerCallback) throws RecoverableMessageException{
		// get the exclusive lock to update the table
		try {
			tableManagerSupport.tryRunWithTableExclusiveLock(outerCallback, tableId, TIMEOUT_MS, new ProgressingCallable<Void, ChangeMessage>() {

				@Override
				public Void call(ProgressCallback<ChangeMessage> innerCallback)
						throws Exception {
					// next level.
					createOrUpdateIndexHoldingLock(tableId, indexManager, innerCallback);
					return null;
				}

			} );
		} catch (LockUnavilableException e) {
			// try again later.
			throw new RecoverableMessageException();
		} catch (RecoverableMessageException e) {
			// pass it along
			throw e;
		} catch (Exception e) {
			log.error("Failed to build index: ", e);
		}
	}
	
	/**
	 * Create or update the index for the given table while holding the lock.
	 * 
	 * @param tableId
	 * @param indexManager
	 * @param callback
	 */
	public void createOrUpdateIndexHoldingLock(String tableId, TableIndexManager indexManager, ProgressCallback<ChangeMessage> callback){
		// Is the index out-of-synch?
		if(tableManagerSupport.isIndexSynchronizedWithTruth(tableId)){
			// nothing to do
			return;
		}
		// Start the worker
		String token = tableManagerSupport.startTableProcessing(tableId);
		long currentProgress = 0L;
		long totalProgress = 100L;
		
		tableManagerSupport.attemptToUpdateTableProgress(tableId, token, "Creating view...", currentProgress, totalProgress);
		// Since this worker re-builds the index, start by deleting it.
		indexManager.deleteTableIndex();
		// Lookup the table's schema
		List<ColumnModel> currentSchema = tableManagerSupport.getColumnModelsForTable(tableId);
		// create the table
		indexManager.setIndexSchema(currentSchema);
		
		
	}

}
