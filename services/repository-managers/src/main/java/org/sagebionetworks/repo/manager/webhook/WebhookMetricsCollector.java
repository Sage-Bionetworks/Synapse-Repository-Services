package org.sagebionetworks.repo.manager.webhook;

import java.util.ArrayList;
import java.util.Date;
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
public class WebhookMetricsCollector {
	
	static final String METRIC_REQ_COUNT = "requestsCount";
	static final String METRIC_FAIL_COUNT = "failureCount";
	static final String METRIC_RUNTIME = "runtime";
	static final String WEBHOOK_ID_ALL = "all";
	
	private Consumer metricsClient;
	private Clock clock;
	private String namespace;
	
	protected ConcurrentHashMap<String, WebhookMetrics> metrics = new ConcurrentHashMap<>();
	
	public WebhookMetricsCollector(Consumer metricsClient, Clock clock) {
		this.metricsClient = metricsClient;
		this.clock = clock;
	}
	
	@Autowired
	void configure(StackConfiguration config) {
		this.namespace = "Webhooks-" + config.getStackInstance();
	}
		
	public void requestCompleted(String webhookId, long runtimeMs, boolean failed) {
		metrics.compute(webhookId, (id, existing) -> {
			WebhookMetrics requestMetrics = existing == null ? new WebhookMetrics(id) : existing;
			
			return requestMetrics.addRequestData(runtimeMs, failed);
		});
	}
	
	public void collectMetrics() {
		List<ProfileData> collectedMetrics = new ArrayList<>();
		
		Date timestamp = clock.now();
		
		// Also send aggregated metrics across all the webhooks
		ProfileData allRequests = WebhookMetrics.profileData(timestamp, namespace, METRIC_REQ_COUNT, WEBHOOK_ID_ALL).setValue(0.0);
		ProfileData allFailed = WebhookMetrics.profileData(timestamp, namespace, METRIC_FAIL_COUNT, WEBHOOK_ID_ALL).setValue(0.0);
		ProfileData allRuntime = WebhookMetrics.profileData(timestamp, namespace, METRIC_RUNTIME, WEBHOOK_ID_ALL).setUnit(StandardUnit.Milliseconds.name()).setMetricStats(new MetricStats()
			.setCount(0.0)
			.setSum(0.0)
			.setMinimum(Double.POSITIVE_INFINITY)
			.setMaximum(Double.NEGATIVE_INFINITY)
		);
		
		metrics.keySet().forEach(webhookId -> {
			WebhookMetrics webhookData = metrics.remove(webhookId);
			
			if (webhookData == null) {
				return;
			}
			
			collectedMetrics.addAll(webhookData.toProfileData(timestamp, namespace, allRequests, allFailed, allRuntime));
		});
		
		if (!collectedMetrics.isEmpty()) {
			
			collectedMetrics.add(allRequests);
			collectedMetrics.add(allFailed);
			collectedMetrics.add(allRuntime);
			
			metricsClient.addProfileData(collectedMetrics);
			
		}
		
	}
	
	private static final class WebhookMetrics {
						
		static ProfileData profileData(Date timestamp, String namespace, String name, String webhookId) {
			return new ProfileData()
					.setTimestamp(timestamp)
					.setNamespace(namespace)
					.setDimension(Map.of("webhookId", webhookId))
					.setName(name)
					.setUnit(StandardUnit.Count.name());
		}
		
		private final String webhookId;
		private double totalCount = 0;
		private double failureCount = 0;
		private double totalRuntime = 0;
		private double minRuntime = Double.POSITIVE_INFINITY;
		private double maxRuntime = Double.NEGATIVE_INFINITY;
		
		private WebhookMetrics(String webhookId) {
			this.webhookId = webhookId;
		}
				
		WebhookMetrics addRequestData(long runtimeMs, boolean failed) {
			totalCount++;			
			if (failed) {
				failureCount++;
			}			
			totalRuntime += runtimeMs;
			minRuntime = Math.min(runtimeMs, minRuntime);
			maxRuntime = Math.max(runtimeMs, maxRuntime);
			
			return this;
		}
		
		List<ProfileData> toProfileData(Date timestamp, String namespace, ProfileData requestAcc, ProfileData failureAcc, ProfileData runtimeAcc) {
			List<ProfileData> list = new ArrayList<>();
			
			list.add(profileData(timestamp, namespace, METRIC_REQ_COUNT, webhookId).setValue(totalCount));
			list.add(profileData(timestamp, namespace, METRIC_FAIL_COUNT, webhookId).setValue(failureCount));
			
			MetricStats runtimeStats = new MetricStats()
				.setCount(totalCount)
				.setSum(totalRuntime)				
				.setMinimum(minRuntime)
				.setMaximum(maxRuntime); 
			
			list.add(profileData(timestamp, namespace, METRIC_RUNTIME, webhookId).setUnit(StandardUnit.Milliseconds.name()).setMetricStats(runtimeStats));			
			
			requestAcc.setValue(requestAcc.getValue() + totalCount);
			failureAcc.setValue(failureAcc.getValue() + failureCount);
								
			MetricStats runtimeAccStats = runtimeAcc.getMetricStats();
			
			runtimeAccStats.setCount(runtimeAccStats.getCount() + runtimeStats.getCount());
			runtimeAccStats.setSum(runtimeAccStats.getSum() + runtimeStats.getSum());
			runtimeAccStats.setMinimum(Math.min(runtimeAccStats.getMinimum(), runtimeStats.getMinimum()));
			runtimeAccStats.setMaximum(Math.max(runtimeAccStats.getMaximum(), runtimeStats.getMaximum()));
			
			return list;
		}

		@Override
		public String toString() {
			return "WebhookMetrics [totalCount=" + totalCount + ", failureCount=" + failureCount
					+ ", totalRuntime=" + totalRuntime + ", minRuntime=" + minRuntime + ", maxRuntime=" + maxRuntime + "]";
		}
	}

}
