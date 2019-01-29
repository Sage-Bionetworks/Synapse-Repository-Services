package org.sagebionetworks.repo.manager;

/**
 * Used to check if password is a commonly used passwords, which would likely be used in a dictionary attack
 */
public interface BannedPasswords {
	boolean isPasswordBanned(String password);
}
