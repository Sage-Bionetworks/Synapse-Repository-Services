package org.sagebionetworks.trash.worker;

import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.manager.trash.TrashManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;

public class TrashWorker implements ProgressingRunner {
	private final static Logger LOG = LogManager.getLogger(TrashWorker.class);
	protected static final long TRASH_BATCH_SIZE = 10000;
	protected static final long CUTOFF_TRASH_AGE_IN_DAYS = 30; // about 1 month

	@Autowired
	private TrashManager trashManager;

	@Autowired
	private StackStatusDao stackStatusDao;

	@Autowired
	private WorkerLogger workerLogger;

	private UserInfo adminUser = new UserInfo(true, BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

	@Override
	public void run(ProgressCallback progressCallback) {

		try {
			long startTime = System.currentTimeMillis();

			// Retrieve a batch of entities to delete, this will be sorted by deletion date desc
			// Within one run the worker will delete a maximum of TRASH_BATCH_SIZE entities to avoid starvation
			List<Long> batch = trashManager.getTrashLeavesBefore(CUTOFF_TRASH_AGE_IN_DAYS, TRASH_BATCH_SIZE);
			
			if (batch.isEmpty()) {
				return;
			}

			LOG.info("Purging {} entities, older than {} days, from the trash can", batch.size(), CUTOFF_TRASH_AGE_IN_DAYS);

			int count = 0;
			int errors = 0;

			// Deletes one at the time (the sub-tree of a single entity might contain millions of nodes)
			for (Long id : batch) {

				// If the status of the stack changed makes sure to interrupt the execution

				if (!stackStatusDao.isStackReadWrite()) {
					LOG.info("Stack status changed from READ_WRITE, stopping execution.");
					logProgress(count, errors, startTime);
					return;
				}

				try {
					trashManager.purgeTrash(adminUser, Collections.singletonList(id));
				} catch (Exception e) {
					// Log the error and keep going, we want to get as far as we can
					LOG.error("Could not delete entity with id {}: {}", id, e.getMessage(), e);
					errors++;
				}
				count++;
			}

			logProgress(count, errors, startTime);
		} catch (Throwable e) {
			LOG.error(e.getMessage(), e);

			boolean willRetry = false;
			// Sends a fail metric for cloud watch
			workerLogger.logWorkerFailure(TrashWorker.class.getName(), e, willRetry);
		}
	}

	private void logProgress(int count, int errors, long startTime) {
		LOG.info("Sucessfully purged {} trashed entities (Skipped: {}, Time: {} ms).", count, errors,
				System.currentTimeMillis() - startTime);
	}

}
