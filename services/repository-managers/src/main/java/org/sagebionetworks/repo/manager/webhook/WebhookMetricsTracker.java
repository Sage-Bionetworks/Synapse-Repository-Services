package org.sagebionetworks.repo.manager.webhook;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.MetricStats;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.cloudwatch.model.StandardUnit;

@Service
public class WebhookMetricsTracker {
	
	static final String METRIC_REQ_COUNT = "requestsCount";
	static final String METRIC_FAIL_COUNT = "failureCount";
	static final String METRIC_RUNTIME = "runtime";
	static final String WEBHOOK_ID_ALL = "all";
	
	private Consumer metricsClient;
	private Clock clock;
	private String namespace;
	
	private ConcurrentHashMap<String, DeliveryMetrics> metrics = new ConcurrentHashMap<>();
	
	public WebhookMetricsTracker(Consumer metricsClient, Clock clock) {
		this.metricsClient = metricsClient;
		this.clock = clock;
	}
	
	@Autowired
	void configure(StackConfiguration config) {
		this.namespace = "Webhooks-" + config.getStackInstance();
	}
	
	public void requestStarted(String webhookId) {
		metrics.computeIfAbsent(webhookId, k-> new DeliveryMetrics()).incrementTotalCount();
	}
	
	public void requestCompleted(String webhookId, long runtimeMs) {
		metrics.computeIfAbsent(webhookId, k-> new DeliveryMetrics()).accumulateRuntime(runtimeMs);
	}
		
	public void requestFailed(String webhookId) {
		metrics.computeIfAbsent(webhookId, k-> new DeliveryMetrics()).incrementFailureCount();
	}
	
	public void collectMetrics() {
		
		List<ProfileData> collectedMetrics = new ArrayList<>();
		
		// Also send aggregated metrics for all the webhooks
		ProfileData allRequests = profileData(METRIC_REQ_COUNT, WEBHOOK_ID_ALL).setValue(0.0);
		ProfileData allFailed = profileData(METRIC_FAIL_COUNT, WEBHOOK_ID_ALL).setValue(0.0);
		ProfileData allRuntime = profileData(METRIC_RUNTIME, WEBHOOK_ID_ALL).setUnit(StandardUnit.Milliseconds.name()).setMetricStats(new MetricStats()
			.setCount(0.0)
			.setSum(0.0)
			.setMinimum(Double.POSITIVE_INFINITY)
			.setMaximum(Double.NEGATIVE_INFINITY)
		);
				
		metrics.keySet().forEach(webhookId -> {
			DeliveryMetrics webhookData = metrics.remove(webhookId);
			
			if (webhookData == null) {
				return;
			}
			
			// TODO need synchronization on the data
			collectedMetrics.add(profileData(METRIC_REQ_COUNT, webhookId).setValue(webhookData.totalCount));
			collectedMetrics.add(profileData(METRIC_FAIL_COUNT, webhookId).setValue(webhookData.failureCount));
		
			allRequests.setValue(allRequests.getValue() + webhookData.totalCount);
			allFailed.setValue(allFailed.getValue() + webhookData.failureCount);
			
			// It is possible that the request started but was never completed and we didn't collect runtime stats
			if (webhookData.totalRuntime > 0) {
				MetricStats runtimeStats = new MetricStats()
					.setCount(webhookData.totalCount)
					.setSum(webhookData.totalRuntime)				
					.setMinimum(webhookData.minRuntime)
					.setMaximum(webhookData.maxRuntime);
				
				collectedMetrics.add(profileData(METRIC_RUNTIME, webhookId).setUnit(StandardUnit.Milliseconds.name()).setMetricStats(runtimeStats));			
								
				MetricStats allRuntimeStats = allRuntime.getMetricStats();
				
				allRuntimeStats.setCount(allRuntimeStats.getCount() + runtimeStats.getCount());
				allRuntimeStats.setSum(allRuntimeStats.getSum() + runtimeStats.getSum());
				allRuntimeStats.setMinimum(Math.min(allRuntimeStats.getMinimum(), runtimeStats.getMinimum()));
				allRuntimeStats.setMaximum(Math.max(allRuntimeStats.getMaximum(), runtimeStats.getMaximum()));
				allRuntime.setMetricStats(allRuntimeStats);
			}
		});
		
		if (!collectedMetrics.isEmpty()) {
			collectedMetrics.add(allRequests);
			collectedMetrics.add(allFailed);
			
			if (allRuntime.getMetricStats().getCount() > 0) {
				collectedMetrics.add(allRuntime);
			}
			
			metricsClient.addProfileData(collectedMetrics);
		}
		
	}
	
	ProfileData profileData(String name, String webhookId) {
		return new ProfileData()
			.setTimestamp(clock.now())
			.setNamespace(namespace)
			.setDimension(Map.of("webhookId", webhookId))
			.setName(name)
			.setUnit(StandardUnit.Count.name());
	}
	
	static final class DeliveryMetrics {
				
		private double totalCount = 0;
		private double failureCount = 0;
		private double totalRuntime = 0;
		private double minRuntime = Double.POSITIVE_INFINITY;
		private double maxRuntime = Double.NEGATIVE_INFINITY;
		
		synchronized void incrementTotalCount() {
			totalCount++;
		}
		
		synchronized void incrementFailureCount() {
			failureCount++;
		}
		
		synchronized void accumulateRuntime(long runtimeMs) {
			totalRuntime += runtimeMs;
			minRuntime = Math.min(runtimeMs, minRuntime);
			maxRuntime = Math.max(runtimeMs, maxRuntime);
		}
		
	}

}
