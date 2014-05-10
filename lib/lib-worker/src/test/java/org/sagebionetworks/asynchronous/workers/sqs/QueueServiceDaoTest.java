package org.sagebionetworks.asynchronous.workers.sqs;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

public class QueueServiceDaoTest {
	
	AmazonSQSClient mockSQSClient;
	QueueServiceDaoImpl queueServiceDao;
	int maxRequestSize;
	
	@Before
	public void before(){
		mockSQSClient = Mockito.mock(AmazonSQSClient.class);
		queueServiceDao = new QueueServiceDaoImpl();
		maxRequestSize = 2;
		ReflectionTestUtils.setField(queueServiceDao, "amazonSQSClient", mockSQSClient);
		ReflectionTestUtils.setField(queueServiceDao, "maxSQSRequestSize", maxRequestSize);
		
		stub(mockSQSClient.receiveMessage(any(ReceiveMessageRequest.class))).toAnswer(new Answer<ReceiveMessageResult>() {

			@Override
			public ReceiveMessageResult answer(InvocationOnMock invocation)
					throws Throwable {
				ReceiveMessageRequest request= (ReceiveMessageRequest) invocation.getArguments()[0];
				for(int i=0; i<request.getMaxNumberOfMessages(); i++){
					
				}
				return null;
			}
		});
	}
	
	
	@Test
	public void test(){
		
	}

}
