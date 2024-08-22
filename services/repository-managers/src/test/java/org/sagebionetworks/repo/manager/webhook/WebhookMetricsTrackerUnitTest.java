package org.sagebionetworks.repo.manager.webhook;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.webhook.WebhookMetricsTracker.METRIC_FAIL_COUNT;
import static org.sagebionetworks.repo.manager.webhook.WebhookMetricsTracker.METRIC_REQ_COUNT;
import static org.sagebionetworks.repo.manager.webhook.WebhookMetricsTracker.METRIC_RUNTIME;
import static org.sagebionetworks.repo.manager.webhook.WebhookMetricsTracker.WEBHOOK_ID_ALL;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.MetricStats;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.util.Clock;

import com.amazonaws.services.cloudwatch.model.StandardUnit;

@ExtendWith(MockitoExtension.class)
public class WebhookMetricsTrackerUnitTest {
	
	@Mock
	private Consumer mockMetricsClient;
	
	@Mock
	private Clock mockClock;
	
	@InjectMocks
	private WebhookMetricsTracker tracker;
	
	@Mock
	private StackConfiguration mockConfig;
	
	private String namespace;
	
	private Date timestamp;

	@BeforeEach
	public void beforeEach() {
		when(mockConfig.getStackInstance()).thenReturn("dev");
		// This is invoked automatically by spring
		tracker.configure(mockConfig);
		
		namespace = "Webhooks-dev";
		timestamp = new Date();
	}

	@Test
	public void testCollectMetricsWithStartedAndCompleted() {
		when(mockClock.now()).thenReturn(timestamp);
		
		String webhookId = "123";
		
		tracker.requestStarted(webhookId);
		tracker.requestCompleted(webhookId, 150);
		
		// Call under test
		tracker.collectMetrics();
		
		verify(mockMetricsClient).addProfileData(List.of(
			expectedProfileData(METRIC_REQ_COUNT, webhookId, null).setValue(1.0),
			expectedProfileData(METRIC_FAIL_COUNT, webhookId, null).setValue(0.0),
			expectedProfileData(METRIC_RUNTIME, webhookId, expectedMetricStats(1, 150, 150, 150)),
			expectedProfileData(METRIC_REQ_COUNT, WEBHOOK_ID_ALL, null).setValue(1.0),
			expectedProfileData(METRIC_FAIL_COUNT, WEBHOOK_ID_ALL, null).setValue(0.0),
			expectedProfileData(METRIC_RUNTIME, WEBHOOK_ID_ALL, expectedMetricStats(1, 150, 150, 150))
		));
	}
	
	@Test
	public void testCollectMetricsWithNoRequests() {
		when(mockClock.now()).thenReturn(timestamp);
		
		// Call under test
		tracker.collectMetrics();
		
		verifyZeroInteractions(mockMetricsClient);
	}
	
	@Test
	public void testCollectMetricsWithStartedAndNotCompleted() {
		when(mockClock.now()).thenReturn(timestamp);
		
		String webhookId = "123";
		
		tracker.requestStarted(webhookId);
		
		// Call under test
		tracker.collectMetrics();
		
		verify(mockMetricsClient).addProfileData(List.of(
			expectedProfileData(METRIC_REQ_COUNT, webhookId, null).setValue(1.0),
			expectedProfileData(METRIC_FAIL_COUNT, webhookId, null).setValue(0.0),
			expectedProfileData(METRIC_REQ_COUNT, WEBHOOK_ID_ALL, null).setValue(1.0),
			expectedProfileData(METRIC_FAIL_COUNT, WEBHOOK_ID_ALL, null).setValue(0.0)
		));
	}
	
	@Test
	public void testCollectMetricsWithStartedAndFailed() {
		when(mockClock.now()).thenReturn(timestamp);
		
		String webhookId = "123";
		
		tracker.requestStarted(webhookId);
		tracker.requestCompleted(webhookId, 150);
		tracker.requestFailed(webhookId);
		
		// Call under test
		tracker.collectMetrics();
		
		verify(mockMetricsClient).addProfileData(List.of(
			expectedProfileData(METRIC_REQ_COUNT, webhookId, null).setValue(1.0),
			expectedProfileData(METRIC_FAIL_COUNT, webhookId, null).setValue(1.0),
			expectedProfileData(METRIC_RUNTIME, webhookId, expectedMetricStats(1, 150, 150, 150)),
			expectedProfileData(METRIC_REQ_COUNT, WEBHOOK_ID_ALL, null).setValue(1.0),
			expectedProfileData(METRIC_FAIL_COUNT, WEBHOOK_ID_ALL, null).setValue(1.0),
			expectedProfileData(METRIC_RUNTIME, WEBHOOK_ID_ALL, expectedMetricStats(1, 150, 150, 150))
		));
	}	
	
