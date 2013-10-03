package org.sagebionetworks.auth.services;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.dbo.dao.DBOCrowdMigrationDAO;
import org.springframework.beans.factory.annotation.Autowired;

public class CrowdSynchronizerService {

	@Autowired
	private DBOCrowdMigrationDAO crowdMigrationDAO;

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
	public List<String> migrateSomeUsers(long limit, long offset) {
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
				e.printStackTrace(resultStream);
			}
			
			messages.add(new String(resultbuffer.toByteArray()));
		}
		
		return messages;
	}
}
