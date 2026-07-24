package org.sagebionetworks.search.workers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.repo.manager.search.SearchIndexLifecycleManager;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.stereotype.Service;

import org.sagebionetworks.database.semaphore.LockReleaseFailedException;

@Service
public class SearchIndexLifecycleWorker implements ChangeMessageDrivenRunner {

	private static final Logger LOG = LogManager.getLogger(SearchIndexLifecycleWorker.class);

	private final NodeDAO nodeDao;
	private final SearchIndexLifecycleManager searchIndexLifecycleManager;

	public SearchIndexLifecycleWorker(NodeDAO nodeDao,
			SearchIndexLifecycleManager searchIndexLifecycleManager) {
		this.nodeDao = nodeDao;
		this.searchIndexLifecycleManager = searchIndexLifecycleManager;
	}

	@Override
	public void run(ProgressCallback progressCallback, ChangeMessage message)
			throws RecoverableMessageException, Exception {
		if (message.getObjectType() != ObjectType.ENTITY && message.getObjectType() != ObjectType.SEARCH_INDEX) {
			return;
		}
		processMessage(progressCallback, message);
	}

	private void processMessage(ProgressCallback progressCallback, ChangeMessage message)
			throws RecoverableMessageException {
		String entityId = message.getObjectId();
		try {
			if (message.getObjectType() == ObjectType.SEARCH_INDEX) {
				// Source-dependency fan-out: a source table/view became available, rebuild if stale.
				searchIndexLifecycleManager.rebuildIfStale(progressCallback, entityId);
				return;
			}
			EntityType nodeType = nodeDao.getNodeTypeById(entityId);
			if (nodeType != EntityType.searchindex) {
				return;
			}
			switch (message.getChangeType()) {
				case CREATE:
					searchIndexLifecycleManager.handleCreate(progressCallback, entityId);
					break;
				case UPDATE:
					searchIndexLifecycleManager.handleUpdate(progressCallback, entityId);
					break;
				case DELETE:
					searchIndexLifecycleManager.handleDelete(progressCallback, entityId);
					break;
				default:
					break;
			}
		} catch (RecoverableMessageException e) {
			// Recoverable paths fire frequently under normal operation — log message
			// only, not the stack trace, to avoid log spam.
			LOG.warn("Recoverable exception for entity {}: {}", entityId, e.getMessage());
			throw e;
		} catch (TableUnavailableException | LockUnavilableException e) {
			LOG.warn("Source table unavailable for entity {}, retrying: {}", entityId, e.getMessage());
			throw new RecoverableMessageException(e);
		} catch (TableFailedException e) {
			// Permanent failure — the manager already recorded FAILED in the DAO.
			LOG.error("Source table failed for entity {}: {}", entityId, e.getMessage());
		} catch (LockReleaseFailedException | CannotAcquireLockException | DeadlockLoserDataAccessException e) {
			LOG.warn("Transient lock exception for entity {}, retrying: {}", entityId, e.getMessage());
			throw new RecoverableMessageException(e);
		} catch (NotFoundException e) {
			try {
				searchIndexLifecycleManager.handleDelete(progressCallback, entityId);
			} catch (RecoverableMessageException rme) {
				LOG.warn("Recoverable exception for entity {}: {}", entityId, rme.getMessage());
				throw rme;
			} catch (Exception deleteEx) {
				LOG.error("Failed to process lifecycle message for entity: " + entityId, deleteEx);
			}
		} catch (Throwable e) {
			// Unexpected — keep full stack trace; this is the path that surfaces real bugs.
			LOG.error("Failed to process lifecycle message for entity: " + entityId, e);
		}
	}
}
