package org.sagebionetworks.worker.utils;

import org.sagebionetworks.workers.util.Gate;

/**
 * A simple gate that is always open. Specifically, {@link #canRun()} will
 * always return true. This gate is used for workers that need to run in read-only mode.
 *
 */
public class AlwaysOpen implements Gate {

	@Override
	public boolean canRun() {
		return true;
	}

}
