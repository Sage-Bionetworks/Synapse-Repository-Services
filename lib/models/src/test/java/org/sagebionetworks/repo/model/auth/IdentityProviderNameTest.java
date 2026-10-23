package org.sagebionetworks.repo.model.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;

public class IdentityProviderNameTest {

	/**
	 * Every {@link OAuthProvider} must be named by an {@link IdentityProviderName} constant of the same
	 * name, since a provider that authenticates a user is recorded by converting between the two. A
	 * provider added without its counterpart would otherwise fail only at runtime, when someone actually
	 * signed in with it.
	 * <p>
	 * The reverse does not hold: {@link IdentityProviderName} also names Synapse itself and providers
	 * that are not OAuth providers, so it is expected to hold values with no {@link OAuthProvider}.
	 */
	@ParameterizedTest
	@EnumSource(OAuthProvider.class)
	public void testValueOfWithEveryOAuthProvider(OAuthProvider provider) {
		// call under test
		IdentityProviderName identityProviderName = assertDoesNotThrow(
				() -> IdentityProviderName.valueOf(provider.toString()),
				() -> "IdentityProviderName has no constant '" + provider.name()
						+ "'. Add it to IdentityProviderName.json so a user authenticated by that provider can be named.");

		assertEquals(provider.name(), identityProviderName.name());
	}
}
