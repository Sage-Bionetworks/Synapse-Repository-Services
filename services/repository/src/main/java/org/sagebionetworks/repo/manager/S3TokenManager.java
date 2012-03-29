package org.sagebionetworks.repo.manager;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.S3Token;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Abstraction for the S3TokenManager.
 * 
 * @author John
 *
 */
public interface S3TokenManager {
	
	/**
	 * Create a new S3Token
	 * @param userId
	 * @param id
	 * @param s3Token
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 */
	public 	S3Token createS3Token(String userId, String id, S3Token s3Token, EntityType type) throws DatastoreException,	NotFoundException, UnauthorizedException, InvalidModelException;

	/**
	 * Create a new S3AttachmentToken
	 * @param userId
	 * @param entityId
	 * @param token
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 */
	public S3AttachmentToken createS3AttachmentToken(String userId, String entityId, S3AttachmentToken token) throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException;
	
	
	/**
	 * Create a new pre-signed URL for an attachment.
	 * @param userId
	 * @param entityId
	 * @param tokenId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 */
	PresignedUrl getAttachmentUrl(String userId, String entityId,String tokenId) throws NotFoundException,	DatastoreException, UnauthorizedException, InvalidModelException;


}
