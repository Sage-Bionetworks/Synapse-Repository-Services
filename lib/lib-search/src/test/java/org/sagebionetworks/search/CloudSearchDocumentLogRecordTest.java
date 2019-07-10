package org.sagebionetworks.search;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeType;

class CloudSearchDocumentLogRecordTest {

	@Test
	void testToBytes() {
		CloudSearchDocumentLogRecord logRecord = new CloudSearchDocumentLogRecord()
				.withChangeNumber(123L)
				.withChangeType(ChangeType.CREATE)
				.withObjectType(ObjectType.ENTITY)
				.withObjectId("12345")
				.withWikiOwner("456")
				.withAction(DocumentAction.CREATE_OR_UPDATE)
				.withDocumentBatchUpdateStatus("success")
				.withTimestamp(98765L)
				.withDocumentBatchUUID("uuuuuuuuuuuuuuuuuuID")
				.withStack("dev")
				.withInstance("test");

		byte[] bytes = logRecord.toBytes();

		//convert bytes back to JSON string to compare
		String expectedJSON = "{\"changeNumber\":123," +
				"\"objectId\":\"12345\"," +
				"\"objectType\":\"ENTITY\","+
				"\"changeType\":\"CREATE\"," +
				"\"wikiOwner\":\"456\"," +
				"\"action\":\"CREATE_OR_UPDATE\"," +
				"\"documentBatchUUID\":\"uuuuuuuuuuuuuuuuuuID\"," +
				"\"documentBatchUpdateStatus\":\"success\"," +
				"\"timestamp\":98765," +
				"\"stack\":\"dev\"," +
				"\"instance\":\"test\"}" +
				"\n"; //important that documents are separated by a new line for AWS Athena to process them
		assertEquals(expectedJSON, new String(bytes, StandardCharsets.UTF_8));
	}
}