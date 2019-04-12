package org.sagebionetworks.repo.manager.migration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;

@ExtendWith(MockitoExtension.class)
class NodeRevisionAnnotationFixerMigrationTypeListenerTest {

	@Mock
	AmazonSQSClient mockSqsClient;

	@Mock
	StackConfiguration mockStackConfiguration;

	final String stackQueueName = "STACK-QUEUE-NAME";
	final String queueURL = "queue.url";

	@BeforeEach
	public void setUp(){
		when(mockStackConfiguration.getQueueName(anyString())).thenReturn(stackQueueName);
		when(mockSqsClient.createQueue(stackQueueName)).thenReturn(new CreateQueueResult().withQueueUrl(queueURL));
	}

	@Test
	void afterCreateOrUpdate() {

	}

	@Test
	void init() {
	}
}