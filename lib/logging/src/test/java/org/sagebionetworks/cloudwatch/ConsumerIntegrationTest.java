package org.sagebionetworks.cloudwatch;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;

@RunWith(SpringJUnit4ClassRunner.class)
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
		profileData.setDimension(map);
		consumer.addProfileData(profileData);
		consumer.executeCloudWatchPut();
		
		// now let's see if we can find the result
		AmazonCloudWatch client = consumer.getCW();
		GetMetricStatisticsRequest metricStatisticsRequest = new GetMetricStatisticsRequest();
		metricStatisticsRequest.setNamespace(namespace);
		metricStatisticsRequest.setMetricName(metricName);
		// we query for a 20 ms window around our test point
		metricStatisticsRequest.setStartTime(new Date(now.getTime()-120000L));
		metricStatisticsRequest.setEndTime(new Date(now.getTime()+120000L));
		metricStatisticsRequest.setUnit(unit);
		metricStatisticsRequest.setStatistics(Collections.singletonList("Average"));
		metricStatisticsRequest.setPeriod(60);
		List<Dimension> queryDimensions = new ArrayList<Dimension>();
		for (String key : map.keySet()) {
			Dimension d = new Dimension();
			d.setName(key);
			d.setValue(map.get(key));
			queryDimensions.add(d);
		}
		metricStatisticsRequest.setDimensions(queryDimensions);
		
		List<Datapoint> datapoints = null;
		long start = System.currentTimeMillis();
		do {
			GetMetricStatisticsResult result = client.getMetricStatistics(metricStatisticsRequest);
			result.getLabel();
			datapoints = result.getDatapoints();
			assertTrue("Timed out after "+MAX_CLOUD_WATCH_WAIT_TIME_MILLIS+
					" millisec, waiting for CloudWatch metric to be published.", System.currentTimeMillis()-start<MAX_CLOUD_WATCH_WAIT_TIME_MILLIS);
			Thread.sleep(5000L);
		} while (datapoints.isEmpty());
		
		// if we reach this point the test is successful
	}

}
