package org.sagebionetworks.repo.util;

import java.net.URL;

import org.joda.time.DateTime;
import org.sagebionetworks.repo.model.DAOFactory;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserCredentials;
import org.sagebionetworks.repo.model.UserCredentialsDAO;
import org.sagebionetworks.repo.model.gaejdo.GAEJDODAOFactoryImpl;
import org.sagebionetworks.repo.web.NotFoundException;

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
 * @author deflaux
 * 
 */
public class LocationHelpers {

	private static final int EXPIRES_MINUTES = 10;
	private static final String S3_BUCKET = "data01.sagebase.org";
	private static final String READ_ONLY_GROUP = "ReadOnlyUnrestrictedDataUsers";
	// TODO @Autowired, no GAE references allowed in this class
	private static final DAOFactory DAO_FACTORY = new GAEJDODAOFactoryImpl();

	private static String iamCanCreateUserCredsAccessId = "thisIsAFakeAWSAccessId";
	private static String iamCanCreateUserCredsSecretKey = "thisIsAFakeAWSSecretKey";

	private static AWSCredentials iamCanCreateUsersCreds;
	private static AmazonIdentityManagement iamClient;

	/**
	 * Helper method for integration tests, spring test config could call this
	 * method but we should not store these keys in our spring config because
	 * then they will get checked into svn (which is bad)
	 * 
	 * TODO nuke this when we have an integration instance of the user service
	 */
	public static void useTestKeys() {
		if ((null != System.getenv("accessId"))
				&& (null != System.getenv("secretKey"))) {
			iamCanCreateUserCredsAccessId = System.getenv("accessId");
			iamCanCreateUserCredsSecretKey = System.getenv("secretKey");
		}
		iamCanCreateUsersCreds = new BasicAWSCredentials(iamCanCreateUserCredsAccessId,
				iamCanCreateUserCredsSecretKey);
		iamClient = new AmazonIdentityManagementClient(iamCanCreateUsersCreds);
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
	public static String getS3Url(String userId, String cleartextPath)
			throws DatastoreException, UnauthorizedException {

		if (null == userId) {
			// We should really be checking this further upstream but a little
			// defensive coding here is okay
			throw new UnauthorizedException();
		}

		DateTime now = new DateTime();
		DateTime expires = now.plusMinutes(EXPIRES_MINUTES);

		AWSCredentials creds = getCredentialsForUser(userId);

		AmazonS3 client = new AmazonS3Client(creds);
		URL signedPath = client.generatePresignedUrl(S3_BUCKET, cleartextPath,
				expires.toDate());
		return signedPath.toString();
	}

	private static AWSCredentials getCredentialsForUser(String userId)
			throws DatastoreException, UnauthorizedException {

		UserCredentialsDAO credsDao = DAO_FACTORY.getUserCredentialsDAO(userId);

		// Check whether we already have credentials stored for this user and
		// return them if we do
		UserCredentials storedCreds;
		try {
			storedCreds = credsDao.get(userId);
			if (null != storedCreds.getIamAccessId()
					&& null != storedCreds.getIamSecretKey()) {
				return new BasicAWSCredentials(storedCreds.getIamAccessId(),
						storedCreds.getIamSecretKey());
			}
		} catch (NotFoundException ex) {
			// This should not happen because we should already have a user
			// object stored for this user to get this far
			throw new DatastoreException("Unable to retrieve info for user "
					+ userId, ex);
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
			credsDao.update(storedCreds);
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
