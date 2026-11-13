package org.sagebionetworks.repo.model.ar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.auth.IdentityProviderName;

public class UsersRequirementStatusTest {

	@Test
	public void testIsIdentityProviderRequirementMetWithNoRestriction() {
		UsersRequirementStatus status = new UsersRequirementStatus().withIdentityProviders(null);

		// call under test
		assertTrue(status.isIdentityProviderRequirementMet(null));
		assertTrue(status.isIdentityProviderRequirementMet(IdentityProviderName.ORCID));
	}

	@Test
	public void testIsIdentityProviderRequirementMetWithMatchingProvider() {
		UsersRequirementStatus status = new UsersRequirementStatus()
				.withIdentityProviders(List.of(IdentityProviderName.SYNAPSE, IdentityProviderName.NIH_RESEARCHER_AUTH_SERVICE));

		// call under test
		assertTrue(status.isIdentityProviderRequirementMet(IdentityProviderName.NIH_RESEARCHER_AUTH_SERVICE));
	}

	@Test
	public void testIsIdentityProviderRequirementMetWithOtherProvider() {
		UsersRequirementStatus status = new UsersRequirementStatus()
				.withIdentityProviders(List.of(IdentityProviderName.NIH_RESEARCHER_AUTH_SERVICE));

		// call under test
		assertFalse(status.isIdentityProviderRequirementMet(IdentityProviderName.ORCID));
	}

	@Test
	public void testIsIdentityProviderRequirementMetWithNoCallersProvider() {
		UsersRequirementStatus status = new UsersRequirementStatus()
				.withIdentityProviders(List.of(IdentityProviderName.NIH_RESEARCHER_AUTH_SERVICE));

		// call under test
		assertFalse(status.isIdentityProviderRequirementMet(null));
	}
}
