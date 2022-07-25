package org.sagebionetworks.repo.manager.principal;

import org.sagebionetworks.repo.model.auth.NewUser;

/**
 * Helper for preparing new users.
 * 
 * @author John
 *
 */
public class NewUserUtils {

	/**
	 * Validate and trim a new user.
	 * 
	 * @param newUser
	 * @return
	 */
	public static NewUser validateAndTrim(NewUser newUser){
		if(newUser == null) throw new IllegalArgumentException("NewUser cannot be null");
		if(newUser.getEmail() == null) throw new IllegalArgumentException("New users must provide a valid email address");
		if(newUser.getUserName() == null) throw new IllegalArgumentException("New users must provide a unique username");
		if (newUser.getOauthProvider() != null && newUser.getSubject() == null) {
			throw new IllegalArgumentException("New users registered through an oauth provider must include the provider subject");
		}
		// Trim the email and username.
		newUser.setEmail(newUser.getEmail().trim());
		newUser.setUserName(newUser.getUserName().trim());
		if(newUser.getFirstName() != null){
			newUser.setFirstName(newUser.getFirstName().trim());
		}
		if(newUser.getLastName() != null){
			newUser.setLastName(newUser.getLastName().trim());
		}
		return newUser;
	}
	
}
