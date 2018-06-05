package org.sagebionetworks.audit.dao;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.sagebionetworks.aws.utils.s3.KeyGeneratorUtil;
import org.sagebionetworks.csv.utils.ObjectCSVReader;
import org.sagebionetworks.csv.utils.ObjectCSVWriter;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * This class helps write records to a file and push the file to S3.
 */
public class ObjectCSVDAO<T> {
	private AmazonS3 s3Client;
	private int stackInstanceNumber;
	private String bucketName;
	private Class<T> objectClass;
	private String[] headers;
	String stackInstancePrefixString;

	public ObjectCSVDAO(AmazonS3 s3Client, int stackInstanceNumber, 
			String bucketName, Class<T> objectClass, String[] headers) {
		this.s3Client = s3Client;
		this.stackInstanceNumber = stackInstanceNumber;
		this.bucketName = bucketName;
		this.objectClass = objectClass;
		this.headers = headers;
		this.stackInstancePrefixString = KeyGeneratorUtil.getInstancePrefix(stackInstanceNumber);
	}

	/**
	 * Save a batch of T records to the permanent store using the current time as the timestamp.
	 *
	 * @param rolling  Whether the batch is saved as "rolling".
	 */
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

	/**
	 * Get a batch of T records from the permanent store using its key
	 * 
	 * @param key - The key of the batch
	 * @return a batch of T records from the S3 bucket
	 * @throws IOException 
	 */
	public List<T> read(String key) throws IOException {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		// Attempt to download the object
		S3Object object = s3Client.getObject(bucketName, key);
		InputStream input = object.getObjectContent();
		return readFromStream(input);
	}

	/**
	 * Read the objects from the stream
	 * @param input
	 * @return
	 * @throws IOException
	 */
	public List<T> readFromStream(InputStream input) throws IOException {
		try {
			// Read the data
			GZIPInputStream zipIn = new GZIPInputStream(input);
			InputStreamReader isr = new InputStreamReader(zipIn);
			ObjectCSVReader<T> reader = new ObjectCSVReader<T>(
					isr, objectClass, headers);
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

	/**
	 * Delete a batch.
	 * @param key
	 */
	public void delete(String key) {
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		s3Client.deleteObject(bucketName, key);
	}

	/**
	 * Delete all stack instance batches from the bucket. This should never be called on a production system.
	 * 
	 */
	public void deleteAllStackInstanceBatches() {
		// List all object with the prefix
		boolean done = false;
		while(!done){
			ObjectListing listing = s3Client.listObjects(bucketName, stackInstancePrefixString);
			done = !listing.isTruncated();
			// Delete all
			if(listing.getObjectSummaries() != null){
				for(S3ObjectSummary summary: listing.getObjectSummaries()){
					s3Client.deleteObject(bucketName, summary.getKey());
				}
			}
		}
	}

	/**
	 * List all of the objects in this bucket with the stack instance prefix string and the provided marker.
	 */
	public ObjectListing listBatchKeys(String marker) {
		return s3Client.listObjects(new ListObjectsRequest().withBucketName(this.bucketName).withPrefix(stackInstancePrefixString).withMarker(marker));
	}

	/**
	 * @return all keys found in this bucket
	 */
	public Set<String> listAllKeys() {
		Set<String> foundKeys = new HashSet<String>();
		String marker = null;
		do{
			ObjectListing listing = listBatchKeys(marker);
			marker = listing.getNextMarker();
			for(S3ObjectSummary summ: listing.getObjectSummaries()){
				foundKeys.add(summ.getKey());
			}
		}while(marker != null);
		return foundKeys;
	}
}
