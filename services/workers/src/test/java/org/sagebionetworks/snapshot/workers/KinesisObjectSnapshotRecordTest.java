package org.sagebionetworks.snapshot.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.audit.NodeRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;

@ExtendWith(MockitoExtension.class)
public class KinesisObjectSnapshotRecordTest {

	@Test
	public void testMapFromChange() {
		
		ChangeMessage message = new ChangeMessage()
			.setObjectId("123")
			.setUserId(456L)
			.setChangeType(ChangeType.CREATE)
			.setObjectType(ObjectType.ENTITY)
			.setTimestamp(new Date());
		
		NodeRecord record = new NodeRecord();
		record.setId("syn123");
		
		KinesisObjectSnapshotRecord<NodeRecord> expected = new KinesisObjectSnapshotRecord<NodeRecord>()
			.withChangeType(ChangeType.CREATE)
			.withObjectType(ObjectType.ENTITY)
			.withUserId(456L)
			.withSnapshot(record)
			.withChangeTimestamp(message.getTimestamp().getTime()
		);
		
		// Call under test
		KinesisObjectSnapshotRecord<NodeRecord> result = KinesisObjectSnapshotRecord.map(message, record);
		
		assertNotNull(result.getSnapshotTimestamp());
		
		expected.withSnapshotTimestamp(result.getSnapshotTimestamp());
		
		assertEquals(expected, result);
	}

}
