package org.sagebionetworks.bridge.service;

import org.sagebionetworks.bridge.manager.community.CommunityManager;
import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.*;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class CommunityServiceImpl implements CommunityService {
	@Autowired
	private UserManager userManager;

	@Autowired
	private CommunityManager communityManager;

	@Override
	public Community create(String userId, Community community) throws DatastoreException, InvalidModelException, UnauthorizedException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return communityManager.create(userInfo, community);
	}

	@Override
	public Community get(String userId, String communityId) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return communityManager.get(userInfo, communityId);
	}

	@Override
	public Community update(String userId, Community community) throws InvalidModelException, DatastoreException, UnauthorizedException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return communityManager.update(userInfo, community);
	}

	@Override
	public void delete(String userId, String communityId) throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		communityManager.delete(userInfo, communityId);
	}
}
