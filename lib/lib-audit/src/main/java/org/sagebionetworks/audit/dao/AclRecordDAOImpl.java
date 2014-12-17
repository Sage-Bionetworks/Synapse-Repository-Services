package org.sagebionetworks.audit.dao;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import org.sagebionetworks.audit.utils.KeyGeneratorUtil;
import org.sagebionetworks.audit.utils.ObjectCSVWriter;
import org.sagebionetworks.repo.model.audit.AclRecord;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;

public class AclRecordDAOImpl implements AclRecordDAO {

	@Autowired
	private AmazonS3Client s3Client;
	private static final String BUCKET_NAME = "prod.acl.record.sagebase.org";
	/**
	 * Injected via Spring
	 */
	int stackInstanceNumber;

	/**
	 * Injected via Spring
	 * 
	 * @param stackInstanceNumber
	 */
	public void setStackInstanceNumber(int stackInstanceNumber) {
		this.stackInstanceNumber = stackInstanceNumber;
	}
	
	@Override
	public String write(AclRecord record) throws IOException {
		File file = createNewFile();
		
		ObjectCSVWriter<AclRecord> csvWriter = new ObjectCSVWriter<AclRecord>(new FileWriter(file.getAbsoluteFile()), AclRecord.class);
		csvWriter.append(record);
		csvWriter.close();
		
		String fileName = sendFileToS3(file);
		Files.delete(file.toPath());
		file = null;
	
		return fileName;
	}

	private String sendFileToS3(File file) {
		if (!s3Client.doesBucketExist(BUCKET_NAME)) {
			s3Client.createBucket(BUCKET_NAME);
		}
		String fileName = getKey();
		s3Client.putObject(BUCKET_NAME, fileName, file);
		
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
