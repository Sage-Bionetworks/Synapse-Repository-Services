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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;

/**
 * Unit test for the cloud watch consumer.
 * 
 * @author John
 *
 */
@ExtendWith(MockitoExtension.class)
public class ConsumerTest {
	
	@Mock
	private AmazonCloudWatch mockClient;
	
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
		// Conver to a put metric.
		MetricDatum expectedDatum = new MetricDatum();
		expectedDatum.setMetricName(pd.getName());
		expectedDatum.setValue(pd.getValue());
		expectedDatum.setUnit(pd.getUnit());
		expectedDatum.setTimestamp(pd.getTimestamp());
		Collection<Dimension> dimensions=new ArrayList<Dimension>();
		dimensions.add(new Dimension().withName("foo").withValue("bar"));
		expectedDatum.setDimensions(dimensions);
		
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
		PutMetricDataRequest batch0 = new PutMetricDataRequest();
		batch0.setNamespace("namespace0");
		addRangeToRequst(list, 0, list.size(), batch0);
		
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
		PutMetricDataRequest batch0 = new PutMetricDataRequest();
		batch0.setNamespace("namespace0");
		addRangeToRequst(list, 0, list.size(), batch0);
		
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
		PutMetricDataRequest batch0 = new PutMetricDataRequest();
		batch0.setNamespace("namespace0");
		addRangeToRequst(list, 0, Consumer.MAX_BATCH_SIZE, batch0);
		// the second batch should have the same namespace with one.
		PutMetricDataRequest batch1 = new PutMetricDataRequest();
		batch1.setNamespace("namespace0");
		addRangeToRequst(list, Consumer.MAX_BATCH_SIZE, list.size(), batch1);
		
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
		
		when(mockClient.putMetricData(any())).thenThrow(ex);
		
		PutMetricDataRequest request = new PutMetricDataRequest()
			.withNamespace("namespace")
			.withMetricData(List.of(new MetricDatum().withMetricName("name").withValue(1.0)));
		
		RuntimeException result = assertThrows(RuntimeException.class, () -> {			
			consumer.sendMetrics(request, mockClient);
		});
		
		assertEquals(ex, result.getCause());
	}
	
	/**
	 * Helper used to build up expected PutMetricDataRequest
	 * @param list
	 * @param start
	 * @param end
	 * @param request
	 */
	private static void addRangeToRequst(List<ProfileData> list, int start, int end, PutMetricDataRequest request){
		for(int i=start; i<end; i++){
			request.getMetricData().add(Consumer.makeMetricDatum(list.get(i)));
		}
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
