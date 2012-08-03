package org.sagebionetworks.repo.web.service;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

public class UserProfileServiceImpl implements UserProfileService {
	
	@Autowired
	UserProfileManager userProfileManager;
	
	@Autowired
	UserManager userManager;
	
	@Autowired
	ObjectTypeSerializer objectTypeSerializer;

	@Override
	public UserProfile getMyOwnUserProfile(String userId) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		// do not allow 'anonymous'
		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(userInfo.getUser().getUserId()))
			throw new UnauthorizedException("Cannot retrieve user profile for anonymous user.");
		return userProfileManager.getUserProfile(userInfo, userInfo.getIndividualGroup().getId());
	}
	

	@Override
	public UserProfile getUserProfileByOwnerId(String userId, String profileId) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return userProfileManager.getUserProfile(userInfo, profileId);
	}
	
	@Override
	public PaginatedResults<UserProfile> getUserProfilesPaginated(HttpServletRequest request,
			String userId, Integer offset, Integer limit, String sort, Boolean ascending)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		long endExcl = offset+limit;
		QueryResults<UserProfile >results = userProfileManager.getInRange(userInfo, offset, endExcl);
		
		return new PaginatedResults<UserProfile>(
				request.getServletPath()+UrlHelpers.USER, 
				results.getResults(),
				(int)results.getTotalNumberOfResults(), 
				offset, 
				limit,
				sort, 
				ascending);
	}
	
	@Override
	public UserProfile updateUserProfile(String userId, HttpHeaders header, 
			String etag, HttpServletRequest request) throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		UserProfile entity = (UserProfile) objectTypeSerializer.deserialize(request.getInputStream(), header, UserProfile.class, header.getContentType());
		if(etag != null){
			entity.setEtag(etag.toString());
		}
		return userProfileManager.updateUserProfile(userInfo, entity);
	}

	@Override
	public S3AttachmentToken createUserProfileS3AttachmentToken(String userId, String profileId, 
			S3AttachmentToken token, HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException, InvalidModelException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return userProfileManager.createS3UserProfileAttachmentToken(userInfo, profileId, token);
	}
	
	/**
	 * Create a token used to upload an attachment.
	 * 
	 * @param userId
	 * @param id
	 * @param token
	 * @param request
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws InvalidModelException
	 */
	@Override
	public PresignedUrl getUserProfileAttachmentUrl(String userId, String profileId,
			PresignedUrl url, HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException, InvalidModelException {
		if(url == null) throw new IllegalArgumentException("A PresignedUrl must be provided");
		// Pass it along.
		UserInfo userInfo = userManager.getUserInfo(userId);
		return userProfileManager.getUserProfileAttachmentUrl(userInfo, profileId, url.getTokenID());
	}
}
