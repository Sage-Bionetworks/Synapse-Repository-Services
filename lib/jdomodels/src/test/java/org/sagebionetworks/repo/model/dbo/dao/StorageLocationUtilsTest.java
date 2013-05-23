package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.storage.StorageUsage;

public class StorageLocationUtilsTest {

	@Test
	public void testCreateFileHandle(){

		StorageUsage su = new StorageUsage();
		su.setUserId("9999");
		su.setContentMd5("f3b41feeb58b1dd8a0110cb3f7990c7d");
		su.setContentSize(4444l);
		su.setLocation("/1419770/1419994/COPDPathwayReport.pdf");
		su.setContentType("application/pdf");

		S3FileHandle expected = StorageLocationUtils.createFileHandle(su);
		assertNotNull(expected);
		assertEquals(StackConfiguration.getS3Bucket(), expected.getBucketName());
		assertEquals(su.getUserId(), expected.getCreatedBy());
		assertEquals(su.getContentMd5(), expected.getContentMd5());
		assertEquals(su.getContentSize(), expected.getContentSize());
		assertEquals(su.getContentType(), expected.getContentType());
		assertEquals("1419770/1419994/COPDPathwayReport.pdf", expected.getKey());
		assertEquals("COPDPathwayReport.pdf", expected.getFileName());
		assertNotNull(expected.getEtag());
		assertNotNull(expected.getCreatedOn());
	}
}
