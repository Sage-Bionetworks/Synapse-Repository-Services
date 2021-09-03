package org.sagebionetworks.repo.manager.asynch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dbo.asynch.AsynchJobType;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AsynchJobQueuePublisherImplTest {
	
	private static long MAX_WAIT = 1000*60*2;
	
	@Autowired
	private AsynchJobQueuePublisher asynchJobQueuePublisher;
	
	@BeforeEach
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
	 * @throws Exception 
	 */
	public Message waitForOneMessage() throws Exception {
		return TimeUtils.waitFor(MAX_WAIT, 1000L, () -> {
			Message message = asynchJobQueuePublisher.recieveOneMessage(AsynchJobType.TABLE_UPDATE_TRANSACTION);
			return new Pair<>(message != null, message);
		});
	}

}
