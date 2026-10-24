package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.repo.model.ErrorResponseCode;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.AccessTokenGenerationRequest;
import org.sagebionetworks.repo.model.auth.RedactProfileRequest;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.TotpSecret;
import org.sagebionetworks.repo.model.auth.TotpSecretActivationRequest;
import org.sagebionetworks.repo.model.auth.TwoFactorAuthRecoveryCodes;
import org.sagebionetworks.repo.model.auth.TwoFactorState;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.exceptions.TimeProviderException;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

@ExtendWith(ITTestExtension.class)
public class ITRedactOwnProfile {

	private CodeGenerator totpGenerator = new DefaultCodeGenerator();
	private TimeProvider timeProvider = new SystemTimeProvider();

	@Test
	public void testRedactOwnProfileWithTotp(SynapseAdminClient adminClient) throws SynapseException, JSONObjectAdapterException {
		String username = UUID.randomUUID().toString();
		String password = "password" + UUID.randomUUID().toString();
		SynapseClient userClient = new SynapseClientImpl();
		Long userId = SynapseClientHelper.createUser(adminClient, userClient, username, password, true, false);

		try {
			UserProfile profile = userClient.getMyProfile();
			profile.setFirstName("First");
			profile.setLastName("Last");
			profile.setCompany("Organization");
			userClient.updateMyProfile(profile);

			// Content created by the user is shared with others and must not be deleted when the profile is redacted
			Project project = new Project();
			project.setName(UUID.randomUUID().toString());
			String projectId = userClient.createEntity(project).getId();

			String secret = enable2Fa(userClient);

			// The account is still active, a wrong code does not redact it
			assertThrows(SynapseUnauthorizedException.class, () -> {
				userClient.redactOwnProfile(new RedactProfileRequest().setOtpCode("000000"));
			});
			assertEquals(username, adminClient.getUserProfile(userId.toString()).getUserName());

			// A valid code redacts the profile
			// call under test
			userClient.redactOwnProfile(new RedactProfileRequest().setOtpCode(generateTotpCode(secret)));

			UserProfile redacted = adminClient.getUserProfile(userId.toString());
			assertEquals(userId.toString(), redacted.getUserName());
			assertEquals("gdpr-synapse+" + userId + "@sagebase.org", redacted.getEmail());
			assertEquals("", redacted.getFirstName());
			assertEquals("", redacted.getLastName());
			assertEquals(Collections.emptyList(), redacted.getOpenIds());
			assertNull(redacted.getCompany());
			assertNull(redacted.getDisplayName());
			assertFalse(redacted.getNotificationSettings().getSendEmailNotifications());

			// The content is not deleted and is still attributed to the account
			assertEquals(userId.toString(), adminClient.getEntityById(projectId).getCreatedBy());

			// The old credentials cannot be used anymore
			SynapseClient loginClient = new SynapseClientImpl();
			SynapseClientHelper.setEndpoints(loginClient);
			LoginRequest loginRequest = new LoginRequest().setUsername(username).setPassword(password);
			assertThrows(SynapseUnauthorizedException.class, () -> loginClient.login(loginRequest));
			loginRequest.setUsername(userId.toString());
			assertThrows(SynapseUnauthorizedException.class, () -> loginClient.login(loginRequest));
		} finally {
			try {
				adminClient.deleteUser(userId);
			} catch (SynapseException e) {
				// The user owns content, the principal cannot be deleted
			}
		}
	}

	@Test
	public void testRedactOwnProfileWithRecoveryCode(SynapseAdminClient adminClient) throws SynapseException, JSONObjectAdapterException {
		String username = UUID.randomUUID().toString();
		String password = "password" + UUID.randomUUID().toString();
		SynapseClient userClient = new SynapseClientImpl();
		Long userId = SynapseClientHelper.createUser(adminClient, userClient, username, password, true, false);

		try {
			enable2Fa(userClient);
			TwoFactorAuthRecoveryCodes recoveryCodes = userClient.generate2FaRecoveryCodes();

			// Recovery codes are not accepted, only the code generated by the authenticator application
			// call under test
			assertThrows(SynapseUnauthorizedException.class, () -> {
				userClient.redactOwnProfile(new RedactProfileRequest().setOtpCode(recoveryCodes.getCodes().get(0)));
			});

			// Nothing was modified
			assertEquals(username, adminClient.getUserProfile(userId.toString()).getUserName());
		} finally {
			try {
				adminClient.deleteUser(userId);
			} catch (SynapseException e) {
			}
		}
	}

