package org.sagebionetworks.repo.model.dbo.otp;

import java.util.List;
import java.util.Optional;

public interface OtpSecretDao {
	
	/**
	 * Stores the given secret for the user, the secret will be inactive
	 * 
	 * @param userId 
	 * @param secret
	 * @return The id of the secret
	 */
	DBOOtpSecret storeSecret(Long userId, String secret);
	
	/**
	 * @param userId
	 * @param secretId
	 * @return The secret that matches the given id for the user
	 */
	Optional<DBOOtpSecret> getSecret(Long userId, Long secretId);
	
	/**
	 * @param userId
	 * @return The active otp secret for the user if any
	 */
	Optional<DBOOtpSecret> getActiveSecret(Long userId);
	
	/**
	 * @param userId
	 * @return True if the user has an active secret
	 */
	boolean hasActiveSecret(Long userId);
	
	/**
	 * Activates the given secret
	 * 
	 * @param id
	 */
	DBOOtpSecret activateSecret(Long userId, Long secretId);
	
	/**
	 * Deletes the given user secret
	 * 
	 * @param userId
	 * @param secretId
	 */
	void deleteSecret(Long userId, Long secretId);
		
	/**
	 * Deletes all the user secrets
	 * @param userId
	 */
	void deleteSecrets(Long userId);
	
	/**
	 * Updates the etag of the secret with the given id
	 * 
	 * @param secretId
	 */
	void touchSecret(Long secretId);
	
	/**
	 * Stores the give list of recovery codes for the given secret, just store an hash of each recovery code
	 * 
	 * @param secretId
	 * @param recoveryCodes
	 */
	void storeRecoveryCodes(Long secretId, List<String> recoveryCodes);
	
	/**
	 * Deletes the recovery code associated with the given secret id
	 * 
	 * @param secretId
	 * @param recoveryCode
	 * @return True if a recoveryCode associated with the given secret was deleted, false otherwise
	 */
	boolean deleteRecoveryCode(Long secretId, String recoveryCode);
	
	/**
	 * Deletes all the recovery codes associated with the given secret id
	 * 
	 * @param secretId
	 */
	void deleteRecoveryCodes(Long secretId);
	
	/**
	 * @param secretId
	 * @return The list of recovery codes for the given secret id
	 */
	List<String> getRecoveryCodes(Long secretId);
	
	// For testing
	
	void truncateAll();

}
