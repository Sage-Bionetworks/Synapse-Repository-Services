package org.sagebionetworks.repo.manager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.sagebionetworks.aws.SynapseAWSCredentialsProviderChain;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.RetryException;
import org.sagebionetworks.util.TimeUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.HeadBucketRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.google.common.collect.Lists;

public class S3TestUtils {
	private static final String UTF_8 = "UTF-8";

	static ThreadLocal<LinkedList<Pair<String, String>>> s3ObjectsToDelete = new ThreadLocal<LinkedList<Pair<String, String>>>();

	/**
	 * Creates the given bucket if it does not already exist. Used by tests that exercise external
	 * S3 storage locations, where the bucket must be present before a storage location can be
	 * linked to it.
	 *
	 * @param bucket The bucket name
	 * @throws AmazonS3Exception If the bucket exists but cannot be reached (e.g. access denied), or
	 *                          if the bucket does not exist and cannot be created
	 */
	public static void createBucketIfMissing(String bucket) {
		// Bucket existence is probed with a raw client rather than through SynapseS3Client so that a
		// 404 (create the bucket) stays distinguishable from any other failure (fail the test).
		AmazonS3 rawClient = RawUsStandardClient.INSTANCE;
		try {
			rawClient.headBucket(new HeadBucketRequest(bucket));
		} catch (AmazonS3Exception e) {
			if (e.getStatusCode() != 404) {
				throw e;
			}
			rawClient.createBucket(bucket);
		}
	}

	/**
	 * Holds the single raw client shared by all {@link #createBucketIfMissing(String)} calls. The
	 * client owns a connection pool and an idle-connection reaper thread, so one is built lazily for
	 * the whole JVM rather than per call. Configured like the clients {@code AwsClientFactory} builds
	 * for the facade, since this replaces a facade call.
	 */
	private static class RawUsStandardClient {
		private static final AmazonS3 INSTANCE = AmazonS3ClientBuilder.standard()
				.withCredentials(SynapseAWSCredentialsProviderChain.getInstance())
				.withRegion(Regions.US_EAST_1)
				.withPathStyleAccessEnabled(true)
				.withForceGlobalBucketAccessEnabled(true)
				.build();
	}

	public static String createObjectFromString(String bucket, String key, String data, SynapseS3Client s3Client) throws Exception {
		ObjectMetadata metadata = new ObjectMetadata();
		byte[] bytes = data.getBytes(UTF_8);
		metadata.setContentLength(bytes.length);
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		PutObjectResult putObject = s3Client.putObject(bucket, key, bis, metadata);
		addObjectToDelete(bucket, key);
		return putObject.getContentMd5();
	}
	
	public static String getObjectAsString(String bucket, String key, SynapseS3Client s3Client) throws Exception {
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
	public static boolean doesFileExist(final String bucket, final String key, final SynapseS3Client s3Client, final long maxWaitTimeInMillis) {
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
	
	public static void deleteFile(String bucket, String key, SynapseS3Client s3Client) {
		s3Client.deleteObject(bucket, key);
	}

	public static void addObjectToDelete(String bucket, String key){
		if (s3ObjectsToDelete.get() == null) {
			s3ObjectsToDelete.set(Lists.<Pair<String, String>> newLinkedList());
		}
		s3ObjectsToDelete.get().add(Pair.create(bucket, key));
	}
	
	public static void doDeleteAfter(SynapseS3Client s3Client) {
		if(s3ObjectsToDelete.get()!=null){
			while(!s3ObjectsToDelete.get().isEmpty()){
				Pair<String, String> toDelete = s3ObjectsToDelete.get().removeLast();
				s3Client.deleteObject(toDelete.getFirst(), toDelete.getSecond());
			}
		}
	}
}
