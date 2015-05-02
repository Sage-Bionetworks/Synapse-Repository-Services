package org.sagebionetworks.asynchronous.workers.sqs;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.sagebionetworks.repo.model.ObjectType;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;


public class MessageQueueImplTest {
	
	AmazonSQSClient mockSQSClient;
	AmazonSNSClient mockSNSClient;
	MessageQueueImpl msgQImpl;

	@Before
	public void setUp() throws Exception {
		mockSQSClient = Mockito.mock(AmazonSQSClient.class);
		mockSNSClient = Mockito.mock(AmazonSNSClient.class);
		List<ObjectType> objTypes = new ArrayList<ObjectType>();
		objTypes.add(ObjectType.ENTITY);
		msgQImpl = new MessageQueueImpl("queueName", "topicPrefixOrName", objTypes, true,
			 5, "deadLetterQueueName");
		ReflectionTestUtils.setField(msgQImpl, "awsSQSClient", mockSQSClient);
		ReflectionTestUtils.setField(msgQImpl, "awsSNSClient", mockSNSClient);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCreateQueue() {
		CreateQueueResult expectedRes = new CreateQueueResult().withQueueUrl("url");
		when(mockSQSClient.createQueue(any(CreateQueueRequest.class))).thenReturn(expectedRes);
		String qUrl = msgQImpl.createQueue("queueName");
		assertEquals("url", qUrl);
	}
	
}
