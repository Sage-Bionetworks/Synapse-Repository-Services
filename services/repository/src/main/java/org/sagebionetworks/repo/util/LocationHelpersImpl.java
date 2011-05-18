package org.sagebionetworks.repo.util;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserCredentials;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

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

/**
 * This class is a singleton because of the hack below to bootstrap a user and a
 * group. TODO once we have login working, decide whether this still need to be
 * a singleton for any other reason.
 * 
 * @author deflaux
 * 
 */
public class LocationHelpersImpl implements LocationHelper2{
	
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
	private static final Pattern INCORRECT_DOMAIN = Pattern.compile(Matcher.quoteReplacement(S3_BUCKET + "." + S3_DOMAIN));


	// The IAM user who has permissions to make new IAM users
	private String iamCanCreateUserCredsAccessId = FAKE_ACCESS_ID;
	private String iamCanCreateUserCredsSecretKey = FAKE_SECRET_KEY;
	private AWSCredentials iamCanCreateUsersCreds;

	// The integration test IAM user
	private String iamIntegrationTestCredsAccessId = FAKE_ACCESS_ID;
	private String iamIntegrationTestCredsSecretKey = FAKE_SECRET_KEY;
	private AWSCredentials iamIntegrationTestCreds;

	private AmazonIdentityManagement iamClient;

//	private volatile static LocationHelpers2 theInstance = null;



	private LocationHelpersImpl() {

		// If we pass them as environment variables
		if ((null != System.getenv("accessId"))
				&& (null != System.getenv("secretKey"))) {
			iamCanCreateUserCredsAccessId = System.getenv("accessId");
			iamCanCreateUserCredsSecretKey = System.getenv("secretKey");
		}
		// If we pass them as -D on the command line
		else if ((null != System.getProperty("AWS_ACCESS_KEY_ID"))
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
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 */
	@Override
	public String getS3Url(String userId, String cleartextPath)
			throws DatastoreException, UnauthorizedException, NotFoundException {

		if (null == userId) {
			// We should really be checking this further upstream but a little
			// defensive coding here is okay
			// throw new UnauthorizedException();

			// TODO delete me once we have log in stuff working
			// TODO SERIOUSLY, DELETE THIS, IT IS A SECURITY HOLE
			userId = "integration.test@sagebase.org";
		}

		DateTime now = new DateTime();
		DateTime expires = now.plusMinutes(EXPIRES_MINUTES);

		AWSCredentials creds = getCredentialsForUser(userId);

		AmazonS3 client = new AmazonS3Client(creds);
		URL presignedUrl= client.generatePresignedUrl(S3_BUCKET, cleartextPath,
				expires.toDate());
		
		// By default the AmazonS3 library makes an https url that will result
		// in an SSL cert error if the bucket name has any dots in it. Therefore
		// rewrite the url to put the bucket name in the path instead of the
		// domain.
		Matcher matcher = INCORRECT_DOMAIN.matcher(presignedUrl.toString());
		String correctedPresignedUrl = matcher.replaceFirst(CORRECT_DOMAIN);
		return correctedPresignedUrl;
	}

	private AWSCredentials getCredentialsForUser(String userId)
			throws DatastoreException, UnauthorizedException, NotFoundException {

		if (userId.equals(INTEGRATION_TEST_READ_ONLY_USER_ID)) {
			return iamIntegrationTestCreds;
		}

		// Check whether we already have credentials stored for this user and
		// return them if we do
		UserCredentials storedCreds;
//		try {
			User user = userDAO.getUser(userId);
			storedCreds = new UserCredentials(userId,user.getIamAccessId(), user.getIamSecretKey(), user.getCreationDate());
			
			if (null != storedCreds.getIamAccessId()
					&& null != storedCreds.getIamSecretKey()) {
				return new BasicAWSCredentials(storedCreds.getIamAccessId(),
						storedCreds.getIamSecretKey());
			}
//		} catch (NotFoundException ex) {
//			// This should not happen because we should already have a user
//			// object stored for this user to get this far
//			throw new DatastoreException("Unable to retrieve info for user "
//					+ userId, ex);
//		}

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
