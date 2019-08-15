package org.sagebionetworks.repo.manager.statistics.records;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;

public class StatisticsFileEventRecordUnitTest {
	
	@Test
	void testToBytes() {
		StatisticsFileEventLogRecord logRecord = new StatisticsFileEventLogRecord()
				.withAssociation(FileHandleAssociateType.FileEntity, "123")
				.withUserId(123L)
				.withProjectId(123L)
				.withTimestamp(98765L)
				.withFileHandleId("123")
				.withStack("dev")
				.withInstance("test");

		byte[] bytes = logRecord.toBytes();

		//convert bytes back to JSON string to compare
		String expectedJSON = 
				"{\"userId\":123," +
				"\"timestamp\":98765," +
				"\"projectId\":123,"+
				"\"fileHandleId\":\"123\"," +
				"\"associateType\":\"FileEntity\"," +
				"\"associateId\":\"123\"," +
				"\"stack\":\"dev\"," +
				"\"instance\":\"test\"}" +
				"\n"; //important that documents are separated by a new line for AWS Athena to process them
		assertEquals(expectedJSON, new String(bytes, StandardCharsets.UTF_8));
	}

}
