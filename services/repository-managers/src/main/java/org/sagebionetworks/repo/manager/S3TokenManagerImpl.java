package org.sagebionetworks.repo.manager;

import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.regex.Pattern;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.repo.model.S3Token;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.model.attachment.URLStatus;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.util.LocationHelper;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import com.amazonaws.services.securitytoken.model.Credentials;

/**
 * Creates tokens for S3.
 * 
 * @author deflaux
 */
public class S3TokenManagerImpl implements S3TokenManager {

	// http://docs.amazonwebservices.com/AmazonS3/latest/dev/UsingMetadata.html
	private static final int MAX_S3_KEY_LENGTH = 2048;
	private static final Pattern MD5_REGEX = Pattern.compile("[0-9a-fA-F]{32}");
	private static final String PREVIEW_SUFFIX = "preview.png";
	private static final String DEFAULT_MIME_TYPE = "application/binary";
	private static final FileNameMap FILE_EXTENSION2MIME_TYPE_MAP = URLConnection
			.getFileNameMap();
	
	@Autowired
	private EntityPermissionsManager entityPermissionsManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private LocationHelper locationHelper;
	@Autowired
	AmazonS3Utility s3Utility;
	/**
	 * This constructor is used by Spring and integration tests.
	 */
	public S3TokenManagerImpl(){};
	/**
	 * This version is used for unit test.
	 * @param permissionsManager
	 * @param userManager
	 * @param idGenerator
	 * @param locationHelper
	 */
	public S3TokenManagerImpl(EntityPermissionsManager permissionsManager,
			UserManager userManager, IdGenerator idGenerator,
			LocationHelper locationHelper, AmazonS3Utility s3Utility) {
		super();
		this.entityPermissionsManager = permissionsManager;
		this.userManager = userManager;
		this.idGenerator = idGenerator;
		this.locationHelper = locationHelper;
		this.s3Utility = s3Utility;
	}

	/**
	 * This could be accomplished with our schema validation, be we have to do
	 * so much other customized validation, let's keep this here for
	 * completeness
	 * 
	 * @param s3Token
	 * @throws InvalidModelException
	 */
	void validateMd5(S3Token s3Token) throws InvalidModelException {
		if (null == s3Token.getMd5())
			throw new IllegalArgumentException("S3Token md5 cannot be null");
		validateMd5(s3Token.getMd5());
	}

	/**
	 * Validate the md5
	 * 
	 * @param md5
	 * @throws InvalidModelException
	 */
	void validateMd5(String md5) throws InvalidModelException {
		if (null == md5)
			throw new IllegalArgumentException("md5 cannot be null");
		if (!MD5_REGEX.matcher(md5).matches()) {
			throw new InvalidModelException(
					"md5sum is malformed, it must be a 32 digit hexadecimal string");
		}
	}

	/**
	 * We expect that users typically will not provide a mime type, we look at
	 * the file extension here to pick one if needed
	 */
	void validateContentType(S3Token s3Token) {
		if (null == s3Token.getContentType()) {
			String mimeType = validateContentType(s3Token.getPath());
			s3Token.setContentType(mimeType);
		}
	}

