package org.sagebionetworks.repo.web.controller;

import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.manager.PermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.repo.model.S3Token;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.util.LocationHelper;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.ServiceConstants;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.amazonaws.services.securitytoken.model.Credentials;

/**
 * S3Tokens have their very own controller because they are not entities, and
 * not stored. You can imagine we might have something similar for other storage
 * providers over time.
 * 
 * @author deflaux
 * 
 */
@Controller
public class S3TokenController extends BaseController {

	// http://docs.amazonwebservices.com/AmazonS3/latest/dev/UsingMetadata.html
	private static final int MAX_S3_KEY_LENGTH = 2048;
	private static final Pattern MD5_REGEX = Pattern.compile("[0-9a-fA-F]{32}");
	private static final String DEFAULT_MIME_TYPE = "application/binary";
	private static final FileNameMap FILE_EXTENSION2MIME_TYPE_MAP = URLConnection
			.getFileNameMap();

	@Autowired
	private PermissionsManager permissionsManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private LocationHelper locationHelper;

	/**
	 * Create a security token for use for a particular with a particular
	 * locationable entity to be stored in AWS S3
	 * 
	 * @param userId
	 * @param id
	 * @param etag
	 * @param s3Token
	 * @param request
	 * @return a filled-in S3Token
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 */
	@Transactional(readOnly = false)
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = { UrlHelpers.DATASET_S3TOKEN,
			UrlHelpers.LAYER_S3TOKEN, UrlHelpers.CODE_S3TOKEN }, method = RequestMethod.POST)
	public @ResponseBody
	S3Token createS3Token(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, @RequestBody S3Token s3Token,
			HttpServletRequest request) throws DatastoreException,
			NotFoundException, UnauthorizedException, InvalidModelException {

		// Infer one more parameter
		EntityType type = EntityType.getFirstTypeInUrl(request.getRequestURI());

		// Validate the parameters
		if (userId == null)
			throw new IllegalArgumentException("UserId cannot be null");
		if (id == null)
			throw new IllegalArgumentException("EntityId cannot be null");
		if (s3Token == null)
			throw new IllegalArgumentException("S3Token cannot be null");
		if (!Locationable.class.isAssignableFrom(type.getClassForType())) {
			throw new IllegalArgumentException(
					"Cannot generate S3Tokens for non-Locationable entities");
		}

		// Manipulate the pass-in S3 token to be correct
		validateAccess(userId, id);
		validateMd5(s3Token);
		validateContentType(s3Token);
		validatePath(id, s3Token);

		// Generate session credentials (needed for multipart upload)
		Credentials sessionCredentials = locationHelper
				.createFederationTokenForS3(userId, HttpMethod.PUT, s3Token
						.getPath());
		s3Token.setAccessKeyId(sessionCredentials.getAccessKeyId());
		s3Token.setSecretAccessKey(sessionCredentials.getSecretAccessKey());
		s3Token.setSessionToken(sessionCredentials.getSessionToken());

		// Generate the presigned url (needed for regular upload)
		String presignedUrl = locationHelper.presignS3PUTUrl(
				sessionCredentials, s3Token.getPath(), s3Token.getMd5(),
				s3Token.getContentType());
		s3Token.setPresignedUrl(presignedUrl);

		// Set the destination bucket
		s3Token.setBucket(StackConfiguration.getS3Bucket());
		
		return s3Token;
	}

	/**
	 * Dev Note: since the user has update permission, we do not need to check
	 * whether they have signed the use agreement, also this is just for uploads
	 * 
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	private void validateAccess(String userId, String entityId)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		if (!permissionsManager.hasAccess(entityId,
				AuthorizationConstants.ACCESS_TYPE.UPDATE, userInfo)) {
			throw new UnauthorizedException(
					"update access is required to obtain an S3Token for entity "
							+ entityId);
		}
	}

	/**
	 * This could be accomplished with our schema validation, be we have to do
	 * so much other customized validation, let's keep this here for
	 * completeness
	 * 
	 * @param s3Token
	 * @throws InvalidModelException
	 */
	private void validateMd5(S3Token s3Token) throws InvalidModelException {
		if (null == s3Token.getMd5())
			throw new IllegalArgumentException("S3Token md5 cannot be null");
		if (!MD5_REGEX.matcher(s3Token.getMd5()).matches()) {
			throw new InvalidModelException(
					"md5sum is malformed, it must be a 32 digit hexadecimal string");
		}
	}

	/**
	 * We expect that users typically will not provide a mime type, we look at
	 * the file extension here to pick one if needed
	 */
	private void validateContentType(S3Token s3Token) {
		if (null == s3Token.getContentType()) {
			String mimeType = FILE_EXTENSION2MIME_TYPE_MAP
					.getContentTypeFor(s3Token.getPath());
			if (null == mimeType) {
				mimeType = DEFAULT_MIME_TYPE;
			}
			s3Token.setContentType(mimeType);
		}
	}

	/**
	 * Set a system-controlled path prefix on this entity so that (1) we ensure
	 * uniqueness for S3 keys and (2) it has some relationship back to the
	 * owning entity so that we can correctly enforce authorization
	 * 
	 * @throws InvalidModelException
	 * @throws DatastoreException 
	 */
	private void validatePath(String entityId, S3Token s3Token)
			throws InvalidModelException, DatastoreException {

		if (null == s3Token.getPath())
			throw new IllegalArgumentException("S3Token path cannot be null");

		String pathPrefix = "/" + KeyFactory.stringToKey(entityId) + "/" + idGenerator.generateNewId();

		// If this is an update, the user may have passed an S3 URL from a
		// prior GET of a location, scrub the S3 stuff out of the URL. This
		// will have no effect if the path is not an S3 URL
		String path = locationHelper.getS3KeyFromS3Url(s3Token.getPath());

		if (path.startsWith(pathPrefix)) {
			s3Token.setPath(path);
		} else {
			String s3Key = pathPrefix
					+ (s3Token.getPath().startsWith("/") ? s3Token.getPath()
							: "/" + s3Token.getPath());
			s3Token.setPath(s3Key);
		}

		if (MAX_S3_KEY_LENGTH < s3Token.getPath().length()) {
			throw new InvalidModelException("path is too long for S3");
		}
	}
}
