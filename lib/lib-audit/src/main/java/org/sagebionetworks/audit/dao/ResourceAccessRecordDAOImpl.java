package org.sagebionetworks.audit.dao;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import org.sagebionetworks.audit.utils.KeyGeneratorUtil;
import org.sagebionetworks.audit.utils.ObjectCSVWriter;
import org.sagebionetworks.repo.model.audit.ResourceAccessRecord;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;

public class ResourceAccessRecordDAOImpl implements ResourceAccessRecordDAO {

	@Autowired
	private AmazonS3Client s3Client;
	/**
	 * Injected via Spring
	 */
	int stackInstanceNumber;
	/**
	 * Injected via Spring
	 */
	private String resourceAccessRecordBucketName;

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
	
	@Override
	public String write(ResourceAccessRecord record) throws IOException {
		File file = createNewFile();
		
		ObjectCSVWriter<ResourceAccessRecord> csvWriter = 
				new ObjectCSVWriter<ResourceAccessRecord>(new FileWriter(file.getAbsoluteFile()), ResourceAccessRecord.class);
		csvWriter.append(record);
		csvWriter.close();
		
		String fileName = sendFileToS3(file);
		Files.delete(file.toPath());
		file = null;
	
		return fileName;
	}

	private String sendFileToS3(File file) {
		if (!s3Client.doesBucketExist(resourceAccessRecordBucketName)) {
			s3Client.createBucket(resourceAccessRecordBucketName);
		}
		String fileName = getKey();
		s3Client.putObject(resourceAccessRecordBucketName, fileName, file);
		
		return fileName;
	}

	private File createNewFile() throws IOException {
		File file = new File("temp.csv");
		if (!file.exists()) {
			file.createNewFile();
		}
		if (!file.canWrite()) {
			file.setWritable(true);
		}
		return file;
	}

	private String getKey() {
		return KeyGeneratorUtil.createNewKey(stackInstanceNumber, System.currentTimeMillis(), false);
	}
}
