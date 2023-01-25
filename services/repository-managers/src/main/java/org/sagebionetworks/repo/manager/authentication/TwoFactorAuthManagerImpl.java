package org.sagebionetworks.repo.manager.authentication;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.TotpSecret;
import org.sagebionetworks.repo.model.auth.TotpSecretActivationRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthOtpType;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthStatus;
import org.sagebionetworks.repo.model.auth.TwoFactorState;
import org.sagebionetworks.repo.model.dbo.otp.DBOOtpSecret;
import org.sagebionetworks.repo.model.dbo.otp.OtpSecretDao;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.securitytools.AESEncryptionUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class TwoFactorAuthManagerImpl implements TwoFactorAuthManager {
	
	private TotpManager totpMananger;
	private OtpSecretDao otpDao;
	private StackConfiguration config;

	public TwoFactorAuthManagerImpl(TotpManager totpManager, OtpSecretDao otpDao, StackConfiguration config) {
		this.totpMananger = totpManager;
		this.otpDao = otpDao;
		this.config = config;
	}

	@Override
	@WriteTransaction
	public TotpSecret init2Fa(UserInfo user) {		
		assertValidUser(user);
		
		String unencryptedSecret = totpMananger.generateTotpSecret();
		
		String userEncryptionKey = getUserEncryptionKey(user);
		
		String encryptedSecret = AESEncryptionUtils.encryptWithAESGCM(unencryptedSecret, userEncryptionKey);
		
		Long secretId = otpDao.storeSecret(user.getId(), encryptedSecret).getId();

		return new TotpSecret()
			.setSecretId(secretId.toString())
			.setSecret(unencryptedSecret)
			.setAlg(TotpManager.HASH_ALG.getFriendlyName())
			.setDigits(Long.valueOf(TotpManager.DIGITS_COUNT))
			.setPeriod(Long.valueOf(TotpManager.PERIOD));
	}

	@Override
	@WriteTransaction
	public void enable2Fa(UserInfo user, TotpSecretActivationRequest request) {
		assertValidUser(user);
		
		ValidateArgument.required(request, "The request");
		ValidateArgument.required(request.getSecretId(), "The secret id");
		ValidateArgument.required(request.getTotp(), "The totp code");
		
		Long userId = user.getId();
		Long secretId = Long.valueOf(request.getSecretId());
		
		DBOOtpSecret secret = otpDao.getSecret(userId, secretId)
			.orElseThrow(() -> new UnauthorizedException("Invalid secret id"));
		
		if (secret.getActive()) {
			throw new IllegalArgumentException("Two factor authentication is already enabled with this secret");
		}
		
		if (!isTotpValid(user, secret, request.getTotp())) {
			throw new IllegalArgumentException("Invalid totp code");
		}
		
		// If the user has a secret already in use, delete it first
		otpDao.getActiveSecret(userId).ifPresent( existingSecret -> otpDao.deleteSecret(userId, existingSecret.getId()));
		otpDao.activateSecret(userId, secretId);
	}

	@Override
	public TwoFactorAuthStatus get2FaStatus(UserInfo user) {
		assertValidUser(user);
		
		TwoFactorState state = otpDao.hasActiveSecret(user.getId()) ? TwoFactorState.ENABLED : TwoFactorState.DISABLED;
		
		return new TwoFactorAuthStatus().setStatus(state);
	}

	@Override
	@WriteTransaction
	public void disable2Fa(UserInfo user) {
		assertValidUser(user);
		
		if (!otpDao.hasActiveSecret(user.getId())) {
			throw new IllegalArgumentException("Two factor authentication is not enabled");
		}
		
		otpDao.deleteSecrets(user.getId());
	}
	
	@Override
	public boolean is2FaCodeValid(UserInfo user, TwoFactorAuthOtpType otpType, String otpCode) {
		assertValidUser(user);
		
		ValidateArgument.required(otpType, "The otpType");
		ValidateArgument.requiredNotBlank(otpCode, "The otpCode");
		
		DBOOtpSecret secret = otpDao.getActiveSecret(user.getId()).orElseThrow(() -> new IllegalArgumentException("Two factor authentication is not enabled"));
		
		switch (otpType) {
		case TOTP:
			return isTotpValid(user, secret, otpCode);
		default:
			throw new UnsupportedOperationException("2FA code type " + otpType + " not supported yet.");
		}
		
	}
	
	boolean isTotpValid(UserInfo user, DBOOtpSecret secret, String otpCode) {
		String encryptedSecret = secret.getSecret();
		
		String userEncryptionKey = getUserEncryptionKey(user); 
		
		String unencryptedSecret = AESEncryptionUtils.decryptWithAESGCM(encryptedSecret, userEncryptionKey);
		
		return totpMananger.isTotpValid(unencryptedSecret, otpCode);
	}
	
	/**
	 * @param user
	 * @return User encryption key derived from a password. Uses the user id as the salt. 
	 */
	String getUserEncryptionKey(UserInfo user) {
		return AESEncryptionUtils.newSecretKeyFromPassword(config.getOtpSecretsPassword(), user.getId().toString());
	}

	void assertValidUser(UserInfo user) {
		ValidateArgument.required(user, "The user");
		if (AuthorizationUtils.isUserAnonymous(user)) {
			throw new UnauthorizedException("You need to authenticate to perform this action");
		}
	}
}
