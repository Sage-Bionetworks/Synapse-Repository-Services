package org.sagebionetworks.asynchronous.workers.timed;

import org.sagebionetworks.util.ProgressCallback;

public abstract class TimedWorker {

	public abstract void run(ProgressCallback<Void> progressCallback) throws Exception;
}
