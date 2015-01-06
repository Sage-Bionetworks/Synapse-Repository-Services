package org.sagebionetworks.audit.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

/**
 * This class helps write records to a file and push the file to S3.
 */
public class SimpleRecordWriter<T> {
	private AmazonS3Client s3Client;
	private int stackInstanceNumber;
	private String bucketName;
	private Class<T> objectClass;
	private String[] headers;

	public SimpleRecordWriter(AmazonS3Client s3Client, int stackInstanceNumber, 
			String bucketName, Class<T> objectClass, String[] headers) {
		this.s3Client = s3Client;
		this.stackInstanceNumber = stackInstanceNumber;
		this.bucketName = bucketName;
		this.objectClass = objectClass;
		this.headers = headers;
	}
	
	public String write(List<T> batch, long timestamp, boolean rolling) throws IOException {
		// Write the data to a gzip
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPOutputStream zipOut = new GZIPOutputStream(out);
		OutputStreamWriter osw = new OutputStreamWriter(zipOut);
		ObjectCSVWriter<T> writer = new ObjectCSVWriter<T>(osw, objectClass, headers);
		// Write all of the data
		for (T ar : batch) {
			writer.append(ar);
		}
		writer.close();
		// Create an input stream
		byte[] bytes = out.toByteArray();
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		// Build a new key
		String key = KeyGeneratorUtil.createNewKey(stackInstanceNumber,
				timestamp, rolling);
		ObjectMetadata om = new ObjectMetadata();
		om.setContentType("application/x-gzip");
		om.setContentEncoding("gzip");
		om.setContentDisposition("attachment; filename=" + key + ";");
		om.setContentLength(bytes.length);
		s3Client.putObject(bucketName, key, in, om);
		return key;
	}
}
