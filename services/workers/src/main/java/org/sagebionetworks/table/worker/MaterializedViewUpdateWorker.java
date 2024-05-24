package org.sagebionetworks.table.worker;

import org.apache.logging.log4j.Logger;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.repo.manager.table.MaterializedViewManager;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionUnavailableException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This worker will build/rebuild the index for materialized views.
 *
 */
@Service
public class MaterializedViewUpdateWorker implements ChangeMessageDrivenRunner {

	private final MaterializedViewManager materializedViewManager;
	private final Logger log;

	@Autowired
	public MaterializedViewUpdateWorker(MaterializedViewManager materializedViewManager,
			LoggerProvider loggerProvider) {
		super();
		this.materializedViewManager = materializedViewManager;
		this.log = loggerProvider.getLogger(MaterializedViewUpdateWorker.class.getName());
	}

	@Override
	public void run(ProgressCallback progressCallback, ChangeMessage message)
			throws RecoverableMessageException, Exception {
		if (ObjectType.MATERIALIZED_VIEW.equals(message.getObjectType())) { 
			final IdAndVersion idAndVersion = KeyFactory.idAndVersion(message.getObjectId(), message.getObjectVersion());
			try {
				if (ChangeType.DELETE.equals(message.getChangeType())) {
					materializedViewManager.deleteViewIndex(idAndVersion);
				} else {
					materializedViewManager.createOrUpdateViewIndex(progressCallback, idAndVersion);
				}
			} catch (RecoverableMessageException e) {
				throw e;
			} catch (TableIndexConnectionUnavailableException | LockUnavilableException | TableUnavailableException e) {
				// try again later.
				throw new RecoverableMessageException(e);
			} catch (Exception e) {
				log.error("Failed to build materialized view index: ", e);
			}
		}
	}

}
