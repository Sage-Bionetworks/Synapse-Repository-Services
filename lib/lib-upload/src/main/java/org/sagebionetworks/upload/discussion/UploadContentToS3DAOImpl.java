package org.sagebionetworks.upload.discussion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;
import com.amazonaws.services.s3.model.CORSRule;
import com.amazonaws.services.s3.model.CORSRule.AllowedMethods;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class UploadContentToS3DAOImpl implements UploadContentToS3DAO {

	private static final String S3_PREFIX = "https://s3.amazonaws.com/";


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
		CORSRule cors = new CORSRule();
		cors.setAllowedHeaders("Authorization");
		cors.setAllowedMethods(AllowedMethods.GET);
		cors.setAllowedOrigins("*");
		BucketCrossOriginConfiguration bucketConfig = new BucketCrossOriginConfiguration(Arrays.asList(cors ));
		s3Client.setBucketCrossOriginConfiguration(bucketName, bucketConfig);
	}

	@Override
	public String uploadThreadMessage(String content, String forumId, String threadId) throws IOException{
		String key = MessageKeyUtils.generateThreadKey(forumId, threadId);
		doUpload(content, key);
		return key;
	}
	
	@Override
	public String uploadReplyMessage(String content, String forumId, String threadId, String replyId) throws IOException{
		String key = MessageKeyUtils.generateReplyKey(forumId, threadId, replyId);
		doUpload(content, key);
		return key;
	}

	private void doUpload(String content, String key) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(content.length());
		GZIPOutputStream gzip = new GZIPOutputStream(out);
		gzip.write(content.getBytes());
		gzip.close();
		byte[] compressedBytes = out.toByteArray();
		ByteArrayInputStream in = new ByteArrayInputStream(compressedBytes);
		ObjectMetadata om = new ObjectMetadata();
		om.setContentType("plain/text");
		om.setContentDisposition("attachment; filename=" + key + ";");
		om.setContentEncoding("gzip");
		om.setContentLength(compressedBytes.length);
		PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, in, om)
				.withCannedAcl(CannedAccessControlList.PublicRead);
		s3Client.putObject(putObjectRequest);
	}

	@Override
	public String getThreadUrl(String key) {
		MessageKeyUtils.validateThreadKey(key);
		return S3_PREFIX + bucketName + "/" + key;
	}

	@Override
	public String getReplyUrl(String key) {
		MessageKeyUtils.validateReplyKey(key);
		return S3_PREFIX + bucketName + "/" + key;
	}
}
