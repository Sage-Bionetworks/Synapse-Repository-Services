package org.sagebionetworks.audit.dao;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.sagebionetworks.audit.utils.AccessRecordUtils;
import org.sagebionetworks.audit.utils.KeyGeneratorUtil;
import org.sagebionetworks.audit.utils.ObjectCSVReader;
import org.sagebionetworks.audit.utils.ObjectCSVWriter;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.sagebionetworks.repo.model.audit.BatchListing;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
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
	}

	@Override
	public String saveBatch(List<AccessRecord> batch) throws IOException {
		if(batch == null) throw new IllegalArgumentException("Batch cannot be null");
		// Order the batch by timestamp
		AccessRecordUtils.sortByTimestamp(batch);
		// Write the data to a gzip
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPOutputStream zipOut = new GZIPOutputStream(out);
		OutputStreamWriter osw = new OutputStreamWriter(zipOut);
		ObjectCSVWriter<AccessRecord> writer = new ObjectCSVWriter<AccessRecord>(
				osw, AccessRecord.class);
		// Write all of the data
		for (AccessRecord ar : batch) {
			writer.append(ar);
		}
		writer.close();
		// Create an input stream
		byte[] bytes = out.toByteArray();
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		// Build a new key
		String key = KeyGeneratorUtil.createNewKey(stackInstanceNumber,
				System.currentTimeMillis());
		ObjectMetadata om = new ObjectMetadata();
		om.setContentType("application/x-gzip");
		om.setContentEncoding("gzip");
		om.setContentDisposition("attachment; filename=" + key + ";");
		om.setContentLength(bytes.length);
		s3Client.putObject(auditRecordBucketName, key, in, om);
		return key;

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
					isr, AccessRecord.class);
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
		String prefix = KeyGeneratorUtil.getInstancePrefix(stackInstanceNumber);
		boolean done = false;
		while(!done){
			ObjectListing listing = s3Client.listObjects(auditRecordBucketName, prefix);
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
	public BatchListing listBatchKeys(String marker) {
		// TODO Auto-generated method stub
		return null;
	}




}
