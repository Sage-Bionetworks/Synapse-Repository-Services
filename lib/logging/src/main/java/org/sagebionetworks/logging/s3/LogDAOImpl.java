package org.sagebionetworks.logging.s3;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * Basic S3 implementation of the LogDAO.
 * @author John
 *
 */
public class LogDAOImpl implements LogDAO {
	
	Logger log = LogManager.getLogger(LogDAOImpl.class);
	
	@Autowired
	private AmazonS3 s3Client;
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
		ObjectMetadata om = new ObjectMetadata();
		om.setContentType("application/x-gzip");
		om.setContentEncoding("gzip");
		om.setContentDisposition("attachment; filename=" + key + ";");
		s3Client.putObject(new PutObjectRequest(bucketName, key, toSave)
				.withMetadata(om));
		return key;
	}

	@Override
	public void deleteLogFile(String key) {
		// Delete a log file by its key
		s3Client.deleteObject(bucketName, key);
	}

	/**
	 * Called when the bean is initialized.
	 */
	public void initialize() {
		if (bucketName == null)
			throw new IllegalArgumentException("bucketName has not been set and cannot be null");
		// Create the bucket if it does not exist
		s3Client.createBucket(bucketName);
	}
	
	@Override
	public void deleteAllStackInstanceLogs() {
		// List all object with the prefix
		boolean done = false;
		while(!done){
			ObjectListing listing = s3Client.listObjects(bucketName, this.stackInstancePrefixString);
			done = !listing.isTruncated();
			// Delete all
			if(listing.getObjectSummaries() != null){
				for(S3ObjectSummary summary: listing.getObjectSummaries()){
					log.debug("Deleting log from S3: "+summary.getKey());
					s3Client.deleteObject(bucketName, summary.getKey());
				}
			}
		}
	}

	@Override
	public LogReader getLogFileReader(String key) throws IOException {
		// First get this object
		S3Object s3Ob = s3Client.getObject(this.bucketName, key);
		// Wrap the input in a gzip, then input stream read, a buffered reader and finally the log reader.
		return new LogReader(new BufferedReader(new InputStreamReader(new GZIPInputStream(s3Ob.getObjectContent()))));
	}

	@Override
	public ObjectListing listAllStackInstanceLogs(String marker) {
		// List all of the objects in this bucket with the stack instance prefix string and the provided marker.
		return s3Client.listObjects(new ListObjectsRequest().withBucketName(this.bucketName).withPrefix(this.stackInstancePrefixString).withMarker(marker));
		
	}

	@Override
	public String findLogContainingUUID(String uuidTofind) throws InterruptedException, IOException {
		String marker = null;
		do{
			ObjectListing listing = listAllStackInstanceLogs(marker);
			marker = listing.getNextMarker();
			// Try each file in this batch
			for(S3ObjectSummary sum: listing.getObjectSummaries()){
				if(doesLogContaineUUID(sum.getKey(), uuidTofind)){
					return sum.getKey();
				}
				Thread.sleep(100);
			}
		}while(marker != null);
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
	public ObjectMetadata downloadLogFile(String key, File destination)
			throws IOException {
		return this.s3Client.getObject(new GetObjectRequest(bucketName, key), destination);
	}
}
