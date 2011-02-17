package org.sagebionetworks.repo.util;

import java.net.URL;

import org.joda.time.DateTime;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * @author deflaux
 * 
 */
public class LocationHelpers {

	private static final int EXPIRES_MINUTES = 1;
	private static final String S3_BUCKET = "data01.sagebase.org";

	private static String testAccessKey = "thisIsAFakeAccessKey";
	private static String testSecretKey = "thisIsAFakeSecretKey";

	/**
	 * @param username
	 * @param cleartextPath
	 * @return a pre-signed S3 URL
	 */
	public static String getS3Url(String username, String cleartextPath) {
		DateTime now = new DateTime();
		DateTime expires = now.plusMinutes(EXPIRES_MINUTES);

		// TODO get IAM creds for user
		String accessKey = testAccessKey;
		String secretKey = testSecretKey;
		AWSCredentials creds = new BasicAWSCredentials(accessKey, secretKey);

		AmazonS3 client = new AmazonS3Client(creds);
		URL signedPath = client.generatePresignedUrl(S3_BUCKET, cleartextPath,
				expires.toDate());
		return signedPath.toString();
	}

	/**
	 * Helper method for integration tests, spring test config could call this
	 * method but we should not store these keys in our spring config because
	 * then they will get checked into svn (which is bad)
	 * 
	 * TODO nuke this when we have an integration instance of the user service
	 */
	public static void useTestKeys() {

		if ((null != System.getenv("accessKey"))
				&& (null != System.getenv("secretKey"))) {
			testAccessKey = System.getenv("accessKey");
			testSecretKey = System.getenv("secretKey");
		}
	}
}
