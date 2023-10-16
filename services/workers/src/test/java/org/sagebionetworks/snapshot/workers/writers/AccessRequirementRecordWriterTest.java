package org.sagebionetworks.snapshot.workers.writers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.dataaccess.AccessRequirementManager;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.snapshot.workers.KinesisObjectSnapshotRecord;

@ExtendWith(MockitoExtension.class)
public class AccessRequirementRecordWriterTest {
	
	@Mock
	private AwsKinesisFirehoseLogger mockLogger;
	
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
	public void testBuildAndWriteRecordsWithCreate() throws IOException {
		
		AccessRequirement ar = new ManagedACTAccessRequirement().setId(123L).setVersionNumber(1L).setCreatedOn(new Date()).setName("ar1");
		
		when(mockManager.getAccessRequirementVersion(any(), any())).thenReturn(Optional.of(ar));
		
		ChangeMessage  message = new ChangeMessage().setObjectId("123").setObjectVersion(1L).setUserId(456L).setChangeType(ChangeType.CREATE).setObjectType(ObjectType.ACCESS_REQUIREMENT).setTimestamp(new Date());
		
		KinesisObjectSnapshotRecord<?> expected = KinesisObjectSnapshotRecord.map(message, ar);
		
		// Call under test
		writer.buildAndWriteRecords(null, List.of(message));
		
		verify(mockManager).getAccessRequirementVersion(message.getObjectId(), message.getObjectVersion());
		
		verify(mockLogger).logBatch(eq("accessRequirementSnapshots"), recordCaptor.capture());
		
		verifyNoMoreInteractions(mockManager);
		
		List<KinesisObjectSnapshotRecord<?>> result = recordCaptor.getValue();
		
		assertEquals(List.of(expected.withSnapshotTimestamp(result.get(0).getSnapshotTimestamp())), result);
	}
	
	@Test
	public void testBuildAndWriteRecordsWithUpdate() throws IOException {
		
		AccessRequirement ar = new ManagedACTAccessRequirement().setId(123L).setVersionNumber(2L).setCreatedOn(new Date()).setName("ar1");
		
		when(mockManager.getAccessRequirementVersion(any(), any())).thenReturn(Optional.of(ar));
		
		ChangeMessage  message = new ChangeMessage().setObjectId("123").setObjectVersion(2L).setUserId(456L).setChangeType(ChangeType.UPDATE).setObjectType(ObjectType.ACCESS_REQUIREMENT).setTimestamp(new Date());
		
		KinesisObjectSnapshotRecord<?> expected = KinesisObjectSnapshotRecord.map(message, ar);
		
		// Call under test
		writer.buildAndWriteRecords(null, List.of(message));
		
		verify(mockManager).getAccessRequirementVersion(message.getObjectId(), message.getObjectVersion());
		
		verify(mockLogger).logBatch(eq("accessRequirementSnapshots"), recordCaptor.capture());
		
		verifyNoMoreInteractions(mockManager);
		
		List<KinesisObjectSnapshotRecord<?>> result = recordCaptor.getValue();
		
		assertEquals(List.of(expected.withSnapshotTimestamp(result.get(0).getSnapshotTimestamp())), result);
	}
	
	@Test
	public void testBuildAndWriteRecordsWithDelete() throws IOException {
				
		ChangeMessage  message = new ChangeMessage().setObjectId("123").setObjectVersion(null).setUserId(456L).setChangeType(ChangeType.DELETE).setObjectType(ObjectType.ACCESS_REQUIREMENT).setTimestamp(new Date());
		
		KinesisObjectSnapshotRecord<?> expected = KinesisObjectSnapshotRecord.map(message, new ManagedACTAccessRequirement().setId(123L).setConcreteType(null));
		
		// Call under test
		writer.buildAndWriteRecords(null, List.of(message));
				
		verify(mockLogger).logBatch(eq("accessRequirementSnapshots"), recordCaptor.capture());
		
		verifyZeroInteractions(mockManager);
		
		List<KinesisObjectSnapshotRecord<?>> result = recordCaptor.getValue();
		
		assertEquals(List.of(expected.withSnapshotTimestamp(result.get(0).getSnapshotTimestamp())), result);
	}
	
	@Test
	public void testBuildAndWriteRecordsWithNotFound() throws IOException {
				
		when(mockManager.getAccessRequirementVersion(any(), any())).thenReturn(Optional.empty());
		
		ChangeMessage  message = new ChangeMessage().setObjectId("123").setObjectVersion(1L).setUserId(456L).setChangeType(ChangeType.CREATE).setObjectType(ObjectType.ACCESS_REQUIREMENT).setTimestamp(new Date());
				
		// Call under test
		writer.buildAndWriteRecords(null, List.of(message));
						
		verify(mockManager).getAccessRequirementVersion(message.getObjectId(), message.getObjectVersion());
		
		verifyZeroInteractions(mockLogger);
	}
	
}
