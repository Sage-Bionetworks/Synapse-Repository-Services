package org.sagebionetworks.cloudwatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;

import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.google.common.collect.ImmutableMap;

@ExtendWith(MockitoExtension.class)
public class WorkerLoggerImplTest {
	
	@Mock
	private Consumer mockConsumer;
	
	@Mock
	private StackConfiguration mockStackConfig;

	private WorkerLoggerImpl logger;
	
	private String stackInstance;
	
	@Captor
	private ArgumentCaptor<ProfileData> dataCaptor;
	
	@BeforeEach
	public void before() {
		MockitoAnnotations.initMocks(this);
		
		stackInstance = "stack";
		
		when(mockStackConfig.getCloudWatchOnOff()).thenReturn(true);
		when(mockStackConfig.getStackInstance()).thenReturn(stackInstance);
		
		logger = new WorkerLoggerImpl(mockConsumer, mockStackConfig);
	}
	
	@Test
	public void testLogWorkerFailureWithClass() {
		Class<? extends Object> workerClass = WorkerLogger.class; // nonsensical, just for testing
		ChangeMessage changeMessage = new ChangeMessage();
		changeMessage.setChangeType(ChangeType.CREATE);
		changeMessage.setObjectId("101");
		changeMessage.setObjectType(ObjectType.ENTITY);
		boolean willRetry = false;
		String message = "Entity syn12345 failed";
		Throwable throwable = new Exception(message);
		
		ProfileData expected = new ProfileData();
		expected.setName(workerClass.getName());
		expected.setNamespace(WorkerLogger.WORKER_NAMESPACE + " - " + stackInstance);
		expected.setUnit("Count");
		expected.setValue(1D);
		expected.setDimension(ImmutableMap.of(
				WorkerLogger.DIMENSION_OBJECT_TYPE, changeMessage.getObjectType().name(),
				WorkerLogger.DIMENSION_CHANGE_TYPE, changeMessage.getChangeType().name(),
				WorkerLogger.DIMENSION_WILL_RETRY, String.valueOf(willRetry),
				WorkerLogger.DIMENSION_STACK_TRACE, MetricUtils.stackTracetoString(throwable)
		));
		
		// Call under test
		logger.logWorkerFailure(workerClass, changeMessage, throwable, willRetry);

		verify(mockConsumer).addProfileData(dataCaptor.capture());
		
		ProfileData result = dataCaptor.getValue();
		
		assertNotNull(result.getTimestamp());
		
		expected.setTimestamp(result.getTimestamp());
		assertEquals(expected, result);
	}
	
	@Test
	public void testLogWorkerFailureWithClassAndNoChangeMessage() {
		Class<? extends Object> workerClass = WorkerLogger.class; // nonsensical, just for testing
		boolean willRetry = false;
		String message = "Entity syn12345 failed";
		Throwable throwable = new Exception(message);
		
		ProfileData expected = new ProfileData();
		expected.setName(workerClass.getName());
		expected.setNamespace(WorkerLogger.WORKER_NAMESPACE + " - " + stackInstance);
		expected.setUnit("Count");
		expected.setValue(1D);
		expected.setDimension(ImmutableMap.of(
				WorkerLogger.DIMENSION_WILL_RETRY, String.valueOf(willRetry),
				WorkerLogger.DIMENSION_STACK_TRACE, MetricUtils.stackTracetoString(throwable)
		));
		
		// Call under test
		logger.logWorkerFailure(workerClass, null, throwable, willRetry);

		verify(mockConsumer).addProfileData(dataCaptor.capture());
		
		ProfileData result = dataCaptor.getValue();
		
		assertNotNull(result.getTimestamp());
		
		expected.setTimestamp(result.getTimestamp());
		assertEquals(expected, result);
	}
	
	@Test
	public void testLogWorkerFailure() {

		String metricName = "Some metric";
		boolean willRetry = false;
		String message = "Entity syn12345 failed";
		Throwable throwable = new Exception(message);
		
		ProfileData expected = new ProfileData();
		expected.setName(metricName);
		expected.setNamespace(WorkerLogger.WORKER_NAMESPACE + " - " + stackInstance);
		expected.setUnit("Count");
		expected.setValue(1D);
		expected.setDimension(ImmutableMap.of(
				WorkerLogger.DIMENSION_WILL_RETRY, String.valueOf(willRetry),
				WorkerLogger.DIMENSION_STACK_TRACE, MetricUtils.stackTracetoString(throwable)
		));
		
		// Call under test
		logger.logWorkerFailure(metricName, throwable, willRetry);

		verify(mockConsumer).addProfileData(dataCaptor.capture());
		
		ProfileData result = dataCaptor.getValue();
		
		assertNotNull(result.getTimestamp());
		
		expected.setTimestamp(result.getTimestamp());
		assertEquals(expected, result);
	}
	
	@Test
	public void testLogWorkerCountMetric() {

		Class<? extends Object> workerClass = WorkerLogger.class; // nonsensical, just for testing
		String metricName = "Some metric";
		
		ProfileData expected = new ProfileData();
		expected.setName(metricName);
		expected.setNamespace(WorkerLogger.WORKER_NAMESPACE + " - " + stackInstance);
		expected.setUnit("Count");
		expected.setValue(1D);
		expected.setDimension(ImmutableMap.of(
				WorkerLogger.DIMENSION_WORKER_CLASS, workerClass.getSimpleName()
		));
		
		// Call under test
		logger.logWorkerCountMetric(workerClass, metricName);

		verify(mockConsumer).addProfileData(dataCaptor.capture());
		
		ProfileData result = dataCaptor.getValue();
		
		assertNotNull(result.getTimestamp());
		
		expected.setTimestamp(result.getTimestamp());
		assertEquals(expected, result);
	}
	
