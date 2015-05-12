package org.sagebionetworks.repo.manager;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;

import org.apache.commons.io.IOUtils;
import org.sagebionetworks.util.Pair;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.StringInputStream;
import com.google.common.collect.Lists;

public class S3TestUtils {
	static ThreadLocal<LinkedList<Pair<String, String>>> s3ObjectsToDelete = new ThreadLocal<LinkedList<Pair<String, String>>>();
	
	public static String createObjectFromString(String bucket, String key, String data, AmazonS3Client s3Client) throws Exception {
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(data.length());
		PutObjectResult putObject = s3Client.putObject(bucket, key, new StringInputStream(data), metadata);
		addObjectToDelete(bucket, key);
		return putObject.getContentMd5();
	}
	
	public static String getObjectAsString(String bucket, String key, AmazonS3Client s3Client) throws Exception {
		S3Object getObject = s3Client.getObject(bucket, key);
		S3ObjectInputStream sois = getObject.getObjectContent();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			IOUtils.copy(sois, baos);
			return baos.toString();
		} finally {
			sois.close();
			baos.close();
		}
	}
	
	public static boolean doesFileExist(String bucket, String key, AmazonS3Client s3Client) {
		try {
			return null != s3Client.getObjectMetadata(bucket, key);
		} catch (AmazonS3Exception e) {
			return false; // NOT FOUND
		}
	}
	
	public static void deleteFile(String bucket, String key, AmazonS3Client s3Client) {
		s3Client.deleteObject(bucket, key);
	}

	public static void addObjectToDelete(String bucket, String key){
		if (s3ObjectsToDelete.get() == null) {
			s3ObjectsToDelete.set(Lists.<Pair<String, String>> newLinkedList());
		}
		s3ObjectsToDelete.get().add(Pair.create(bucket, key));
	}
	
	public static void doDeleteAfter(AmazonS3Client s3Client) {
		if(s3ObjectsToDelete.get()!=null){
			while(!s3ObjectsToDelete.get().isEmpty()){
				Pair<String, String> toDelete = s3ObjectsToDelete.get().removeLast();
				s3Client.deleteObject(toDelete.getFirst(), toDelete.getSecond());
			}
		}
	}
}
