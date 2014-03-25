package org.sagebionetworks.repo.manager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.entity.ContentType;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.DatastoreException;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

/**
 * A simple utility for uploading and downloading from S3.
 * 
 * @author jmhill
 *
 */
public class AmazonS3UtilityImpl implements AmazonS3Utility{
	
	static private Log log = LogFactory.getLog(AmazonS3UtilityImpl.class);

	private static final String S3_BUCKET = StackConfiguration.getS3Bucket();
	
	/**
	 * Create a new AWS client using the configuration credentials.
	 * @return
	 */
	private AmazonS3Client createNewAWSClient() {
		String iamId = StackConfiguration.getIAMUserId();
		String iamKey = StackConfiguration.getIAMUserKey();
		if (iamId == null) throw new IllegalArgumentException("IAM id cannot be null");
		if (iamKey == null)	throw new IllegalArgumentException("IAM key cannot be null");
		AWSCredentials creds = new BasicAWSCredentials(iamId, iamKey);
		AmazonS3Client client = new AmazonS3Client(creds);
		return client;
	}

	@Override
	public File downloadFromS3(String key) throws DatastoreException {
		AmazonS3Client client = createNewAWSClient();
		log.info("Attempting to download: "+key+" from "+S3_BUCKET);
		GetObjectRequest getObjectRequest = new GetObjectRequest(S3_BUCKET, key);
		File temp;
		try {
			temp = File.createTempFile("AmazonS3Utility", ".tmp");
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
		client.getObject(getObjectRequest, temp);
		return temp;
	}
	
	@Override
	public void uploadStringToS3File(String key, String content, String charSet) {
		if (charSet==null) charSet="utf-8";
		byte[] buffer;
		try {
			buffer = content.getBytes(charSet);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException("charSet="+charSet, e);
		}
		InputStream is = new ByteArrayInputStream(buffer);
		uploadInputStreamToS3File(key, is, charSet);
	}

	@Override
	public void uploadInputStreamToS3File(String key, InputStream is, String charSet) {
		if (charSet==null) throw new IllegalArgumentException("charSet required.");
		AmazonS3Client client = createNewAWSClient();
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentType("text/plain);charset="+charSet);
		try {
			client.putObject(S3_BUCKET, key, is, metadata);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	
	@Override
	public String downloadFromS3ToString(String key) {
		AmazonS3Client client = createNewAWSClient();
		S3Object s3Object = client.getObject(S3_BUCKET, key);
		ObjectMetadata metadata = s3Object.getObjectMetadata();
		String contentTypeString = metadata.getContentType();
		ContentType contentType = ContentType.parse(contentTypeString);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		S3ObjectInputStream is = s3Object.getObjectContent();
		try {
			int n = 0;
			byte[] buffer = new byte[1024];
			while (n>-1) {
				n = is.read(buffer);
				if (n>0) baos.write(buffer, 0, n);
			}
			return baos.toString(contentType.getCharset().name());
		} catch (IOException e) {
			throw new RuntimeException("contentType="+contentType, e);
		} finally {
			try {
				is.close();
				baos.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public boolean uploadToS3(File toUpload, String key) {
		log.info("Attempting to upload: "+key+" to "+S3_BUCKET);
		AmazonS3Client client = createNewAWSClient();
		PutObjectResult results = client.putObject(S3_BUCKET, key, toUpload);
		log.info(results);
		return results.getETag() != null;
	}

	@Override
	public boolean doesExist(String key) {
		AmazonS3Client client = createNewAWSClient();
		try{
			ObjectMetadata metadata = client.getObjectMetadata(S3_BUCKET, key);
			if(metadata == null) return false;
			return metadata.getETag() != null;
		}catch (Exception e){
			return false;
		}
	}

	@Override
	public boolean deleteFromS3(String key) {
		AmazonS3Client client = createNewAWSClient();
		try{
			log.info("Deleting: "+key+" from "+S3_BUCKET);
			client.deleteObject(S3_BUCKET, key);
			return true;
		}catch(Exception e){
			return false;
		}
	}
}
