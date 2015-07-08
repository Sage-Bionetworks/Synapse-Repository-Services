package org.sagebionetworks.asynchronous.workers.sqs;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageBatchProcessor;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.progress.ProgressCallback;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;

public class ChangeMessageBatchProcessorTest {

	ProgressCallback<Message> mockProgressCallback;
	AmazonSQSClient mockAwsSQSClient;
	ChangeMessageDrivenRunner mockRunner;
	String queueName;
	String queueUrl;

	ChangeMessageBatchProcessor processor;
	Message awsMessage;
	ChangeMessage one;
	ChangeMessage two;

	@Before
	public void before() throws Exception {
		queueName = "someQueue";
		queueUrl = "someQueueUrl";
		mockProgressCallback = Mockito.mock(ProgressCallback.class);
		mockAwsSQSClient = Mockito.mock(AmazonSQSClient.class);
		mockRunner = Mockito.mock(ChangeMessageDrivenRunner.class);
		when(mockAwsSQSClient.getQueueUrl(queueName)).thenReturn(
				new GetQueueUrlResult().withQueueUrl(queueUrl));
		processor = new ChangeMessageBatchProcessor(mockAwsSQSClient,
				queueName, mockRunner);

		// one
		one = new ChangeMessage();
		one.setChangeType(ChangeType.DELETE);
		one.setObjectEtag("etag1");
		one.setObjectId("456");
		one.setObjectId("synABC");
		// two
		two = new ChangeMessage();
		two.setChangeType(ChangeType.DELETE);
		two.setObjectEtag("etag2");
		two.setObjectId("789");
		two.setObjectId("synXYZ");
		ChangeMessages messages = new ChangeMessages();
		messages.setList(Arrays.asList(one, two));
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
		verify(mockProgressCallback, times(2)).progressMade(awsMessage);
	}

	@Test
	public void testRecoverableBatch() throws RecoverableMessageException,
			Exception {
		// setup RecoverableMessageException failures
		doThrow(new RecoverableMessageException()).when(mockRunner).run(
				any(ProgressCallback.class), any(ChangeMessage.class));
		// call under test
		processor.run(mockProgressCallback, awsMessage);
		verify(mockRunner, times(2)).run(any(ProgressCallback.class),
				any(ChangeMessage.class));
		verify(mockProgressCallback, times(2)).progressMade(awsMessage);
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
				ProgressCallback<ChangeMessage> progress = (ProgressCallback<ChangeMessage>) args[0];
				ChangeMessage change = (ChangeMessage) args[1];
				progress.progressMade(change);
				
				return null;
			}
		}).when(mockRunner).run(any(ProgressCallback.class),
				any(ChangeMessage.class));
		// call under test
		processor.run(mockProgressCallback, awsMessage);
		verify(mockRunner, times(2)).run(any(ProgressCallback.class),
				any(ChangeMessage.class));
		verify(mockProgressCallback, times(4)).progressMade(awsMessage);
	}
	
	@Test
	public void testMixedSuccess() throws RecoverableMessageException,
			Exception {
		// For this case the worker makes progress.
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				ProgressCallback<ChangeMessage> progress = (ProgressCallback<ChangeMessage>) args[0];
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
		verify(mockProgressCallback, times(2)).progressMade(awsMessage);
	}
}
