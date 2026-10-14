package org.sagebionetworks.recordset.worker;

import org.apache.logging.log4j.Logger;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.repo.manager.table.RecordSetIndexManager;
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
 * Builds the queryable indexes for a RecordSet on each RECORDSET change
 * message. Each new version triggers a fresh rebuild of both the entity-level
 * alias T{id} and the per-version snapshot T{id}_{v}; both are populated from
 * the same single-pass read of the current version's CSV.
 */
@Service
public class RecordSetIndexWorker implements ChangeMessageDrivenRunner {

	private final RecordSetIndexManager recordSetIndexManager;
	private final Logger log;

	@Autowired
	public RecordSetIndexWorker(RecordSetIndexManager recordSetIndexManager, LoggerProvider loggerProvider) {
		this.recordSetIndexManager = recordSetIndexManager;
		this.log = loggerProvider.getLogger(RecordSetIndexWorker.class.getName());
	}

	@Override
	public void run(ProgressCallback progressCallback, ChangeMessage message)
			throws RecoverableMessageException, Exception {
		if (!ObjectType.RECORDSET.equals(message.getObjectType())) {
			return;
		}
		final IdAndVersion idAndVersion = IdAndVersion.newBuilder()
				.setId(KeyFactory.stringToKey(message.getObjectId()))
				.setVersion(message.getObjectVersion())
				.build();
		try {
			if (ChangeType.CREATE.equals(message.getChangeType()) || ChangeType.UPDATE.equals(message.getChangeType())) {
				recordSetIndexManager.createOrUpdateRecordSetIndex(idAndVersion, progressCallback);
			}
		} catch (RecoverableMessageException | TableIndexConnectionUnavailableException | TableUnavailableException | LockUnavilableException e) {
			log.error("Will retry.  Message: {}", e.getMessage());
			if (e instanceof RecoverableMessageException) {
				throw e;
			}
			throw new RecoverableMessageException(e);
		} catch (Exception e) {
			log.error("Failed to build RecordSet index for " + idAndVersion, e);
		}
	}

}
