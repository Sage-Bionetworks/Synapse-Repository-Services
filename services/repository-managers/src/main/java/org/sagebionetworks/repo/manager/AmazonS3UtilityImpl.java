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
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.model.DatastoreException;
import org.springframework.beans.factory.annotation.Autowired;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

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
	SynapseS3Client client;


	@Override
	public File downloadFromS3(String key) throws DatastoreException {
		log.info("Attempting to download: "+key+" from "+S3_BUCKET);
		File temp;
		try {
			temp = File.createTempFile("AmazonS3Utility", ".tmp");
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
		client.getObjectV2(GetObjectRequest.builder().bucket(S3_BUCKET).key(key).build(), temp.toPath());
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
		try {
			// S3 needs the content length upfront, so the stream is read into memory. Callers only pass
			// small documents such as the certified-user questionnaire.
			byte[] content = is.readAllBytes();
			PutObjectRequest request = PutObjectRequest.builder()
					.bucket(S3_BUCKET)
					.key(key)
					.contentType("text/plain);charset="+charSet)
					.build();
			client.putObjectV2(request, RequestBody.fromBytes(content));
		} catch (IOException e) {
			throw new RuntimeException(e);
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
		GetObjectRequest request = GetObjectRequest.builder().bucket(S3_BUCKET).key(key).build();
		try (ResponseInputStream<GetObjectResponse> is = client.getObjectV2(request)) {
			ContentType contentType = ContentType.parse(is.response().contentType());
			Charset contentTypeCharSet = contentType.getCharset();
			if (contentTypeCharSet==null) contentTypeCharSet = Charset.defaultCharset();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
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
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean uploadToS3(File toUpload, String key) {
		log.info("Attempting to upload: "+key+" to "+S3_BUCKET);
		PutObjectResponse results = client.putObjectV2(
				PutObjectRequest.builder().bucket(S3_BUCKET).key(key).build(), RequestBody.fromFile(toUpload));
		log.info(results);
		return results.eTag() != null;
	}

	@Override
	public boolean doesExist(String key) {
		try{
			HeadObjectResponse metadata = client.getObjectMetadataV2(S3_BUCKET, key);
			if(metadata == null) return false;
			return metadata.eTag() != null;
		}catch (Exception e){
			return false;
		}
	}

	@Override
	public boolean deleteFromS3(String key) {
		try{
			log.info("Deleting: "+key+" from "+S3_BUCKET);
			client.deleteObjectV2(S3_BUCKET, key);
			return true;
		}catch(Exception e){
			return false;
		}
	}
}
