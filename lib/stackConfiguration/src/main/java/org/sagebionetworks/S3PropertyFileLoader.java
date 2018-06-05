package org.sagebionetworks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.aws.AwsClientFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;

/**
 * A helper to load properties files from S3
 * 
 * @author jmhill
 * 
 */
public class S3PropertyFileLoader {
	private static final Logger log = LogManager.getLogger(S3PropertyFileLoader.class
			.getName());
	/**
	 * 
	 * @param propertyFileUrl
	 * @param AMIid
	 * @param AMIkey
	 * @return
	 * @throws IOException
	 */
	public static void loadPropertiesFromS3(String propertyFileUrl, Properties properties) throws IOException {
		log.info("propertyFileUrl="+propertyFileUrl);
		if (propertyFileUrl == null)throw new IllegalArgumentException("The file URL cannot be null");
		if(properties == null) throw new IllegalArgumentException("Properties cannot be null");
		AmazonS3 client = AwsClientFactory.createAmazonS3Client();
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
		if (split.length < 3)
			throw new IllegalArgumentException(
					"Could not get the bucket and object ID from the URL: "+propertyFileUrl);
		String bucket = split[1];
		String key = split[2];
		if(split.length > 3) {
			for(int i = 3; i < split.length; i++) {
				key += "/" + split[i];
			}
		}
		return new GetObjectRequest(bucket, key);
	}

}
