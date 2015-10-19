package org.sagebionetworks.repo.manager.asynch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dbo.asynch.AsynchJobType;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.sqs.model.Message;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AsynchJobQueuePublisherImplTest {
	
	private static long MAX_WAIT = 1000*30;
	
	@Autowired
	AsynchJobQueuePublisher asynchJobQueuePublisher;
	
	@Before
	public void before(){
		// Start with empty queues
		asynchJobQueuePublisher.emptyAllQueues();
	}
	
	@Ignore // PLFM-3560
	@Test
	public void testPublishRoundTrip() throws Exception{
		AsynchronousJobStatus status = new AsynchronousJobStatus();
		status.setJobId("123");
		UploadToTableRequest body = new UploadToTableRequest();
		body.setTableId("syn8786");
		body.setUploadFileHandleId("333");
		status.setRequestBody(body);
		// publish it
		asynchJobQueuePublisher.publishMessage(status);
		// There should be one message on the queue
		Message message = waitForOneMessage();
		assertNotNull(message);
		AsynchronousJobStatus clone = EntityFactory.createEntityFromJSONString(message.getBody(), AsynchronousJobStatus.class);
		assertEquals(clone, status);
		// Delete the message
		asynchJobQueuePublisher.deleteMessage(AsynchJobType.UPLOAD_CSV_TO_TABLE, message);
	}

	/**
	 * @return
	 * @throws InterruptedException 
	 */
	public Message waitForOneMessage() throws InterruptedException {
		long start = System.currentTimeMillis();
		while(true){
			Message message = asynchJobQueuePublisher.recieveOneMessage(AsynchJobType.UPLOAD_CSV_TO_TABLE);
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
