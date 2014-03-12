package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.S3Token;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.service.ServiceProvider;
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
	ServiceProvider serviceProvider;

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
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id, @RequestBody S3Token s3Token,
			HttpServletRequest request) throws DatastoreException,
			NotFoundException, UnauthorizedException, InvalidModelException {
		return serviceProvider.getS3TokenService().createEntityS3Token(userId, id, s3Token, request);
	}
}
