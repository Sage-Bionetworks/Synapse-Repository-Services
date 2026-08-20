package org.sagebionetworks.logging.s3;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.util.ContentDispositionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Basic S3 implementation of the LogDAO.
 * @author John
 *
 */
public class LogDAOImpl implements LogDAO {
	
	Logger log = LogManager.getLogger(LogDAOImpl.class);
	
	@Autowired
	private SynapseS3Client s3Client;
	private int stackInstanceNumber;
	private String stackInstancePrefixString;
	private String bucketName;
	
	/**
	 * Injected via Spring
	 * @param instanceNumber
	 */
	public void setStackInstanceNumber(int stackInstanceNumber) {
		this.stackInstanceNumber = stackInstanceNumber;
		this.stackInstancePrefixString = LogKeyUtils.getInstancePrefix(stackInstanceNumber);
	}

	/**
	 * Injected via Spring
	 * @param bucketName
	 */
	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	@Override
	public String saveLogFile(File toSave, long timestamp) {
		// Create the key for the new log file.
		String key = LogKeyUtils.createKeyForFile(this.stackInstanceNumber,
				toSave.getName(), timestamp);
		s3Client.putObjectV2(PutObjectRequest.builder()
				.bucket(bucketName)
				.key(key)
				.contentType("application/x-gzip")
				.contentEncoding("gzip")
				.contentDisposition(ContentDispositionUtils.getContentDispositionValue(key))
				.build(), RequestBody.fromFile(toSave));
		return key;
	}

	@Override
	public void deleteLogFile(String key) {
		// Delete a log file by its key
		s3Client.deleteObjectV2(bucketName, key);
	}

	@Override
	public void deleteAllStackInstanceLogs() {
		// Each pass deletes the page it just listed, so re-listing from the start eventually drains the
		// prefix without having to page through keys that are already gone.
		boolean done = false;
		while(!done){
			ListObjectsV2Response listing = listAllStackInstanceLogs(null);
			done = !listing.isTruncated();
			for(S3Object summary: listing.contents()){
				log.debug("Deleting log from S3: "+summary.key());
				s3Client.deleteObjectV2(bucketName, summary.key());
			}
		}
	}

	@Override
	public LogReader getLogFileReader(String key) throws IOException {
		// First get this object
		ResponseInputStream<GetObjectResponse> s3Ob = s3Client
				.getObjectV2(GetObjectRequest.builder().bucket(this.bucketName).key(key).build());
		// Wrap the input in a gzip, then input stream read, a buffered reader and finally the log reader.
		return new LogReader(new BufferedReader(new InputStreamReader(new GZIPInputStream(s3Ob))));
	}

	@Override
	public ListObjectsV2Response listAllStackInstanceLogs(String continuationToken) {
		// List all of the objects in this bucket with the stack instance prefix string and the provided token.
		return s3Client.listObjectsV2(ListObjectsV2Request.builder().bucket(this.bucketName)
				.prefix(this.stackInstancePrefixString).continuationToken(continuationToken).build());
	}

	@Override
	public String findLogContainingUUID(String uuidTofind) throws InterruptedException, IOException {
		String continuationToken = null;
		do{
			ListObjectsV2Response listing = listAllStackInstanceLogs(continuationToken);
			continuationToken = listing.nextContinuationToken();
			// Try each file in this batch
			for(S3Object sum: listing.contents()){
				if(doesLogContaineUUID(sum.key(), uuidTofind)){
					return sum.key();
				}
				Thread.sleep(100);
			}
		}while(continuationToken != null);
		// If here we did not find a file that contained the UUID
		return null;
	}
	
	/**
	 * Read the log file to determine if it contains the passed UUID.
	 * 
	 * @param key
	 * @param uuid
	 * @return
	 * @throws IOException
	 */
	private boolean doesLogContaineUUID(String key, String uuid) throws IOException{
		LogReader reader = getLogFileReader(key);
		try{
			LogEntry entry = null;
			do{
				entry = reader.read();
				if(entry != null){
					if(entry.getEntryString().contains(uuid)) return true;
				}
			}while(entry != null);
			return false;
		}finally{
			reader.close();
		}

	}

	@Override
	public GetObjectResponse downloadLogFile(String key, File destination)
			throws IOException {
		return this.s3Client.getObjectV2(GetObjectRequest.builder().bucket(bucketName).key(key).build(),
				destination.toPath());
	}
}
