package org.sagebionetworks.upload.discussion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.sagebionetworks.repo.model.discussion.MessageURL;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;
import com.amazonaws.services.s3.model.CORSRule;
import com.amazonaws.services.s3.model.CORSRule.AllowedMethods;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

public class UploadContentToS3DAOImpl implements UploadContentToS3DAO {

	private static final String TEXT_PLAIN_CHARSET_UTF_8 = "text/plain; charset=utf-8";
	private static final int PRE_SIGNED_URL_EXPIRATION_MS = 60*1000;


	@Autowired
	private AmazonS3 s3Client;

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
		byte[] compressedBytes = compress(content);
		ByteArrayInputStream in = new ByteArrayInputStream(compressedBytes);
		ObjectMetadata om = new ObjectMetadata();
		om.setContentType(TEXT_PLAIN_CHARSET_UTF_8);
		om.setContentDisposition("attachment; filename=" + key + ";");
		om.setContentEncoding("gzip");
		om.setContentLength(compressedBytes.length);
		PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, in, om)
				.withCannedAcl(CannedAccessControlList.PublicRead);
		s3Client.putObject(putObjectRequest);
	}

	public static byte[] compress(String content) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(content.length());
		GZIPOutputStream gzip = new GZIPOutputStream(out);
		gzip.write(content.getBytes());
		gzip.close();
		byte[] compressedBytes = out.toByteArray();
		return compressedBytes;
	}

	@Override
	public MessageURL getThreadUrl(String key) {
		MessageKeyUtils.validateThreadKey(key);
		MessageURL url = new MessageURL();
		GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
				bucketName, key).withMethod(HttpMethod.GET).withExpiration(
				new Date(System.currentTimeMillis()
				+ PRE_SIGNED_URL_EXPIRATION_MS)).withContentType(TEXT_PLAIN_CHARSET_UTF_8);
		url.setMessageUrl(s3Client.generatePresignedUrl(request).toString());
		return url;
	}

	@Override
	public MessageURL getReplyUrl(String key) {
		MessageKeyUtils.validateReplyKey(key);
		MessageURL url = new MessageURL();
		GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
				bucketName, key).withMethod(HttpMethod.GET).withExpiration(
				new Date(System.currentTimeMillis()
				+ PRE_SIGNED_URL_EXPIRATION_MS)).withContentType(TEXT_PLAIN_CHARSET_UTF_8);
		url.setMessageUrl(s3Client.generatePresignedUrl(request).toString());
		return url;
	}

	@Override
	public String getMessage(String key) {
		InputStream input = null;
		try {
			S3Object object = s3Client.getObject(bucketName, key);
			input = object.getObjectContent();
			GZIPInputStream zipIn = new GZIPInputStream(input);
			return IOUtils.toString(zipIn, "UTF-8");
		} catch(IOException e) {
			throw new RuntimeException("Failed to retrieve message for key "+key);
		} finally {
			IOUtils.closeQuietly(input);
		}
	}
}
