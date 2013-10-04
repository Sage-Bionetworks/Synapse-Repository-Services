package org.sagebionetworks.auth.services;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.DBOCrowdMigrationDAO;
import org.sagebionetworks.repo.model.migration.CrowdMigrationResult;
import org.sagebionetworks.repo.model.migration.CrowdMigrationResultType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.beans.factory.annotation.Autowired;

public class CrowdSynchronizerService {

	@Autowired
	private DBOCrowdMigrationDAO crowdMigrationDAO;
	
	@Autowired
	private UserManager userManager;

	/**
	 * Pulls data from Crowd into RDS 
	 * Note: does not create any principals
	 * 
	 * Data includes: 
	 * - Basic profile info (only if the profile does not exist)
	 * - Terms of use acceptance 
	 * - API keys 
	 * - Password hashes
	 * - Group membership 
	 */
	public PaginatedResults<CrowdMigrationResult> migrateSomeUsers(String username, long limit, long offset, String servletPath) {
		// Service is restricted to admins
		try {
			UserInfo userInfo = userManager.getUserInfo(username);
			if (!userInfo.isAdmin()) {
				throw new UnauthorizedException("Must be an admin to use this service");
			}
		} catch (NotFoundException e1) {
			throw new UnauthorizedException("Must be an admin to use this service");
		}
		
		// Keep track of success and errors
		List<CrowdMigrationResult> messages = new ArrayList<CrowdMigrationResult>();
		
		List<User> toBeMigrated = crowdMigrationDAO.getUsersFromCrowd(limit, offset);
		for (User user : toBeMigrated) {
			CrowdMigrationResult result = new CrowdMigrationResult();
			result.setUsername(user.getDisplayName());
			String userId;
			String message;
			
			// Try to migrate and construct the result for each of the three cases (success, failure, abort)
			try {
				userId = crowdMigrationDAO.migrateUser(user);
				if (userId != null) {
					result.setResultType(CrowdMigrationResultType.SUCCESS);
					message = "Successfully migrated " + user.getDisplayName();
					result.setUserId(Long.parseLong(userId));
					
				} else {
					result.setResultType(CrowdMigrationResultType.ABORT);
					message = "User " + user.getDisplayName() + " does not exist in RDS so will not be migrated";
				}
				
			} catch (Exception e) {
				result.setResultType(CrowdMigrationResultType.FAILURE);
				message = "Could not migrate " + user.getDisplayName() + " because:\n";
				message += e + "\n";
	            for (StackTraceElement st : e.getStackTrace()) {
	            	if (st.getClassName().contains("sagebionetworks")) {
	            		message += "  " + st + "\n";
	            	}
	            }
			}
			
			result.setMessage(message);
			messages.add(result);
		}
		
		return new PaginatedResults<CrowdMigrationResult>(
				servletPath + UrlHelpers.ADMIN_MIGRATE_FROM_CROWD,
				messages, crowdMigrationDAO.getCount(), offset, limit, "",
				false);
	}
}