	@Test
	public void testLogWorkerTimeMetric() {

		Class<? extends Object> workerClass = WorkerLogger.class; // nonsensical, just for testing
		long timeMillis = 1000;
		Map<String, String> dimensions = Collections.emptyMap();
		
		ProfileData expected = new ProfileData();
		expected.setName(WorkerLogger.METRIC_NAME_WORKER_TIME);
		expected.setNamespace(WorkerLogger.WORKER_NAMESPACE + " - " + stackInstance);
		expected.setUnit(StandardUnit.Milliseconds.name());
		expected.setValue(Double.valueOf(timeMillis));
		
		Map<String, String> expectedDimensions = new HashMap<>();
		
		expectedDimensions.putAll(dimensions);
		expectedDimensions.put(WorkerLogger.DIMENSION_WORKER_CLASS, workerClass.getSimpleName());
		
		expected.setDimension(expectedDimensions);
		
		// Call under test
		logger.logWorkerTimeMetric(workerClass, timeMillis, dimensions);

		verify(mockConsumer).addProfileData(dataCaptor.capture());
		
		ProfileData result = dataCaptor.getValue();
		
		assertNotNull(result.getTimestamp());
		
		expected.setTimestamp(result.getTimestamp());
		assertEquals(expected, result);
	}
	
	@Test
	public void testLogWorkerTimeMetricWithNUllDimensions() {

		Class<? extends Object> workerClass = WorkerLogger.class; // nonsensical, just for testing
		long timeMillis = 1000;
		Map<String, String> dimensions = null;
		
		ProfileData expected = new ProfileData();
		expected.setName(WorkerLogger.METRIC_NAME_WORKER_TIME);
		expected.setNamespace(WorkerLogger.WORKER_NAMESPACE + " - " + stackInstance);
		expected.setUnit(StandardUnit.Milliseconds.name());
		expected.setValue(Double.valueOf(timeMillis));
		
		Map<String, String> expectedDimensions = new HashMap<>();
		
		expectedDimensions.put(WorkerLogger.DIMENSION_WORKER_CLASS, workerClass.getSimpleName());
		
		expected.setDimension(expectedDimensions);
		
		// Call under test
		logger.logWorkerTimeMetric(workerClass, timeMillis, dimensions);

		verify(mockConsumer).addProfileData(dataCaptor.capture());
		
		ProfileData result = dataCaptor.getValue();
		
		assertNotNull(result.getTimestamp());
		
		expected.setTimestamp(result.getTimestamp());
		assertEquals(expected, result);
	}
	
	@Test
	public void testLogWorkerTimeMetricWithOtherDimensions() {

		Class<? extends Object> workerClass = WorkerLogger.class; // nonsensical, just for testing
		long timeMillis = 1000;
		Map<String, String> dimensions = ImmutableMap.of("myDimension", "myValue");
		
		ProfileData expected = new ProfileData();
		expected.setName(WorkerLogger.METRIC_NAME_WORKER_TIME);
		expected.setNamespace(WorkerLogger.WORKER_NAMESPACE + " - " + stackInstance);
		expected.setUnit(StandardUnit.Milliseconds.name());
		expected.setValue(Double.valueOf(timeMillis));
		
		Map<String, String> expectedDimensions = new HashMap<>();
		
		expectedDimensions.putAll(dimensions);
		expectedDimensions.put(WorkerLogger.DIMENSION_WORKER_CLASS, workerClass.getSimpleName());
		
		expected.setDimension(expectedDimensions);
		
		// Call under test
		logger.logWorkerTimeMetric(workerClass, timeMillis, dimensions);

		verify(mockConsumer).addProfileData(dataCaptor.capture());
		
		ProfileData result = dataCaptor.getValue();
		
		assertNotNull(result.getTimestamp());
		
		expected.setTimestamp(result.getTimestamp());
		assertEquals(expected, result);
	}
	
	@Test
	public void testLogWorkerTimeMetricWithOverrideWorkerClassDimension() {

		Class<? extends Object> workerClass = WorkerLogger.class; // nonsensical, just for testing
		long timeMillis = 1000;
		Map<String, String> dimensions = ImmutableMap.of(WorkerLogger.DIMENSION_WORKER_CLASS, "myCustomValue");
		
		ProfileData expected = new ProfileData();
		expected.setName(WorkerLogger.METRIC_NAME_WORKER_TIME);
		expected.setNamespace(WorkerLogger.WORKER_NAMESPACE + " - " + stackInstance);
		expected.setUnit(StandardUnit.Milliseconds.name());
		expected.setValue(Double.valueOf(timeMillis));
		
		Map<String, String> expectedDimensions = new HashMap<>();
		
		expectedDimensions.putAll(dimensions);
		expectedDimensions.put(WorkerLogger.DIMENSION_WORKER_CLASS, workerClass.getSimpleName());
		
		expected.setDimension(expectedDimensions);
		
		// Call under test
		logger.logWorkerTimeMetric(workerClass, timeMillis, dimensions);

		verify(mockConsumer).addProfileData(dataCaptor.capture());
		
		ProfileData result = dataCaptor.getValue();
		
		assertNotNull(result.getTimestamp());
		
		expected.setTimestamp(result.getTimestamp());
		assertEquals(expected, result);
	}
	
}