	/**
	 * Validate the content type.
	 * 
	 * @param name
	 * @return
	 */
	String validateContentType(String name) {
		if (name == null)
			return null;
		String mimeType = FILE_EXTENSION2MIME_TYPE_MAP.getContentTypeFor(name);
		if (null == mimeType) {
			mimeType = DEFAULT_MIME_TYPE;
		}
		return mimeType;
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

		String pathPrefix = "/" + KeyFactory.stringToKey(entityId) + "/"
				+ idGenerator.generateNewId();

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

	/**
	 * Dev Note: since the user has update permission, we do not need to check
	 * whether they have signed the use agreement, also this is just for uploads
	 * 
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	void validateUpdateAccess(UserInfo userInfo, String entityId)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		if (!entityPermissionsManager.hasAccess(entityId,
				ACCESS_TYPE.UPDATE, userInfo)) {
			throw new UnauthorizedException(
					"update access is required to obtain an S3Token for entity "
							+ entityId);
		}
	}

	@Override
	public S3Token createS3Token(String userId, String id, S3Token s3Token,
			EntityType type) throws DatastoreException, NotFoundException,
			UnauthorizedException, InvalidModelException {
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
		UserInfo userInfo = userManager.getUserInfo(userId);
		validateUpdateAccess(userInfo, id);
		validateMd5(s3Token);
		validateContentType(s3Token);
		validatePath(id, s3Token);
		
		// Replace all non-url chars
		String path = s3Token.getPath();
		path = SpecialUrlEncoding.replaceUrlCharsIgnoreSlashes(path);
		s3Token.setPath(path);

		// Generate session credentials (needed for multipart upload)
		Credentials sessionCredentials = locationHelper
				.createFederationTokenForS3(userId, HttpMethod.PUT,
						s3Token.getPath());
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
	
	@Override
	public S3AttachmentToken createS3AttachmentToken(String userId, String entityId,
			S3AttachmentToken token) throws NotFoundException,
			DatastoreException, UnauthorizedException, InvalidModelException {
		// Wrap it up and pass it along
		// Manipulate the pass-in S3 token to be correct
		validateMd5(token.getMd5());
		String contentType = validateContentType(token.getFileName());
		token.setContentType(contentType);
		// Issue a new id for this entity.
		String tokenId = createTokenId(idGenerator.generateNewId(), token.getFileName());
		// The path of any attachment is is simply the entity-id/token-id
		
		String path=createAttachmentPathSlash(entityId, tokenId);
		// Generate session credentials (needed for multipart upload)
		Credentials sessionCredentials = locationHelper
				.createFederationTokenForS3(userId, HttpMethod.PUT, path);
		// Generate the presigned url (needed for regular upload)
		String presignedUrl = locationHelper.presignS3PUTUrl(
				sessionCredentials, path, token.getMd5(),
				token.getContentType());
		
		token.setPresignedUrl(presignedUrl);
		token.setTokenId(tokenId);
		return token;
	}
	
	/**
	 * Create a token using a uniqueId and a filename.
	 * @param id
	 * @param fileName
	 * @return
	 */
	public static String createTokenId(Long id, String fileName){
		if(id == null) throw new IllegalArgumentException("Id cannot be null");
		if(fileName == null) throw new IllegalArgumentException("Name cannot be null");
		// Replace all non-url chars
		fileName = SpecialUrlEncoding.replaceUrlChars(fileName);
		return id.toString()+"/"+fileName;
	}
		
	/**
	 * Create an attachment path.
	 * @param entityId
	 * @param tokenId
	 * @return
	 * @throws DatastoreException
	 */
	public static String createAttachmentPathSlash(String entityId, String tokenId) throws DatastoreException{
		return "/"+createAttachmentPathNoSlash(entityId, tokenId);
	}
	
	/**
	 * This version does not have a slash
	 * @param entityId
	 * @param tokenId
	 * @return
	 * @throws DatastoreException
	 */
	public static String createAttachmentPathNoSlash(String entityId, String tokenId) throws DatastoreException{
		return KeyFactory.stringToKey(entityId) + "/" + tokenId;
	}

	@Override
	public PresignedUrl getAttachmentUrl(String userId, String entityId, String tokenId) throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return getAttachmentUrl(userInfo, entityId, tokenId);
	}
	
	@Override
	public PresignedUrl getAttachmentUrl(UserInfo user, String entityId,
			String tokenId) throws NotFoundException, DatastoreException,
			UnauthorizedException, InvalidModelException {
		return presignedUrl(user, entityId, tokenId, false);
	}
	
	/**
	 * Generates the actual presigned URL.
	 * @param userId
	 * @param entityId
	 * @param tokenId
	 * @param isPreview
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	private PresignedUrl presignedUrl(UserInfo user, String entityId, String tokenId, boolean isPreview) throws DatastoreException, NotFoundException, UnauthorizedException{
		if(tokenId == null) throw new IllegalArgumentException("TokenId cannot be null");
		// First determine if this exists
		String pathNoSlash = createAttachmentPathNoSlash(entityId, tokenId);
		if(!s3Utility.doesExist(pathNoSlash)){
			PresignedUrl url = new PresignedUrl();
			url.setPresignedUrl(null);
			url.setStatus(URLStatus.DOES_NOT_EXIST);
			return url;
		}
		// The path of any attachment is is simply the entity-id/token-id
		String path = createAttachmentPathSlash(entityId, tokenId);
		// Generate the presigned url for download
		String presignedUrl = locationHelper.presignS3GETUrlShortLived(user.getUser().getId(), path);
		PresignedUrl url = new PresignedUrl();
		url.setPresignedUrl(presignedUrl);
		url.setTokenID(tokenId);
		url.setStatus(URLStatus.READ_FOR_DOWNLOAD);
		return url;
	}

	
}
