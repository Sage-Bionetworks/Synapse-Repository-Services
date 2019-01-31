package org.sagebionetworks.repo.manager.password;

/**
 * Used to check if a password meets requirements for a secure password
 */
public interface PasswordValidator {
	/**
	 * Checks if the password meets requirements
	 * @param password
	 * @throws InvalidPasswordException when the requirements are not met
	 */
	void validatePassword(String password);
}
