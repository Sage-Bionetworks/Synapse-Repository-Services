package org.sagebionetworks.audit.dao;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.audit.utils.ObjectCSVDAO;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;

public class ObjectRecordDAOImpl implements ObjectRecordDAO {

	private final static String[] HEADERS = new String[]{"timestamp", "changeNumber", "objectType", "jsonString"};

	@Autowired
	private AmazonS3Client s3Client;
	/**
	 * Injected via Spring
	 */
	private int stackInstanceNumber;
	/**
	 * Injected via Spring
	 */
	private String objectRecordBucketFormat;
	
	/**
	 * Injected via Spring
	 */
	public void setStackInstanceNumber(int stackInstanceNumber) {
		this.stackInstanceNumber = stackInstanceNumber;
	}
	/**
	 * Injected via Spring
	 */
	public void setObjectRecordBucketName(String objectRecordBucketFormat) {
		this.objectRecordBucketFormat = objectRecordBucketFormat;
	}
	/**
	 * Initialize is called when this bean is first created.
	 * 
	 */
	public void initialize() {
		if (objectRecordBucketFormat == null)
			throw new IllegalArgumentException(
					"bucketName has not been set and cannot be null");
		// Create the bucket if it does not exist
		// s3Client.createBucket(objectRecordBucketFormat);
	}
	
	@Override
	public String saveBatch(List<ObjectRecord> records) throws IOException {
		return null;
	}

	@Override
	public List<ObjectRecord> getBatch(String key) throws IOException {
		return null;
	}
	@Override
	public void deleteAllStackInstanceBatches() {
	}
	@Override
	public Set<String> listAllKeys() {
		return null;
	}
	@Override
	public void deleteBactch(String key) {
	}
}
