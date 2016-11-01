package org.sagebionetworks.migration.worker;

import java.util.concurrent.Callable;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.migration.MigrationManager;
import org.sagebionetworks.repo.manager.migration.MigrationManagerSupport;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumResult;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountResult;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.springframework.beans.factory.annotation.Autowired;

public class AsyncMigrationRequestProcessorImpl implements AsyncMigrationRequestProcessor {

	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private MigrationManager migrationManager;
	@Autowired
	private MigrationManagerSupport migrationManagerSupport;

	@Override
	public void processAsyncMigrationTypeCountRequest(
			final ProgressCallback<Void> progressCallback, final UserInfo user,
			final AsyncMigrationTypeCountRequest mtcReq, final String jobId) throws Throwable {

		final String t = mtcReq.getType();
		final MigrationType mt = MigrationType.valueOf(t);
		try {
			migrationManagerSupport.callWithAutoProgress(progressCallback, new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					MigrationTypeCount mtc = migrationManager.getMigrationTypeCount(user, mt);
					AsyncMigrationTypeCountResult res = new AsyncMigrationTypeCountResult();
					res.setCount(mtc);
					asynchJobStatusManager.setComplete(jobId, res);
					return null;
				}
			});
		} catch (Throwable e) {
			// Record the error
			asynchJobStatusManager.setJobFailed(jobId, e);
			throw e;
		}

	}

	@Override
	public void processAsyncMigrationRangeChecksumRequest(
			final ProgressCallback<Void> progressCallback, final UserInfo user,
			final AsyncMigrationRangeChecksumRequest mrcReq, final String jobId)
			throws Throwable {
		
		final String t = mrcReq.getType();
		final MigrationType mt = MigrationType.valueOf(t);
		final String salt = mrcReq.getSalt();
		final long minId = mrcReq.getMinId();
		final long maxId = mrcReq.getMaxId();
		try {
			migrationManagerSupport.callWithAutoProgress(progressCallback, new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					MigrationRangeChecksum mrc = migrationManager.getChecksumForIdRange(user, mt, salt, minId, maxId);
					AsyncMigrationRangeChecksumResult res = new AsyncMigrationRangeChecksumResult();
					res.setCount(mrc);
					asynchJobStatusManager.setComplete(jobId, res);
					return null;
				}
			});
		} catch (Throwable e) {
			// Record the error
			asynchJobStatusManager.setJobFailed(jobId, e);
			throw e;
		}
		
	}

}
