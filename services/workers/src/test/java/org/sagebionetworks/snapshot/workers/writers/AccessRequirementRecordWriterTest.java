package org.sagebionetworks.snapshot.workers.writers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.dataaccess.AccessRequirementManager;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.snapshot.workers.KinesisObjectSnapshotRecord;

@ExtendWith(MockitoExtension.class)
public class AccessRequirementRecordWriterTest {
	
	@Mock
	private AwsKinesisFirehoseLogger mockLoger;
	
	@Mock
	private AccessRequirementManager mockManager;
	
	@InjectMocks
	private AccessRequirementRecordWriter writer;

	@Captor
	private ArgumentCaptor<List<KinesisObjectSnapshotRecord<?>>> recordCaptor;
	
	@Test
	public void getObjectType() {
		assertEquals(ObjectType.ACCESS_REQUIREMENT, writer.getObjectType());
	}
	
	@Test
	public void testBuildAndWriteRecords() throws IOException {
		
		Map<String, AccessRequirement> ars = Map.of(
			"123", new ManagedACTAccessRequirement().setId(123L).setCreatedOn(new Date()).setName("ar1"),
			"456", new LockAccessRequirement().setId(456L).setCreatedOn(new Date()).setName("ar2"),
			"789", new TermsOfUseAccessRequirement().setId(789L).setCreatedOn(new Date()).setName("ar3")
		);
		
		when(mockManager.getAccessRequirement(anyString())).thenAnswer( invocation -> {
			String id = invocation.getArgument(0, String.class);
			AccessRequirement ar = ars.get(id);
			if (ar == null) {
				throw new NotFoundException("nope");
			}
			return ar;			
		});
		
		List<ChangeMessage> messages = List.of(
			new ChangeMessage().setObjectId("123").setUserId(456L).setChangeType(ChangeType.CREATE).setObjectType(ObjectType.ACCESS_REQUIREMENT).setTimestamp(new Date()),
			new ChangeMessage().setObjectId("456").setUserId(456L).setChangeType(ChangeType.DELETE).setObjectType(ObjectType.ACCESS_REQUIREMENT).setTimestamp(new Date()),
			new ChangeMessage().setObjectId("789").setUserId(456L).setChangeType(ChangeType.UPDATE).setObjectType(ObjectType.ACCESS_REQUIREMENT).setTimestamp(new Date()),
			// Non existing
			new ChangeMessage().setObjectId("1111").setUserId(456L).setChangeType(ChangeType.UPDATE).setObjectType(ObjectType.ACCESS_REQUIREMENT).setTimestamp(new Date())
		);
		
		List<KinesisObjectSnapshotRecord<AccessRequirement>> expectedRecords = messages.stream().map( m-> 
			KinesisObjectSnapshotRecord.map(m, ars.get(m.getObjectId()))
		).filter(r -> r.getSnapshot() != null).collect(Collectors.toList());
				
		// Call under test
		writer.buildAndWriteRecords(null, messages);
		
		verify(mockLoger).logBatch(eq("accessRequirementSnapshots"), recordCaptor.capture());
		
		List<KinesisObjectSnapshotRecord<?>> result = recordCaptor.getValue();
		
		for (int i=0; i<result.size(); i++) {
			expectedRecords.get(i).withSnapshotTimestamp(result.get(i).getSnapshotTimestamp());
		}
		
		assertEquals(expectedRecords, result);
	}
	
}
