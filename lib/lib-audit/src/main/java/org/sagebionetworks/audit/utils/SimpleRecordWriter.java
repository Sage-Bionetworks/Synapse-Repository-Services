package org.sagebionetworks.audit.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import com.amazonaws.services.s3.AmazonS3Client;

/**
 * This class helps write a record to a file and push the file to S3.
 */
public class SimpleRecordWriter<T> {
	private AmazonS3Client s3Client;
	private int stackInstanceNumber;
	private String bucketName;
	private Class<T> objectClass;

	public SimpleRecordWriter(AmazonS3Client s3Client, int stackInstanceNumber, 
			String bucketName, Class<T> objectClass) {
		this.s3Client = s3Client;
		this.stackInstanceNumber = stackInstanceNumber;
		this.bucketName = bucketName;
		this.objectClass = objectClass;
	}
	
	public String write(T record) throws IOException {
		File file = createNewFile();
		
		ObjectCSVWriter<T> csvWriter = new ObjectCSVWriter<T>(new FileWriter(file.getAbsoluteFile()), objectClass);
		csvWriter.append(record);
		csvWriter.close();
		
		String fileName = sendFileToS3(file);
		Files.delete(file.toPath());
		file = null;
	
		return fileName;
	}

	private String sendFileToS3(File file) {
		if (!s3Client.doesBucketExist(bucketName)) {
			s3Client.createBucket(bucketName);
		}
		String fileName = getKey();
		s3Client.putObject(bucketName, fileName, file);
		
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
