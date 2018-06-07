package org.sagebionetworks.cloudwatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;

public class WorkerLoggerImplTest {
	
	@Test
	public void testMakeProfileDataDTO() throws Exception {
		Class<? extends Object> workerClass = WorkerLogger.class; // nonsensical, just for testing
		ChangeMessage changeMessage = new ChangeMessage();
		changeMessage.setChangeType(ChangeType.CREATE);
		changeMessage.setObjectId("101");
		changeMessage.setObjectType(ObjectType.ENTITY);
		boolean willRetry = false;
		Date timestamp = new Date();
		String message = "Entity syn12345 failed";
		Throwable throwable = new Exception(message);
		ProfileData pd = WorkerLoggerImpl.makeProfileDataDTO(workerClass, changeMessage, throwable, willRetry, timestamp);

		assertNull(pd.getMetricStats());
		assertEquals("org.sagebionetworks.cloudwatch.WorkerLogger", pd.getName());
		assertEquals("Asynchronous Workers - "+StackConfigurationSingleton.singleton().getStackInstance(), pd.getNamespace());
		assertTrue(pd.getTimestamp().getTime()-timestamp.getTime()<10L);
		assertEquals("Count", pd.getUnit());
		assertEquals(1D, pd.getValue(), 1E-10);
		Map<String,String> dimension = pd.getDimension();
		assertEquals(4, dimension.size());
		assertEquals("false", dimension.get("willRetry"));
		assertEquals("CREATE", dimension.get("changeType"));
		assertEquals("ENTITY", dimension.get("objectType"));
		
		String origStackTrace = ExceptionUtils.getStackTrace(throwable);
		String retrievedStackTrace = dimension.get("stackTrace");
		// check that the message has been removed
		assertTrue(retrievedStackTrace.indexOf(message)<0);
		// check that after the first (modified) line they are the same
		assertEquals(
				origStackTrace.substring(origStackTrace.indexOf("at")),
				retrievedStackTrace.substring(retrievedStackTrace.indexOf("at"))
		);
	}
	
	@Test
	public void testMakeProfileDataDTOGeneric() throws Exception {
		String name = "some metric name";
		ChangeMessage nullChange = null;
		boolean willRetry = false;
		Date timestamp = new Date();
		String message = "Entity syn12345 failed";
		Throwable throwable = new Exception(message);
		ProfileData pd = WorkerLoggerImpl.makeProfileDataDTO(name, nullChange, throwable, willRetry, timestamp);

		assertNull(pd.getMetricStats());
		assertEquals(name, pd.getName());
		assertEquals("Asynchronous Workers - "+StackConfigurationSingleton.singleton().getStackInstance(), pd.getNamespace());
		assertTrue(pd.getTimestamp().getTime()-timestamp.getTime()<10L);
		assertEquals("Count", pd.getUnit());
		assertEquals(1D, pd.getValue(), 1E-10);
		Map<String,String> dimension = pd.getDimension();
		assertEquals(2, dimension.size());
		assertEquals("false", dimension.get("willRetry"));
		
		String origStackTrace = ExceptionUtils.getStackTrace(throwable);
		String retrievedStackTrace = dimension.get("stackTrace");
		// check that the message has been removed
		assertTrue(retrievedStackTrace.indexOf(message)<0);
		// check that after the first (modified) line they are the same
		assertEquals(
				origStackTrace.substring(origStackTrace.indexOf("at")),
				retrievedStackTrace.substring(retrievedStackTrace.indexOf("at"))
		);
	}
	
	@Test
	public void testCallConsumer() throws Exception {
		WorkerLoggerImpl workerLoggerImpl = new WorkerLoggerImpl();
		Consumer mockConsumer = Mockito.mock(Consumer.class);
		workerLoggerImpl.setConsumer(mockConsumer);
		workerLoggerImpl.setShouldProfile(true);
		
		Class<? extends Object> workerClass = WorkerLogger.class; // nonsensical, just for testing
		ChangeMessage changeMessage = new ChangeMessage();
		changeMessage.setChangeType(ChangeType.CREATE);
		changeMessage.setObjectId("101");
		changeMessage.setObjectType(ObjectType.ENTITY);
		Throwable cause = new Exception();
		boolean willRetry = false;
		workerLoggerImpl.logWorkerFailure(workerClass, changeMessage, cause, willRetry);
		Mockito.verify(mockConsumer).addProfileData((ProfileData)anyObject());
	}

	@Test
	public void testCallConsumerGeneric() throws Exception {
		WorkerLoggerImpl workerLoggerImpl = new WorkerLoggerImpl();
		Consumer mockConsumer = Mockito.mock(Consumer.class);
		workerLoggerImpl.setConsumer(mockConsumer);
		workerLoggerImpl.setShouldProfile(true);
		Throwable cause = new Exception();
		boolean willRetry = false;
		workerLoggerImpl.logWorkerFailure("generic ", cause, willRetry);
		Mockito.verify(mockConsumer).addProfileData((ProfileData)anyObject());
	}
}
