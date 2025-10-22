package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@ExtendWith(MockitoExtension.class)
public class PatchBuilderPublisherImplTest {

	@Mock
	private SqsClient mockSqsClient;
	@Mock
	private StackConfiguration mockConfig;

	private String queueUrl;
	private String queueName;
	private IntendedChangeSet changeSet;

	private PatchBuilderPublisherImpl publisher;

	@BeforeEach
	public void before() {
		queueUrl = "https://some.com/queue";
		queueName = "dev-jmhill-GRID_REPLICA_PATCH_BUILDER.fifo";
		when(mockConfig.getQueueName("GRID_REPLICA_PATCH_BUILDER.fifo")).thenReturn(queueName);
		when(mockSqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()))
				.thenReturn(GetQueueUrlResponse.builder().queueUrl(queueUrl).build());
		publisher = new PatchBuilderPublisherImpl(mockSqsClient, mockConfig);

		changeSet = new IntendedChangeSet().setConnectionId("con123").setReplicaId(33L).setSessionId("session99")
				.setChanges(List.of(new UpdateMetadataChange()
						.setRowMetadataId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L))))
				.setClockSequenceMaximum(43l);
	}

	@Test
	public void testSendChangesToPatchBuilder() {
		// call under test
		publisher.sendChangesToPatchBuilder(changeSet);
		verify(mockSqsClient).sendMessage(SendMessageRequest.builder().queueUrl(queueUrl)
				.messageBody("{\"con\":\"con123\",\"ses\":\"session99\",\"rep\":33,\"max\":43,\"set\":[[0,{\"m\":[1,2]}]]}")
				.messageGroupId("con123").messageDeduplicationId("d3e4b78f2bec942e32e56640a2a49cb6").build());
	}

}
