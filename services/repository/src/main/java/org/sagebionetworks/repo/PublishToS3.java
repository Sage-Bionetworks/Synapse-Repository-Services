package org.sagebionetworks.repo;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.aws.AwsClientFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketWebsiteConfiguration;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * Publish files from the local disk to S3
 * @author John
 *
 */
public class PublishToS3 {

	private static Logger log = LogManager.getLogger(PublishToS3.class);
	
	private static String POLICY_PREFIX = "{ \"Version\":\"2008-10-17\", \"Statement\":[{ \"Sid\":\"PublicReadForGetBucketObjects\", \"Effect\":\"Allow\", \"Principal\": { \"AWS\": \"*\" }, \"Action\":[\"s3:GetObject\"], \"Resource\":[\"arn:aws:s3:::";
	private static String POLICY_SUFFIX = "/*\" ] }]}";
	
	public static void main(String[] args){
		if(args == null) throw new IllegalArgumentException("Input args cannot be null");
		if(args.length != 1) throw new IllegalArgumentException("The first argument should be the source file path to copy from");
		File source = new File(args[0]);
		if(!source.isDirectory()) throw new IllegalArgumentException("Expected the source path to be a directory: "+source.getAbsolutePath());
		if(!source.exists()) throw new IllegalArgumentException("Source path does not exist: "+source.getAbsolutePath());
		// Create the bucket if needed
		String stack = StackConfigurationSingleton.singleton().getStack();
		String instance = StackConfigurationSingleton.singleton().getStackInstance();
		String bucketName = stack+"."+instance+".rest.doc.sagebase.org";
		
		// Create an S3 Connection
		AmazonS3 s3Client = AwsClientFactory.createAmazonS3Client();
		// Create the bucket if it does not exist
		Bucket bucket = s3Client.createBucket(bucketName);
		// Set the bucket to be a static website
		s3Client.setBucketWebsiteConfiguration(bucketName, new BucketWebsiteConfiguration("index.html"));
		// Make the bucket public
		String policy = POLICY_PREFIX+bucketName+POLICY_SUFFIX;
		s3Client.setBucketPolicy(bucketName, policy);
		
		log.info("Files will be copied to bucket: "+bucketName);
		// Now empty the bucket before we start
		emptyBucket(s3Client, bucketName);
		// For each file copy it to the bucket
		Iterator<File> toCopyIt = recursiveFileIterator(source);
		String sourcePath = source.getAbsolutePath();
		int count = 0;
		while(toCopyIt.hasNext()){
			File toCopy = toCopyIt.next();
			String fullPath = toCopy.getAbsolutePath();
			String key = fullPath.substring(sourcePath.length()+1, fullPath.length());
			key = key.replaceAll("\\\\", "/");
			// Upload it to s3
			PutObjectResult result = s3Client.putObject(bucketName, key, toCopy);
			log.info("Uploaded file: "+key);
			count++;
		}
		log.debug("Finished publishing "+count+" files to S3");
	}
	
	public static Iterator<File> recursiveFileIterator(File source){
		List<File> files = new LinkedList<File>();
		recursiveFileIterator(source, files);
		return files.iterator();
	}
	
	public static void recursiveFileIterator(File source, List<File> files ){
		if(source.isDirectory()){
			File[] children = source.listFiles();
			if(children != null){
				for(File child: children){
					recursiveFileIterator(child, files);
				}
			}
		}else{
			files.add(source);
		}
	}
	
	/**
	 * Delete all objects found in the given bucket
	 * 
	 * @param s3Client
	 * @param bucketName
	 */
	public static void emptyBucket(AmazonS3 s3Client, String bucketName){
		String token = null;
		do{
			ObjectListing ol = s3Client.listObjects(new ListObjectsRequest().withBucketName(bucketName).withMarker(token));
			token = ol.getNextMarker();
			List<S3ObjectSummary> sums = ol.getObjectSummaries();
			for(S3ObjectSummary sum: sums){
				log.info("Deleting: "+sum.getKey());
				s3Client.deleteObject(bucketName, sum.getKey());
			}
		}while(token != null);
	}
}
