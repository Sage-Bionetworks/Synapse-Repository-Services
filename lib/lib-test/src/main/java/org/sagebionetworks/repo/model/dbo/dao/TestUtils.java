package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.GoogleCloudFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;

import com.amazonaws.util.BinaryUtils;

public class TestUtils {

	/**
	 * Helper to create a S3FileHandle
	 * 
	 * @return
	 */
	public static S3FileHandle createS3FileHandle(String createdById, String fileHandleId) {
		return createS3FileHandle(createdById, 123, fileHandleId);
	}

	/**
	 * Helper to create a S3FileHandle
	 * 
	 * @return
	 */
	public static S3FileHandle createS3FileHandle(String createdById, int sizeInBytes, String fileHandleId) {
		return createS3FileHandle(createdById, sizeInBytes, "content type", fileHandleId);
	}

	/**
	 * Helper to create a S3FileHandle
	 * 
	 * @return
	 */
	public static S3FileHandle createS3FileHandle(String createdById, int sizeInBytes, String contentType, String fileHandleId) {
		S3FileHandle meta = new S3FileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType(contentType);
		meta.setContentSize((long)sizeInBytes);
		meta.setContentMd5("md5");
		meta.setCreatedBy(createdById);
		meta.setFileName("foobar.txt");
		meta.setId(fileHandleId);
		meta.setEtag(UUID.randomUUID().toString());
		meta.setIsPreview(false);
		return meta;
	}

	public static GoogleCloudFileHandle createGoogleCloudFileHandle(String createdById, String fileHandleId) {
		return createGoogleCloudFileHandle(createdById, 123, fileHandleId);
	}

	public static GoogleCloudFileHandle createGoogleCloudFileHandle(String createdById, int sizeInBytes, String fileHandleId) {
		return createGoogleCloudFileHandle(createdById, sizeInBytes, "content type", fileHandleId);
	}

	public static GoogleCloudFileHandle createGoogleCloudFileHandle(String createdById, int sizeInBytes, String contentType, String fileHandleId) {
		GoogleCloudFileHandle meta = new GoogleCloudFileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType(contentType);
		meta.setContentSize((long)sizeInBytes);
		meta.setContentMd5("md5");
		meta.setCreatedBy(createdById);
		meta.setFileName("foobar.txt");
		meta.setId(fileHandleId);
		meta.setEtag(UUID.randomUUID().toString());
		meta.setIsPreview(false);
		return meta;
	}

	/**
	 * Helper to create a PreviewFileHandle
	 * @return
	 */
	public static S3FileHandle createPreviewFileHandle(String createdById, String fileHandleId) {
		return createPreviewFileHandle(createdById, 123, fileHandleId);
	}

	/**
	 * Helper to create a PreviewFileHandle
	 * @return
	 */
	public static S3FileHandle createPreviewFileHandle(String createdById, int sizeInBytes, String fileHandleId) {
		return createPreviewFileHandle(createdById, sizeInBytes, "content type", fileHandleId);
	}

	/**
	 * Helper to create a PreviewFileHandle
	 * @return
	 */
	public static S3FileHandle createPreviewFileHandle(String createdById, int sizeInBytes, String contentType, String fileHandleId) {
		S3FileHandle meta = new S3FileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType(contentType);
		meta.setContentSize((long)sizeInBytes);
		meta.setContentMd5("md5");
		meta.setCreatedBy(createdById);
		meta.setFileName("preview.jpg");
		meta.setEtag(UUID.randomUUID().toString());
		meta.setId(fileHandleId);
		meta.setIsPreview(true);
		return meta;
	}

	/**
	 * Helper to create a PreviewFileHandle
	 * @return
	 */
	public static ExternalFileHandle createExternalFileHandle(String createdById) {
		return createExternalFileHandle(createdById, null);
	}

