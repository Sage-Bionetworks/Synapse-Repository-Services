package org.sagebionetworks.audit.dao;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.aws.utils.s3.KeyGeneratorUtil;
import org.sagebionetworks.aws.utils.s3.BucketDao;
import org.sagebionetworks.aws.utils.s3.BucketDaoImpl;
import org.sagebionetworks.aws.utils.s3.GzipCsvS3ObjectReader;
import org.sagebionetworks.aws.utils.s3.GzipCsvS3ObjectWriter;
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
	
	private GzipCsvS3ObjectReader<ObjectRecord> reader;
	private GzipCsvS3ObjectWriter<ObjectRecord> writer;
	private Map<String, BucketDao> bucketDaoMap;
	private Map<String, String> bucketNameMap;
	
	/**
	 * Injected via Spring
	 */
	public void setStackInstanceNumber(int stackInstanceNumber) {
		this.stackInstanceNumber = stackInstanceNumber;
	}
	/**
	 * Injected via Spring
	 */
	public void setObjectRecordBucketFormat(String objectRecordBucketFormat) {
		this.objectRecordBucketFormat = objectRecordBucketFormat;
	}
	/**
	 * Initialize is called when this bean is first created.
	 * 
	 */
	public void initialize() {
		if (objectRecordBucketFormat == null)
			throw new IllegalArgumentException(
					"bucket name format has not been set and cannot be null");
		reader = new GzipCsvS3ObjectReader<ObjectRecord>(s3Client, ObjectRecord.class, HEADERS);
		writer = new GzipCsvS3ObjectWriter<ObjectRecord>(s3Client, ObjectRecord.class, HEADERS);
		bucketDaoMap = new HashMap<String, BucketDao>();
		bucketNameMap = new HashMap<String, String>();
	}
	
	@Override
	public String saveBatch(List<ObjectRecord> batch, String type) throws IOException {
		typeCheck(type);
		String key = KeyGeneratorUtil.createNewKey(stackInstanceNumber, System.currentTimeMillis(), true);
		writer.write(batch, bucketNameMap.get(type), key);
		return key;
	}

	private void typeCheck(String type) {
		if (!bucketDaoMap.containsKey(type)) {
			String bucketName = String.format(objectRecordBucketFormat, type);
			// Create the bucket if it does not exist
			s3Client.createBucket(bucketName);
			bucketNameMap.put(type, bucketName);
			BucketDao bucketDao = new BucketDaoImpl(s3Client, bucketName);
			bucketDaoMap.put(type, bucketDao);
		}
	}

	@Override
	public List<ObjectRecord> getBatch(String key, String type) throws IOException {
		typeCheck(type);
		return reader.read(bucketNameMap.get(type), key);
	}
	
	@Override
	public void deleteAllStackInstanceBatches(String type) {
		typeCheck(type);
		bucketDaoMap.get(type).deleteAllObjectsWithPrefix(KeyGeneratorUtil.getInstancePrefix(stackInstanceNumber));
	}
	
	@Override
	public Iterator<String> keyIterator(String type) {
		return bucketDaoMap.get(type).keyIterator(KeyGeneratorUtil.getInstancePrefix(stackInstanceNumber));
	}
}
