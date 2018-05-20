package org.sagebionetworks.audit.dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.sagebionetworks.csv.utils.ObjectCSVReader;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;

/**
 * A reader that reads csv.gz object files from S3.
 * 
 * @param <T>
 *            For each row in the CSV file, an object of this type will be built
 *            according to the provided header mapping.
 */
public class GzipCsvS3ObjectReader<T> {

	private AmazonS3 awsS3Client;
	private Class<T> objectClass;
	private String[] headers;

	/**
	 * Create a new reader for each object type to read.
	 * 
	 * @param awsS3Client
	 * @param objectClass
	 *            The type of object that will be created for row found in the
	 *            read CSV.
	 * @param headers
	 *            Maps the column of the read CSV to fields of the objectClass.
	 */
	public GzipCsvS3ObjectReader(AmazonS3 awsS3Client,
			Class<T> objectClass, String[] headers) {
		super();
		this.awsS3Client = awsS3Client;
		this.objectClass = objectClass;
		this.headers = headers;
	}

	/**
	 * Read a single csv.gz file from S3. For each row read from the CSV a
	 * corresponding object will be included in the resulting list.
	 * 
	 * @param key
	 *            - The key of the batch
	 * @return
	 * @throws IOException
	 */
	public List<T> read(String bucket, String key) throws IOException {
		if (key == null)
			throw new IllegalArgumentException("Key cannot be null");
		// Attempt to download the object
		S3Object object = awsS3Client.getObject(bucket, key);
		InputStream input = object.getObjectContent();
		return readFromStream(input);
	}

	/**
	 * Read the objects from the stream
	 * 
	 * @param input
	 * @return
	 * @throws IOException
	 */
	public List<T> readFromStream(InputStream input) throws IOException {
		try {
			// Read the data
			GZIPInputStream zipIn = new GZIPInputStream(input);
			InputStreamReader isr = new InputStreamReader(zipIn);
			ObjectCSVReader<T> reader = new ObjectCSVReader<T>(isr,
					objectClass, headers);
			List<T> results = new LinkedList<T>();
			T record = null;
			while ((record = reader.next()) != null) {
				results.add(record);
			}
			reader.close();
			return results;
		} finally {
			if (input != null) {
				input.close();
			}
		}
	}
}
