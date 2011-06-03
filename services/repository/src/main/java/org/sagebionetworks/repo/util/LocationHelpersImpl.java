package org.sagebionetworks.repo.util;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTime;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserCredentials;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.AddUserToGroupRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyResult;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.GetUserRequest;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;

/**
 * This utility class holds methods for dealing with pre-signed S3 urls and IAM
 * users and groups for AWS authentication and authorization. Note that Synapse
 * authentication and authorization is layered *on* *top* of this and should be
 * checked either here or at a higher layer in the code.
 * 
 * @author deflaux
 * 
 */
public class LocationHelpersImpl implements LocationHelper {

	@Autowired
	UserDAO userDAO;

	/**
	 * A user for use in integration tests
	 */
	public static final String INTEGRATION_TEST_READ_ONLY_USER_ID = "integration.test@sagebase.org";
	/**
	 * 
	 */
	public static final String FAKE_ACCESS_ID = "thisIsAFakeAWSAccessId";
	/**
	 * 
	 */
	public static final String FAKE_SECRET_KEY = "thisIsAFakeAWSSecretKey";

	private static final int EXPIRES_MINUTES = 24 * 60; // 1 day
	private static final String S3_BUCKET = "data01.sagebase.org";
	private static final String S3_DOMAIN = "s3.amazonaws.com";
	private static final String READ_ONLY_GROUP = "ReadOnlyUnrestrictedDataUsers";
	private static final String CORRECT_DOMAIN = S3_DOMAIN + "/" + S3_BUCKET;
	private static final Pattern INCORRECT_DOMAIN = Pattern.compile(Matcher
			.quoteReplacement(S3_BUCKET + "." + S3_DOMAIN));

	// The IAM user who has permissions to make new IAM users
	private String iamCanCreateUserCredsAccessId = FAKE_ACCESS_ID;
	private String iamCanCreateUserCredsSecretKey = FAKE_SECRET_KEY;
	private AWSCredentials iamCanCreateUsersCreds;

	// The integration test IAM user
	private String iamIntegrationTestCredsAccessId = FAKE_ACCESS_ID;
	private String iamIntegrationTestCredsSecretKey = FAKE_SECRET_KEY;
	private AWSCredentials iamIntegrationTestCreds;

	private AmazonIdentityManagement iamClient;

	/**
	 * 
	 */
	public LocationHelpersImpl() {

		if ((null != System.getProperty("AWS_ACCESS_KEY_ID"))
				&& (null != System.getProperty("AWS_SECRET_KEY"))) {
			// Dev Note: these particular environment variable names are what
			// Elastic Beanstalk supports for passing creds via environment
			// properties
			// https://forums.aws.amazon.com/thread.jspa?messageID=217139&#217139
			iamCanCreateUserCredsAccessId = System
					.getProperty("AWS_ACCESS_KEY_ID");
			iamCanCreateUserCredsSecretKey = System
					.getProperty("AWS_SECRET_KEY");
		}

		iamCanCreateUsersCreds = new BasicAWSCredentials(
				iamCanCreateUserCredsAccessId, iamCanCreateUserCredsSecretKey);
		iamClient = new AmazonIdentityManagementClient(iamCanCreateUsersCreds);

		// TODO hack, delete this once authentication and authorization are in
		// place
		if ((null != System.getProperty("PARAM3"))
				&& (null != System.getProperty("PARAM4"))) {
			// Dev Note: these particular environment variable names are what
			// Elastic Beanstalk supports
			iamIntegrationTestCredsAccessId = System.getProperty("PARAM3");
			iamIntegrationTestCredsSecretKey = System.getProperty("PARAM4");
		}

		iamIntegrationTestCreds = new BasicAWSCredentials(
				iamIntegrationTestCredsAccessId,
				iamIntegrationTestCredsSecretKey);
	}

	/**
	 * Note that the AWS libraries throw many runtime exceptions (e.g., if stuff
	 * times out). The user should retry what ever it is they were trying to do
	 * if they get one of those errors. Our code here should not perform the
	 * retry, but should instead handle situations where prior operations were
	 * only partially completed such as we created a new IAM user but we were
	 * not able to add them to the right IAM group.
	 * 
	 * @param userId
	 * @param cleartextPath
	 * @return a pre-signed S3 URL
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws DatastoreException
	 */
	@Override
	public String getS3Url(String userId, String cleartextPath)
			throws DatastoreException, NotFoundException {

		DateTime now = new DateTime();
		DateTime expires = now.plusMinutes(EXPIRES_MINUTES);

		AWSCredentials creds = getCredentialsForUser(userId);

		// TODO is this person allowed to read this file from S3?

		AmazonS3 client = new AmazonS3Client(creds);
		URL presignedUrl = client.generatePresignedUrl(S3_BUCKET,
				cleartextPath, expires.toDate());

		return fixS3UrlDomain(presignedUrl.toString());
	}

