package org.sagebionetworks.asynchronous.workers.sqs;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;


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
	
	@Test
	public void testGetQueueArn() {
		Map<String, String> qAttr = new HashMap<String, String>();
		qAttr.put("QueueArn", "theQueueArn");
		GetQueueAttributesResult qAttRes = new GetQueueAttributesResult().withAttributes(qAttr);
		when(mockSQSClient.getQueueAttributes(any(GetQueueAttributesRequest.class))).thenReturn(qAttRes);
		String qArn = msgQImpl.getQueueArn("qUrl");
		assertEquals("theQueueArn", qArn);
	}
	
	@Test
	public void testGetRedrivePolicy() {
		String expectedPolicy = "{\"maxReceiveCount\":\"5\", \"deadLetterTargetArn\":\"deadLetterQueueArn\"}";
		String s = msgQImpl.getRedrivePolicy(5, "deadLetterQueueArn");
		assertEquals(expectedPolicy, s);
	}
	
}
