package org.sagebionetworks.cloudwatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;

/**
 * Unit test for the cloud watch consumer.
 *
 * @author John
 *
 */
@ExtendWith(MockitoExtension.class)
public class ConsumerTest {

	@Mock
	private CloudWatchClient mockClient;
	
	@InjectMocks
	private Consumer consumer; 
	
	@Test
	public void testScrubDimensionString() {
		assertNull(Consumer.scrubDimensionString(null));
	}
	
	@Test
	public void testMakeMetricDatum(){
		// Start with a profile data
		ProfileData pd = new ProfileData();
		pd.setValue(123D);
		pd.setName("name");
		pd.setNamespace("nameSpace");
		pd.setTimestamp(new Date());
		pd.setUnit("Count");
		Map<String,String> dimensionMap=new TreeMap<String,String>();
		dimensionMap.put("foo", "bar");
		dimensionMap.put("bar", null);
		dimensionMap.put("baz", "  ");
		pd.setDimension(dimensionMap);
		// Convert to a put metric.
		MetricDatum expectedDatum = MetricDatum.builder()
			.metricName(pd.getName())
			.value(pd.getValue())
			.unit(pd.getUnit())
			.timestamp(pd.getTimestamp().toInstant())
			.dimensions(Dimension.builder().name("foo").value("bar").build())
			.build();

		MetricDatum mdResult = Consumer.makeMetricDatum(pd);
		assertEquals(expectedDatum, mdResult);
	}
	
	@Test
	public void testGetAllNamespaces(){
		// Create two namepace, the first with two elements, and the second with 3 elements.
		List<ProfileData> list = createTestData(2,3);
		assertNotNull(list);
		assertEquals(2+3,list.size());
		// Create a few
		Map<String, List<MetricDatum>> resultMap = consumer.getAllNamespaces(list);
		// the map should contain two namespaces, the first with two elements and the second with three.
		assertNotNull(resultMap);
		assertEquals(2, resultMap.size());
		assertNotNull(resultMap.get("namespace0"));
		assertEquals(2, resultMap.get("namespace0").size());
		assertNotNull(resultMap.get("namespace1"));
		assertEquals(3, resultMap.get("namespace1").size());
	}
	
	@Test
	public void testExecuteCloudWatchPutUnderBachSize(){
		// create some test data
		// test a batch under the max size
		List<ProfileData> list = createTestData(Consumer.MAX_BATCH_SIZE-1);
		assertNotNull(list);
		// This is our single batch.
		PutMetricDataRequest batch0 = buildRequest("namespace0", makeRange(list, 0, list.size()));

		// Add all of this profile data to the consumer
		for(ProfileData pd: list){
			consumer.addProfileData(pd);
		}
		// Now fire off the putting the data to cloud watch.
		consumer.executeCloudWatchPut();
		// Verify each batch was sent as expected
		verify(mockClient, times(1)).putMetricData(batch0);
	}

	@Test
	public void testExecuteCloudWatchPutEqualBatchSize(){
		// Test a batch equal to the max size
		List<ProfileData> list = createTestData(Consumer.MAX_BATCH_SIZE);
		assertNotNull(list);
		// This is our single batch.
		PutMetricDataRequest batch0 = buildRequest("namespace0", makeRange(list, 0, list.size()));

		// Add all of this profile data to the consumer
		for(ProfileData pd: list){
			consumer.addProfileData(pd);
		}
		// Now fire off the putting the data to cloud watch.
		consumer.executeCloudWatchPut();
		// Verify each batch was sent as expected
		verify(mockClient, times(1)).putMetricData(batch0);
	}

	@Test
	public void testExecuteCloudWatchPutOverBatchSize(){
		// Test a batch over the batch size.
		List<ProfileData> list = createTestData(Consumer.MAX_BATCH_SIZE+1);
		assertNotNull(list);
		// This is our single batch.
		PutMetricDataRequest batch0 = buildRequest("namespace0", makeRange(list, 0, Consumer.MAX_BATCH_SIZE));
		// the second batch should have the same namespace with one.
		PutMetricDataRequest batch1 = buildRequest("namespace0", makeRange(list, Consumer.MAX_BATCH_SIZE, list.size()));

		// Add all of this profile data to the consumer
		for(ProfileData pd: list){
			consumer.addProfileData(pd);
		}
		// Now fire off the putting the data to cloud watch.
		consumer.executeCloudWatchPut();
		// Verify each batch was sent as expected
		verify(mockClient, times(1)).putMetricData(batch0);
		verify(mockClient, times(1)).putMetricData(batch1);
	}

	@Test
	public void testSendMetricsWithException() {
		IllegalStateException ex = new IllegalStateException("nope");

		when(mockClient.putMetricData(any(PutMetricDataRequest.class))).thenThrow(ex);

		List<MetricDatum> metricData = List.of(MetricDatum.builder().metricName("name").value(1.0).build());

		RuntimeException result = assertThrows(RuntimeException.class, () -> {
			consumer.sendMetrics("namespace", metricData, mockClient);
		});

		assertEquals(ex, result.getCause());
	}

	/**
	 * Helper used to build up the expected list of MetricDatum for a range.
	 * @param list
	 * @param start
	 * @param end
	 * @return
	 */
	private static List<MetricDatum> makeRange(List<ProfileData> list, int start, int end){
		List<MetricDatum> metricData = new ArrayList<MetricDatum>();
		for(int i=start; i<end; i++){
			metricData.add(Consumer.makeMetricDatum(list.get(i)));
		}
		return metricData;
	}

	private static PutMetricDataRequest buildRequest(String namespace, List<MetricDatum> metricData){
		return PutMetricDataRequest.builder()
			.namespace(namespace)
			.metricData(metricData)
			.build();
	}
	
	/**
	 * build up some test data 
	 * @param for each integer a namespace will be created with the integer number of elements.
	 * @return
	 */
	public List<ProfileData> createTestData(int ... array){
		List<ProfileData> list = new ArrayList<ProfileData>();
		for(int namespace=0; namespace<array.length; namespace++){
			for(int name = 0; name < array[namespace]; name++){
				ProfileData pd = new ProfileData();
				pd.setName("name"+name);
				pd.setNamespace("namespace"+namespace);
				pd.setTimestamp(new Date());
				pd.setUnit("Count");
				pd.setValue((double)(namespace*name+1)+name);
				list.add(pd);
			}
		}
		return list;
	}
	

}
