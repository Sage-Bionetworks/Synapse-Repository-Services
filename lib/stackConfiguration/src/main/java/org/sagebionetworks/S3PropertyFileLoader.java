package org.sagebionetworks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;

/**
 * A helper to load properties files from S3
 * 
 * @author jmhill
 * 
 */
public class S3PropertyFileLoader {
	
	/**
	 * 
	 * @param propertyFileUrl
	 * @param AMIid
	 * @param AMIkey
	 * @return
	 * @throws IOException
	 */
	public static void loadPropertiesFromS3(String propertyFileUrl, String IAMId, String IAMKey, Properties properties) throws IOException {
		if (propertyFileUrl == null)throw new IllegalArgumentException("The file URL cannot be null");
		if (IAMId == null) throw new IllegalArgumentException("IAM id cannot be null");
		if (IAMKey == null)	throw new IllegalArgumentException("IAM key cannot be null");
		if(properties == null) throw new IllegalArgumentException("Properties cannot be null");
		AWSCredentials creds = new BasicAWSCredentials(IAMId, IAMKey);
		AmazonS3Client client = new AmazonS3Client(creds);
		// Create a temp file to store the properties file.
		File temp = null;
		FileInputStream in = null;
		try {
			temp = File.createTempFile("propertyFileUrl", ".tmp");
			GetObjectRequest request = createObjectRequestForUrl(propertyFileUrl);
			client.getObject(request, temp);
			// Read the file
			in = new FileInputStream(temp);
			properties.load(in);
		} finally {
			if(in != null) in.close();
			if(temp != null) temp.delete();
		}
	}

	/**
	 * 
	 * @param url
	 * @return
	 * @throws MalformedURLException
	 */
	public static GetObjectRequest createObjectRequestForUrl(
			String propertyFileUrl) throws MalformedURLException {
		URL url = new URL(propertyFileUrl);
		String path = url.getPath();
		if (path == null)
			throw new IllegalArgumentException(
					"Cannot read URL.  URL.getPath() was null");
		String[] split = path.split("/");
		if (split.length != 3)
			throw new IllegalArgumentException(
					"Could not get the buck and object ID from the URL");
		String bucket = split[1];
		String key = split[2];
		return new GetObjectRequest(bucket, key);
	}

}
