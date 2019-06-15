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
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This worker will completely re-build a table view
 * on any change to a the view schema or scope.
 *
 */
public class TableViewWorker implements ChangeMessageDrivenRunner {
	
	public static final String DEFAULT_ETAG = "DEFAULT";

	static private Logger log = LogManager.getLogger(TableViewWorker.class);
	
	/**
	 * See: PLFM-5456
	 */
	public static int TIMEOUT_SECONDS = 60*10;
	public static int BATCH_SIZE_BYTES = 1024*1024*5; // 5 MBs

	@Autowired
	TableViewManager tableViewManager;
	@Autowired
	TableManagerSupport tableManagerSupport;
	@Autowired
	TableIndexConnectionFactory connectionFactory;
	

	@Override
	public void run(ProgressCallback progressCallback,
			ChangeMessage message) throws RecoverableMessageException,
			Exception {
		// This worker is only works on FileView messages
		if(ObjectType.ENTITY_VIEW.equals(message.getObjectType())){
			final String tableId = message.getObjectId();
			final IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
			final TableIndexManager indexManager;
			try {
				indexManager = connectionFactory.connectToTableIndex(idAndVersion);
				if(ChangeType.DELETE.equals(message.getChangeType())){
					// just delete the index
					indexManager.deleteTableIndex(idAndVersion);
					return;
				}else{
					// create or update the index
					createOrUpdateIndex(idAndVersion, indexManager, progressCallback, message);
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
	public void createOrUpdateIndex(final IdAndVersion idAndVersion, final TableIndexManager indexManager, ProgressCallback outerCallback, final ChangeMessage message) throws RecoverableMessageException{
		// get the exclusive lock to update the table
		try {
			tableManagerSupport.tryRunWithTableExclusiveLock(outerCallback, idAndVersion, TIMEOUT_SECONDS, new ProgressingCallable<Void>() {

				@Override
				public Void call(ProgressCallback innerCallback)
						throws Exception {
					// next level.
					createOrUpdateIndexHoldingLock(idAndVersion, indexManager, innerCallback, message);
					return null;
				}

			} );
		} catch (TableUnavailableException e) {
			// try again later.
			throw new RecoverableMessageException();
		} catch (LockUnavilableException e) {
			// try again later.
			throw new RecoverableMessageException();
		} catch (RecoverableMessageException e) {
			throw e;
		}  catch (Exception e) {
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
	public void createOrUpdateIndexHoldingLock(final IdAndVersion idAndVersion, final TableIndexManager indexManager, final ProgressCallback callback, final ChangeMessage message){
		// Is the index out-of-synch?
		if(!tableManagerSupport.isIndexWorkRequired(idAndVersion)){
			// nothing to do
			return;
		}
		// Start the worker
		final String token = tableManagerSupport.startTableProcessing(idAndVersion);
		try{
			// Look-up the type for this table.
			Long viewTypeMask = tableManagerSupport.getViewTypeMask(idAndVersion);
			// Since this worker re-builds the index, start by deleting it.
			indexManager.deleteTableIndex(idAndVersion);
			// Need the MD5 for the original schema.
			String originalSchemaMD5Hex = tableManagerSupport.getSchemaMD5Hex(idAndVersion);
			// The expanded schema includes etag and benefactorId even if they are not included in the original schema.
			List<ColumnModel> expandedSchema = tableViewManager.getViewSchema(idAndVersion.getId().toString());
			
			// Get the containers for this view.
			Set<Long> allContainersInScope  = tableManagerSupport.getAllContainerIdsForViewScope(idAndVersion, viewTypeMask);

			// create the table in the index.
			boolean isTableView = true;
			indexManager.setIndexSchema(idAndVersion, isTableView, expandedSchema);
			tableManagerSupport.attemptToUpdateTableProgress(idAndVersion, token, "Copying data to view...", 0L, 1L);
			// populate the view by coping data from the entity replication tables.
			Long viewCRC = indexManager.populateViewFromEntityReplication(idAndVersion.getId(), callback, viewTypeMask, allContainersInScope, expandedSchema);
			// now that table is created and populated the indices on the table can be optimized.
			indexManager.optimizeTableIndices(idAndVersion);
			// both the CRC and schema MD5 are used to determine if the view is up-to-date.
			indexManager.setIndexVersionAndSchemaMD5Hex(idAndVersion, viewCRC, originalSchemaMD5Hex);
			// Attempt to set the table to complete.
			tableManagerSupport.attemptToSetTableStatusToAvailable(idAndVersion, token, DEFAULT_ETAG);
		}catch (Exception e){
			// failed.
			tableManagerSupport.attemptToSetTableStatusToFailed(idAndVersion, token, e);
			throw e;
		}

	}	
	
}
