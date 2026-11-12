package org.sagebionetworks.repo.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.auth.IdentityProviderName;

public class IdentityProviderThreadLocalTest {

	private static final Long CALLER_ID = 123L;
	private static final Long OTHER_PRINCIPAL_ID = 456L;

	@AfterEach
	public void after() {
		IdentityProviderThreadLocal.clear();
	}

	@Test
	public void testGetWithNoBinding() {
		// call under test
		assertEquals(Optional.empty(), IdentityProviderThreadLocal.get(CALLER_ID));
	}

	@Test
	public void testGetWithBindingForPrincipal() {
		IdentityProviderThreadLocal.set(CALLER_ID, IdentityProviderName.ORCID);

		// call under test
		assertEquals(Optional.of(IdentityProviderName.ORCID), IdentityProviderThreadLocal.get(CALLER_ID));
	}

	@Test
	public void testGetWithBindingForOtherPrincipal() {
		IdentityProviderThreadLocal.set(CALLER_ID, IdentityProviderName.ORCID);

		// call under test
		assertEquals(Optional.empty(), IdentityProviderThreadLocal.get(OTHER_PRINCIPAL_ID));
	}

	@Test
	public void testGetWithNullPrincipalId() {
		IdentityProviderThreadLocal.set(CALLER_ID, IdentityProviderName.ORCID);

		// call under test
		assertEquals(Optional.empty(), IdentityProviderThreadLocal.get(null));
	}

	@Test
	public void testSetWithExistingBinding() {
		IdentityProviderThreadLocal.set(CALLER_ID, IdentityProviderName.ORCID);

		// call under test
		IdentityProviderThreadLocal.set(CALLER_ID, IdentityProviderName.SYNAPSE);

		assertEquals(Optional.of(IdentityProviderName.SYNAPSE), IdentityProviderThreadLocal.get(CALLER_ID));
	}

	@Test
	public void testClear() {
		IdentityProviderThreadLocal.set(CALLER_ID, IdentityProviderName.ORCID);

		// call under test
		IdentityProviderThreadLocal.clear();

		assertEquals(Optional.empty(), IdentityProviderThreadLocal.get(CALLER_ID));
	}

	@Test
	public void testGetWithBindingOnAnotherThread() throws Exception {
		IdentityProviderThreadLocal.set(CALLER_ID, IdentityProviderName.ORCID);

		// call under test
		Optional<IdentityProviderName> onOtherThread = CompletableFuture
				.supplyAsync(() -> IdentityProviderThreadLocal.get(CALLER_ID)).get();

		assertEquals(Optional.empty(), onOtherThread);
	}
}
