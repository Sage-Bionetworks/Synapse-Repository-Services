package org.sagebionetworks.repo.memory;

import java.util.Collections;
import java.util.Date;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.audit.utils.VirtualMachineIdProvider;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.MetricStats;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.cloudwatch.model.StandardUnit;

/**
 * A memory logger that publishes memory statistics to cloud watch. This logger
 * gathers maximum, minimum, count and, sum statistics for memory used
 * (total-free) for a fixed period of time. Memory statistics are gathered every
 * 10 MS, however the statistics are only pushed to cloud watch once per minute.
 * 
 */
public class MemoryLogger {

	public static final String INSTANCE = "instance";
	public static final String USED = "used";
	public static final String ALL_INSTANCES = "all";
			
	/*
	 * Since the smallest period supported by cloud watch is one minute, metrics
	 * are only pushed to cloud watch once per minute.
	 */
	public static final long PUBLISH_PERIOD_MS = 60 * 1000;

	@Autowired
	Consumer consumer;
	@Autowired
	Clock clock;
	@Autowired
	StackConfiguration stackConfig;
	
	String nameSapcePrefix;
	String nameSpace;

	long lastPublishToCloudWatchMS;

	private long maximum;
	private long minimum;
	private long count;
	private long sum;

	public MemoryLogger(String nameSpacePrefix) {
		lastPublishToCloudWatchMS = 0;
		if(nameSpacePrefix == null) {
			throw new IllegalArgumentException("NamespacePrefix cannot be null");
		}
		this.nameSapcePrefix = nameSpacePrefix;
		resetStats();
	}

	/**
	 * Call from a timer.
	 */
	public void onTimerFired() {
		Runtime rt = Runtime.getRuntime();
		// Calculate the memory used at this time
		long memoryUsedBytes = rt.totalMemory() - rt.freeMemory();
		// calculate the metrics
		maximum = Math.max(maximum, memoryUsedBytes);
		minimum = Math.min(minimum, memoryUsedBytes);
		count++;
		sum += memoryUsedBytes;

		long nowMS = clock.currentTimeMillis();
		// publish the max memory to cloud watch once per minute.
		if (nowMS - lastPublishToCloudWatchMS > PUBLISH_PERIOD_MS) {
			lastPublishToCloudWatchMS = nowMS;
			MetricStats stats = createMetricStats();
			Date date = new Date(nowMS);
			// Add a metric for all instances
			String instance = ALL_INSTANCES;
			consumer.addProfileData(createMetric(stats, date, instance));
			// only publish for this instances if this is production.
			if (stackConfig.isProductionStack()) {
				// Add a metric for this instance
				instance = VirtualMachineIdProvider.getVMID();
				consumer.addProfileData(createMetric(stats, date, instance));
			}
			resetStats();
		}
	}

	/**
	 * Reset the stats to the starting values.
	 */
	private void resetStats() {
		maximum = 0L;
		minimum = Long.MAX_VALUE;
		count = 0L;
		sum = 0L;
	}

	/**
	 * Create a MetricStats from the gathered stats
	 * 
	 * @return
	 */
	private MetricStats createMetricStats() {
		MetricStats stats = new MetricStats();
		stats.setMaximum(new Double(maximum));
		stats.setMinimum(new Double(minimum));
		stats.setSum(new Double(sum));
		stats.setCount(new Double(count));
		return stats;
	}

	/**
	 * Create a metric for a single instance.
	 * 
	 * @param value
	 * @param date
	 * @param instance
	 * @return
	 */
	public ProfileData createMetric(MetricStats stats, Date date,
			String instance) {
		ProfileData pd = new ProfileData();
		pd.setNamespace(getNamespace());
		pd.setTimestamp(date);
		pd.setName(USED);
		pd.setUnit(StandardUnit.Bytes.name());
		pd.setMetricStats(stats);
		pd.setDimension(Collections.singletonMap(INSTANCE, instance));
		return pd;
	}

	/**
	 * The metric name space.
	 * @return
	 */
	public String getNamespace() {
		if(nameSpace == null) {
			nameSpace = nameSapcePrefix +"-Memory-"+stackConfig.getStackInstance();
		}
		return nameSpace;
	}

	public long getMaximum() {
		return maximum;
	}

	public long getMinimum() {
		return minimum;
	}

	public long getCount() {
		return count;
	}

	public long getSum() {
		return sum;
	}

}
