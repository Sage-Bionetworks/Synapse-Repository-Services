package org.sagebionetworks.worker;

import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressingRunner;
import org.springframework.beans.factory.annotation.Autowired;

public class SemaphoreGarbageCollection implements ProgressingRunner {
	
	@Autowired
	CountingSemaphore countingSemaphore;

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		countingSemaphore.runGarbageCollection();
	}

}
