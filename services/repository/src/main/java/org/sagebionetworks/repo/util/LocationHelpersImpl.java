package org.sagebionetworks.repo.util;

import java.net.URLEncoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTime;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.SigningAlgorithm;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AddUserToGroupRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyResult;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.GetUserRequest;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;

/**
 * This utility class holds methods for dealing with pre-signed S3 urls and IAM
 * users and groups for AWS authentication and authorization. Note that Synapse
 * authentication and authorization is layered *on* *top* of this and should be
 * checked at a higher layer in the code.
 * 
 * @author deflaux
 * 
 */
public class LocationHelpersImpl implements LocationHelper {

	@Autowired
	UserDAO userDAO;

	private static final int READ_ACCESS_EXPIRY_MINUTES = StackConfiguration
			.getS3ReadAccessExpiryMinutes();
	private static final int WRITE_ACCESS_EXPIRY_MINUTES = StackConfiguration
			.getS3WriteAccessExpiryMinutes();
	private static final String S3_DOMAIN = "s3.amazonaws.com";
	private static final String UPLOAD_APPLICATION_TYPE = "application/binary";
	private static final String S3_BUCKET = StackConfiguration.getS3Bucket();
	private static final String CORRECT_DOMAIN = S3_DOMAIN + "/" + S3_BUCKET;
	private static final String IAM_S3_GROUP = StackConfiguration
			.getS3IamGroup();
	private static final String STACK = StackConfiguration.getStack();
	private static final String IAM_USERNAME_PREFIX = STACK + "-";
	private Boolean hasSanityBeenChecked = false;

	@Autowired
	private AmazonIdentityManagementFactory iamClientFactory;

	private AmazonIdentityManagement getIamClient() {
		return iamClientFactory.getAmazonIdentityManagement();
	}

	/**
	 * John, do you want to define an interface for classes to implement that
	 * want to have their configuration checked upon start up?
	 * 
	 * This throws DatastoreException. It could throw another exception if that
	 * made more sense. The most important thing is that it bubbles back to
	 * clients as a 5XX "our fault" error instead of a 4XX "user error"
	 * 
	 * @throws DatastoreException
	 */
	public static void validateConfiguration() throws DatastoreException {
		if (!IAM_S3_GROUP.startsWith(STACK)) {
			throw new DatastoreException("Invalid configuration: stack name "
					+ STACK + " does not match IAM group name " + IAM_S3_GROUP);
		}
		if (!S3_BUCKET.startsWith(STACK)) {
			throw new DatastoreException("Invalid configuration: stack name "
					+ STACK + " does not match S3 bucket " + S3_BUCKET);
		}

	}

	public void sanityCheckConfiguration() throws DatastoreException {
		// Dev Note: this is idempotent and therefore does not need to be
		// threadsafe
		if (hasSanityBeenChecked) {
			return;
		}
		validateConfiguration();
		hasSanityBeenChecked = true;
	}

	@Override
	public String getS3Url(String userId, String s3key)
			throws DatastoreException, NotFoundException {
		return getS3Url(userId, s3key, HttpMethod.GET);
	}

	@Override
	public String getS3HeadUrl(String userId, String s3key)
			throws DatastoreException, NotFoundException {
		return getS3Url(userId, s3key, HttpMethod.HEAD);
	}

	private String getS3Url(String userId, String s3key, HttpMethod method)
			throws DatastoreException, NotFoundException {

		AWSCredentials creds = getCredentialsForUser(userId);

		DateTime now = new DateTime();
		DateTime expires = now.plusMinutes(READ_ACCESS_EXPIRY_MINUTES);
		String expirationInSeconds = Long.toString(expires.getMillis() / 1000L);

		// Formulate the canonical string to sign
		StringBuilder buf = new StringBuilder();
		buf.append(method.name()).append("\n");
		buf.append("\n"); // no md5 for a GET
		buf.append("\n"); // no content-type for a GET
		buf.append(expirationInSeconds + "\n");
		buf.append("/").append(S3_BUCKET).append(s3key);

		return sign(buf.toString(), creds, s3key, expirationInSeconds);
	}

	/**
	 * Create presigned S3 PUT URLs
	 * 
	 * MD5 checks and custom x-amz headers are not supported by the AWS Java SDK
	 * so I had to implement signing from scratch. Details here:
	 * http://s3.amazonaws.com/doc/s3-developer-guide/RESTAuthentication.html
	 */
	@Override
	public String createS3Url(String userId, String s3key, String md5)
			throws DatastoreException, NotFoundException {

		// Get the credentials with which to sign the request
		AWSCredentials creds = getCredentialsForUser(userId);

		// Do the necessary encoding to make this stuff okay for urls and
		// headers
		String base64Md5;
		try {
			// TODO find a more direct way to go from hex to base64
			byte[] encoded = Base64.encodeBase64(Hex.decodeHex(md5
					.toCharArray()));
			base64Md5 = new String(encoded, "ASCII");

		} catch (Exception e) {
			throw new DatastoreException(e);
		}

		// Compute our expires time for this URL
		DateTime now = new DateTime();
		DateTime expires = now.plusMinutes(WRITE_ACCESS_EXPIRY_MINUTES);
		String expirationInSeconds = Long.toString(expires.getMillis() / 1000L);

		// Formulate the canonical string to sign
		StringBuilder buf = new StringBuilder();
		buf.append(HttpMethod.PUT.name()).append("\n");
		buf.append(base64Md5).append("\n");
		buf.append(UPLOAD_APPLICATION_TYPE).append("\n");
		buf.append(expirationInSeconds + "\n");
		buf.append("x-amz-acl").append(':').append("bucket-owner-full-control")
				.append("\n");
		buf.append("/").append(S3_BUCKET).append(s3key);

		return sign(buf.toString(), creds, s3key, expirationInSeconds);
	}

