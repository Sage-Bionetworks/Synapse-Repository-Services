package org.sagebionetworks.repo.model.dbo.asynch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;

import org.junit.Test;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;

public class AsynchJobTypeTest {

	@Test
	public void testFindTypeFromRequestClass() throws Exception {
		for (AsynchJobType t: AsynchJobType.values()) {
			AsynchJobType type = AsynchJobType.findTypeFromRequestClass(t.getRequestClass());
			assertEquals(t, type);
		}
	}


	@Test
	public void XStreamAliases__ForRequestObject(){
		BulkFileDownloadRequest request = new BulkFileDownloadRequest();
		StringWriter writer = new StringWriter();
		AsynchJobType.getRequestXStream().toXML(request, writer);

		assertTrue(writer.toString().contains("BULK__FILE__DOWNLOAD"));
	}

	@Test
	public void XStreamAliases__ForResponseObject(){
		BulkFileDownloadResponse response = new BulkFileDownloadResponse();
		StringWriter writer = new StringWriter();
		AsynchJobType.getResponseXStream().toXML(response, writer);

		assertTrue(writer.toString().contains("BULK__FILE__DOWNLOAD"));
	}
}