	@Test
	public void testCollectMetricsWithMultipleWebhooks() {
		when(mockClock.now()).thenReturn(timestamp);
		
		String webhookId1 = "123";
		String webhookId2 = "456";
		
		tracker.requestStarted(webhookId1);
		tracker.requestCompleted(webhookId1, 150);
		
		tracker.requestStarted(webhookId2);
		tracker.requestCompleted(webhookId2, 100);
		
		// Call under test
		tracker.collectMetrics();
		
		verify(mockMetricsClient).addProfileData(List.of(
			expectedProfileData(METRIC_REQ_COUNT, webhookId1, null).setValue(1.0),
			expectedProfileData(METRIC_FAIL_COUNT, webhookId1, null).setValue(0.0),
			expectedProfileData(METRIC_RUNTIME, webhookId1, expectedMetricStats(1, 150, 150, 150)),
			expectedProfileData(METRIC_REQ_COUNT, webhookId2, null).setValue(1.0),
			expectedProfileData(METRIC_FAIL_COUNT, webhookId2, null).setValue(0.0),
			expectedProfileData(METRIC_RUNTIME, webhookId2, expectedMetricStats(1, 100, 100, 100)),
			expectedProfileData(METRIC_REQ_COUNT, WEBHOOK_ID_ALL, null).setValue(2.0),
			expectedProfileData(METRIC_FAIL_COUNT, WEBHOOK_ID_ALL, null).setValue(0.0),
			expectedProfileData(METRIC_RUNTIME, WEBHOOK_ID_ALL, expectedMetricStats(2, 250, 100, 150))
		));
	}
	
	@Test
	public void testCollectMetricsWithMultipleRequests() {
		when(mockClock.now()).thenReturn(timestamp);
		
		String webhookId = "123";
		
		tracker.requestStarted(webhookId);
		tracker.requestCompleted(webhookId, 150);
		
		tracker.requestStarted(webhookId);
		tracker.requestCompleted(webhookId, 100);
		
		tracker.requestStarted(webhookId);
		tracker.requestCompleted(webhookId, 50);
		tracker.requestFailed(webhookId);
		
		// Call under test
		tracker.collectMetrics();
		
		verify(mockMetricsClient).addProfileData(List.of(
			expectedProfileData(METRIC_REQ_COUNT, webhookId, null).setValue(3.0),
			expectedProfileData(METRIC_FAIL_COUNT, webhookId, null).setValue(1.0),
			expectedProfileData(METRIC_RUNTIME, webhookId, expectedMetricStats(3, 300, 50, 150)),
			expectedProfileData(METRIC_REQ_COUNT, WEBHOOK_ID_ALL, null).setValue(3.0),
			expectedProfileData(METRIC_FAIL_COUNT, WEBHOOK_ID_ALL, null).setValue(1.0),
			expectedProfileData(METRIC_RUNTIME, WEBHOOK_ID_ALL, expectedMetricStats(3, 300, 50, 150))
		));
	}
	
	@Test
	public void testCollectMetricsWithMultipleWebhooksAndRequests() {
		when(mockClock.now()).thenReturn(timestamp);
		
		String webhookId1 = "123";
		String webhookId2 = "456";
		
		tracker.requestStarted(webhookId1);
		tracker.requestCompleted(webhookId1, 150);
		
		tracker.requestStarted(webhookId1);
		tracker.requestCompleted(webhookId1, 50);
		
		tracker.requestStarted(webhookId1);
		tracker.requestCompleted(webhookId1, 100);
		tracker.requestFailed(webhookId1);
		
		tracker.requestStarted(webhookId2);
		tracker.requestCompleted(webhookId2, 100);
		
		tracker.requestStarted(webhookId2);
		tracker.requestCompleted(webhookId2, 250);
		
		tracker.requestStarted(webhookId2);
		tracker.requestCompleted(webhookId2, 100);
		tracker.requestFailed(webhookId2);
		
		// Call under test
		tracker.collectMetrics();
		
		verify(mockMetricsClient).addProfileData(List.of(
			expectedProfileData(METRIC_REQ_COUNT, webhookId1, null).setValue(3.0),
			expectedProfileData(METRIC_FAIL_COUNT, webhookId1, null).setValue(1.0),
			expectedProfileData(METRIC_RUNTIME, webhookId1, expectedMetricStats(3, 300, 50, 150)),
			expectedProfileData(METRIC_REQ_COUNT, webhookId2, null).setValue(3.0),
			expectedProfileData(METRIC_FAIL_COUNT, webhookId2, null).setValue(1.0),
			expectedProfileData(METRIC_RUNTIME, webhookId2, expectedMetricStats(3, 450, 100, 250)),
			expectedProfileData(METRIC_REQ_COUNT, WEBHOOK_ID_ALL, null).setValue(6.0),
			expectedProfileData(METRIC_FAIL_COUNT, WEBHOOK_ID_ALL, null).setValue(2.0),
			expectedProfileData(METRIC_RUNTIME, WEBHOOK_ID_ALL, expectedMetricStats(6, 750, 50, 250))
		));
	}
	
	ProfileData expectedProfileData(String name, String webhookId, MetricStats withStats) {
		return new ProfileData()
			.setTimestamp(timestamp)
			.setNamespace(namespace)
			.setDimension(Map.of("webhookId", webhookId))
			.setName(name)
			.setUnit(withStats == null ? StandardUnit.Count.name() : StandardUnit.Milliseconds.name())
			.setMetricStats(withStats);
	}
	
	MetricStats expectedMetricStats(double count, double sum, double min, double max) {
		return new MetricStats()
			.setCount(count)
			.setSum(sum)
			.setMaximum(max)
			.setMinimum(min);
	}
	
	
 }
