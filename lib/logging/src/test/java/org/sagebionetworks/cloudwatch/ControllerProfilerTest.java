package org.sagebionetworks.cloudwatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.mockito.Mockito.mock;

import java.util.Date;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.amazonaws.services.cloudwatch.model.MetricDatum;

/**
 * Unit test to test functionality of ControllerProfiler.
 * @author ntiedema
 */
public class ControllerProfilerTest {	
	
	ControllerProfiler controllerProfiler;
	Consumer mockConsumer;

	@Before
	public void setUp() throws Exception {
		mockConsumer = Mockito.mock(Consumer.class);
		controllerProfiler = new ControllerProfiler(mockConsumer);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	/**
	 * Tests that constructor that takes Consumer parameter
	 * correctly sets consumer.
	 */
	@Test
	public void testConstructorWithConsumer() throws Exception {
		Consumer testConsumer = new Consumer();
		ControllerProfiler testCP = new ControllerProfiler(testConsumer);
		assertEquals(testConsumer, testCP.getConsumer());
	}
	
	/**
	 * Tests that autowired ControllerProfiler and it's autowired member
	 * variable consumer are both not null.
	 */
	@Test
	public void testConsumerNotNull() throws Exception {
		assertNotNull(controllerProfiler.getConsumer());
	}
	
	/**
	 * Tests that makeProfileDataDTO returns the correct object.
	 */
	@Test
	public void makeProfileDataDTO() throws Exception {
		//need the parameters
		String testNamespace = "test namespace";
		String tempMetricName = "testMetric";
		long testLatency = (long) 30.0;
		
		ProfileData results = controllerProfiler.makeProfileDataDTO(testNamespace, tempMetricName, testLatency);
		
		assertNotNull(results);
		assertEquals(testNamespace, results.getNamespace());
		assertEquals(tempMetricName, results.getName());
		assertEquals(testLatency, results.getValue().longValue());
		assertEquals("Milliseconds", results.getUnit());
		Date testjdkDate = results.getTimestamp();
		assertNotNull(testjdkDate);
	}
	
	/**
	 * Tests that makeProfileDataDTO throws IllegalArgumentException when
	 * it does not receive valid String parameters
	 */
	@Test (expected = IllegalArgumentException.class)
	public void testMakeProfileDataDTOWithInvalidParameters() throws Exception {
		//need good and bad parameters
		String testNamespace = "test namespace";
		String testName = "testName";
		long testLatency = (long) 3.3;
		String nullNamespace = null;
		String nullName = null;
		
		//try call for bad namespace but good everything else
		ProfileData results = controllerProfiler.makeProfileDataDTO(nullNamespace, testName, testLatency);
		assertEquals(null, results);
		
		//try call for good namespace and bad name
		results = controllerProfiler.makeProfileDataDTO(testNamespace, nullName, testLatency);
		assertEquals(null, results);
		
		//try call for bad namespace and bad name
		results = controllerProfiler.makeProfileDataDTO(nullNamespace, nullName, testLatency);
		assertEquals(null, results);		
	}
	
	/**
	 * Test that setter for consumer works correctly.
	 */
	@Test
	public void testSetConsumer() throws Exception {
		Consumer testConsumer = new Consumer();
		assertNotSame(controllerProfiler.getConsumer(), testConsumer);
		controllerProfiler.setConsumer(testConsumer);
		assertEquals(testConsumer, controllerProfiler.getConsumer());
	}
	
	/**
	 * Test consumer's behavior in doBasicProfiling.
	 */
	@Test
	public void testConsumerInDoBasicProfiling() throws Exception {
		Consumer mockConsumer = mock(Consumer.class);
		ControllerProfiler controllerWithMockConsumer = new ControllerProfiler(mockConsumer);
		ProceedingJoinPoint testPJP = mock(ProceedingJoinPoint.class);
		MetricDatum testMD = new MetricDatum();
		
		try {
			controllerWithMockConsumer.doBasicProfiling(testPJP);
		} catch (Throwable e) {}
		
		//verify(mockConsumer, atLeastOnce()).addMetric((MetricDatum)anyObject());
	}
}