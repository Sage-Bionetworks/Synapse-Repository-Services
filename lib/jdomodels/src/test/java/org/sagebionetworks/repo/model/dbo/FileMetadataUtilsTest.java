package org.sagebionetworks.repo.model.dbo;

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;

public class FileMetadataUtilsTest {
	
	@Test
	public void testExternalFileMetadataRoundTrip() throws MalformedURLException{
		// External
		ExternalFileHandle meta = new ExternalFileHandle();
		meta.setCreatedBy("456");
		meta.setCreatedOn(new Date());
		meta.setExternalURL("http://google.com");
		meta.setId("987");
		meta.setPreviewId("456");
		meta.setEtag("etag");
		meta.setFileName("fileName");
		System.out.println(meta);
		// Convert to dbo
		DBOFileHandle dbo = FileMetadataUtils.createDBOFromDTO(meta);
		assertNotNull(dbo);
		FileHandle clone = FileMetadataUtils.createDTOFromDBO(dbo);
		assertEquals(meta, clone);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testExternalFileMalformedURL() {
		// External
		ExternalFileHandle meta = new ExternalFileHandle();
		meta.setCreatedBy("456");
		meta.setCreatedOn(new Date());
		// malformed URL
		meta.setExternalURL("F:/file");
		meta.setId("987");
		meta.setPreviewId("456");
		meta.setEtag("etag");
		System.out.println(meta);
		// Convert to dbo
		DBOFileHandle dbo = FileMetadataUtils.createDBOFromDTO(meta);
		assertNotNull(dbo);
		FileHandle clone = FileMetadataUtils.createDTOFromDBO(dbo);
		assertEquals(meta, clone);
	}
	
	@Test
	public void testS3FileMetadataRoundTrip() throws MalformedURLException{
		// External
		S3FileHandle meta = new S3FileHandle();
		meta.setCreatedBy("456");
		meta.setCreatedOn(new Date());
		meta.setId("987");
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentMd5("md5");
		meta.setContentSize(123l);
		meta.setContentType("contentType");
		meta.setPreviewId("9999");
		meta.setEtag("etag");
		meta.setFileName("foo.txt");
		
		System.out.println(meta);
		// Convert to dbo
		DBOFileHandle dbo = FileMetadataUtils.createDBOFromDTO(meta);
		assertNotNull(dbo);
		FileHandle clone = FileMetadataUtils.createDTOFromDBO(dbo);
		assertEquals(meta, clone);
	}
	
	@Test
	public void testPreviewFileMetadataRoundTrip() throws MalformedURLException{
		// External
		PreviewFileHandle meta = new PreviewFileHandle();
		meta.setCreatedBy("456");
		meta.setCreatedOn(new Date());
		meta.setId("987");
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentMd5("md5");
		meta.setContentSize(123l);
		meta.setContentType("contentType");
		meta.setFileName("preview.txt");
		meta.setEtag("etag");
		System.out.println(meta);
		// Convert to dbo
		DBOFileHandle dbo = FileMetadataUtils.createDBOFromDTO(meta);
		assertNotNull(dbo);
		FileHandle clone = FileMetadataUtils.createDTOFromDBO(dbo);
		assertEquals(meta, clone);
	}


}
