package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.GetObjectRequest;

public class S3PropertyFileLoaderTest {
	
	/**
	 * This test is ignored because it requires a real url, id, and key.
	 * It was tested once with with real data.
	 * @throws IOException
	 */
	@Test (expected=AmazonServiceException.class)
	public void testLoadFails() throws IOException{
		String url = "https://s3.amazonaws.com/fake-bucket/fake-file.properties";
		String id = "fake-id";
		String key = "fake-key";
		Properties props = new Properties();
		S3PropertyFileLoader.loadPropertiesFromS3(url, id, key, props);
	}
	
	/**
	 * This test is ignored because it requires a real url, id, and key.
	 * It was tested once with with real data.
	 * @throws IOException
	 */
	@Ignore
	@Test
	public void testLoad() throws IOException{
		String url = "Set a real URL to test";
		String id = "Set a real ID to test";
		String key = "Set a real key to test.";
		Properties props = new Properties();
		S3PropertyFileLoader.loadPropertiesFromS3(url, id, key, props);
		assertNotNull(props.getProperty("org.sagebionetworks.repository.database.connection.url"));
	}
	
	@Test
	public void testcreateObjectRequestForUrl() throws MalformedURLException{
		String propertyFileUrl = "https://s3.amazonaws.com/elasticbeanstalk-us-east-1-325565585839/my-local.properties";
		GetObjectRequest request = S3PropertyFileLoader.createObjectRequestForUrl(propertyFileUrl);
		assertNotNull(request);
		assertEquals("elasticbeanstalk-us-east-1-325565585839", request.getBucketName());
		assertEquals("my-local.properties", request.getKey());
	}

}
