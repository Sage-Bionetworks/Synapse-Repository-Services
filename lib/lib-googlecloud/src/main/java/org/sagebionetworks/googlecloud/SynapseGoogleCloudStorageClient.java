package org.sagebionetworks.googlecloud;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.StorageException;

/**
 * Abstracts away Google Cloud API to expose just the methods needed by Synapse.
 */
public interface SynapseGoogleCloudStorageClient {

	/**
	 * Get an object from Google Cloud storage
	 * @param bucket the bucket containing the object
	 * @param key the path/filename of the object
	 * @return the object {@link Blob Blob}
	 */
	Blob getObject(String bucket, String key);

	/**
	 * Create a pre-signed URL for a GET/PUT/POST/DELETE action on a particular object in Google Cloud storage
	 * @param bucket the bucket containing the object
	 * @param key the path to the object to get, create, or modify
	 * @param expirationInMinutes how long the URL should be valid
	 * @param requestMethod the HTTP method that the user of the URL can invoke
	 * @return a URL providing timed access to the resource for the particular method specified
	 */
	URL createSignedUrl(String bucket, String key, long expirationInMinutes, HttpMethod requestMethod);

	/**
	 * Upload an object to a Google Cloud Storage bucket
	 * @param bucket the name of the bucket to upload to
	 * @param key the path/filename of the object to upload
	 * @param inputStream an InputStream containing the contents to upload
	 * @return the uploaded object
	 * @throws IOException if the stream cannot be read
	 */
	Blob putObject(String bucket, String key, InputStream inputStream) throws IOException;


	/**
	 * Upload an object to a Google Cloud Storage bucket
	 * @param bucket the name of the bucket to upload to
	 * @param key the path/filename of the object to upload
	 * @param file a File containing the contents to upload
	 * @return the uploaded object
	 * @throws FileNotFoundException if the file cannot be opened
	 */
	Blob putObject(String bucket, String key, File file) throws FileNotFoundException, StorageException;


	/**
	 * Deletes the object in Google Cloud Storage
	 * @param bucket The bucket containing the object
	 * @param key The path/filename of the object
	 */
	void deleteObject(String bucket, String key);

	/**
	 * Combines (concatenates) objects in the order they are given to create a new object.
	 * The objects specified in partKeys should already exist in the bucket.
	 * @param bucket the name of the bucket that contains the parts and will contain the composed object
	 * @param newKey the new path/name of the object to compose
	 * @param partKeys the paths of the objects that will comprise the new composed object.
	 *                 the parts will be combined in the order given
	 * @return the composed object
	 */
	Blob composeObjects(String bucket, String newKey, List<String> partKeys);

	/**
	 * Rename an object.
	 * @param bucket the bucket containing the object
	 * @param oldKey the current name of the object
	 * @param newKey the new name of the object
	 */
	void rename(String bucket, String oldKey, String newKey);

	/**
	 * Gets the list of Blobs in a bucket with a particular prefix.
	 * @param bucket the bucket containing objects to return
	 * @param keyPrefix the prefix that objects should match
	 * @return
	 */
	List<Blob> getObjects(String bucket, String keyPrefix);

	/**
	 * Checks to see if a Google Cloud Storage bucket exists or not
	 * @param bucket the name of the bucket
	 * @return whether or not the bucket exists
	 */
	Boolean bucketExists(String bucket);

	/**
	 * Get a BufferedReader that contains the data of the object
	 * @param bucket the bucket containing the object
	 * @param key the name of the object
	 * @return
	 */
	BufferedReader getObjectContent(String bucket, String key);
}
