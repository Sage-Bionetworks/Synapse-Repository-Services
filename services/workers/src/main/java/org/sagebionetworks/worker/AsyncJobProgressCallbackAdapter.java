package org.sagebionetworks.worker;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressListener;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;

/**
 * Adapter for an {@link AsyncJobProgressCallback} that wraps the original {@link ProgressCallback} passed by the worker framework
 */
public class AsyncJobProgressCallbackAdapter implements AsyncJobProgressCallback {
	
	private AsynchJobStatusManager manager;
	private ProgressCallback progressCallback;
	private String jobId;

	public AsyncJobProgressCallbackAdapter(AsynchJobStatusManager manager, ProgressCallback progressCallback, String jobId) {
		this.manager = manager;
		this.progressCallback = progressCallback;
		this.jobId = jobId;
	}

	@Override
	public void addProgressListener(ProgressListener listener) {
		progressCallback.addProgressListener(listener);
	}

	@Override
	public void removeProgressListener(ProgressListener listener) {
		progressCallback.removeProgressListener(listener);
	}

	@Override
	public long getLockTimeoutSeconds() {
		return progressCallback.getLockTimeoutSeconds();
	}

	@Override
	public void updateProgress(String message, Long progressCurrent, Long progressTotal) {
		manager.updateJobProgress(jobId, progressCurrent, progressTotal, message);
	}

}
