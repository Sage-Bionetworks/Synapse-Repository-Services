package org.sagebionetworks.auth.services;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.semaphore.SemaphoreDao;
import org.sagebionetworks.repo.model.dbo.dao.DBOCrowdMigrationDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class CrowdSynchronizerService {

	@Autowired
	private DBOCrowdMigrationDAO crowdMigrationDAO;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private SemaphoreDao semaphoreDAO;
	
	private static final String SEMAPHORE_KEY = "CrowdSyncService";
	private static final long LOCK_TIME_MS = 60 * 1000;

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
	public List<String> migrateSomeUsers(String username, long limit, long offset) {
		// Service is restricted to admins
		try {
			UserInfo userInfo = userManager.getUserInfo(username);
			if (!userInfo.isAdmin()) {
				throw new UnauthorizedException("Must be an admin to use this service");
			}
		} catch (NotFoundException e1) {
			throw new UnauthorizedException("Must be an admin to use this service");
		}
		
		// Although it may only result in deadlocks
		// This service should not be allowed to run concurrently with itself 
		String semaphoreToken = semaphoreDAO.attemptToAcquireLock(SEMAPHORE_KEY, LOCK_TIME_MS);
		
		// Keep track of success and errors
		List<String> messages = new ArrayList<String>();
		
		List<User> toBeMigrated = crowdMigrationDAO.getUsersFromCrowd(limit, offset);
		for (User user : toBeMigrated) {
			ByteArrayOutputStream resultbuffer = new ByteArrayOutputStream();
			PrintStream resultStream = new PrintStream(resultbuffer);
			try {
				crowdMigrationDAO.migrateUser(user);
				resultStream.println("Successfully migrated " + user.getDisplayName());
			} catch (Exception e) {
				resultStream.println("Could not migrate " + user.getDisplayName() + " because:");

				// Print out some debug info
				resultStream.println(e);
                for (StackTraceElement st : e.getStackTrace()) {
                	if (st.getClassName().contains("sagebionetworks")) {
                		resultStream.println("  " + st);
                	}
                }
			}
			
			messages.add(new String(resultbuffer.toByteArray()));
		}
		
		semaphoreDAO.releaseLock(SEMAPHORE_KEY, semaphoreToken);
		
		return messages;
	}
}