	/**
	 * http://s3.amazonaws.com/doc/s3-developer-guide/RESTAuthentication.html
	 * Computes RFC 2104-compliant HMAC signature.
	 */
	private String sign(String data, AWSCredentials creds, String s3key,
			String expirationInSeconds) throws DatastoreException {

		String signature;
		try {
			Mac mac = Mac.getInstance(SigningAlgorithm.HmacSHA1.toString());
			mac.init(new SecretKeySpec(creds.getAWSSecretKey().getBytes(),
					SigningAlgorithm.HmacSHA1.toString()));
			byte[] sig = Base64.encodeBase64(mac
					.doFinal(data.getBytes("UTF-8")));
			signature = URLEncoder.encode(new String(sig), ("UTF-8"));
		} catch (Exception e) {
			throw new DatastoreException("Failed to generate signature: "
					+ e.getMessage(), e);
		}

		StringBuilder presignedUrl = new StringBuilder();
		presignedUrl.append("https://").append(CORRECT_DOMAIN).append(s3key)
				.append("?");
		presignedUrl.append("Expires").append("=").append(expirationInSeconds)
				.append("&");
		presignedUrl.append("AWSAccessKeyId").append("=").append(
				creds.getAWSAccessKeyId()).append("&");
		presignedUrl.append("Signature").append("=").append(signature);

		return presignedUrl.toString();
	}

	private AWSCredentials getCredentialsForUser(String userId)
			throws DatastoreException, NotFoundException {

		// Configuration is checked upon startup, we are being extra safe here
		// by re-validating our "contract" with the configuration system to
		// ensure that our invariates hold true
		sanityCheckConfiguration();

		// Append the stack name to the IAM username for prod vs. test isolation
		// since we cannot ensure that folks do not use the same user name on
		// various stacks.
		String iamUserId = IAM_USERNAME_PREFIX + userId;

		// TODO note that we have two instances of Crowd but N stacks. It would
		// be better to have one Crowd per stack. If that really is not doable,
		// we may need to make multiple slots for AWS credentials in the User
		// model object stored in crowd.

		// Check whether we already have IAM credentials stored for this user
		// and return them if we do
		User user = userDAO.getUser(userId);
		if (null != user.getIamAccessId() && null != user.getIamSecretKey()) {
			
			if (!iamUserId.equals(user.getIamUserId())) {
				throw new DatastoreException("IAM username " + iamUserId
						+ " does not match the one stored in Crowd "
						+ user.getIamUserId());
			}

			return new BasicAWSCredentials(user.getIamAccessId(), user
					.getIamSecretKey());
		}

		// Make a new IAM user, if needed
		try {
			GetUserRequest request = new GetUserRequest();
			// If we can get the user, then we know the IAM user exists
			getIamClient().getUser(request.withUserName(iamUserId));
		} catch (NoSuchEntityException ex) {
			// We need to make a new IAM user
			CreateUserRequest request = new CreateUserRequest();
			getIamClient().createUser(request.withUserName(iamUserId));
		}

		// Add the user to the right IAM group (even if they were already added
		// previously, this should be okay)
		AddUserToGroupRequest groupRequest = new AddUserToGroupRequest();
		groupRequest.setGroupName(IAM_S3_GROUP);
		groupRequest.setUserName(iamUserId);
		getIamClient().addUserToGroup(groupRequest);

		// Dev Note: if we make mistakes and somehow fail
		// to persist the credentials for the same IAM user twice, on the third
		// time we try to create an access key for them we will get a
		// LimitExceededException. We could code a solution to delete their old
		// keys and make new ones, but for now its better to let this sort of
		// issue require human intervention because we don't expect this to
		// happen and want to know if it does.

		// Make new credentials
		CreateAccessKeyRequest request = new CreateAccessKeyRequest();
		CreateAccessKeyResult result = getIamClient().createAccessKey(
				request.withUserName(iamUserId));

		// And store them
		try {
			user = userDAO.getUser(userId);
			user.setIamUserId(iamUserId);
			user.setIamAccessId(result.getAccessKey().getAccessKeyId());
			user.setIamSecretKey(result.getAccessKey().getSecretAccessKey());
			userDAO.update(user);
		} catch (InvalidModelException e) {
			// This should not happen but if it does it is our error, not the
			// user's error
			throw new DatastoreException(e);
		} catch (ConflictingUpdateException e) {
			throw new DatastoreException(e);
		} catch (NotFoundException e) {
			// This should not happen but if it does it is our error, not the
			// user's error
			throw new DatastoreException(e);
		}

		return new BasicAWSCredentials(user.getIamAccessId(), user
				.getIamSecretKey());
	}

	public static void main(String args[]) throws Exception {
		// TODO find a more direct way to go from hex to base64
		byte[] encoded = Base64.encodeBase64(Hex.decodeHex(args[0]
				.toCharArray()));
		String base64Md5 = new String(encoded, "ASCII");
		System.out.println(args[0] + " base64 encoded= " + base64Md5);
	}

}
