package org.sagebionetworks.audit.dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.sagebionetworks.audit.utils.AccessRecordUtils;
import org.sagebionetworks.audit.utils.KeyGeneratorUtil;
import org.sagebionetworks.audit.utils.ObjectCSVReader;
import org.sagebionetworks.audit.utils.SimpleRecordWriter;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * This implementation of the AccessRecordDAO uses S3 as the permanent
 * datastore.
 * 
 * @author John
 * 
 */
public class AccessRecordDAOImpl implements AccessRecordDAO {
	
	/**
	 * This is the schema. If it changes we will not be able to read old data.
	 */
	private final static String[] HEADERS = new String[]{"returnObjectId", "elapseMS","timestamp","via","host","threadId","userAgent","queryString","sessionId","xForwardedFor","requestURL","userId","origin", "date","method","vmId","instance","stack","success", "responseStatus"};

	@Autowired
	private AmazonS3Client s3Client;

	/**
	 * Injected via Spring
	 */
	private String auditRecordBucketName;
	/**
	 * Injected via Spring
	 */
	int stackInstanceNumber;
	String stackInstancePrefixString;
	private SimpleRecordWriter<AccessRecord> writer;

	/**
	 * Injected via Spring
	 * 
	 * @param auditRecordBucketName
	 */
	public void setAuditRecordBucketName(String auditRecordBucketName) {
		this.auditRecordBucketName = auditRecordBucketName;
	}

	/**
	 * Injected via Spring
	 * 
	 * @param stackInstanceNumber
	 */
	public void setStackInstanceNumber(int stackInstanceNumber) {
		this.stackInstanceNumber = stackInstanceNumber;
		this.stackInstancePrefixString = KeyGeneratorUtil.getInstancePrefix(stackInstanceNumber);
	}

	/**
	 * Initialize is called when this bean is first created.
	 * 
	 */
	public void initialize() {
		if (auditRecordBucketName == null)
			throw new IllegalArgumentException(
					"bucketName has not been set and cannot be null");
		// Create the bucket if it does not exist
		s3Client.createBucket(auditRecordBucketName);
		writer = new SimpleRecordWriter<AccessRecord>(s3Client, stackInstanceNumber, 
				auditRecordBucketName, AccessRecord.class, HEADERS);
	}
	
	@Override
	public String saveBatch(List<AccessRecord> batch, boolean rolling) throws IOException {
		// Save with the current timesamp
		return saveBatch(batch, System.currentTimeMillis(), rolling);
	}

	@Override
	public String saveBatch(List<AccessRecord> batch, long timestamp, boolean rolling) throws IOException {
		if(batch == null) throw new IllegalArgumentException("Batch cannot be null");
		// Order the batch by timestamp
		AccessRecordUtils.sortByTimestamp(batch);
		return writer.write(batch, timestamp, rolling);
	}

	@Override
	public List<AccessRecord> getBatch(String key) throws IOException {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		// Attempt to download the object
		S3Object object = s3Client.getObject(auditRecordBucketName, key);
		InputStream input = object.getObjectContent();
		try {
			// Read the data
			GZIPInputStream zipIn = new GZIPInputStream(input);
			InputStreamReader isr = new InputStreamReader(zipIn);
			ObjectCSVReader<AccessRecord> reader = new ObjectCSVReader<AccessRecord>(
					isr, AccessRecord.class, HEADERS);
			List<AccessRecord> results = new LinkedList<AccessRecord>();
			AccessRecord record = null;
			while ((record = reader.next()) != null) {
				results.add(record);
			}
			reader.close();
			return results;
		} finally {
			if (input != null) {
				input.close();
			}
		}
	}

	@Override
	public void deleteBactch(String key) {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		s3Client.deleteObject(auditRecordBucketName, key);
	}
	
	@Override
	public void deleteAllStackInstanceBatches() {
		// List all object with the prefix
		boolean done = false;
		while(!done){
			ObjectListing listing = s3Client.listObjects(auditRecordBucketName, this.stackInstancePrefixString);
			done = !listing.isTruncated();
			// Delete all
			if(listing.getObjectSummaries() != null){
				for(S3ObjectSummary summary: listing.getObjectSummaries()){
					s3Client.deleteObject(auditRecordBucketName, summary.getKey());
				}
			}
		}
	}
	
	@Override
	public ObjectListing listBatchKeys(String marker) {
		// List all of the objects in this bucket with the stack instance prefix string and the provided marker.
		return s3Client.listObjects(new ListObjectsRequest().withBucketName(this.auditRecordBucketName).withPrefix(this.stackInstancePrefixString).withMarker(marker));
	}

}
