package org.sagebionetworks.repo.manager.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.webhook.WebhookMetricsCollector.METRIC_FAIL_COUNT;
import static org.sagebionetworks.repo.manager.webhook.WebhookMetricsCollector.METRIC_REQ_COUNT;
import static org.sagebionetworks.repo.manager.webhook.WebhookMetricsCollector.METRIC_RUNTIME;
import static org.sagebionetworks.repo.manager.webhook.WebhookMetricsCollector.WEBHOOK_ID_ALL;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
public class WebhookMetricsCollectorUnitTest {
	
	@Mock
	private Consumer mockMetricsClient;
	
	@Mock
	private Clock mockClock;
	
	@InjectMocks
	private WebhookMetricsCollector collector;
	
	@Mock
	private StackConfiguration mockConfig;
	
	private String namespace;
	
	private Date timestamp;

	@BeforeEach
	public void beforeEach() {
		when(mockConfig.getStackInstance()).thenReturn("dev");
		// This is invoked automatically by spring
		collector.configure(mockConfig);
		
		namespace = "Webhooks-dev";
		timestamp = new Date();
	}

	@Test
	public void testCollectMetrics() {
		when(mockClock.now()).thenReturn(timestamp);
		
		String webhookId = "123";
		
		collector.requestCompleted(webhookId, 150, false);
		
		// Call under test
		collector.collectMetrics();
		
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
		collector.collectMetrics();
		
		verifyZeroInteractions(mockMetricsClient);
	}
	
	@Test
	public void testCollectMetricsWithFailure() {
		when(mockClock.now()).thenReturn(timestamp);
		
		String webhookId = "123";
		
		collector.requestCompleted(webhookId, 150, true);
		
		// Call under test
		collector.collectMetrics();
		
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
	public void testCollectMetricsWithMultipleRequests() {
		when(mockClock.now()).thenReturn(timestamp);
		
		String webhookId1 = "123";
		String webhookId2 = "456";
		
		collector.requestCompleted(webhookId1, 150, false);
		collector.requestCompleted(webhookId1, 50, false);
		collector.requestCompleted(webhookId1, 100, true);
		
		collector.requestCompleted(webhookId2, 100, false);
		collector.requestCompleted(webhookId2, 250, false);
		collector.requestCompleted(webhookId2, 100, true);
		
		// Call under test
		collector.collectMetrics();
		
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
	
	@Test
	public void testCollectMetricsWithMultipleThreads() throws InterruptedException {
		when(mockClock.now()).thenReturn(timestamp);
		
		String webhookId1 = "123";
		String webhookId2 = "456";
		
		ExecutorService service = Executors.newCachedThreadPool();
		
		for (int i=0; i<100; i++) {
			boolean failed = i % 2 == 0;
			service.submit(() -> {
				collector.requestCompleted(webhookId1, failed ? 50 : 100, failed);
			});
		}
		
		for (int i=0; i<50; i++) {
			boolean failed = i % 2 == 0;
			service.submit(() -> {				
				collector.requestCompleted(webhookId2, failed ? 50 : 100, failed);
			});
		}
		
		for (int i=0; i<5; i++) {
			service.submit(() -> collector.collectMetrics());
		}
		
		service.shutdown();
		service.awaitTermination(10, TimeUnit.SECONDS);
		
		// A final call to the collectMetrics is needed since there is a chance that the thread(s) that collected metrics 
		// left some data behind due to contention (See https://sagebionetworks.jira.com/browse/PLFM-8612)
		collector.collectMetrics();
		
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<ProfileData>> captor = ArgumentCaptor.forClass(List.class);
		
		verify(mockMetricsClient, atLeastOnce()).addProfileData(captor.capture());
		
		Map<String, Double> total = new HashMap<>();
		Map<String, Double> failed = new HashMap<>();
		Map<String, Double> runtime = new HashMap<>();
				
		for (List<ProfileData> sentData : captor.getAllValues()) {
			for (ProfileData data : sentData) {
				String webhookId = data.getDimension().get("webhookId");
				if (data.getName().equals(METRIC_REQ_COUNT)) {
					total.merge(webhookId, data.getValue(), (oldValue, newValue) -> oldValue + newValue);
				} else if (data.getName().equals(METRIC_FAIL_COUNT)) {
					failed.merge(webhookId, data.getValue(), (oldValue, newValue) -> oldValue + newValue);
				} else {
					runtime.merge(webhookId, data.getMetricStats().getSum(), (oldValue, newValue) -> oldValue + newValue);
				}
			}
		}
		
		Map<String, Double> expectedTotal = Map.of(
				webhookId1, 100.0,
				webhookId2, 50.0,
				"all", 150.0
		);
		
		Map<String, Double> expectedFail = Map.of(
			webhookId1, 50.0,
			webhookId2, 25.0,
			"all", 75.0
		);
		
		Map<String, Double> expectedRuntime = Map.of(
			webhookId1, 7500.0,
			webhookId2, 3750.0,
			"all", 11250.0
		);
				
		assertEquals(expectedTotal, total);
		assertEquals(expectedFail, failed);
		assertEquals(expectedRuntime, runtime);
		
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
