package org.sagebionetworks.cloudwatch;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:cloudwatch-spb.xml" })
public class WorkerLoggerIntegrationTest {
	
	@Autowired
	Consumer consumer;
	
	private static final long MAX_CLOUD_WATCH_WAIT_TIME_MILLIS = 60000L; // one minute

	@Ignore // PLFM-3559
	@Test
	public void testMetricPublishing() throws Exception {
		assertNotNull(consumer);
		consumer.clearProfileData();
		Class<? extends Object> workerClass = WorkerLogger.class; // nonsensical, just for testing
		ChangeMessage changeMessage = new ChangeMessage();
		changeMessage.setChangeType(ChangeType.CREATE);
		changeMessage.setObjectId("101");
		changeMessage.setObjectType(ObjectType.ENTITY);
		Throwable cause = new Exception();

		boolean willRetry = false;
		Date timestamp = new Date();
		ProfileData profileData = WorkerLoggerImpl.makeProfileDataDTO(workerClass, changeMessage, cause, willRetry, timestamp);
		consumer.addProfileData(profileData);
		
		// the main point of the test is to see if the content of ProfileData 
		// is rejected by CloudWatch
		consumer.executeCloudWatchPut();
		
		// as a bonus we check the results:
		
		// now let's see if we can find the result
		AmazonCloudWatch client = consumer.getCW();
		GetMetricStatisticsRequest metricStatisticsRequest = new GetMetricStatisticsRequest();
		metricStatisticsRequest.setNamespace(profileData.getNamespace());
		metricStatisticsRequest.setMetricName(profileData.getName());
		// we query for a 20 ms window around our test point
		metricStatisticsRequest.setStartTime(new Date(timestamp.getTime()-120000L));
		metricStatisticsRequest.setEndTime(new Date(timestamp.getTime()+120000L));
		metricStatisticsRequest.setUnit("Count");
		metricStatisticsRequest.setStatistics(Collections.singletonList("Average"));
		metricStatisticsRequest.setPeriod(60);
		metricStatisticsRequest.setDimensions(Consumer.makeMetricDatum(profileData).getDimensions());
		
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
