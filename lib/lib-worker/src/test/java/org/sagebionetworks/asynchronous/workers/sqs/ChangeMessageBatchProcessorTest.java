package org.sagebionetworks.asynchronous.workers.sqs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.asynchronous.workers.changes.BatchChangeMessageDrivenRunner;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageBatchProcessor;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;

public class ChangeMessageBatchProcessorTest {

	@Mock
	ProgressCallback mockProgressCallback;
	@Mock
	AmazonSQS mockAwsSQSClient;
	@Mock
	ChangeMessageDrivenRunner mockRunner;
	@Mock
	BatchChangeMessageDrivenRunner mockBatchRunner;
	@Mock
	ChangeMessageRunner mockUnknownRunner;
	
	String queueName;
	String queueUrl;

	ChangeMessageBatchProcessor processor;
	Message awsMessage;
	ChangeMessage one;
	ChangeMessage two;
	List<ChangeMessage> messageList;
	

	@Before
	public void before() throws Exception {
		MockitoAnnotations.initMocks(this);
		queueName = "someQueue";
		queueUrl = "someQueueUrl";
		when(mockAwsSQSClient.getQueueUrl(queueName)).thenReturn(
				new GetQueueUrlResult().withQueueUrl(queueUrl));
		processor = new ChangeMessageBatchProcessor(mockAwsSQSClient,
				queueName, mockRunner);

		// one
		one = new ChangeMessage();
		one.setChangeType(ChangeType.DELETE);
		one.setObjectId("456");
		one.setObjectId("synABC");
		// two
		two = new ChangeMessage();
		two.setChangeType(ChangeType.DELETE);
		two.setObjectId("789");
		two.setObjectId("synXYZ");
		ChangeMessages messages = new ChangeMessages();
		messageList = Arrays.asList(one, two);
		messages.setList(messageList);
		// Set the message
		awsMessage = MessageUtils.createTopicMessage(messages, "topic:arn",
				"id", "handle");
	}

	@Test
	public void testHappy() throws RecoverableMessageException, Exception {
		// call under test
		processor.run(mockProgressCallback, awsMessage);
		// The runner should be called twice
		verify(mockRunner, times(2)).run(any(ProgressCallback.class),
				any(ChangeMessage.class));
	}

	@Test
	public void testRecoverableBatchWithSingleProcessor() throws RecoverableMessageException,
			Exception {
		// setup RecoverableMessageException failures
		doThrow(new RecoverableMessageException()).when(mockRunner).run(
				any(ProgressCallback.class), any(ChangeMessage.class));
		// call under test
		processor.run(mockProgressCallback, awsMessage);
		verify(mockRunner, times(2)).run(any(ProgressCallback.class),
				any(ChangeMessage.class));
		verify(mockAwsSQSClient).sendMessage(queueUrl,
				EntityFactory.createJSONStringForEntity(one));
		verify(mockAwsSQSClient).sendMessage(queueUrl,
				EntityFactory.createJSONStringForEntity(two));
	}
	
	/**
	 * When a batch processor fails with RecoverableMessageException, each
	 * change message should be restored to the queue individually.  
	 * 
	 * @throws RecoverableMessageException
	 * @throws Exception
	 */
	@Test
	public void testRecoverableBatchWithBatchProcessor() throws RecoverableMessageException,
			Exception {
		processor = new ChangeMessageBatchProcessor(mockAwsSQSClient,
				queueName, mockBatchRunner);
		// setup RecoverableMessageException failures
		doThrow(new Exception()).when(mockBatchRunner).run(
				any(ProgressCallback.class), anyListOf(ChangeMessage.class));
		// call under test
		processor.run(mockProgressCallback, awsMessage);
		verify(mockBatchRunner).run(any(ProgressCallback.class),
				anyListOf(ChangeMessage.class));
		verify(mockAwsSQSClient).sendMessage(queueUrl,
				EntityFactory.createJSONStringForEntity(one));
		verify(mockAwsSQSClient).sendMessage(queueUrl,
				EntityFactory.createJSONStringForEntity(two));
	}

	@Test(expected = RecoverableMessageException.class)
	public void testRecoverableSingle() throws RecoverableMessageException,
			Exception {
		doThrow(new RecoverableMessageException()).when(mockRunner).run(
				any(ProgressCallback.class), any(ChangeMessage.class));
		// for this case there is only a single messages
		awsMessage = MessageUtils.createMessage(one, "messageId",
				"messageHandle");
		// call under test
		processor.run(mockProgressCallback, awsMessage);
	}

	@Test
	public void testProgressMade() throws RecoverableMessageException,
			Exception {
		// For this case the worker makes progress.
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				ProgressCallback progress = (ProgressCallback) args[0];
				return null;
			}
		}).when(mockRunner).run(any(ProgressCallback.class),
				any(ChangeMessage.class));
		// call under test
		processor.run(mockProgressCallback, awsMessage);
		verify(mockRunner, times(2)).run(any(ProgressCallback.class),
				any(ChangeMessage.class));
	}
	
	@Test
	public void testMixedSuccess() throws RecoverableMessageException,
			Exception {
		// For this case the worker makes progress.
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				ChangeMessage change = (ChangeMessage) args[1];
				if(one.equals(change)){
					throw new IllegalArgumentException("Something is not right");
				}
				return null;
			}
		}).when(mockRunner).run(any(ProgressCallback.class),
				any(ChangeMessage.class));
		// call under test
		processor.run(mockProgressCallback, awsMessage);
		// Even though the first message triggered an error the second was processed.
		verify(mockRunner, times(2)).run(any(ProgressCallback.class),
				any(ChangeMessage.class));
	}
	
	@Test
	public void testBatchRunner() throws RecoverableMessageException, Exception{
		processor = new ChangeMessageBatchProcessor(mockAwsSQSClient,
				queueName, mockBatchRunner);
		// call under test
		processor.run(mockProgressCallback, awsMessage);
		verify(mockBatchRunner).run(mockProgressCallback, messageList);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testUnkownRunnerType() throws RecoverableMessageException, Exception{
		processor = new ChangeMessageBatchProcessor(mockAwsSQSClient,
				queueName, mockUnknownRunner);
		// call under test
		processor.run(mockProgressCallback, awsMessage);
	}
}
