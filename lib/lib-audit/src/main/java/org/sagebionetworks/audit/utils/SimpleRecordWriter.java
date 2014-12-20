package org.sagebionetworks.audit.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import com.amazonaws.services.s3.AmazonS3Client;

/**
 * This class helps write records to a file and push the file to S3.
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
	
	public String write(List<T> records) throws IOException {
		File file = File.createTempFile("temp", ".csv");
		try {
			ObjectCSVWriter<T> csvWriter = new ObjectCSVWriter<T>(new FileWriter(file), objectClass);
			try {
				for (T record : records) {
					csvWriter.append(record);
				}
			} finally {
				csvWriter.close();
			}
			return sendFileToS3(file);
			
		} finally {
			file.delete();
		}
	}

	private String sendFileToS3(File file) {
		if (!s3Client.doesBucketExist(bucketName)) {
			s3Client.createBucket(bucketName);
		}
		String fileName = getKey();
		s3Client.putObject(bucketName, fileName, file);
		
		return fileName;
	}

	private String getKey() {
		return KeyGeneratorUtil.createNewKey(stackInstanceNumber, System.currentTimeMillis(), false);
	}
}
