package org.sagebionetworks.worker.job.tracking;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.audit.utils.VirtualMachineIdProvider;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.MetricStats;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.cloudwatch.model.StandardUnit;

/**
 * This class is driven by a timer thread, to calculate metric statistics for
 * running jobs at a regular interval.
 * 
 */
public class JobIntervalProcessor {

	/**
	 * The frequency that metrics are pushed to CloudWatch.
	 */
	public static final long METRIC_PUSH_FREQUENCY_MS = 1000 * 60; // one minute

	public static final String DIMENSION_WORKER_NAME = "Worker Name";
	public static final String DIMENSION_INSTANCE_ID = "Instance ID";
	public static final String METRIC_NAME_PERCENT_TIMET_RUNNING = "% Time Running";
	public static final String NAMESPACE_WORKER_STATISTICS = "Worker-Statistics-Test-5"+ StackConfiguration.getStackInstance();

	@Autowired
	JobTracker jobTracker;

	@Autowired
	Consumer consumer;

	@Autowired
	Clock clock;

	Long lastPushTimeMS;

	/*
	 * These maps are only access from the timer thread.
	 */
	Map<String, IntervalStatistics> percentTimeRunningStatistics = new HashMap<>();

	/**
	 * Called from a timer to gather statistics at a regular interval.
	 * 
	 */
	public void timerFired() {
		long now = clock.currentTimeMillis();
		if (lastPushTimeMS == null) {
			// this is the first call
			lastPushTimeMS = now;
		}
		// Calculate the metrics for this time slice
		calculateMetrics(now);

		long timeSinceLastPush = now - lastPushTimeMS;
		if (timeSinceLastPush > METRIC_PUSH_FREQUENCY_MS) {
			// publish the current metrics.
			publishAndRestMetrics(now);
			lastPushTimeMS = now;
		}
	}

	/**
	 * Calculate all of the metrics for a single time slice.
	 * 
	 */
	public void calculateMetrics(long now) {
		/*
		 * Copies of the synchronized collections are made to minimize the
		 * impact of synchronization of the running jobs. The Copies are not
		 * synchronized since they are only access within this method.
		 */
		TrackedData consumedData = jobTracker.consumeTrackedData();
		
		// percentage of time spent running.
		calculatePercentTimeRunning(consumedData);
	}

	/**
	 * For each job type calculate the percentage of time that the job is spent
	 * running.
	 * 
	 * @param consumedData
	 */
	public void calculatePercentTimeRunning(TrackedData consumedData) {
		/*
		 *  A job that is currently running is at 100% while a job that is not
		 *  running is at 0%.
		 */
		for(String jobName: consumedData.getAllKnownJobNames()){
			// start with 0%
			double percentage = 0;
			if(consumedData.getStartedJobTimes().containsKey(jobName)){
				// this job is running so it is at 100% for this interval
				percentage = 100;
			}
			addValueToIntervalMap(jobName, percentage, percentTimeRunningStatistics);
		}
		
	}
	
	/**
	 * Add a value to the IntervalStatistics for the given job name.
	 * @param jobName
	 * @param value
	 * @param map
	 */
	public static void addValueToIntervalMap(String jobName, double value, Map<String, IntervalStatistics> map){
		IntervalStatistics jobStats = map.get(jobName);
		if(jobStats == null){
			// new interval
			jobStats = new IntervalStatistics(value);
			map.put(jobName, jobStats);
		}else{
			// existing interval
			jobStats.addValue(value);
		}
	}
	
	
	/**
	 * Publish all of the current metrics to CloudWatch and then reset all of
	 * the metric data.
	 */
	public void publishAndRestMetrics(long now) {
		Date timestamp = new Date(now);
		// Create the profile data
		List<ProfileData> percentageData = createProfileDataForMap(
				percentTimeRunningStatistics, 
				NAMESPACE_WORKER_STATISTICS,
				METRIC_NAME_PERCENT_TIMET_RUNNING,
				StandardUnit.Percent.name(),
				timestamp);
		
		// publish the metric to cloudwatch
		consumer.addProfileData(percentageData);
		
		// reset the interval data
		percentTimeRunningStatistics.clear();
	}
	
	/**
	 * Create a list of metrics for a given interval map.
	 * @param nameSpace
	 * @param metricName
	 * @param units
	 * @param intervalMap
	 * @param timestamp
	 * @return
	 */
	public static List<ProfileData> createProfileDataForMap(Map<String, IntervalStatistics> intervalMap, String nameSpace, String metricName, String units, Date timestamp){
		List<ProfileData> results = new LinkedList<>();
		for(String jobName: intervalMap.keySet()){
			IntervalStatistics interval = intervalMap.get(jobName);
			ProfileData pd = createProfileData(nameSpace, units, metricName, interval, jobName, timestamp);
			results.add(pd);
		}
		return results;
	}
	
	/**
	 * Helper to create a single ProfileData for an interval.
	 * @param nameSpace
	 * @param units
	 * @param dimension
	 * @param interval
	 * @param name
	 * @return
	 */
	public static ProfileData createProfileData(String nameSpace, String units, String metricName, IntervalStatistics interval, String workerName, Date timestamp){
		// convert the interval to a metric
		MetricStats stats = new MetricStats();
		stats.setMaximum(interval.getMaximumValue());
		stats.setMinimum(interval.getMinimumValue());
		stats.setSum(interval.getValueSum());
		stats.setCount(new Double(interval.getValueCount()));
		// build the data
		ProfileData pd = new ProfileData();
		pd.setName(metricName);
		pd.setMetricStats(stats);
		pd.setNamespace(nameSpace);
		pd.setUnit(units);
		Map<String, String> dimensions = new HashMap<String, String>(1);
		dimensions.put(DIMENSION_WORKER_NAME, workerName);
		pd.setDimension(dimensions);
		pd.setTimestamp(timestamp);
		return pd;
	}
}
