package org.sagebionetworks.repo.web.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.beans.factory.annotation.Autowired;

public class UserGroupServiceImpl implements UserGroupService {
	
	@Autowired
	UserManager userManager;
	
	@Override
	public PaginatedResults<UserGroup> getUserGroups(HttpServletRequest request,
			String userId, Integer offset, Integer limit, String sort, Boolean ascending) 
			throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		long endExcl = offset+limit;
		List<UserGroup> results = userManager.getGroupsInRange(userInfo, offset, endExcl, sort, ascending);
		int totalNumberOfResults = userManager.getGroups().size();
		return new PaginatedResults<UserGroup>(
				request.getServletPath()+UrlHelpers.USERGROUP, 
				results,
				totalNumberOfResults, 
				offset, 
				limit,
				sort, 
				ascending);
	}
}
