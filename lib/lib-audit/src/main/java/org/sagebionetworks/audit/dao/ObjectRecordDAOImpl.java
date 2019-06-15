package org.sagebionetworks.audit.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.aws.utils.s3.BucketDaoImpl;
import org.sagebionetworks.aws.utils.s3.KeyData;
import org.sagebionetworks.aws.utils.s3.KeyGeneratorUtil;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.springframework.beans.factory.annotation.Autowired;

public class ObjectRecordDAOImpl implements ObjectRecordDAO {

	private final static String[] HEADERS = new String[] { "timestamp", "jsonClassName", "jsonString" };

	@Autowired
	private SynapseS3Client s3Client;

	/**
	 * Injected via Spring
	 */
	private int stackInstanceNumber;
	private String snapshotRecordBucketName;

	private GzipCsvS3ObjectReader<ObjectRecord> reader;
	private GzipCsvS3ObjectWriter<ObjectRecord> writer;

	private BucketDaoImpl bucketDao;

	/**
	 * Injected via Spring
	 */
	public void setSnapshotRecordBucketName(String snapshotRecordBucketName) {
		this.snapshotRecordBucketName = snapshotRecordBucketName;
	};
	/**
	 * Injected via Spring
	 */
	public void setStackInstanceNumber(int stackInstanceNumber) {
		this.stackInstanceNumber = stackInstanceNumber;
	}

	/**
	 * Initialize is called when this bean is first created.
	 * 
	 */
	public void initialize() {
		reader = new GzipCsvS3ObjectReader<ObjectRecord>(s3Client,
				ObjectRecord.class, HEADERS);
		writer = new GzipCsvS3ObjectWriter<ObjectRecord>(s3Client,
				ObjectRecord.class, HEADERS);
		bucketDao = new BucketDaoImpl(s3Client.getUSStandardAmazonClient(), snapshotRecordBucketName);
		s3Client.createBucket(snapshotRecordBucketName);
	}

	@Override
	public String saveBatch(List<ObjectRecord> batch, String type)
			throws IOException {
		String key = KeyGeneratorUtil.createNewKey(stackInstanceNumber, type,
				System.currentTimeMillis(), true);
		writer.write(batch, snapshotRecordBucketName, key);
		return key;
	}

	@Override
	public List<ObjectRecord> getBatch(String key, String type) throws IOException {
		KeyData keyData = KeyGeneratorUtil.parseKey(key);
		if (!keyData.getType().equals(type)) {
			return new ArrayList<ObjectRecord>();
		}
		return reader.read(snapshotRecordBucketName, key);
	}

	@Override
	public void deleteAllStackInstanceBatches(String type) {
		bucketDao.deleteAllObjectsWithPrefix(
				KeyGeneratorUtil.getInstanceAndTypePrefix(stackInstanceNumber, type));
	}

	@Override
	public Iterator<String> keyIterator(String type) {
		return bucketDao.keyIterator(
				KeyGeneratorUtil.getInstanceAndTypePrefix(stackInstanceNumber, type));
	}
}
