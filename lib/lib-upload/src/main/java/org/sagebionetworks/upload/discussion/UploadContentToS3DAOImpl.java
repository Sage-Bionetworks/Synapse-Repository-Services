package org.sagebionetworks.upload.discussion;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class UploadContentToS3DAOImpl implements UploadContentToS3DAO {

	private static final String UTF_8 = "utf-8";
	private static final String S3_PREFIX = "https://s3.amazonaws.com/";
	private static final String KEY_FORMAT = "%1$s/%2$s/%3$s";

	@Autowired
	private AmazonS3Client s3Client;

	private String bucketName;

	/**
	 * injected
	 */
	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	/**
	 * Initialize is called when this bean is first created.
	 * 
	 */
	public void initialize() {
		s3Client.createBucket(bucketName);
	}

	@Override
	public String uploadDiscussionContent(String content, String forumId, String threadId) throws UnsupportedEncodingException{
		String key = generateKey(forumId, threadId);
		byte[] bytes = content.getBytes(UTF_8);
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		ObjectMetadata om = new ObjectMetadata();
		om.setContentType("plain/text");
		om.setContentDisposition("attachment; filename=" + key + ";");
		om.setContentLength(bytes.length);
		PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, in, om)
				.withCannedAcl(CannedAccessControlList.PublicRead);
		s3Client.putObject(putObjectRequest);
		return key;
	}

	private String generateKey(String forumId, String threadId) {
		return String.format(KEY_FORMAT, forumId, threadId, UUID.randomUUID().toString());
	}

	@Override
	public String getUrl(String key) {
		return S3_PREFIX + bucketName + "/" + key;
	}
}
