package org.sagebionetworks.worker;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Simple worker to trigger the cleanup of rows in the ID generator database.
 * 
 *
 */
public class IdGeneratorCleanupWorker  implements ProgressingRunner  {
	
	/**
	 * A limit of 100K is small enough that calls should return in under
	 * a few seconds but large enough to stay ahead of data generation.
	 */
	public static final long ROW_LIMIT = 100000;
	
	public static final long TWO_SECONDS_MS =  2*1000;
	
	@Autowired
	IdGenerator idGenerator;
	@Autowired
	Clock clock;

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		// Trigger cleanup of each type when fired
		for(IdType type: IdType.values()) {
			cleanupType(type);
		}
	}

	/**
	 * Cleanup a single type.
	 * @param type
	 * @throws InterruptedException 
	 */
	void cleanupType(IdType type) throws InterruptedException {
		// time each call
		long statTimeMs = clock.currentTimeMillis();
		idGenerator.cleanupType(type, ROW_LIMIT);
		long elapseMS = clock.currentTimeMillis()-statTimeMs;
		// Sleep to guarantee the cleanup does not dominate the database activity.
		clock.sleep(2*elapseMS);
	}

}
