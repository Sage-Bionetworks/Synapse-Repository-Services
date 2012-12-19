package org.sagebionetworks.repo.model.dbo;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileMetadata;
import org.sagebionetworks.repo.model.file.ExternalFileMetadata;
import org.sagebionetworks.repo.model.file.FileMetadata;
import org.sagebionetworks.repo.model.file.PreviewFileMetadata;
import org.sagebionetworks.repo.model.file.S3FileMetadata;

public class FileMetadataUtilsTest {
	
	@Test
	public void testExternalFileMetadataRoundTrip(){
		// External
		ExternalFileMetadata meta = new ExternalFileMetadata();
		meta.setCreatedBy("456");
		meta.setCreatedOn(new Date());
		meta.setExternalURL("http://google.com");
		meta.setId("987");
		meta.setPreviewId("456");
		System.out.println(meta);
		// Convert to dbo
		DBOFileMetadata dbo = FileMetadataUtils.createDBOFromDTO(meta);
		assertNotNull(dbo);
		FileMetadata clone = FileMetadataUtils.createDTOFromDBO(dbo);
		assertEquals(meta, clone);
	}
	
	@Test
	public void testS3FileMetadataRoundTrip(){
		// External
		S3FileMetadata meta = new S3FileMetadata();
		meta.setCreatedBy("456");
		meta.setCreatedOn(new Date());
		meta.setId("987");
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentMd5("md5");
		meta.setContentSize(123l);
		meta.setContentType("contentType");
		meta.setPreviewId("9999");
		meta.setFileName("foo.txt");
		
		System.out.println(meta);
		// Convert to dbo
		DBOFileMetadata dbo = FileMetadataUtils.createDBOFromDTO(meta);
		assertNotNull(dbo);
		FileMetadata clone = FileMetadataUtils.createDTOFromDBO(dbo);
		assertEquals(meta, clone);
	}
	
	@Test
	public void testPreviewFileMetadataRoundTrip(){
		// External
		PreviewFileMetadata meta = new PreviewFileMetadata();
		meta.setCreatedBy("456");
		meta.setCreatedOn(new Date());
		meta.setId("987");
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentMd5("md5");
		meta.setContentSize(123l);
		meta.setContentType("contentType");
		meta.setFileName("preview.txt");
		System.out.println(meta);
		// Convert to dbo
		DBOFileMetadata dbo = FileMetadataUtils.createDBOFromDTO(meta);
		assertNotNull(dbo);
		FileMetadata clone = FileMetadataUtils.createDTOFromDBO(dbo);
		assertEquals(meta, clone);
	}


}
