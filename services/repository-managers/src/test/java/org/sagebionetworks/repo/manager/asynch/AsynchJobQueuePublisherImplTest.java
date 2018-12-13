package org.sagebionetworks.repo.manager.asynch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dbo.asynch.AsynchJobType;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.sqs.model.Message;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AsynchJobQueuePublisherImplTest {
	
	private static long MAX_WAIT = 1000*60;
	
	@Autowired
	AsynchJobQueuePublisher asynchJobQueuePublisher;
	
	@Before
	public void before(){
		// Start with empty queues
		asynchJobQueuePublisher.emptyAllQueues();
	}
	
	@Test
	public void testPublishRoundTrip() throws Exception{
		AsynchronousJobStatus status = new AsynchronousJobStatus();
		status.setJobId("123");
		UploadToTableRequest uploadToTableRequest = new UploadToTableRequest();
		uploadToTableRequest.setTableId("syn8786");
		uploadToTableRequest.setUploadFileHandleId("333");
		TableUpdateTransactionRequest body = new TableUpdateTransactionRequest();
		body.setChanges(Collections.singletonList(uploadToTableRequest));
		status.setRequestBody(body);
		// publish it
		asynchJobQueuePublisher.publishMessage(status);
		// There should be one message on the queue
		Message message = waitForOneMessage();
		assertNotNull(message);
		assertEquals(status.getJobId(), message.getBody());
		// Delete the message
		asynchJobQueuePublisher.deleteMessage(AsynchJobType.TABLE_UPDATE_TRANSACTION, message);
	}

	/**
	 * @return
	 * @throws InterruptedException 
	 */
	public Message waitForOneMessage() throws InterruptedException {
		long start = System.currentTimeMillis();
		while(true){
			Message message = asynchJobQueuePublisher.recieveOneMessage(AsynchJobType.TABLE_UPDATE_TRANSACTION);
			if(message != null){
				return message;
			}else{
				assertTrue("Timed out waiting for message",System.currentTimeMillis() - start < MAX_WAIT);
				System.out.println("Waiting for message...");
				Thread.sleep(1000L);
			}
		}
	}

}
