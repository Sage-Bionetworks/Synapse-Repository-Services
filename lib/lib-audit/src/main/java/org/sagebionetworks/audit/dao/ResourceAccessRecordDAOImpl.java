package org.sagebionetworks.audit.dao;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.audit.utils.SimpleRecordWorker;
import org.sagebionetworks.repo.model.audit.ResourceAccessRecord;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;

public class ResourceAccessRecordDAOImpl implements ResourceAccessRecordDAO {

	private final static String[] HEADERS = new String[]{"principalId", "changeNumber", "accessType"};

	@Autowired
	private AmazonS3Client s3Client;
	/**
	 * Injected via Spring
	 */
	private int stackInstanceNumber;
	/**
	 * Injected via Spring
	 */
	private String resourceAccessRecordBucketName;
	private SimpleRecordWorker<ResourceAccessRecord> worker;

	/**
	 * Injected via Spring
	 */
	public void setStackInstanceNumber(int stackInstanceNumber) {
		this.stackInstanceNumber = stackInstanceNumber;
	}
	/**
	 * Injected via Spring
	 */
	public void setResourceAccessRecordBucketName(String resourceAccessRecordBucketName) {
		this.resourceAccessRecordBucketName = resourceAccessRecordBucketName;
	}
	/**
	 * Initialize is called when this bean is first created.
	 * 
	 */
	public void initialize() {
		if (resourceAccessRecordBucketName == null)
			throw new IllegalArgumentException(
					"bucketName has not been set and cannot be null");
		// Create the bucket if it does not exist
		s3Client.createBucket(resourceAccessRecordBucketName);
		worker = new SimpleRecordWorker<ResourceAccessRecord>(s3Client, stackInstanceNumber, 
				resourceAccessRecordBucketName, ResourceAccessRecord.class, HEADERS);
	}

	@Override
	public String saveBatch(List<ResourceAccessRecord> records) throws IOException {
		return worker.write(records, System.currentTimeMillis(), false);
	}

	@Override
	public List<ResourceAccessRecord> getBatch(String key) throws IOException {
		return worker.read(key);
	}
	@Override
	public void deleteAllStackInstanceBatches() {
		worker.deleteAllStackInstanceBatches();
	}
}
