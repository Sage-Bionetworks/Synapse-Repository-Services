package org.sagebionetworks.cloudwatch;


import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:cloudwatch-spb.xml" })
public class ConsumerIntegrationTest {
	
	@Autowired
	Consumer consumer;
	
	private static final long MAX_CLOUD_WATCH_WAIT_TIME_MILLIS = 60000L; // one minute

	@Test
	public void testMetricPublishing() throws Exception {
		assertNotNull(consumer);
		consumer.clearProfileData();
		ProfileData profileData = new ProfileData();
		Random random = new Random();
		String namespace = getClass().getName();
		profileData.setNamespace(namespace);
		// to avoid 'collisions' we'd like to have a unique metric for each instance
		// of this integration test.  However metrics cost $0.50/metric/month (first 10 metrics
		// free) so we use just 10 distinct metrics.  This helps ensure collisions don't happen
		// to further reduce the likelihood of collisions we query on a narrow time window (below)
		String metricName = "testmetric-"+random.nextInt(10);
		profileData.setName(metricName);
		Date now = new Date();
		profileData.setTimestamp(now);
		String unit = "Count";
		profileData.setUnit(unit);
		Double value = 1.0;
		profileData.setValue(value);
		Map<String,String> map = new HashMap<String,String>();
		
		map.put("foo", "bar");
		map.put("bar", "");
		
		profileData.setDimension(map);
		
		consumer.addProfileData(profileData);
		consumer.executeCloudWatchPut();
		
		// now let's see if we can find the result
		CloudWatchClient client = consumer.getCW();
		// we query for a 20 ms window around our test point
		GetMetricStatisticsRequest metricStatisticsRequest = GetMetricStatisticsRequest.builder()
			.namespace(namespace)
			.metricName(metricName)
			.startTime(now.toInstant().minusMillis(120000L))
			.endTime(now.toInstant().plusMillis(120000L))
			.unit(unit)
			.statistics(Statistic.AVERAGE)
			.period(60)
			.dimensions(Dimension.builder().name("foo").value("bar").build())
			.build();

		TimeUtils.waitFor(MAX_CLOUD_WATCH_WAIT_TIME_MILLIS, 1000, () -> {
			GetMetricStatisticsResponse result = client.getMetricStatistics(metricStatisticsRequest);
			return Pair.create(!result.datapoints().isEmpty(), null);
		});
	}

}
