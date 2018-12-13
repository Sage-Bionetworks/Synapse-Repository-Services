package org.sagebionetworks.repo.manager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.RetryException;
import org.sagebionetworks.util.TimeUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.StringInputStream;
import com.google.common.collect.Lists;

public class S3TestUtils {
	private static final String UTF_8 = "UTF-8";
	
	static ThreadLocal<LinkedList<Pair<String, String>>> s3ObjectsToDelete = new ThreadLocal<LinkedList<Pair<String, String>>>();
	
	public static String createObjectFromString(String bucket, String key, String data, AmazonS3 s3Client) throws Exception {
		ObjectMetadata metadata = new ObjectMetadata();
		byte[] bytes = data.getBytes(UTF_8);
		metadata.setContentLength(bytes.length);
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		PutObjectResult putObject = s3Client.putObject(bucket, key, bis, metadata);
		addObjectToDelete(bucket, key);
		return putObject.getContentMd5();
	}
	
	public static String getObjectAsString(String bucket, String key, AmazonS3 s3Client) throws Exception {
		S3Object getObject = s3Client.getObject(bucket, key);
		S3ObjectInputStream sois = getObject.getObjectContent();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			IOUtils.copy(sois, baos);
			return new String(baos.toByteArray(), UTF_8);
		} finally {
			sois.close();
			baos.close();
		}
	}
	
	/*
	 * We use exponential retry to let the 'eventually complete' S3 service make the file available.
	 */
	public static boolean doesFileExist(final String bucket, final String key, final AmazonS3 s3Client, final long maxWaitTimeInMillis) {
		final long startTime = System.currentTimeMillis();
		boolean result = false;
		try {
			result = TimeUtils.waitForExponentialMaxRetry(10, 1000L, new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					boolean result = false;
					try {
						 result = (null != s3Client.getObjectMetadata(bucket, key));
					} catch (AmazonClientException e) {
						result = false;
					}
					if (!result) {
						if (System.currentTimeMillis() - startTime < maxWaitTimeInMillis) {
							throw new RetryException("file does not exist");
						}
					}
					return result;
				}
			});
		} catch (Exception e) {
			result =  false; // NOT FOUND
		}
		return result;
	}
	
	public static void deleteFile(String bucket, String key, AmazonS3 s3Client) {
		s3Client.deleteObject(bucket, key);
	}

	public static void addObjectToDelete(String bucket, String key){
		if (s3ObjectsToDelete.get() == null) {
			s3ObjectsToDelete.set(Lists.<Pair<String, String>> newLinkedList());
		}
		s3ObjectsToDelete.get().add(Pair.create(bucket, key));
	}
	
	public static void doDeleteAfter(AmazonS3 s3Client) {
		if(s3ObjectsToDelete.get()!=null){
			while(!s3ObjectsToDelete.get().isEmpty()){
				Pair<String, String> toDelete = s3ObjectsToDelete.get().removeLast();
				s3Client.deleteObject(toDelete.getFirst(), toDelete.getSecond());
			}
		}
	}
}
