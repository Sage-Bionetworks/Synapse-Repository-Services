package org.sagebionetworks.audit.dao;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import org.sagebionetworks.audit.utils.KeyGeneratorUtil;
import org.sagebionetworks.repo.model.audit.AclRecord;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.bytecode.opencsv.CSVWriter;

import com.amazonaws.services.s3.AmazonS3Client;

public class AclRecordDAOImpl implements AclRecordDAO {

	@Autowired
	private AmazonS3Client s3Client;

	private static final int MAX_LINES = 2000;
	private static final String BUCKET_NAME = "prod.acl.record.sagebase.org";
	/**
	 * Injected via Spring
	 */
	int stackInstanceNumber;
	File currentFile = null;
	int lineCount = 0;

	@Override
	public void write(AclRecord record) throws IOException {
		if (currentFile == null ) {
			createNewFile();
		}

		CSVWriter csvWriter = new CSVWriter(new FileWriter(currentFile.getAbsoluteFile()));
		csvWriter.writeNext(nextLine(record));
		lineCount++;
		csvWriter.close();
		
		if (lineCount >= MAX_LINES) {
			sendFileToS3();
			Files.delete(currentFile.toPath());
			currentFile = null;
			lineCount = 0;
		}
	}

	private String[] nextLine(AclRecord record) {
		String[] nextLine = new String[5];
		nextLine[0] = record.getTimestamp().toString();
		nextLine[1] = record.getChangeNumber();
		nextLine[2] = record.getObjectId();
		nextLine[3] = record.getChangeType();
		nextLine[4] = record.getEtag();
		return nextLine;
	}

	private void sendFileToS3() {
		if (!s3Client.doesBucketExist(BUCKET_NAME)) {
			s3Client.createBucket(BUCKET_NAME);
		}
		s3Client.putObject(BUCKET_NAME, getKey(), currentFile);
	}

	private void createNewFile() throws IOException {
		currentFile = new File(getKey() + ".csv");
		if (!currentFile.exists()) {
			currentFile.createNewFile();
		}
		if (!currentFile.canWrite()) {
			currentFile.setWritable(true);
		}
	}

	private String getKey() {
		return KeyGeneratorUtil.createNewKey(stackInstanceNumber, System.currentTimeMillis(), false);
	}

}
