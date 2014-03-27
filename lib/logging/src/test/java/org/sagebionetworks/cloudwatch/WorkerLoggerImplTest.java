package org.sagebionetworks.cloudwatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;

import java.util.Date;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
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
		ProfileData pd = WorkerLoggerImpl.makeProfileDataDTO(workerClass, changeMessage, null, willRetry, timestamp);

		assertNull(pd.getMetricStats());
		assertEquals("org.sagebionetworks.cloudwatch.WorkerLogger", pd.getName());
		assertEquals("Asynchronous Workers - "+StackConfiguration.getStackInstance(), pd.getNamespace());
		assertTrue(pd.getTimestamp().getTime()-timestamp.getTime()<10L);
		assertEquals("Count", pd.getUnit());
		assertEquals(1D, pd.getValue(), 1E-10);
		Map<String,String> dimension = pd.getDimension();
		assertEquals(4, dimension.size());
		assertEquals("false", dimension.get("willRetry"));
		assertEquals("CREATE", dimension.get("changeType"));
		assertEquals("ENTITY", dimension.get("objectType"));
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

}
