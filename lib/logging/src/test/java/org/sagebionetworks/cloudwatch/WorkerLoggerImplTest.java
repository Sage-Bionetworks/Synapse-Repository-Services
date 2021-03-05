package org.sagebionetworks.cloudwatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
	public void testLogWorkerMetric() {

		Class<? extends Object> workerClass = WorkerLogger.class; // nonsensical, just for testing
		String metricName = "Some metric";
		boolean willRetry = false;
		
		ProfileData expected = new ProfileData();
		expected.setName(metricName);
		expected.setNamespace(WorkerLogger.WORKER_NAMESPACE + " - " + stackInstance);
		expected.setUnit("Count");
		expected.setValue(1D);
		expected.setDimension(ImmutableMap.of(
				WorkerLogger.DIMENSION_WORKER_CLASS, workerClass.getSimpleName(),
				WorkerLogger.DIMENSION_WILL_RETRY, String.valueOf(willRetry)
		));
		
		// Call under test
		logger.logWorkerMetric(workerClass, metricName, willRetry);

		verify(mockConsumer).addProfileData(dataCaptor.capture());
		
		ProfileData result = dataCaptor.getValue();
		
		assertNotNull(result.getTimestamp());
		
		expected.setTimestamp(result.getTimestamp());
		assertEquals(expected, result);
	}
	
}
