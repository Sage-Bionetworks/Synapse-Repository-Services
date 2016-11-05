package org.sagebionetworks.migration.worker;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.sagebionetworks.common.util.progress.AutoProgressingCallable;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.migration.MigrationManager;
import org.sagebionetworks.repo.manager.migration.MigrationManagerSupport;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumResult;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRowMetadataRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRowMetadataResult;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeChecksumResult;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountResult;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.springframework.beans.factory.annotation.Autowired;

public class AsyncMigrationRequestProcessorImpl implements AsyncMigrationRequestProcessor {

	public static final long AUTO_PROGRESS_FREQUENCY_MS = 5*1000; // 5 seconds

	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private MigrationManager migrationManager;
	@Autowired
	ExecutorService migrationExecutorService;


	@Override
	public void processAsyncMigrationTypeCountRequest(
			final ProgressCallback<Void> progressCallback, final UserInfo user,
			final AsyncMigrationTypeCountRequest mtcReq, final String jobId) throws Throwable {

		final String t = mtcReq.getType();
		final MigrationType mt = MigrationType.valueOf(t);

		try {
			callWithAutoProgress(progressCallback, new Callable<Void>() {
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
			callWithAutoProgress(progressCallback, new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					MigrationRangeChecksum mrc = migrationManager.getChecksumForIdRange(user, mt, salt, minId, maxId);
					AsyncMigrationRangeChecksumResult res = new AsyncMigrationRangeChecksumResult();
					res.setChecksum(mrc);
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
	public void processAsyncMigrationTypeChecksumRequest(
			final ProgressCallback<Void> progressCallback, final UserInfo user,
			final AsyncMigrationTypeChecksumRequest mtcReq, final String jobId)
			throws Throwable {

		final String t = mtcReq.getType();
		final MigrationType mt = MigrationType.valueOf(t);

		try {
			callWithAutoProgress(progressCallback, new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					MigrationTypeChecksum mtc = migrationManager.getChecksumForType(user, mt);
					AsyncMigrationTypeChecksumResult res = new AsyncMigrationTypeChecksumResult();
					res.setChecksum(mtc);
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
	public void processAsyncMigrationRowMetadataRequest(
			final ProgressCallback<Void> progressCallback, final UserInfo user,
			final AsyncMigrationRowMetadataRequest mrmReq, final String jobId)
			throws Throwable {

		final String t = mrmReq.getType();
		final MigrationType mt = MigrationType.valueOf(t);
		final Long minId = mrmReq.getMinId();
		final Long maxId = mrmReq.getMaxId();
		final Long limit = mrmReq.getLimit();
		final Long offset = mrmReq.getOffset();

		try {
			callWithAutoProgress(progressCallback, new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					RowMetadataResult rmr = migrationManager.getRowMetadataByRangeForType(user, mt, minId, maxId, limit, offset);
					AsyncMigrationRowMetadataResult res = new AsyncMigrationRowMetadataResult();
					res.setRowMetadata(rmr);
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

	private <R> R callWithAutoProgress(ProgressCallback<Void> callback, Callable<R> callable) throws Exception {
		AutoProgressingCallable<R> auto = new AutoProgressingCallable<R>(
				migrationExecutorService, callable, AUTO_PROGRESS_FREQUENCY_MS);
		return auto.call(callback);
	}

}
