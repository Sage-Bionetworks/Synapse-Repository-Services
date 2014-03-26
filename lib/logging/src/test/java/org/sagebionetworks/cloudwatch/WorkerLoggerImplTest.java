package org.sagebionetworks.cloudwatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;

public class WorkerLoggerImplTest {

	@Ignore // See PLFM-2655
	@Test
	public void testStackTraceToString() throws Exception {
		Throwable t = new Exception();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		t.printStackTrace(ps);
		baos.close();
		System.out.println(baos.toString());		
		assertTrue(baos.toString().startsWith(WorkerLoggerImpl.stackTraceToString(t)));
	}
	
	@Test
	public void testMakeProfileDataDTO() throws Exception {
		Class<? extends Object> workerClass = WorkerLogger.class; // nonsensical, just for testing
		ChangeMessage changeMessage = new ChangeMessage();
		changeMessage.setChangeType(ChangeType.CREATE);
		changeMessage.setObjectId("101");
		changeMessage.setObjectType(ObjectType.ENTITY);
		Throwable cause = new Exception();
		String stackTrace = WorkerLoggerImpl.stackTraceToString(cause);
		boolean willRetry = false;
		Date timestamp = new Date();
		ProfileData pd = WorkerLoggerImpl.makeProfileDataDTO(workerClass, changeMessage, cause, willRetry, timestamp);

		assertNull(pd.getMetricStats());
		assertEquals("org.sagebionetworks.cloudwatch.WorkerLogger", pd.getName());
		assertEquals("Asynchronous Workers", pd.getNamespace());
		assertTrue(pd.getTimestamp().getTime()-timestamp.getTime()<10L);
		assertEquals("Count", pd.getUnit());
		assertEquals(1D, pd.getValue(), 1E-10);
		Map<String,String> dimension = pd.getDimension();
		assertEquals(4, dimension.size());
		assertEquals("false", dimension.get("willRetry"));
		assertEquals("CREATE", dimension.get("changeType"));
		assertEquals("ENTITY", dimension.get("objectType"));
		assertEquals(stackTrace, dimension.get("stackTrace"));
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
