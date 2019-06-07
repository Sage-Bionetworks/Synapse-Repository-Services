package org.sagebionetworks.search;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeType;

class CloudSearchDocumentGenerationAwsKinesisLogRecordTest {

	@Test
	void testToBytes() {
		CloudSearchDocumentGenerationAwsKinesisLogRecord logRecord = new CloudSearchDocumentGenerationAwsKinesisLogRecord()
				.withChangeNumber(123L)
				.withChangeType(ChangeType.CREATE)
				.withObjectType(ObjectType.ENTITY)
				.withSynapseId("12345")
				.withEtag("eeeeeeeeeeeeeeeeetag")
				.withExistsOnIndex(false)
				.withDocumentBatchUpdateStatus("success")
				.withDocumentBatchUpdateTimestamp(98765L)
				.withDocumentBatchUUID("uuuuuuuuuuuuuuuuuuID")
				.withStack("dev")
				.withInstance("test");

		byte[] bytes = logRecord.toBytes();

		//convert bytes back to JSON string to compare
		String expectedJSON = "{\"changeNumber\":123," +
				"\"synapseId\":\"12345\"," +
				"\"etag\":\"eeeeeeeeeeeeeeeeetag\"," +
				"\"objectType\":\"ENTITY\","+
				"\"changeType\":\"CREATE\"," +
				"\"existsOnIndex\":false," +
				"\"documentBatchUUID\":\"uuuuuuuuuuuuuuuuuuID\"," +
				"\"documentBatchUpdateStatus\":\"success\"," +
				"\"documentBatchUpdateTimestamp\":98765," +
				"\"stack\":\"dev\"," +
				"\"instance\":\"test\"}" +
				"\n"; //important that documents are separated by a new line for AWS Athena to process them
		assertEquals(expectedJSON, new String(bytes, StandardCharsets.UTF_8));
	}
}