	@Test
	public void testRedactOwnProfileWithoutTwoFa(SynapseAdminClient adminClient) throws SynapseException, JSONObjectAdapterException {
		String username = UUID.randomUUID().toString();
		String password = "password" + UUID.randomUUID().toString();
		SynapseClient userClient = new SynapseClientImpl();
		Long userId = SynapseClientHelper.createUser(adminClient, userClient, username, password, true, false);

		try {
			assertEquals(TwoFactorState.DISABLED, userClient.get2FaStatus().getStatus());

			// call under test
			SynapseUnauthorizedException ex = assertThrows(SynapseUnauthorizedException.class, () -> {
				userClient.redactOwnProfile(new RedactProfileRequest().setOtpCode("123456"));
			});

			assertEquals(ErrorResponseCode.TWO_FA_ENABLED_REQUIRED, ex.getErrorResponseCode());
			// Nothing was modified
			assertEquals(username, adminClient.getUserProfile(userId.toString()).getUserName());
		} finally {
			try {
				adminClient.deleteUser(userId);
			} catch (SynapseException e) {
			}
		}
	}

	@Test
	public void testRedactOwnProfileWithPersonalAccessToken(SynapseAdminClient adminClient) throws SynapseException, JSONObjectAdapterException {
		String password = "password" + UUID.randomUUID().toString();
		SynapseClient userClient = new SynapseClientImpl();
		Long userId = SynapseClientHelper.createUser(adminClient, userClient, UUID.randomUUID().toString(), password, true, false);

		try {
			String personalAccessToken = userClient.createPersonalAccessToken(new AccessTokenGenerationRequest()
				.setName("PAT")
				.setScope(Arrays.asList(OAuthScope.modify, OAuthScope.view)));

			SynapseClient patClient = new SynapseClientImpl();
			SynapseClientHelper.setEndpoints(patClient);
			patClient.setBearerAuthorizationToken(personalAccessToken);

			// call under test
			SynapseForbiddenException ex = assertThrows(SynapseForbiddenException.class, () -> {
				patClient.redactOwnProfile(new RedactProfileRequest().setOtpCode("123456"));
			});

			assertEquals("insufficient_scope. Request lacks scope(s) required by this service: authorize", ex.getMessage());
		} finally {
			try {
				adminClient.deleteUser(userId);
			} catch (SynapseException e) {
			}
		}
	}

	@Test
	public void testRedactOwnProfileWithoutAuthentication() {
		SynapseClient anonymousClient = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(anonymousClient);

		// call under test
		SynapseBadRequestException ex = assertThrows(SynapseBadRequestException.class, () -> {
			anonymousClient.redactOwnProfile(new RedactProfileRequest().setOtpCode("123456"));
		});

		assertTrue(ex.getMessage().contains("This profile cannot be redacted."));
	}

	@Test
	public void testRedactOwnProfileWithInvalidAccessToken() {
		SynapseClient invalidTokenClient = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(invalidTokenClient);
		invalidTokenClient.setBearerAuthorizationToken("invalid");

		// call under test
		assertThrows(SynapseUnauthorizedException.class, () -> {
			invalidTokenClient.redactOwnProfile(new RedactProfileRequest().setOtpCode("123456"));
		});
	}

	/**
	 * @return The base32 encoded secret that was enabled for the user
	 */
	private String enable2Fa(SynapseClient userClient) throws SynapseException, JSONObjectAdapterException {
		TotpSecret secret = userClient.init2Fa();
		userClient.enable2Fa(new TotpSecretActivationRequest()
			.setSecretId(secret.getSecretId())
			.setTotp(generateTotpCode(secret.getSecret())));
		return secret.getSecret();
	}

	private String generateTotpCode(String secret) {
		try {
			return totpGenerator.generate(secret, Math.floorDiv(timeProvider.getTime(), 30));
		} catch (TimeProviderException | CodeGenerationException e) {
			throw new RuntimeException(e);
		}
	}

}
