package org.sagebionetworks.repo.manager.migration;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.sagebionetworks.common.util.progress.AutoProgressingCallable;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;

public class MigrationManagerSupportImpl implements MigrationManagerSupport {

	public static final long TABLE_PROCESSING_TIMEOUT_MS = 1000*60*10; // 10 mins
	public static final long AUTO_PROGRESS_FREQUENCY_MS = 5*1000; // 5 seconds

	@Autowired
	ExecutorService migrationExecutorService;

	public <R> R callWithAutoProgress(ProgressCallback<Void> callback, Callable<R> callable) throws Exception {
		AutoProgressingCallable<R> auto = new AutoProgressingCallable<R>(
				migrationExecutorService, callable, AUTO_PROGRESS_FREQUENCY_MS);
		return auto.call(callback);
	}

}
