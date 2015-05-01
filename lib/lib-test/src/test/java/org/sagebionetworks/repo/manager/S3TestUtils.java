package org.sagebionetworks.repo.manager;

import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.sagebionetworks.util.Pair;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
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
