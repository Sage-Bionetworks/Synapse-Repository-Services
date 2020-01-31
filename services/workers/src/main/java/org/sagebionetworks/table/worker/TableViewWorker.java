package org.sagebionetworks.table.worker;

import org.apache.logging.log4j.Logger;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionUnavailableException;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
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

	TableViewManager tableViewManager;
	Logger log;
	
	@Autowired
	public TableViewWorker(TableViewManager tableViewManager, LoggerProvider loggerProvider) {
		super();
		this.tableViewManager = tableViewManager;
		this.log = loggerProvider.getLogger(TableViewWorker.class.getName());
	}



	@Override
	public void run(ProgressCallback progressCallback,
			ChangeMessage message) throws RecoverableMessageException,
			Exception {
		// This worker is only works on FileView messages
		if(ObjectType.ENTITY_VIEW.equals(message.getObjectType())){
			final long tableId = KeyFactory.stringToKey(message.getObjectId());
			final IdAndVersion idAndVersion = IdAndVersion.newBuilder().setId(tableId)
					.setVersion(message.getObjectVersion()).build();
			try {
				if(ChangeType.DELETE.equals(message.getChangeType())){
					// just delete the index
					tableViewManager.deleteViewIndex(idAndVersion);
					return;
				}else{
					// create or update the index
					tableViewManager.createOrUpdateViewIndex(idAndVersion, progressCallback);
				}
			} catch (RecoverableMessageException e) {
				throw e;
			} catch (TableIndexConnectionUnavailableException | TableUnavailableException | LockUnavilableException e) {
				// try again later.
				throw new RecoverableMessageException(e);
			} catch (Exception e) {
				log.error("Failed to build view index: ", e);
			}
		}
	}
	
}
