package org.sagebionetworks.audit.dao;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.sagebionetworks.audit.utils.BucketDaoProvider;
import org.sagebionetworks.aws.utils.s3.GzipCsvS3ObjectReader;
import org.sagebionetworks.aws.utils.s3.GzipCsvS3ObjectWriter;
import org.sagebionetworks.aws.utils.s3.KeyGeneratorUtil;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;

public class ObjectRecordDAOImpl implements ObjectRecordDAO {

	private final static String[] HEADERS = new String[] { "timestamp",
			"changeNumber", "jsonClassName", "changeMessageObjectType", "jsonString" };

	@Autowired
	private AmazonS3Client s3Client;

	/**
	 * Injected via Spring
	 */
	private int stackInstanceNumber;
	private String stack;

	private GzipCsvS3ObjectReader<ObjectRecord> reader;
	private GzipCsvS3ObjectWriter<ObjectRecord> writer;
	private BucketDaoProvider provider;

	/**
	 * Injected via Spring
	 */
	public void setStackInstanceNumber(int stackInstanceNumber) {
		this.stackInstanceNumber = stackInstanceNumber;
	}
	/**
	 * Injected via Spring
	 */
	public void setStack(String stack) {
		this.stack = stack;
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
		provider = new BucketDaoProvider(s3Client, stack);
	}

	@Override
	public String saveBatch(List<ObjectRecord> batch, String type)
			throws IOException {
		String key = KeyGeneratorUtil.createNewKey(stackInstanceNumber,
				System.currentTimeMillis(), true);
		writer.write(batch, provider.getBucketName(type), key);
		return key;
	}

	@Override
	public List<ObjectRecord> getBatch(String key, String type)
			throws IOException {
		return reader.read(provider.getBucketName(type), key);
	}

	@Override
	public void deleteAllStackInstanceBatches(String type) {
		provider.getBucketDao(type).deleteAllObjectsWithPrefix(
				KeyGeneratorUtil.getInstancePrefix(stackInstanceNumber));
	}

	@Override
	public Iterator<String> keyIterator(String type) {
		return provider.getBucketDao(type).keyIterator(
				KeyGeneratorUtil.getInstancePrefix(stackInstanceNumber));
	}
}