	private String fixS3UrlDomain(String presignedUrl) {

		// By default the AmazonS3 library makes an https url that will result
		// in an SSL cert error if the bucket name has any dots in it. Therefore
		// rewrite the url to put the bucket name in the path instead of the
		// domain.
		Matcher matcher = INCORRECT_DOMAIN.matcher(presignedUrl);
		String correctedPresignedUrl = matcher.replaceFirst(CORRECT_DOMAIN);
		return correctedPresignedUrl;
	}

	@Override
	public String createS3Url(String userId, String cleartextPath, String md5)
			throws DatastoreException, NotFoundException {

		DateTime now = new DateTime();
		DateTime expires = now.plusMinutes(EXPIRES_MINUTES);

		AWSCredentials creds = getCredentialsForUser(userId);

		// TODO is this person allowed to write this file to S3?
		// TODO is this person in the right IAM group?
		// TODO do we want separate expiry for read versus write presigned URLs?

		String base64Md5 = null;
		try {
			byte[] encoded = Base64.encodeBase64(Hex.decodeHex(md5
					.toCharArray()));
			base64Md5 = new String(encoded, "ASCII");
		} catch (Exception e) {
			throw new DatastoreException(e);
		}

		ResponseHeaderOverrides headers = new ResponseHeaderOverrides();
		headers.setContentType("application/binary");

		GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
				S3_BUCKET, cleartextPath, HttpMethod.PUT);
		request.setExpiration(expires.toDate());
		request.setResponseHeaders(headers);
		request.addRequestParameter("x-amz-acl", "bucket-owner-full-control");
		request.addRequestParameter("Content-MD5", base64Md5);

		AmazonS3 client = new AmazonS3Client(creds);
		URL presignedUrl = client.generatePresignedUrl(request);

		return fixS3UrlDomain(presignedUrl.toString());
	}

	private AWSCredentials getCredentialsForUser(String userId)
			throws DatastoreException, NotFoundException {

		// if (userId.equals(INTEGRATION_TEST_READ_ONLY_USER_ID)) {
		return iamIntegrationTestCreds;
		// }
	}

	private AWSCredentials getCredentialsForUserRealImpl(String userId)
			throws DatastoreException, NotFoundException {

		// Check whether we already have credentials stored for this user and
		// return them if we do
		User user = userDAO.getUser(userId);
		UserCredentials storedCreds = new UserCredentials(userId, user
				.getIamAccessId(), user.getIamSecretKey(), user
				.getCreationDate());

		if (null != storedCreds.getIamAccessId()
				&& null != storedCreds.getIamSecretKey()) {
			return new BasicAWSCredentials(storedCreds.getIamAccessId(),
					storedCreds.getIamSecretKey());
		}

		// Make a new IAM user, if needed
		try {
			GetUserRequest request = new GetUserRequest();
			// If we can get the user, then we know the IAM user exists
			iamClient.getUser(request.withUserName(userId));
		} catch (NoSuchEntityException ex) {
			// We need to make a new IAM user
			CreateUserRequest request = new CreateUserRequest();
			iamClient.createUser(request.withUserName(userId));
		}

		// Add the user to the right IAM group (even if they were already added
		// previously, this should be okay)
		AddUserToGroupRequest groupRequest = new AddUserToGroupRequest();
		groupRequest.setGroupName(READ_ONLY_GROUP);
		groupRequest.setUserName(userId);
		iamClient.addUserToGroup(groupRequest);

		// Dev Note: if we make mistakes and somehow fail
		// to persist the credentials for the same IAM user twice, on the third
		// time we try to create an access key for them we will get a
		// LimitExceededException. We could code a solution to delete their old
		// keys and make new ones, but for now its better to let this sort of
		// issue require human intervention because we don't expect this to
		// happen and want to know if it does.

		// Make new credentials
		CreateAccessKeyRequest request = new CreateAccessKeyRequest();
		CreateAccessKeyResult result = iamClient.createAccessKey(request
				.withUserName(userId));

		// And store them
		storedCreds.setIamAccessId(result.getAccessKey().getAccessKeyId());
		storedCreds.setIamSecretKey(result.getAccessKey().getSecretAccessKey());
		try {
			user = userDAO.getUser(userId);
			user.setIamAccessId(storedCreds.getIamAccessId());
			user.setIamSecretKey(storedCreds.getIamSecretKey());
			userDAO.update(user);
		} catch (InvalidModelException e) {
			// This should not happen but if it does it is our error, not the
			// user's error
			throw new DatastoreException(e);
		} catch (NotFoundException e) {
			// This should not happen but if it does it is our error, not the
			// user's error
			throw new DatastoreException(e);
		}

		return new BasicAWSCredentials(storedCreds.getIamAccessId(),
				storedCreds.getIamSecretKey());
	}
}
