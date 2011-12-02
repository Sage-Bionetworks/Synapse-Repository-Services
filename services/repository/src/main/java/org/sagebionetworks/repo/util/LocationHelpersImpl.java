package org.sagebionetworks.repo.util;

import java.net.URLEncoder;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTime;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.HMACUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetFederationTokenRequest;
import com.amazonaws.services.securitytoken.model.GetFederationTokenResult;

/**
 * This utility class holds methods for dealing with pre-signed S3 urls and
 * security tokens for AWS S3 authentication and authorization. Note that
 * Synapse authentication and authorization is layered *on* *top* of this and
 * should be checked at a higher layer in the code.
 * 
 * @author deflaux
 * 
 */
public class LocationHelpersImpl implements LocationHelper {

	// http://docs.amazonwebservices.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/securitytoken/model/GetFederationTokenRequest.html#setName(java.lang.String)
	// This will be increased to 64 characters some time in October
	private static final int MAX_FEDERATED_NAME_LENGTH = 32;

	private static final int READ_ACCESS_EXPIRY_HOURS = StackConfiguration
			.getS3ReadAccessExpiryHours();
	private static final int WRITE_ACCESS_EXPIRY_HOURS = StackConfiguration
			.getS3WriteAccessExpiryHours();
	private static final String S3_DOMAIN = "s3.amazonaws.com";
	private static final String CANONICALIZED_ACL_HEADER = "x-amz-acl:bucket-owner-full-control";
	private static final String SECURITY_TOKEN_HEADER = "x-amz-security-token";
	private static final String S3_BUCKET = StackConfiguration.getS3Bucket();
	private static final String S3_URL_PREFIX = "https://" + S3_DOMAIN + "/"
			+ S3_BUCKET;
	private static final String STACK = StackConfiguration.getStack();
	private static final String IAM_USERNAME_PREFIX = STACK + "-";
	private static final String S3_KEY_PLACEHOLDER = "REPLACE_ME_WITH_AN_S3_KEY";
	private static final String READONLY_DATA_POLICY = "{\"Statement\": [{\"Effect\": \"Allow\", \"Action\": [\"s3:GetObject\",\"s3:GetObjectVersion\"],\"Resource\": \"arn:aws:s3:::"
			+ S3_BUCKET + S3_KEY_PLACEHOLDER + "\"}]}";
	private static final String READWRITE_DATA_POLICY = "{\"Statement\": [{\"Effect\": \"Allow\", \"Action\": [\"s3:GetObject\",\"s3:GetObjectVersion\",\"s3:PutObject\",\"s3:PutObjectVersion\"],\"Resource\": \"arn:aws:s3:::"
			+ S3_BUCKET + S3_KEY_PLACEHOLDER + "\"}]}";

	private Boolean hasSanityBeenChecked = false;

	@Autowired
	private AmazonClientFactory amazonClientFactory;
	
	/**
	 * Default constructor
	 */
	public LocationHelpersImpl() {
	}
	
	/**
	 * @param amazonClientFactory
	 */
	public LocationHelpersImpl(AmazonClientFactory amazonClientFactory) {
		this.amazonClientFactory = amazonClientFactory;
	}

	/**
	 * This throws DatastoreException. It could throw another exception if that
	 * made more sense. The most important thing is that it bubbles back to
	 * clients as a 5XX "our fault" error instead of a 4XX "user error"
	 * 
	 * @throws DatastoreException
	 */
	public static void validateConfiguration() throws DatastoreException {
		if (!S3_BUCKET.startsWith(STACK)) {
			throw new DatastoreException("Invalid configuration: stack name "
					+ STACK + " does not match S3 bucket " + S3_BUCKET);
		}
	}

	private void sanityCheckConfiguration() throws DatastoreException {
		// Dev Note: this is idempotent and therefore does not need to be
		// threadsafe
		if (hasSanityBeenChecked) {
			return;
		}
		validateConfiguration();
		hasSanityBeenChecked = true;
	}

	@Override
	public String getS3Url(String userId, String s3Key)
			throws DatastoreException, NotFoundException {
		return getS3Url(userId, s3Key, HttpMethod.GET);
	}

	@Override
	public String getS3HeadUrl(String userId, String s3Key)
			throws DatastoreException, NotFoundException {
		return getS3Url(userId, s3Key, HttpMethod.HEAD);
	}

	private String getS3Url(String userId, String s3Key, HttpMethod method)
			throws DatastoreException {

		// Get the credentials with which to sign the request
		Credentials token = createS3Token(userId, method, s3Key);
		AWSCredentials creds = new BasicAWSCredentials(token.getAccessKeyId(),
				token.getSecretAccessKey());

		DateTime now = new DateTime();
		DateTime expires = now.plusHours(READ_ACCESS_EXPIRY_HOURS);
		String expirationInSeconds = Long.toString(expires.getMillis() / 1000L);

		// Formulate the canonical string to sign
		StringBuilder buf = new StringBuilder();
		buf.append(method.name()).append("\n");
		buf.append("\n"); // no md5 for a GET
		buf.append("\n"); // no content-type for a GET
		buf.append(expirationInSeconds + "\n");
		buf.append(SECURITY_TOKEN_HEADER).append(':').append(
				token.getSessionToken()).append("\n");
		buf.append("/").append(S3_BUCKET).append(s3Key);

		return sign(buf.toString(), creds, s3Key, expirationInSeconds, token
				.getSessionToken());
	}

