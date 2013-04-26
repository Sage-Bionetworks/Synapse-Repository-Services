package org.sagebionetworks.repo.manager.migration;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;

import com.amazonaws.util.BinaryUtils;

public class TestUtils {

	/**
	 * Helper to create a S3FileHandle
	 * 
	 * @return
	 */
	public static S3FileHandle createS3FileHandle(String createdById) {
		S3FileHandle meta = new S3FileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy(createdById);
		meta.setFileName("foobar.txt");
		return meta;
	}

	/**
	 * Helper to create a PreviewFileHandle
	 * @return
	 */
	public static PreviewFileHandle createPreviewFileHandle(String createdById) {
		PreviewFileHandle meta = new PreviewFileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy(createdById);
		meta.setFileName("preview.jpg");
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
}
