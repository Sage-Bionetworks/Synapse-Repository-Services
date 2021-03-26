package org.sagebionetworks.repo.model.dbo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.backup.FileHandleBackup;
import org.sagebionetworks.repo.model.dao.FileHandleMetadataType;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.ExternalObjectStoreFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.GoogleCloudFileHandle;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;

public class FileMetadataUtilsTest {
	
	@Test
	public void testExternalFileMetadataRoundTrip() {
		// External
		ExternalFileHandle meta = new ExternalFileHandle();
		meta.setCreatedBy("456");
		meta.setCreatedOn(new Date());
		meta.setExternalURL("http://google.com");
		meta.setId("987");
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
	public void testExternalFileMetadataRoundTripSizeNull() {
		// External
		ExternalFileHandle meta = new ExternalFileHandle();
		meta.setCreatedBy("456");
		meta.setCreatedOn(new Date());
		meta.setExternalURL("http://google.com");
		meta.setId("987");
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
	
	@Test
	public void testExternalFileMalformedURL() {
		// External
		ExternalFileHandle meta = new ExternalFileHandle();
		meta.setCreatedBy("456");
		meta.setCreatedOn(new Date());
		// malformed URL
		meta.setExternalURL("F:/file");
		meta.setId("987");
		meta.setEtag("etag");
		System.out.println(meta);
		// Convert to dbo
		assertThrows(IllegalArgumentException.class, () -> FileMetadataUtils.createDBOFromDTO(meta));
	}

	@Test
	public void testS3FileMetadataRoundTrip() {
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
		meta.setIsPreview(false);

		System.out.println(meta);
		// Convert to dbo
		DBOFileHandle dbo = FileMetadataUtils.createDBOFromDTO(meta);
		assertNotNull(dbo);
		FileHandle clone = FileMetadataUtils.createDTOFromDBO(dbo);
		assertEquals(meta, clone);
	}

	@Test
	public void testGoogleCloudFileMetadataRoundTrip() {
		GoogleCloudFileHandle meta = new GoogleCloudFileHandle();
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
		meta.setIsPreview(false);

		System.out.println(meta);
		// Convert to dbo
		DBOFileHandle dbo = FileMetadataUtils.createDBOFromDTO(meta);
		assertNotNull(dbo);
		FileHandle clone = FileMetadataUtils.createDTOFromDBO(dbo);
		assertEquals(meta, clone);
	}

	@Test
	public void isPreviewIsAlwaysSetToFalse() {
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

		// Set to null
		meta.setIsPreview(null);
		System.out.println(meta);
		// Call under test
		DBOFileHandle dbo = FileMetadataUtils.createDBOFromDTO(meta);
		assertNotNull(dbo);
		assertFalse(dbo.getIsPreview());

		// Set to false
		meta.setIsPreview(false);
		System.out.println(meta);
		// Call under test
		dbo = FileMetadataUtils.createDBOFromDTO(meta);
		assertNotNull(dbo);
		assertFalse(dbo.getIsPreview());
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
		dbo.setContentSize(123L);
		dbo.setContentType("contentType");
		dbo.setCreatedOn(new Timestamp(1L));
		dbo.setCreatedBy(9999L);
		dbo.setEtag("etag");
		dbo.setId(456L);
		dbo.setKey("key");
		dbo.setMetadataType(FileHandleMetadataType.S3);
		dbo.setName("name");
		dbo.setPreviewId(4444L);
		dbo.setIsPreview(false);

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

	@Test
	public void testCreateDBOsFromDTOsWithNullList(){
		assertThrows(IllegalArgumentException.class, () -> FileMetadataUtils.createDBOsFromDTOs(null));
	}

	@Test
	public void testCreateDBOsFromDTOs() {
		ExternalFileHandle external = new ExternalFileHandle();
		external.setCreatedBy("456");
		external.setCreatedOn(new Date());
		external.setExternalURL("http://google.com");
		external.setId("987");
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
		s3.setIsPreview(false);

		ProxyFileHandle proxy = new ProxyFileHandle();
		proxy.setFilePath("/foo/bar/cat.txt");
		proxy.setCreatedBy("456");
		proxy.setCreatedOn(new Date());
		proxy.setId("987");
		proxy.setContentMd5("md5");
		proxy.setContentSize(123l);
		proxy.setContentType("contentType");
		proxy.setFileName("cat.txt");

		List<FileHandle> list = new ArrayList<FileHandle>();
		list.addAll(Arrays.asList(external, s3, proxy));
		List<DBOFileHandle> dbos = FileMetadataUtils.createDBOsFromDTOs(list);

		for (int i = 0; i < list.size(); i++) {
			assertEquals(list.get(i), FileMetadataUtils.createDTOFromDBO(dbos.get(i)));
		}
	}
	
	// See PLFM-6637
	@Test
	public void testBackfillDefaultStorageLocation() {
		FileHandleBackup backup = new FileHandleBackup();
		backup.setMetadataType("S3");
		backup.setBucketName(FileMetadataUtils.DEFAULT_S3_BUCKET);
		backup.setStorageLocationId(null);

		DBOFileHandle expected = new DBOFileHandle();
		expected.setBucketName(FileMetadataUtils.DEFAULT_S3_BUCKET);
		expected.setMetadataType(FileHandleMetadataType.S3);
		expected.setStorageLocationId(StorageLocationDAO.DEFAULT_STORAGE_LOCATION_ID);
		expected.setIsPreview(false);
		
		DBOFileHandle result = FileMetadataUtils.createDBOFromBackup(backup);
		
		assertEquals(expected, result);
	}
	
	// See PLFM-6637
	@Test
	public void testBackfillDefaultStorageLocationWithDifferentBucket() {
		FileHandleBackup backup = new FileHandleBackup();
		backup.setMetadataType("S3");
		backup.setBucketName("anotherBucket");
		backup.setStorageLocationId(null);

		DBOFileHandle expected = new DBOFileHandle();
		expected.setBucketName("anotherBucket");
		expected.setMetadataType(FileHandleMetadataType.S3);
		expected.setStorageLocationId(null);
		expected.setIsPreview(false);
		
		DBOFileHandle result = FileMetadataUtils.createDBOFromBackup(backup);
		
		assertEquals(expected, result);
	}
	
	// See PLFM-6637
	@Test
	public void testBackfillDefaultStorageLocationWithPresent() {
		FileHandleBackup backup = new FileHandleBackup();
		backup.setMetadataType("S3");
		backup.setBucketName(FileMetadataUtils.DEFAULT_S3_BUCKET);
		backup.setStorageLocationId(123L);

		DBOFileHandle expected = new DBOFileHandle();
		expected.setBucketName(FileMetadataUtils.DEFAULT_S3_BUCKET);
		expected.setMetadataType(FileHandleMetadataType.S3);
		expected.setStorageLocationId(123L);
		expected.setIsPreview(false);
		
		DBOFileHandle result = FileMetadataUtils.createDBOFromBackup(backup);
		
		assertEquals(expected, result);
	}
	
	// See PLFM-6637
	@Test
	public void testBackfillDefaultStorageLocationWithNotS3() {
		FileHandleBackup backup = new FileHandleBackup();
		backup.setMetadataType("GOOGLE_CLOUD");
		backup.setBucketName(FileMetadataUtils.DEFAULT_S3_BUCKET);
		backup.setStorageLocationId(123L);

		DBOFileHandle expected = new DBOFileHandle();
		expected.setBucketName(FileMetadataUtils.DEFAULT_S3_BUCKET);
		expected.setMetadataType(FileHandleMetadataType.GOOGLE_CLOUD);
		expected.setStorageLocationId(123L);
		expected.setIsPreview(false);
		
		DBOFileHandle result = FileMetadataUtils.createDBOFromBackup(backup);
		
		assertEquals(expected, result);
	}
	
}
