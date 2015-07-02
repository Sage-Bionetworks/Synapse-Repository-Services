package org.sagebionetworks.repo.util;

import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTime;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
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
	// http://docs.amazonwebservices.com/STS/latest/APIReference/API_GetFederationToken.html
	private static final int MAX_POLICY_LENGTH = 2048;
	private static final int CACHE_SIZE = 4096;

	// Cache for presigned Urls so that (1) we do not beat up STS & get throttled and (2) to help with caching proxies downstream.
	// Note that the cache key must be userId+s3Key+method because its not okay to give out urls signed for one user to another user.
	// This cache is far from perfect, it can get spoiled by folks doing a select * on a large number of locationable 
	// entities since we are still presigning urls when returned as part of a query.
	private static final Map<PresignedUrlCacheKey, PresignedUrlCacheValue> URL_CACHE = Collections
			.synchronizedMap(new LruCache<PresignedUrlCacheKey, PresignedUrlCacheValue>(
					CACHE_SIZE));

	private static final int READ_ACCESS_EXPIRY_HOURS = StackConfiguration
			.getS3ReadAccessExpiryHours();
	// For access URLs that expire in hours (the method expects an time in seconds)
	private static final int READ_ACCESS_EXPIRY_HOURS_IN_SECONDS = READ_ACCESS_EXPIRY_HOURS*60*60;
	private static final int READ_ACCESS_EXPIRY_SECONDS = StackConfiguration
	.getS3ReadAccessExpirySeconds();
	private static final int WRITE_ACCESS_EXPIRY_HOURS = StackConfiguration
			.getS3WriteAccessExpiryHours();
	private static final String S3_DOMAIN = "s3.amazonaws.com";
	private static final String CANONICALIZED_ACL_HEADER = "x-amz-acl:bucket-owner-full-control";
	private static final String SECURITY_TOKEN_HEADER = "x-amz-security-token";
	private static final String S3_BUCKET = StackConfiguration.getS3Bucket();
	private static final String S3_URL_PREFIX = "https://" + S3_DOMAIN + "/"
			+ S3_BUCKET;
	private static final Pattern ENTITY_FROM_S3KEY_REGEX = Pattern.compile("^(" + S3_URL_PREFIX + ")?(/)?(\\d+)/.*$");
	private static final String STACK = StackConfiguration.singleton().getStack();
	private static final String FEDERATED_USERNAME_PREFIX = STACK + "-";
	private static final String ENTITY_ID_PLACEHOLDER = "REPLACE_ME_WITH_AN_ENTITY_ID";
	private static final String READONLY_DATA_POLICY = "{\"Statement\": [{\"Effect\": \"Allow\", \"Action\": \"s3:GetObject\",\"Resource\": \"arn:aws:s3:::"
			+ S3_BUCKET + "/" + ENTITY_ID_PLACEHOLDER + "/*\"}]}";
	private static final String READWRITE_DATA_POLICY = "{\"Statement\": [{\"Effect\": \"Allow\", \"Action\": \"s3:PutObject\",\"Resource\": \"arn:aws:s3:::"
			+ S3_BUCKET + "/" + ENTITY_ID_PLACEHOLDER + "/*\"}]}";

	@Autowired
	private AmazonClientFactory amazonClientFactory;
	
	/**
	 * Default constructor
	 */
	public LocationHelpersImpl() {
		// Configuration is checked upon startup, we are being extra safe here
		// by re-validating our "contract" with the configuration system to
		// ensure that our invariates hold true
		validateConfiguration();
	}
	
	/**
	 * @param amazonClientFactory
	 */
	public LocationHelpersImpl(AmazonClientFactory amazonClientFactory) {
		this.amazonClientFactory = amazonClientFactory;
		// Configuration is checked upon startup, we are being extra safe here
		// by re-validating our "contract" with the configuration system to
		// ensure that our invariates hold true
		validateConfiguration();
	}

	private void validateConfiguration() {
		if (!S3_BUCKET.startsWith(STACK)) {
			throw new IllegalArgumentException("Invalid configuration: stack name "
					+ STACK + " does not match S3 bucket " + S3_BUCKET);
		}
	}

	@Override
	public String presignS3GETUrl(Long userId, String s3Key)
			throws DatastoreException {
		return getS3Url(userId, s3Key, HttpMethod.GET, READ_ACCESS_EXPIRY_HOURS_IN_SECONDS);
	}

	@Override
	public String presignS3GETUrl(Long userId, String s3Key, int expiresSeconds)
			throws DatastoreException {
		return getS3Url(userId, s3Key, HttpMethod.GET, expiresSeconds);
	}
	
	@Override
	public String presignS3HEADUrl(Long userId, String s3Key)
			throws DatastoreException {
		return getS3Url(userId, s3Key, HttpMethod.HEAD, READ_ACCESS_EXPIRY_HOURS_IN_SECONDS);
	}
	
	@Override
	public String presignS3HEADUrl(Long userId, String s3Key, int expiresSeconds)
			throws DatastoreException {
		return getS3Url(userId, s3Key, HttpMethod.HEAD,	expiresSeconds);
	}
	
	@Override
	public String presignS3GETUrlShortLived(Long userId, String s3Key)
			throws DatastoreException {
		return getS3Url(userId, s3Key, HttpMethod.GET, READ_ACCESS_EXPIRY_SECONDS);
	}

	private String getS3Url(Long userId, String s3Key, HttpMethod method,
			int expiresSeconds) throws DatastoreException {

		DateTime now = new DateTime();

		// Check the cache first
		PresignedUrlCacheKey key = new PresignedUrlCacheKey(userId, s3Key,
				method.name());
		PresignedUrlCacheValue value = URL_CACHE.get(key);
		if (null != value) {
			// if url is not too stale, reuse it
			DateTime minimumTimeLeft = now.plusSeconds(expiresSeconds / 2);
			if (value.getExpires().isAfter(minimumTimeLeft)) {
				return value.getUrl();
			}
		}

		// Get the credentials with which to sign the request
		Credentials token = createFederationTokenForS3(userId, method, s3Key);
		AWSCredentials creds = new BasicAWSCredentials(token.getAccessKeyId(),
				token.getSecretAccessKey());

		DateTime expires = now.plusSeconds(expiresSeconds);
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

		String presignedUrl = sign(buf.toString(), creds, s3Key,
				expirationInSeconds, token.getSessionToken());

		// Add this to the cache
		value = new PresignedUrlCacheValue(presignedUrl, expires);
		URL_CACHE.put(key, value);

		return presignedUrl;
	}

	@Override
	public String presignS3PUTUrl(Long userId, String s3Key, String md5,
			String contentType) throws DatastoreException {

		// Get the credentials with which to sign the request
		Credentials sessionCredentials = createFederationTokenForS3(userId, HttpMethod.PUT, s3Key);
		return presignS3PUTUrl(sessionCredentials, s3Key, md5, contentType);
	}
	
	/**
	 * Create presigned S3 PUT URLs
	 * 
	 * MD5 checks and custom x-amz headers are not supported by the AWS Java SDK
	 * so I had to implement signing from scratch. Details here:
	 * http://s3.amazonaws.com/doc/s3-developer-guide/RESTAuthentication.html
	 */
	@Override
	public String presignS3PUTUrl(Credentials sessionCredentials, String s3Key, String md5,
			String contentType) throws DatastoreException {

		AWSCredentials creds = new BasicAWSCredentials(sessionCredentials.getAccessKeyId(),
				sessionCredentials.getSecretAccessKey());

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
				sessionCredentials.getSessionToken()).append("\n");
		buf.append("/").append(S3_BUCKET).append(s3Key);

		return sign(buf.toString(), creds, s3Key, expirationInSeconds, sessionCredentials
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


	@Override
	public Credentials createFederationTokenForS3(Long userId, HttpMethod method,
			String s3Key) throws NumberFormatException, DatastoreException {

		// Append the stack name to the federated username for prod vs. test
		// isolation
		// since we cannot ensure that folks do not use the same user name on
		// various stacks.
		String federatedUserId = FEDERATED_USERNAME_PREFIX + userId;
		if (MAX_FEDERATED_NAME_LENGTH < federatedUserId.length()) {
			federatedUserId = federatedUserId.substring(0,
					MAX_FEDERATED_NAME_LENGTH);
		}

		// Parse out the entity id from the url
		String entityId = getEntityIdFromS3Url(s3Key);
		
		int durationSeconds = ((HttpMethod.PUT == method) ? WRITE_ACCESS_EXPIRY_HOURS
				: READ_ACCESS_EXPIRY_HOURS) * 3600;
		String policy = (HttpMethod.PUT == method) ? READWRITE_DATA_POLICY
				: READONLY_DATA_POLICY;
		// To avoid having to move all of our s3 data, use the entity id without the prefix
		policy = policy.replace(ENTITY_ID_PLACEHOLDER, KeyFactory.stringToKey(entityId).toString());
		if(MAX_POLICY_LENGTH < policy.length()) {
			throw new IllegalArgumentException("Security token policy too long: " + policy);
		}

		AWSSecurityTokenService client = amazonClientFactory
				.getAWSSecurityTokenServiceClient();

		GetFederationTokenRequest request = new GetFederationTokenRequest();
		request.setName(federatedUserId);
		request.setDurationSeconds(durationSeconds);
		request.setPolicy(policy);
		GetFederationTokenResult result = client.getFederationToken(request);

		return result.getCredentials();
	}

	@Override
	public String getS3KeyFromS3Url(String s3Url) {
		if (s3Url.startsWith(S3_URL_PREFIX)) {
			return s3Url.substring(S3_URL_PREFIX.length(), s3Url.indexOf("?"));
		}
		return s3Url;
	}
	
	@Override 
	public String getEntityIdFromS3Url(String s3Url) throws NumberFormatException, DatastoreException {
		Matcher matcher = ENTITY_FROM_S3KEY_REGEX.matcher(s3Url);
		if(!matcher.matches()) {
			throw new IllegalArgumentException("s3 url or key is malformed " + s3Url);
		}
		// To avoid having to move all of our s3 data, use the entity id without the prefix
		return KeyFactory.keyToString(Long.parseLong(matcher.group(3)));
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



}
