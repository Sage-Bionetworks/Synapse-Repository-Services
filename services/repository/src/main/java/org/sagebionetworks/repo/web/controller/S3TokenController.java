package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServletRequest;
import org.sagebionetworks.repo.manager.EntityManager;

import org.sagebionetworks.repo.manager.S3TokenManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.*;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.web.GenericEntityController;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

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

	@Autowired
	private S3TokenManager s3TokenManager;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private UserManager userManager;

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
	@RequestMapping(value = { UrlHelpers.ENTITY_S3TOKEN},
			method = RequestMethod.POST)
	public @ResponseBody
	S3Token createEntityS3Token(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, @RequestBody S3Token s3Token,
			HttpServletRequest request) throws DatastoreException,
			NotFoundException, UnauthorizedException, InvalidModelException {

		// Infer one more parameter
		UserInfo userInfo = userManager.getUserInfo(userId);
		EntityType entityType = entityManager.getEntityType(userInfo, id);
		return s3TokenManager.createS3Token(userId, id, s3Token, entityType);
	}
}
