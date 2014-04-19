package org.sagebionetworks.repo.manager.asynch;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dbo.asynch.AsynchJobType;
import org.sagebionetworks.repo.model.table.AsynchUploadJobBody;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.amazonaws.services.sqs.model.Message;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AsynchJobQueuePublisherImplTest {
	
	@Autowired
	AsynchJobQueuePublisher asynchJobQueuePublisher;
	
	@Before
	public void before(){
		// Start with empty queues
		asynchJobQueuePublisher.emptyAllQueues();
	}
	
	@Test
	public void testPublishRoundTrip() throws JSONObjectAdapterException{
		AsynchronousJobStatus status = new AsynchronousJobStatus();
		status.setJobId("123");
		AsynchUploadJobBody body = new AsynchUploadJobBody();
		body.setTableId("syn8786");
		body.setUploadFileHandleId("333");
		status.setJobBody(body);
		// publish it
		asynchJobQueuePublisher.publishMessage(status);
		// There should be one message on the queue
		Message message = asynchJobQueuePublisher.recieveOneMessage(AsynchJobType.UPLOAD);
		assertNotNull(message);
		AsynchronousJobStatus clone = EntityFactory.createEntityFromJSONString(message.getBody(), AsynchronousJobStatus.class);
		assertEquals(clone, status);
		// Delete the message
		asynchJobQueuePublisher.deleteMessage(AsynchJobType.UPLOAD, message);
	}

}