	/**
	 * Create presigned S3 PUT URLs
	 * 
	 * MD5 checks and custom x-amz headers are not supported by the AWS Java SDK
	 * so I had to implement signing from scratch. Details here:
	 * http://s3.amazonaws.com/doc/s3-developer-guide/RESTAuthentication.html
	 */
	@Override
	public String createS3Url(String userId, String s3Key, String md5,
			String contentType) throws DatastoreException, NotFoundException {

		// Get the credentials with which to sign the request
		Credentials token = createS3Token(userId, HttpMethod.PUT, s3Key);
		AWSCredentials creds = new BasicAWSCredentials(token.getAccessKeyId(),
				token.getSecretAccessKey());

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
		DateTime expires = now.plusHours(WRITE_ACCESS_EXPIRY_HOURS);
		String expirationInSeconds = Long.toString(expires.getMillis() / 1000L);

		// Formulate the canonical string to sign
		StringBuilder buf = new StringBuilder();
		buf.append(HttpMethod.PUT.name()).append("\n");
		buf.append(base64Md5).append("\n");
		buf.append(contentType).append("\n");
		buf.append(expirationInSeconds).append("\n");
		buf.append(CANONICALIZED_ACL_HEADER).append("\n");
		buf.append(SECURITY_TOKEN_HEADER).append(':').append(
				token.getSessionToken()).append("\n");
		buf.append("/").append(S3_BUCKET).append(s3Key);

		return sign(buf.toString(), creds, s3Key, expirationInSeconds, token
				.getSessionToken());
	}

	/**
	 * http://s3.amazonaws.com/doc/s3-developer-guide/RESTAuthentication.html
	 * Computes RFC 2104-compliant HMAC signature.
	 */
	private String sign(String data, AWSCredentials creds, String s3Key,
			String expirationInSeconds, String token) throws DatastoreException {

		String signature;
		String encodedToken;
		try {
			byte[] sig = HMACUtils.generateHMACSHA1SignatureFromRawKey(data,
					creds.getAWSSecretKey().getBytes());
			signature = URLEncoder.encode(new String(sig), "UTF-8");
			encodedToken = URLEncoder.encode(token, "UTF-8");
		} catch (Exception e) {
			throw new DatastoreException("Failed to generate signature: "
					+ e.getMessage(), e);
		}

		StringBuilder presignedUrl = new StringBuilder();
		presignedUrl.append(S3_URL_PREFIX).append(s3Key).append("?");
		presignedUrl.append("Expires").append("=").append(expirationInSeconds)
				.append("&");
		presignedUrl.append(SECURITY_TOKEN_HEADER).append("=").append(encodedToken)
				.append("&");
		presignedUrl.append("AWSAccessKeyId").append("=").append(
				creds.getAWSAccessKeyId()).append("&");
		presignedUrl.append("Signature").append("=").append(signature);

		return presignedUrl.toString();
	}

	public Credentials createS3Token(String userId, HttpMethod method,
			String s3Key) throws DatastoreException {

		// Configuration is checked upon startup, we are being extra safe here
		// by re-validating our "contract" with the configuration system to
		// ensure that our invariates hold true
		sanityCheckConfiguration();

		// Append the stack name to the federated username for prod vs. test
		// isolation
		// since we cannot ensure that folks do not use the same user name on
		// various stacks.
		String federatedUserId = IAM_USERNAME_PREFIX + userId;
		if (MAX_FEDERATED_NAME_LENGTH < federatedUserId.length()) {
			federatedUserId = federatedUserId.substring(0,
					MAX_FEDERATED_NAME_LENGTH);
		}

		if(!s3Key.startsWith("/")) {
			s3Key = "/" + s3Key;
		}
		
		int durationSeconds = ((HttpMethod.PUT == method) ? WRITE_ACCESS_EXPIRY_HOURS
				: READ_ACCESS_EXPIRY_HOURS) * 3600;
		String policy = (HttpMethod.PUT == method) ? READWRITE_DATA_POLICY
				: READONLY_DATA_POLICY;
		policy = policy.replace(S3_KEY_PLACEHOLDER, s3Key);

		AWSSecurityTokenService client = amazonClientFactory
				.getAWSSecurityTokenServiceClient();

		GetFederationTokenRequest request = new GetFederationTokenRequest();
		request.setName(federatedUserId);
		request.setDurationSeconds(durationSeconds);
		request.setPolicy(policy);
		GetFederationTokenResult result = client.getFederationToken(request);

		return result.getCredentials();
	}

	/**
	 * Quick tool for base64 encoding a string by hand
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(final String args[]) throws Exception {
		// TODO find a more direct way to go from hex to base64
		byte[] encoded = Base64.encodeBase64(Hex.decodeHex(args[0]
				.toCharArray()));
		String base64Md5 = new String(encoded, "ASCII");
		System.out.println(args[0] + " base64 encoded= " + base64Md5);
	}

	@Override
	public String getS3KeyFromS3Url(String s3Url) {
		if (s3Url.startsWith(S3_URL_PREFIX)) {
			return s3Url.substring(S3_URL_PREFIX.length(), s3Url.indexOf("?"));
		}
		return s3Url;
	}

}
