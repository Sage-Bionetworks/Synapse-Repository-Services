package org.sagebionetworks.worker.job.tracking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is a thread-safe implementation of a {@link JobTracker}. <b> All methods
 * must be synchronized.</b> Since all methods are synchronized,
 * non-synchronized collections are used internally.
 * 
 */
public class JobTrackerImpl implements JobTracker {

	@Autowired
	Clock clock;

	/**
	 * The names of all known jobs.
	 */
	private Set<String> allKnownJobNames = new HashSet<String>();

	/**
	 * Tracks the start times (MS) of jobs that are currently running.
	 */
	private Map<String, Long> startedJobTimes = new HashMap<String, Long>();

	/**
	 * Tracks the elapse times (MS) of jobs that have completed.
	 */
	private Map<String, Long> finishedJobElapsTimes = new HashMap<String, Long>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.worker.utils.JobTracker#jobStarted(java.lang.String)
	 */
	@Override
	public synchronized void jobStarted(String jobName) {
		allKnownJobNames.add(jobName);
		startedJobTimes.put(jobName, clock.currentTimeMillis());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.worker.utils.JobTracker#jobEnded(java.lang.String)
	 */
	@Override
	public synchronized void jobEnded(String jobName) {
		// this job is no longer active
		Long startTime = startedJobTimes.remove(jobName);
		if (startTime != null) {
			long jobElapseTimeMs = clock.currentTimeMillis() - startTime;
			finishedJobElapsTimes.put(jobName, jobElapseTimeMs);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.worker.utils.JobTracker#consumeTrackedData()
	 */
	@Override
	public synchronized TrackedData consumeTrackedData() {
		// Create a copy of the known job names
		Set<String> copyOfAllKnownJobNames = new HashSet<>(allKnownJobNames);
		// Create a copy of the started job times.
		Map<String, Long> copyStartedJobTimes = new HashMap<>(startedJobTimes);
		/*
		 * Consume the finished jobs. The current map is returned while the
		 * local map is replaced with a new empty map.
		 */
		Map<String, Long> copyFinishedJobElapsTimes = finishedJobElapsTimes;
		finishedJobElapsTimes = new HashMap<>(copyFinishedJobElapsTimes.size());
		return new TrackedData(copyOfAllKnownJobNames, copyStartedJobTimes,
				copyFinishedJobElapsTimes);
	}

}
