package org.sagebionetworks.repo.manager;

public interface UserCredentialValidator {
	boolean checkPassword(Long principalId, String password);

	boolean checkPasswordWithLock(Long principalId, String password);
}
