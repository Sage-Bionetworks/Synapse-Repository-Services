package org.sagebionetworks.bridge.service;

import java.io.IOException;

import org.sagebionetworks.bridge.manager.community.CommunityManager;
import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.*;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;

public class CommunityServiceImpl implements CommunityService {
	@Autowired
	private UserManager userManager;
	@Autowired
	private CommunityManager communityManager;
	@Autowired
	private TeamManager teamManager;

	@Override
	public Community create(String userId, Community community) throws DatastoreException, InvalidModelException, UnauthorizedException,
			NotFoundException, NameConflictException, ACLInheritanceException, ServiceUnavailableException, IOException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return communityManager.create(userInfo, community);
	}

	@Override
	public Community get(String userId, String communityId) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return communityManager.get(userInfo, communityId);
	}

	@Override
	public PaginatedResults<Community> getForMember(String userId, int limit, int offset) throws DatastoreException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return communityManager.getForMember(userInfo, limit, offset);
	}

	@Override
	public PaginatedResults<Community> getAll(String userId, int limit, int offset) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return communityManager.getAll(userInfo, limit, offset);
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

	@Override
	public PaginatedResults<UserGroupHeader> getMembers(String userId, String communityId, Integer limit, Integer offset)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		return communityManager.getMembers(userInfo, communityId, limit, offset);
	}

	@Override
	public void joinCommunity(String userId, String communityId) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		communityManager.join(userInfo, communityId);
	}

	@Override
	public void leaveCommunity(String userId, String communityId) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		communityManager.leave(userInfo, communityId);
	}

	@Override
	public void addCommunityAdmin(String userId, String communityId, String memberId) throws DatastoreException,
			NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		communityManager.addAdmin(userInfo, communityId, memberId);
	}

	@Override
	public void removeCommunityAdmin(String userId, String communityId, String memberId) throws DatastoreException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		communityManager.removeAdmin(userInfo, communityId, memberId);
	}
}
