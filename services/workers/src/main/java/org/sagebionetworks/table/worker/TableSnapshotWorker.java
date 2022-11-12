package org.sagebionetworks.table.worker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatusChangeEvent;
import org.sagebionetworks.worker.TypedMessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.model.Message;

@Service
public class TableSnapshotWorker implements TypedMessageDrivenRunner<TableStatusChangeEvent>  {
	
	private static final Log LOG = LogFactory.getLog(TableSnapshotWorker.class);
	
	private TableEntityManager tableManager;
	private TableManagerSupport tableManagerSupport;

	public TableSnapshotWorker(TableEntityManager tableManager, TableManagerSupport tableManagerSupport) {
		this.tableManager = tableManager;
		this.tableManagerSupport = tableManagerSupport;
	}

	@Override
	public Class<TableStatusChangeEvent> getObjectClass() {
		return TableStatusChangeEvent.class;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message, TableStatusChangeEvent event)
			throws RecoverableMessageException, Exception {
		
		final IdAndVersion tableId = KeyFactory.idAndVersion(event.getObjectId(), event.getObjectVersion());
				
		if (!tableId.getVersion().isPresent()) {
			return;
		}
		
		if (!TableState.AVAILABLE.equals(event.getState())) {
			return;
		}
		
		if (!TableType.table.equals(tableManagerSupport.getTableType(tableId))) {
			return;
		}
		
		try {
			LOG.info("Attempting to store snapshot for table " + tableId +"...");
			tableManager.storeTableSnapshot(tableId, progressCallback);
			LOG.info("Attempting to store snapshot for table " + tableId +"...DONE");
		} catch (LockUnavilableException e) {
			throw new RecoverableMessageException(e);
		} catch (RecoverableMessageException e) {
			throw e;
		} catch (Throwable e) {
			LOG.error("Could not save snapshot for table " + tableId, e);
			throw e;
		}
		
	}

}
