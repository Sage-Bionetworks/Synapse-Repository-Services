package org.sagebionetworks.audit.dao;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.sagebionetworks.csv.utils.ObjectCSVWriter;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;

/**
 * A writer that writes csv.gz object files to S3.
 * 
 * @param <T>
 *            For object of this type provide, a row will be written to a CSV
 *            according to the provided header mapping.
 */
public class GzipCsvS3ObjectWriter<T> {

	private AmazonS3 awsS3Client;
	private Class<T> objectClass;
	private String[] headers;

	/**
	 * Create a new write for each object type to write.
	 * 
	 * @param awsS3Client
	 *            A configured S3 client.
	 * @param objectClass
	 *            The type of Objects to be written to the CSV.
	 * @param headers
	 *            Maps the fields of the given objectClass to columns of the
	 *            resulting CSV file.
	 */
	public GzipCsvS3ObjectWriter(AmazonS3 awsS3Client,
			Class<T> objectClass, String[] headers) {
		super();
		this.awsS3Client = awsS3Client;
		this.objectClass = objectClass;
		this.headers = headers;
	}

	/**
	 * Write a batch of object to a csv.gz file in S3.
	 * 
	 * @param batch
	 * @param bucket
	 * @param key
	 * @throws IOException
	 */
	public void write(List<T> batch, String bucket, String key)
			throws IOException {
		// Write the data to a gzip
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPOutputStream zipOut = new GZIPOutputStream(out);
		OutputStreamWriter osw = new OutputStreamWriter(zipOut);
		ObjectCSVWriter<T> writer = new ObjectCSVWriter<T>(osw, objectClass,
				headers);
		// Write all of the data
		for (T ar : batch) {
			writer.append(ar);
		}
		writer.close();
		// Create an input stream
		byte[] bytes = out.toByteArray();
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		ObjectMetadata om = new ObjectMetadata();
		om.setContentType("application/x-gzip");
		om.setContentEncoding("gzip");
		om.setContentDisposition("attachment; filename=" + key + ";");
		om.setContentLength(bytes.length);
		awsS3Client.putObject(bucket, key, in, om);
	}
}
