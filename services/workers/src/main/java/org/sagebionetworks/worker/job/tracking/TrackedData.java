package org.sagebionetworks.worker.job.tracking;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data tracked for running jobs.
 * 
 */
public class TrackedData {

	/**
	 * The names of all known jobs.
	 */
	private Set<String> allKnownJobNames;

	/**
	 * Tracks the start times (MS) of jobs that are currently running.
	 */
	private Map<String, Long> startedJobTimes;
	/**
	 * Tracks the elapse times (MS) of jobs that have completed.
	 */
	private Map<String, List<Long>> finishedJobElapsTimes;

	/**
	 * Create a new
	 * 
	 * @param allKnownJobNames
	 * @param startedJobTimes
	 * @param finishedJobElapsTimes
	 */
	public TrackedData(Set<String> allKnownJobNames,
			Map<String, Long> startedJobTimes,
			Map<String, List<Long>> finishedJobElapsTimes) {
		super();
		this.allKnownJobNames = allKnownJobNames;
		this.startedJobTimes = startedJobTimes;
		this.finishedJobElapsTimes = finishedJobElapsTimes;
	}

	/**
	 * The names of all known jobs.
	 * 
	 * @return
	 */
	public Set<String> getAllKnownJobNames() {
		return allKnownJobNames;
	}

	/**
	 * Tracks the start times (MS) of jobs that are currently running.
	 * 
	 * @return
	 */
	public Map<String, Long> getStartedJobTimes() {
		return startedJobTimes;
	}

	/**
	 * Tracks the elapse times (MS) of jobs that have completed.
	 * 
	 * @return
	 */
	public Map<String, List<Long>> getFinishedJobElapsTimes() {
		return finishedJobElapsTimes;
	}

}
