package org.sagebionetworks.tool.migration.v3;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.sagebionetworks.client.SynapseAdministrationInt;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;

/**
 * 
 * @author jmhill
 *
 */
public class ReplayWorkerTest {

	SynapseAdministrationInt mockClient;
	
	@Before
	public void before(){
		mockClient = Mockito.mock(SynapseAdministrationInt.class);
	}
	
	@Test
	public void test() throws Exception{
		BasicProgress progress = new BasicProgress();
		FireMessagesResult m1 = new FireMessagesResult();
		m1.setNextChangeNumber(2l);
		FireMessagesResult m2 = new FireMessagesResult();
		m2.setNextChangeNumber(4l);
		FireMessagesResult m3 = new FireMessagesResult();
		m3.setNextChangeNumber(-1l);
		when(mockClient.fireChangeMessages(0l, 2l)).thenReturn(m1);
		when(mockClient.fireChangeMessages(2l, 2l)).thenReturn(m2);
		when(mockClient.fireChangeMessages(4l, 2l)).thenReturn(m3);
		ReplayWorker worker = new ReplayWorker(mockClient, 0, 6, 2, progress);
		long result = worker.call();
		assertEquals(4l, result);
	}
}
