package org.sagebionetworks.repo.manager.migration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessControlList;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.migration.MigrationType;

@ExtendWith(MockitoExtension.class)
class NodeRevisionAnnotationFixerMigrationTypeListenerTest {

	@Mock
	AmazonSQSClient mockSqsClient;

	@Mock
	StackConfiguration mockStackConfiguration;

	@InjectMocks
	NodeRevisionAnnotationFixerMigrationTypeListener listener;

	final String stackQueueName = "STACK-QUEUE-NAME";
	final String queueURL = "queue.url";

	List<DBORevision> delta;

	DBORevision dboRevision1;
	DBORevision dboRevision2;

	@BeforeEach
	public void setUp(){
		when(mockStackConfiguration.getQueueName(anyString())).thenReturn(stackQueueName);
		when(mockSqsClient.createQueue(stackQueueName)).thenReturn(new CreateQueueResult().withQueueUrl(queueURL));

		dboRevision1 = new DBORevision();
		dboRevision1.setOwner(123L);
		dboRevision1.setRevisionNumber(42L);
		dboRevision2 = new DBORevision();
		dboRevision2.setOwner(456L);
		dboRevision2.setRevisionNumber(10L);
		delta = Arrays.asList(dboRevision1, dboRevision2);

		listener.init();
	}

	@Test
	void afterCreateOrUpdate_notDesiredType() {

		listener.afterCreateOrUpdate(MigrationType.ACL_ACCESS, delta);

		verifyNoMoreInteractions(mockSqsClient);
	}

	@Test
	void afterCreateOrUpdate_happyCase() {

		listener.afterCreateOrUpdate(MigrationType.NODE_REVISION, delta);

		verify(mockSqsClient).sendMessage(queueURL, "123;42");
		verify(mockSqsClient).sendMessage(queueURL, "456;10");
		verifyNoMoreInteractions(mockSqsClient);
	}

	@Test
	void afterCreateOrUpdate_NotDBORevisionType() { //This should never happen

		listener.afterCreateOrUpdate(MigrationType.NODE_REVISION, Arrays.asList(new DBOAccessControlList(),dboRevision1));

		verify(mockSqsClient).sendMessage(queueURL, "123;42");
		verifyNoMoreInteractions(mockSqsClient);

	}

	@Test
	void testInit() {
		listener.init();
		assertEquals(queueURL, listener.queueUrl);
	}
}