package org.sagebionetworks.repo.manager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.entity.ContentType;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.DatastoreException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3;
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

	private static final String S3_BUCKET = StackConfigurationSingleton.singleton().getS3Bucket();
	
	@Autowired
	AmazonS3 client;


	@Override
	public File downloadFromS3(String key) throws DatastoreException {
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
		S3Object s3Object = client.getObject(S3_BUCKET, key);
		ObjectMetadata metadata = s3Object.getObjectMetadata();
		String contentTypeString = metadata.getContentType();
		ContentType contentType = ContentType.parse(contentTypeString);
		Charset contentTypeCharSet = contentType.getCharset();
		if (contentTypeCharSet==null) contentTypeCharSet = Charset.defaultCharset();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		S3ObjectInputStream is = s3Object.getObjectContent();
		try {
			int n = 0;
			byte[] buffer = new byte[1024];
			while (n>-1) {
				n = is.read(buffer);
				if (n>0) baos.write(buffer, 0, n);
			}
			return baos.toString(contentTypeCharSet.name());
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
		PutObjectResult results = client.putObject(S3_BUCKET, key, toUpload);
		log.info(results);
		return results.getETag() != null;
	}

	@Override
	public boolean doesExist(String key) {
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
		try{
			log.info("Deleting: "+key+" from "+S3_BUCKET);
			client.deleteObject(S3_BUCKET, key);
			return true;
		}catch(Exception e){
			return false;
		}
	}
}
