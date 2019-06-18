package org.sagebionetworks.repo.model.dbo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.MalformedURLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.backup.FileHandleBackup;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle.MetadataType;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.ExternalObjectStoreFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
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
		meta.setContentType("text/plain");
		// PLFM-3466
		meta.setContentSize(12345L);
		System.out.println(meta);
		// Convert to dbo
		DBOFileHandle dbo = FileMetadataUtils.createDBOFromDTO(meta);
		assertNotNull(dbo);
		FileHandle clone = FileMetadataUtils.createDTOFromDBO(dbo);
		assertEquals(meta, clone);
	}
	
	@Test
	public void testExternalFileMetadataRoundTripSizeNull() throws MalformedURLException{
		// External
		ExternalFileHandle meta = new ExternalFileHandle();
		meta.setCreatedBy("456");
		meta.setCreatedOn(new Date());
		meta.setExternalURL("http://google.com");
		meta.setId("987");
		meta.setPreviewId("456");
		meta.setEtag("etag");
		meta.setFileName("fileName");
		meta.setContentType("text/plain");
		// PLFM-3466
		meta.setContentSize(null);
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
	
	@Test
	public void testCreateBackupFromDBOEmpty(){
		DBOFileHandle dbo = new DBOFileHandle();
		FileHandleBackup backup = FileMetadataUtils.createBackupFromDBO(dbo);
		assertNotNull(backup);
	}
	
	@Test
	public void testBackupRoundTrip(){
		DBOFileHandle dbo = new DBOFileHandle();
		dbo.setBucketName("bucket");
		dbo.setContentMD5("md5");
		dbo.setContentSize(123l);
		dbo.setContentType("contentType");
		dbo.setCreatedOn(new Timestamp(1l));
		dbo.setCreatedBy(9999l);
		dbo.setEtag("etag");
		dbo.setId(456l);
		dbo.setKey("key");
		dbo.setMetadataType(MetadataType.PREVIEW);
		dbo.setName("name");
		dbo.setPreviewId(4444l);

		// to Backup
		FileHandleBackup backup = FileMetadataUtils.createBackupFromDBO(dbo);
		assertNotNull(backup);
		// Clone from the backup
		DBOFileHandle clone = FileMetadataUtils.createDBOFromBackup(backup);
		assertNotNull(clone);
		assertEquals(dbo, clone);
	}
	
	@Test
	public void testProxyFileHandleRoundTrip(){
		ProxyFileHandle proxy = new ProxyFileHandle();
		proxy.setFilePath("/foo/bar/cat.txt");
		proxy.setCreatedBy("456");
		proxy.setCreatedOn(new Date());
		proxy.setId("987");
		proxy.setContentMd5("md5");
		proxy.setContentSize(123l);
		proxy.setContentType("contentType");
		proxy.setPreviewId("9999");
		proxy.setEtag("etag");
		proxy.setFileName("cat.txt");
		
		DBOFileHandle dbo = FileMetadataUtils.createDBOFromDTO(proxy);
		assertNotNull(dbo);
		FileHandle clone = FileMetadataUtils.createDTOFromDBO(dbo);
		assertEquals(proxy, clone);
	}

	@Test
	public void testExternalObjectStoreFileHandleRoundTrip(){
		ExternalObjectStoreFileHandle externalObjFH = new ExternalObjectStoreFileHandle();
		externalObjFH.setCreatedBy("456");
		externalObjFH.setCreatedOn(new Date());
		externalObjFH.setId("987");
		externalObjFH.setContentMd5("md5");
		externalObjFH.setContentSize(123l);
		externalObjFH.setContentType("contentType");
		externalObjFH.setEtag("etag");
		externalObjFH.setFileName("cat.txt");
		externalObjFH.setFileKey("filekey");
		externalObjFH.setEndpointUrl("https//s3.amazonaws.com");
		externalObjFH.setBucket("bucketName");

		DBOFileHandle dbo = FileMetadataUtils.createDBOFromDTO(externalObjFH);
		assertNotNull(dbo);
		FileHandle clone = FileMetadataUtils.createDTOFromDBO(dbo);
		assertEquals(externalObjFH, clone);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testCreateDBOsFromDTOsWithNullList(){
		FileMetadataUtils.createDBOsFromDTOs(null);
	}

	@Test
	public void testCreateDBOsFromDTOs() {
		ExternalFileHandle external = new ExternalFileHandle();
		external.setCreatedBy("456");
		external.setCreatedOn(new Date());
		external.setExternalURL("http://google.com");
		external.setId("987");
		external.setPreviewId("456");
		external.setEtag("etag");
		external.setFileName("fileName");
		external.setContentType("text/plain");
		external.setContentSize(12345L);

		S3FileHandle s3 = new S3FileHandle();
		s3.setCreatedBy("456");
		s3.setCreatedOn(new Date());
		s3.setId("987");
		s3.setBucketName("bucketName");
		s3.setKey("key");
		s3.setContentMd5("md5");
		s3.setContentSize(123l);
		s3.setContentType("contentType");
		s3.setPreviewId("9999");
		s3.setEtag("etag");
		s3.setFileName("foo.txt");

		PreviewFileHandle preview = new PreviewFileHandle();
		preview.setCreatedBy("456");
		preview.setCreatedOn(new Date());
		preview.setId("987");
		preview.setBucketName("bucketName");
		preview.setKey("key");
		preview.setContentMd5("md5");
		preview.setContentSize(123l);
		preview.setContentType("contentType");
		preview.setFileName("preview.txt");
		preview.setEtag("etag");

		ProxyFileHandle proxy = new ProxyFileHandle();
		proxy.setFilePath("/foo/bar/cat.txt");
		proxy.setCreatedBy("456");
		proxy.setCreatedOn(new Date());
		proxy.setId("987");
		proxy.setContentMd5("md5");
		proxy.setContentSize(123l);
		proxy.setContentType("contentType");
		proxy.setPreviewId("9999");
		proxy.setFileName("cat.txt");

		List<FileHandle> list = new ArrayList<FileHandle>();
		list.addAll(Arrays.asList(external, s3, preview, proxy));
		List<DBOFileHandle> dbos = FileMetadataUtils.createDBOsFromDTOs(list);

		for (int i = 0; i < list.size(); i++) {
			assertEquals(list.get(i), FileMetadataUtils.createDTOFromDBO(dbos.get(i)));
		}
	}
}
