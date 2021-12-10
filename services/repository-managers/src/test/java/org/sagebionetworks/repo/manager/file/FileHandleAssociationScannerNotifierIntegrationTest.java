package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.IdRange;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationScanRangeRequest;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class FileHandleAssociationScannerNotifierIntegrationTest {
	
	private static final long TIMEOUT = 60 * 1000;

	@Autowired
	private FileHandleAssociationScannerNotifier notifier;
	
	@Autowired
	private AmazonSQS sqsClient;
	
	@Test
	public void testRoundTrip() throws Exception {
		
		FileHandleAssociationScanRangeRequest expected = new FileHandleAssociationScanRangeRequest()
				.withAssociationType(FileHandleAssociateType.FileEntity)
				.withJobId(123L)
				.withIdRange(new IdRange(1, 10));
		
		notifier.sendScanRequest(expected, /*delay*/ 0);
		
		FileHandleAssociationScanRangeRequest result = TimeUtils.waitFor(TIMEOUT, 1000L, () -> {
			
			ReceiveMessageResult messages = sqsClient.receiveMessage(notifier.getQueueUrl());
						
			if (!messages.getMessages().isEmpty()) {
				FileHandleAssociationScanRangeRequest request = notifier.fromSqsMessage(messages.getMessages().iterator().next());
				if (request.getJobId().equals(expected.getJobId())) {
					return Pair.create(true, request);	
				}
			}
			
			return Pair.create(false, null);
		});
		
		assertEquals(expected, result);
	}

}
