package org.sagebionetworks.audit.dao;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.audit.utils.ObjectCSVDAO;
import org.sagebionetworks.repo.model.audit.AclRecord;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;

public class AclRecordDAOImpl implements AclRecordDAO {

	private final static String[] HEADERS = new String[]{"timestamp", "creationDate", "changeType", "etag", "ownerId", "ownerType", "changeNumber", "aclId"};

	@Autowired
	private AmazonS3Client s3Client;
	/**
	 * Injected via Spring
	 */
	private int stackInstanceNumber;
	/**
	 * Injected via Spring
	 */
	private String aclRecordBucketName;
	private ObjectCSVDAO<AclRecord> objectCsvDao;

	/**
	 * Injected via Spring
	 */
	public void setStackInstanceNumber(int stackInstanceNumber) {
		this.stackInstanceNumber = stackInstanceNumber;
	}
	/**
	 * Injected via Spring
	 */
	public void setAclRecordBucketName(String aclRecordBucketName) {
		this.aclRecordBucketName = aclRecordBucketName;
	}
	/**
	 * Initialize is called when this bean is first created.
	 * 
	 */
	public void initialize() {
		if (aclRecordBucketName == null)
			throw new IllegalArgumentException(
					"bucketName has not been set and cannot be null");
		// Create the bucket if it does not exist
		s3Client.createBucket(aclRecordBucketName);
		objectCsvDao = new ObjectCSVDAO<AclRecord>(s3Client, stackInstanceNumber, 
				aclRecordBucketName, AclRecord.class, HEADERS);
	}
	
	@Override
	public String saveBatch(List<AclRecord> records) throws IOException {
		return objectCsvDao.write(records, System.currentTimeMillis(), false);
	}

	@Override
	public List<AclRecord> getBatch(String key) throws IOException {
		return objectCsvDao.read(key);
	}
	@Override
	public void deleteAllStackInstanceBatches() {
		objectCsvDao.deleteAllStackInstanceBatches();
	}
	@Override
	public Set<String> listAllKeys() {
		return objectCsvDao.listAllKeys();
	}
	@Override
	public void deleteBactch(String key) {
		objectCsvDao.delete(key);
	}
}
