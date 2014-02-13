package org.sagebionetworks.repo.web.service;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.S3TokenManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.S3Token;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * S3Tokens have their very own controller because they are not entities, and
 * not stored. You can imagine we might have something similar for other storage
 * providers over time.
 * 
 * @author deflaux
 * 
 */
public class S3TokenServiceImpl implements S3TokenService {

	@Autowired
	private S3TokenManager s3TokenManager;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private UserManager userManager;

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.S3TokenService#createEntityS3Token(java.lang.String, java.lang.String, org.sagebionetworks.repo.model.S3Token, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public S3Token createEntityS3Token(Long userId, String id, S3Token s3Token,
			HttpServletRequest request) throws DatastoreException,
			NotFoundException, UnauthorizedException, InvalidModelException {

		// Infer one more parameter
		UserInfo userInfo = userManager.getUserInfo(userId);
		EntityType entityType = entityManager.getEntityType(userInfo, id);
		return s3TokenManager.createS3Token(userId, id, s3Token, entityType);
	}
}