	/**
	 * Helper to create a PreviewFileHandle
	 * @return
	 */
	public static ExternalFileHandle createExternalFileHandle(String createdById, String fileHandleId) {
		ExternalFileHandle meta = new ExternalFileHandle();
		meta.setId(fileHandleId);
		meta.setExternalURL("http://www.example.com/");
		meta.setContentType("content type");
		meta.setCreatedBy(createdById);
		meta.setFileName("External");
		meta.setEtag(UUID.randomUUID().toString());
		return meta;
	}

	/**
	 * Calculate the MD5 digest of a given string.
	 * @param tocalculate
	 * @return
	 */
	public static String calculateMD5(String tocalculate){
		try {
			MessageDigest digetst = MessageDigest.getInstance("MD5");
			byte[] bytes = digetst.digest(tocalculate.getBytes("UTF-8"));
			return  BinaryUtils.toHex(bytes);	
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}	
	
	/**
	 * Create a populated Annotations object.
	 * 
	 * @return
	 */
	public static Annotations createDummyAnnotations() {
		return createDummyAnnotations(1);
	}
	
	public static final String PUBLIC_STRING_ANNOTATION_NAME = "string_anno";
	public static final String PUBLIC_STRING_ANNOTATION_WITH_NULLS_NAME = "string anno_null";
	public static final String PRIVATE_LONG_ANNOTATION_NAME = "long_anno";

	public static Annotations createDummyAnnotations(int i) {
		List<StringAnnotation> stringAnnos = new ArrayList<StringAnnotation>();
		StringAnnotation sa = new StringAnnotation();
		sa.setIsPrivate(false);
		sa.setKey(PUBLIC_STRING_ANNOTATION_NAME);
		sa.setValue("foo " + i);
		stringAnnos.add(sa);
		
		if (i % 4 != 3) { // two ways to have a null annot:  set it null (i%4==1) or omit altogether (i%4==3)
			StringAnnotation sa2 = new StringAnnotation();
			sa2.setIsPrivate(false);
			sa2.setKey(PUBLIC_STRING_ANNOTATION_WITH_NULLS_NAME);
			if (i % 2 == 1) { // odd numbered annotations are null
				sa2.setValue(null);
			} else {
				sa2.setValue("not null "+(100-i));
			}
			stringAnnos.add(sa2);
		}
		
		List<LongAnnotation> longAnnos = new ArrayList<LongAnnotation>();
		LongAnnotation la = new LongAnnotation();
		la.setIsPrivate(true);
		la.setKey(PRIVATE_LONG_ANNOTATION_NAME);
		la.setValue(new Long(i*10));
		longAnnos.add(la);
		
		List<DoubleAnnotation> doubleAnnos = new ArrayList<DoubleAnnotation>();
		DoubleAnnotation da = new DoubleAnnotation();
		da.setIsPrivate(false);
		da.setKey("double anno");
		da.setValue(0.5 + i);
		doubleAnnos.add(da);
		
		Annotations annos = new Annotations();
		annos.setStringAnnos(stringAnnos);
		annos.setLongAnnos(longAnnos);
		annos.setDoubleAnnos(doubleAnnos);
		annos.setObjectId("" + i);
		annos.setScopeId("" + 2*i);
		return annos;
	}
	
	public static ExternalStorageLocationSetting createExternalStorageLocation(Long createdBy, String description) {
		ExternalStorageLocationSetting setting = new ExternalStorageLocationSetting();
		setting.setDescription(description);
		setting.setUploadType(UploadType.SFTP);
		setting.setCreatedBy(createdBy);
		setting.setUrl("sftp://someurl.com");
		setting.setCreatedOn(new Date());
		return setting;
	}
	
	public static String loadFromClasspath(String fileName) throws IOException {
		try(InputStream in = TestUtils.class.getClassLoader().getResourceAsStream(fileName)) {
			if (in == null) {
				throw new IllegalArgumentException("Cannot find file " + fileName + " on classpath.");
			}
			return IOUtils.toString(in, StandardCharsets.UTF_8);
		}
	}
	
}
