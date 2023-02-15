package org.sagebionetworks.repo.manager.authentication;

import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.message.MessageTemplate;
import org.sagebionetworks.repo.manager.message.TemplatedMessageSender;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.TotpSecret;
import org.sagebionetworks.repo.model.auth.TotpSecretActivationRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthRecoveryCodes;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthStatus;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthToken;
import org.sagebionetworks.repo.model.auth.TwoFactorState;
import org.sagebionetworks.repo.model.dbo.otp.DBOOtpSecret;
import org.sagebionetworks.repo.model.dbo.otp.OtpSecretDao;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.securitytools.AESEncryptionUtils;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class TwoFactorAuthManagerImpl implements TwoFactorAuthManager {

	public static final long TWO_FA_TOKEN_DURATION_MINS = 10;
	private static final String NOTIFICATION_TEMPLATE_2FA_ENABLED = "message/TwoFaEnabledNotification.html.vtl";
	private static final String NOTIFICATION_TEMPLATE_2FA_DISABLED = "message/TwoFaDisabledNotification.html.vtl";
	
	private TotpManager totpMananger;
	private OtpSecretDao otpDao;
	private AuthenticationDAO authDao;
	private TokenGenerator tokenGenerator;
	private StackConfiguration config;
	private Clock clock;
	private TemplatedMessageSender messageSender;
	private UserProfileManager userProfileManager;

	public TwoFactorAuthManagerImpl(TotpManager totpManager, OtpSecretDao otpDao, AuthenticationDAO authDao, TokenGenerator tokenGenerator, StackConfiguration config, Clock clock, TemplatedMessageSender messageSender, UserProfileManager userProfileManager) {
		this.totpMananger = totpManager;
		this.otpDao = otpDao;
		this.authDao = authDao;
		this.tokenGenerator = tokenGenerator;
		this.config = config;
		this.clock = clock;
		this.messageSender = messageSender;
		this.userProfileManager = userProfileManager;
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
		authDao.setTwoFactorAuthState(userId, true);
		
		send2FaStateChangeNotification(user, TwoFactorState.ENABLED);
	}

	@Override
	public TwoFactorAuthStatus get2FaStatus(UserInfo user) {
		ValidateArgument.required(user, "The user");
		
		if (AuthorizationUtils.isUserAnonymous(user)) {
			return new TwoFactorAuthStatus().setStatus(TwoFactorState.DISABLED);
		}
		
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
		authDao.setTwoFactorAuthState(user.getId(), false);
		
		send2FaStateChangeNotification(user, TwoFactorState.DISABLED);
	}
	
	@Override
	@WriteTransaction
	public boolean validate2FaTotpCode(UserInfo user, String otpCode) {
		assertValidUser(user);
		
		ValidateArgument.requiredNotBlank(otpCode, "The otpCode");
		
		DBOOtpSecret secret = getActiveSecretOrThrow(user);
		
		return isTotpValid(user, secret, otpCode);
		
	}
	
	@Override
	public String generate2FaLoginToken(UserInfo user) {
		assertValidUser(user);
		
		Date now = clock.now();
		Date tokenExpiration = Date.from(now.toInstant().plus(TWO_FA_TOKEN_DURATION_MINS, ChronoUnit.MINUTES)); 
		
		TwoFactorAuthToken token = new TwoFactorAuthToken()
			.setUserId(user.getId())
			.setCreatedOn(now)
			.setExpiresOn(tokenExpiration);
		
		tokenGenerator.signToken(token);
		
		try {
			String tokenJson = EntityFactory.createJSONStringForEntity(token);
			return new String(Base64.getEncoder().encode(tokenJson.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalStateException(e);
		}
	}
	
	@Override
	public boolean validate2FaLoginToken(UserInfo user, String encodedToken) {
		assertValidUser(user);
		
		ValidateArgument.requiredNotBlank(encodedToken, "The token");
		
		try {
			String decodedToken = new String(Base64.getDecoder().decode(encodedToken.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
			TwoFactorAuthToken token = EntityFactory.createEntityFromJSONString(decodedToken, TwoFactorAuthToken.class);
			
			if(!user.getId().equals(token.getUserId())) {
				return false;
			}
			
			tokenGenerator.validateToken(token);
		} catch (JSONObjectAdapterException | UnauthorizedException | IllegalArgumentException e) {
			return false;
		}
		return true;
	}
	
	@Override
	@WriteTransaction
	public TwoFactorAuthRecoveryCodes generate2FaRecoveryCodes(UserInfo user) {
		assertValidUser(user);
		
		DBOOtpSecret secret = getActiveSecretOrThrow(user);
		
		// Remove old codes
		otpDao.deleteRecoveryCodes(secret.getId());
		
		List<String> recoveryCodes = totpMananger.generateRecoveryCodes();
		List<String> recoveryCodesHashed = recoveryCodes.stream()
			.map(recoveryCode -> PBKDF2Utils.hashPassword(recoveryCode, null))
			.collect(Collectors.toList());
		
		otpDao.storeRecoveryCodes(secret.getId(), recoveryCodesHashed);
		
		// For proper migration
		otpDao.touchSecret(secret.getId());
		
		return new TwoFactorAuthRecoveryCodes().setCodes(recoveryCodes);
	}
	
	@Override
	@WriteTransaction
	public boolean validate2FaRecoveryCode(UserInfo user, String recoveryCode) {
		assertValidUser(user);
		
		ValidateArgument.requiredNotBlank(recoveryCode, "The recoveryCode");
		
		DBOOtpSecret secret = getActiveSecretOrThrow(user);
		
		// Each code has a different random salt
		List<String> recoveryCodes = otpDao.getRecoveryCodes(secret.getId());
		
		for (String candidate : recoveryCodes) {
			byte[] salt = PBKDF2Utils.extractSalt(candidate);
			String recoveryCodeHash = PBKDF2Utils.hashPassword(recoveryCode, salt);
			
			// Found the match
			if (candidate.equals(recoveryCodeHash) && otpDao.deleteRecoveryCode(secret.getId(), recoveryCodeHash)) {
				otpDao.touchSecret(secret.getId());
				return true;
			}
		}
		
		return false;
	}
	
	void send2FaStateChangeNotification(UserInfo user, TwoFactorState state) {
		String template = TwoFactorState.ENABLED == state ? NOTIFICATION_TEMPLATE_2FA_ENABLED : NOTIFICATION_TEMPLATE_2FA_DISABLED;
		
		Map<String, Object> context = Map.of(
			"displayName", EmailUtils.getDisplayNameOrUsername(userProfileManager.getUserProfile(user.getId().toString()))
		);
		
		messageSender.sendMessage(MessageTemplate.builder()
			.withNotificationMessage(true)
			.withIncludeProfileSettingLink(true)
			.withIncludeUnsubscribeLink(false)
			.withIgnoreNotificationSettings(true)
			.withSender(new UserInfo(true, AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()))
			.withRecipients(Collections.singleton(user.getId().toString()))
			.withTemplateFile(template)
			.withSubject("Two-Factor Authentication " + StringUtils.capitalize(state.toString().toLowerCase()))
			.withContext(context).build()
		);
		
	}
	
	boolean isTotpValid(UserInfo user, DBOOtpSecret secret, String otpCode) {
		String encryptedSecret = secret.getSecret();
		
		String userEncryptionKey = getUserEncryptionKey(user); 
		
		String unencryptedSecret = AESEncryptionUtils.decryptWithAESGCM(encryptedSecret, userEncryptionKey);
		
		return totpMananger.isTotpValid(unencryptedSecret, otpCode);
	}
	
	DBOOtpSecret getActiveSecretOrThrow(UserInfo user) {
		return otpDao.getActiveSecret(user.getId()).orElseThrow(() -> new IllegalArgumentException("Two factor authentication is not enabled"));
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
