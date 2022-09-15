package org.sagebionetworks.worker.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.workers.util.Gate;

/**
 * A simple gate that is always open. Specifically, {@link #canRun()} will
 * always return true. This gate is used for workers that need to run in read-only mode.
 *
 */
public class AlwaysOpen implements Gate {

	static private Logger log = LogManager.getLogger(AlwaysOpen.class);

	@Override
	public boolean canRun() {
		return true;
	}

	@Override
	public void runFailed(Exception error) {
		log.error("Failed:", error);
	}

}
